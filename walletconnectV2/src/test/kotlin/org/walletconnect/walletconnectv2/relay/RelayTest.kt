package org.walletconnect.walletconnectv2.relay;

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.Message
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter.Factory
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.ValueAssert
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.toApprove
import org.walletconnect.walletconnectv2.common.toPairProposal
import org.walletconnect.walletconnectv2.common.toRelayPublishRequest
import org.walletconnect.walletconnectv2.getRandom64ByteString
import org.walletconnect.walletconnectv2.outofband.client.ClientTypes
import org.walletconnect.walletconnectv2.relay.data.RelayService
import org.walletconnect.walletconnectv2.relay.data.model.Relay
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class RelayTest {

    @Rule
    val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private lateinit var server: MockServerService
    private lateinit var serverEventObserver: TestStreamObserver<WebSocket.Event>

    private lateinit var client: RelayService
    private lateinit var clientEventObserver: TestStreamObserver<WebSocket.Event>

    @BeforeEach
    fun setUp() {
        givenConnectionIsEstablished()
    }

    @Nested
    inner class Publish {

        @Test
        fun sendRelayPublishRequest_shouldBeReceivedByTheServer() {
            // Given
            val pairingParams = ClientTypes.PairParams("wc:0b1a3d6c0336662dddb6278ee0aa25380569b79e7e86cfe39fb20b4b189096a0@2?controller=false&publicKey=66db1bd5fad65392d1d5a4856d0d549d2fca9194327138b41c289b961d147860&relay=%7B%22protocol%22%3A%22waku%22%7D")
            val pairingProposal = pairingParams.uri.toPairProposal()
            val preSettlementPairingApprove = pairingProposal.toApprove(Random.nextInt())
            val relayPublishRequest = preSettlementPairingApprove.toRelayPublishRequest(Random.nextInt(), Topic(getRandom64ByteString()), createMoshi())
            val serverRelayPublishObserver = server.observeRelayPublish().test()

            // When
            client.publishRequest(relayPublishRequest)

            // Then
            serverEventObserver.awaitValues(
                any<WebSocket.Event.OnConnectionOpened<*>>(),
                any<WebSocket.Event.OnMessageReceived>().containingRelayObject(relayPublishRequest)
            )
            serverRelayPublishObserver.awaitValues(
                any<Relay.Publish.Request> { assertThat(this).isEqualTo(relayPublishRequest) }
            )
        }
    }

    @Nested
    inner class Subscribe {

        @Test
        fun sendRelaySubscribeRequest_shouldBeReceivedByTheServer() {
            // Given
            val relaySubscribeRequest = Relay.Subscribe.Request(
                id = 1,
                params = Relay.Subscribe.Request.Params(Topic(getRandom64ByteString()))
            )
            val serverRelayPublishObserver = server.observeSubscribePublish().test()

            // When
            client.subscribeRequest(relaySubscribeRequest)

            // Then
            serverEventObserver.awaitValues(
                any<WebSocket.Event.OnConnectionOpened<*>>(),
                any<WebSocket.Event.OnMessageReceived>().containingRelayObject(relaySubscribeRequest)
            )
            serverRelayPublishObserver.awaitValues(
                any<Relay.Subscribe.Request> { assertThat(this).isEqualTo(relaySubscribeRequest) }
            )
        }
    }

    private fun givenConnectionIsEstablished(config: Factory.Config = Factory.Config()) {
        createClientAndServer(config)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer(config: Factory.Config) {
        val moshi = createMoshi()
        val factory = Factory(moshi, config)
        server = createServer(factory)
        serverEventObserver = server.observeEvents().test()
        client = createClient(factory)
        clientEventObserver = client.observeEvents().test()
    }

    private fun createMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createServer(factory: Factory): MockServerService = Scarlet.Builder()
        .webSocketFactory(mockWebServer.newWebSocketFactory())
        .addMessageAdapterFactory(factory)
        .build()
        .create()

    private fun createClient(factory: Factory): RelayService = Scarlet.Builder()
        .webSocketFactory(createOkHttpClient().newWebSocketFactory(serverUrlString))
        .addMessageAdapterFactory(factory)
        .build().create()

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private fun blockUntilConnectionIsEstablish() {
        serverEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
        clientEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
    }

    internal interface MockServerService {

        @Receive
        fun observeEvents(): Stream<WebSocket.Event>

        @Receive
        fun observeRelayPublish(): Stream<Relay.Publish.Request>

        @Receive
        fun observeSubscribePublish(): Stream<Relay.Subscribe.Request>
    }

    private inline fun <reified T: Relay> ValueAssert<WebSocket.Event.OnMessageReceived>.containingRelayObject(relayObj: T) = assert {
        assertIs<Message.Text>(message)
        val text = message as Message.Text
        val expectedText = Message.Text(createMoshi().adapter(T::class.java).toJson(relayObj))
        assertEquals(expectedText, text)
    }
}