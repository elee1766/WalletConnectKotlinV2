package org.walletconnect.walletconnectv2.relay

import android.app.Application
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.adapter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.supervisorScope
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientsync.pairing.after.PostSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.pairing.before.PreSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.session.after.PostSettlementSession
import org.walletconnect.walletconnectv2.clientsync.session.before.PreSettlementSession
import org.walletconnect.walletconnectv2.common.*
import org.walletconnect.walletconnectv2.errors.UnExpectedError
import org.walletconnect.walletconnectv2.moshi
import org.walletconnect.walletconnectv2.relay.data.RelayService
import org.walletconnect.walletconnectv2.relay.data.init.RelayInitParams
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcError
import org.walletconnect.walletconnectv2.relay.data.model.Relay
import org.walletconnect.walletconnectv2.relay.data.model.Request
import org.walletconnect.walletconnectv2.scope
import org.walletconnect.walletconnectv2.util.adapters.FlowStreamAdapter
import org.walletconnect.walletconnectv2.util.generateId
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class WakuRelayRepository internal constructor(
    private val useTLs: Boolean,
    private val hostName: String,
    private val apiKey: String,
    private val application: Application
) {
    //region Move to DI module
    private val okHttpClient = OkHttpClient.Builder()
        .writeTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
        .callTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
        .connectTimeout(TIMEOUT_TIME, TimeUnit.MILLISECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    private val scarlet by lazy {
        Scarlet.Builder()
            .backoffStrategy(LinearBackoffStrategy(TimeUnit.MINUTES.toMillis(DEFAULT_BACKOFF_MINUTES)))
            .webSocketFactory(okHttpClient.newWebSocketFactory(getServerUrl()))
            .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory(moshi))
            .addStreamAdapterFactory(FlowStreamAdapter.Factory())
            .build()
    }
    private val relay: RelayService by lazy { scarlet.create(RelayService::class.java) }
    //endregion

    internal val eventsFlow = relay.eventsFlow().shareIn(scope, SharingStarted.Lazily, REPLAY)

    internal val observePublishResponse: Flow<Result<Relay.Publish.Response>> =
        relay.observePublishResponse()
            .map { message ->

                Timber.tag("kobe").d("RESULT: $message")


                tryDeserialize(Relay.Publish.Response::class.java, message.toString())?.let { response ->
                    Result.success(response)
                } ?: tryDeserialize(JsonRpcError::class.java, message.toString())?.let { exception ->
                    Result.failure(Throwable(exception.error.errorMessage))
                } ?: Result.failure(UnExpectedError())
            }
    internal val observeSubscribeResponse = relay.observeSubscribeResponse()
    internal val observeUnsubscribeResponse = relay.observeUnsubscribeResponse()

    internal fun subscriptionRequest(): Flow<Relay.Subscription.Request> =
        relay.observeSubscriptionRequest()
            .map { relayRequest ->
                supervisorScope { publishSubscriptionResponse(relayRequest.id) }
                relayRequest
            }

    fun publishPairingApproval(topic: Topic, preSettlementPairingApproval: PreSettlementPairing.Approve) {
        val publishRequest = preSettlementPairingApproval.toRelayPublishRequest(generateId(), topic, moshi)
        relay.publishRequest(publishRequest)
    }

    fun publish(topic: Topic, message: String) {
        val publishRequest =
            Relay.Publish.Request(id = generateId(), params = Relay.Publish.Request.Params(topic = topic, message = message))
        relay.publishRequest(publishRequest)
    }

    fun subscribe(topic: Topic) {
        val subscribeRequest = Relay.Subscribe.Request(id = generateId(), params = Relay.Subscribe.Request.Params(topic))
        relay.subscribeRequest(subscribeRequest)
    }

    fun unsubscribe(topic: Topic, subscriptionId: SubscriptionId) {
        val unsubscribeRequest =
            Relay.Unsubscribe.Request(id = generateId(), params = Relay.Unsubscribe.Request.Params(topic, subscriptionId))
        relay.unsubscribeRequest(unsubscribeRequest)
    }

    fun <T> trySerialize(typeClass: Class<T>, type: T): String = moshi.adapter(typeClass).toJson(type)

    fun <T> tryDeserialize(type: Class<T>, json: String): T? =
        try {
            moshi.adapter(type).fromJson(json)
        } catch (error: JsonDataException) {
            null
        }

    private fun publishSubscriptionResponse(id: Long) {
        val publishRequest = Relay.Subscription.Response(id = id, result = true)
        relay.publishSubscriptionResponse(publishRequest)
    }

    private fun getServerUrl(): String =
        ((if (useTLs) "wss" else "ws") + "://$hostName/?apiKey=$apiKey").trim()

    companion object {
        private const val TIMEOUT_TIME: Long = 5000L
        private const val DEFAULT_BACKOFF_MINUTES: Long = 5L
        private const val REPLAY: Int = 1

        fun initRemote(relayInitParams: RelayInitParams) = with(relayInitParams) {
            WakuRelayRepository(useTls, hostName, apiKey, application)
        }
    }
}