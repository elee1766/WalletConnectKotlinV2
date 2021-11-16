package org.walletconnect.walletconnectv2.relay

import android.app.Application
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientsync.pairing.after.PostSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.pairing.before.PreSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.session.after.PostSettlementSession
import org.walletconnect.walletconnectv2.clientsync.session.before.PreSettlementSession
import org.walletconnect.walletconnectv2.common.*
import org.walletconnect.walletconnectv2.common.network.adapters.*
import org.walletconnect.walletconnectv2.relay.data.RelayService
import org.walletconnect.walletconnectv2.relay.data.init.RelayInitParams
import org.walletconnect.walletconnectv2.relay.data.model.Relay
import org.walletconnect.walletconnectv2.relay.data.model.Request
import org.walletconnect.walletconnectv2.util.adapters.FlowStreamAdapter
import org.walletconnect.walletconnectv2.util.generateId
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

    private val moshi: Moshi = Moshi.Builder()
        .addLast { type, _, _ ->
            when (type.getRawType().name) {
                Expiry::class.qualifiedName -> ExpiryAdapter
                JSONObject::class.qualifiedName -> JSONObjectAdapter
                SubscriptionId::class.qualifiedName -> SubscriptionIdAdapter
                Topic::class.qualifiedName -> TopicAdapter
                Ttl::class.qualifiedName -> TtlAdapter
                else -> null
            }
        }
        .addLast(KotlinJsonAdapterFactory())
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

    internal val eventsFlow = relay.eventsFlow()
    internal val publishAcknowledgement = relay.observePublishAcknowledgement()
    internal val subscribeAcknowledgement = relay.observeSubscribeAcknowledgement()
    internal val unsubscribeAcknowledgement = relay.observeUnsubscribeAcknowledgement()

    internal fun subscriptionRequest(): Flow<Relay.Subscription.Request> =
        relay.observeSubscriptionRequest()
            .map { relayRequest ->
                supervisorScope { publishSubscriptionAcknowledgment(relayRequest.id) }
                relayRequest
            }

    fun publishPairingApproval(
        topic: Topic,
        preSettlementPairingApproval: PreSettlementPairing.Approve
    ) {
        val publishRequest =
            preSettlementPairingApproval.toRelayPublishRequest(generateId(), topic, moshi)
        relay.publishRequest(publishRequest)
    }

    fun publish(topic: Topic, message: String) {
        val publishRequest =
            Relay.Publish.Request(
                id = generateId(),
                params = Relay.Publish.Request.Params(topic = topic, message = message)
            )
        relay.publishRequest(publishRequest)
    }

    fun subscribe(topic: Topic) {
        val subscribeRequest =
            Relay.Subscribe.Request(
                id = generateId(),
                params = Relay.Subscribe.Request.Params(topic)
            )
        relay.subscribeRequest(subscribeRequest)
    }

    fun unsubscribe(topic: Topic, subscriptionId: SubscriptionId) {
        val unsubscribeRequest =
            Relay.Unsubscribe.Request(
                id = generateId(),
                params = Relay.Unsubscribe.Request.Params(topic, subscriptionId)
            )
        relay.unsubscribeRequest(unsubscribeRequest)
    }

    fun getSessionApprovalJson(approveSession: PreSettlementSession.Approve): String =
        moshi.adapter(PreSettlementSession.Approve::class.java).toJson(approveSession)

    fun getSessionRejectionJson(rejectSession: PreSettlementSession.Reject): String =
        moshi.adapter(PreSettlementSession.Reject::class.java).toJson(rejectSession)

    fun getSessionDeleteJson(sessionDelete: PostSettlementSession.SessionDelete): String =
        moshi.adapter(PostSettlementSession.SessionDelete::class.java).toJson(sessionDelete)

    fun parseToPairingPayload(json: String): PostSettlementPairing.PairingPayload? =
        moshi.adapter(PostSettlementPairing.PairingPayload::class.java).fromJson(json)

    fun parseToSessionPayload(json: String): PostSettlementSession.SessionPayload? =
        moshi.adapter(PostSettlementSession.SessionPayload::class.java).fromJson(json)

    fun parseToSessionDelete(json: String): PostSettlementSession.SessionDelete? =
        moshi.adapter(PostSettlementSession.SessionDelete::class.java).fromJson(json)

    fun parseToParamsRequest(json: String): Request? =
        moshi.adapter(Request::class.java).fromJson(json)

    private fun publishSubscriptionAcknowledgment(id: Long) {
        val publishRequest = Relay.Subscription.Acknowledgement(id = id, result = true)
        relay.publishSubscriptionAcknowledgment(publishRequest)
    }

    private fun getServerUrl(): String =
        ((if (useTLs) "wss" else "ws") + "://$hostName/?apiKey=$apiKey").trim()

    companion object {
        private const val TIMEOUT_TIME = 5000L
        private const val DEFAULT_BACKOFF_MINUTES = 5L

        fun initRemote(relayInitParams: RelayInitParams) = with(relayInitParams) {
            WakuRelayRepository(useTls, hostName, apiKey, application)
        }
    }
}