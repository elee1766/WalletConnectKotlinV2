package org.walletconnect.walletconnectv2.engine

import android.app.Application
import com.tinder.scarlet.WebSocket
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientsync.pairing.Pairing
import org.walletconnect.walletconnectv2.clientsync.pairing.SettledPairingSequence
import org.walletconnect.walletconnectv2.clientsync.pairing.after.PostSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.pairing.before.proposal.PairingProposedPermissions
import org.walletconnect.walletconnectv2.clientsync.session.Session
import org.walletconnect.walletconnectv2.clientsync.session.SettledSessionSequence
import org.walletconnect.walletconnectv2.clientsync.session.after.PostSettlementSession
import org.walletconnect.walletconnectv2.clientsync.session.after.params.Reason
import org.walletconnect.walletconnectv2.clientsync.session.before.PreSettlementSession
import org.walletconnect.walletconnectv2.clientsync.session.before.proposal.RelayProtocolOptions
import org.walletconnect.walletconnectv2.clientsync.session.before.success.SessionParticipant
import org.walletconnect.walletconnectv2.clientsync.session.before.success.SessionState
import org.walletconnect.walletconnectv2.common.*
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.codec.AuthenticatedEncryptionCodec
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.data.SharedKey
import org.walletconnect.walletconnectv2.crypto.managers.LazySodiumCryptoManager
import org.walletconnect.walletconnectv2.engine.model.EngineData
import org.walletconnect.walletconnectv2.engine.sequence.SequenceLifecycleEvent
import org.walletconnect.walletconnectv2.engine.serailising.JsonRpcSerialising
import org.walletconnect.walletconnectv2.engine.serailising.JsonRpcSerializer
import org.walletconnect.walletconnectv2.errors.NoSessionDeletePayloadException
import org.walletconnect.walletconnectv2.errors.NoSessionProposalException
import org.walletconnect.walletconnectv2.errors.NoSessionRequestPayloadException
import org.walletconnect.walletconnectv2.errors.exception
import org.walletconnect.walletconnectv2.exceptionHandler
import org.walletconnect.walletconnectv2.moshi
import org.walletconnect.walletconnectv2.relay.WakuRelayRepository
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_PAIRING_PAYLOAD
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_SESSION_DELETE
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_SESSION_PAYLOAD
import org.walletconnect.walletconnectv2.relay.data.model.Relay
import org.walletconnect.walletconnectv2.relay.data.model.Request
import org.walletconnect.walletconnectv2.scope
import org.walletconnect.walletconnectv2.util.generateId
import timber.log.Timber
import java.util.*

class EngineInteractor {
    //region provide with DI
    // TODO: add logic to check hostName for ws/wss scheme with and without ://
    private lateinit var relayRepository: WakuRelayRepository
    private val codec: AuthenticatedEncryptionCodec = AuthenticatedEncryptionCodec()
    private lateinit var crypto: CryptoManager
    private val jsonRpcSerializer: JsonRpcSerialising = JsonRpcSerializer(moshi)
    //endregion

    private var metaData: AppMetaData? = null
    private val _sequenceEvent: MutableStateFlow<SequenceLifecycleEvent> = MutableStateFlow(SequenceLifecycleEvent.Default)
    val sequenceEvent: StateFlow<SequenceLifecycleEvent> = _sequenceEvent

    fun initialize(engine: EngineFactory) {
        this.metaData = engine.metaData
        crypto = LazySodiumCryptoManager()
        relayRepository = WakuRelayRepository.initRemote(engine.toRelayInitParams())

        scope.launch(exceptionHandler) {
            relayRepository.eventsFlow
                .onEach { Timber.tag("WalletConnect connection event").d("$it") }
                .filterIsInstance<WebSocket.Event.OnConnectionFailed>()
                .collect { event -> throw event.throwable.exception }
        }

        scope.launch(exceptionHandler) {
            relayRepository.subscriptionRequest().collect { relayRequest ->
                val topic: Topic = relayRequest.subscriptionTopic
                val (sharedKey, selfPublic) = crypto.getKeyAgreement(topic)
                val encryptionPayload = relayRequest.encryptionPayload
                val message: String = codec.decrypt(encryptionPayload, sharedKey as SharedKey)
                jsonRpcSerializer.tryDeserialize(Request::class.java, message)?.let { request ->
                    when (val rpc = request.method) {
                        WC_PAIRING_PAYLOAD -> onPairingPayload(message, sharedKey, selfPublic as PublicKey)
                        WC_SESSION_PAYLOAD -> onSessionPayload(message, topic)
                        WC_SESSION_DELETE -> onSessionDelete(message, topic)
                        else -> onUnsupported(rpc)
                    }
                } ?: jsonRpcSerializer.tryDeserialize(Relay.Subscription.JsonRpcError::class.java, message)?.let { exception ->
                    Timber.tag("WalletConnect exception").e(exception.error.errorMessage)
                }
            }
        }
    }

    fun pair(uri: String, onResult: (Result<Any>) -> Unit) {
        require(::relayRepository.isInitialized)

        val pairingProposal: Pairing.Proposal = uri.toPairProposal()
        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val expiry = Expiry((Calendar.getInstance().timeInMillis / 1000) + pairingProposal.ttl.seconds)
        val peerPublicKey = PublicKey(pairingProposal.pairingProposer.publicKey)
        val controllerPublicKey = if (pairingProposal.pairingProposer.controller) {
            peerPublicKey
        } else {
            selfPublicKey
        }
        val settledSequence = settlePairingSequence(
            pairingProposal.relay,
            selfPublicKey,
            peerPublicKey,
            pairingProposal.permissions,
            controllerPublicKey,
            expiry
        )
        val preSettlementPairingApprove = pairingProposal.toApprove(generateId(), settledSequence.settledTopic, expiry, selfPublicKey)
        observePublishAcknowledgement(onResult, settledSequence.settledTopic.topicValue)
        observePublishError(onResult)
        relayRepository.eventsFlow
            .onEach {
                supervisorScope {
                    relayRepository.publishPairingApproval(pairingProposal.topic, preSettlementPairingApprove)
                    relayRepository.subscribe(settledSequence.settledTopic)
                    cancel()
                }
            }
            .launchIn(scope)
    }

    fun approve(proposal: EngineData.SessionProposal, accounts: List<String>, onResult: (Result<Any>) -> Unit) {
        require(::relayRepository.isInitialized)

        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val peerPublicKey = PublicKey(proposal.proposerPublicKey)
        val sessionState = SessionState(accounts)
        val expiry = Expiry((Calendar.getInstance().timeInMillis / 1000) + proposal.ttl)
        val settledSession: SettledSessionSequence =
            settleSessionSequence(RelayProtocolOptions(), selfPublicKey, peerPublicKey, expiry, sessionState)

        val sessionApprove = PreSettlementSession.Approve(
            id = generateId(),
            params = Session.Success(
                relay = RelayProtocolOptions(),
                state = settledSession.state,
                expiry = expiry,
                responder = SessionParticipant(
                    selfPublicKey.keyAsHex,
                    metadata = this.metaData
                )
            )
        )

        val approvalJson: String = jsonRpcSerializer.trySerialize(PreSettlementSession.Approve::class.java, sessionApprove)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(proposal.topic))
        val encryptedMessage: String = codec.encrypt(approvalJson, sharedKey as SharedKey, selfPublic as PublicKey)
        with(proposal) {
            observePublishAcknowledgement(onResult, EngineData.SettledSession(icon, name, url, settledSession.topic.topicValue))
        }
        observePublishError(onResult)
        relayRepository.subscribe(settledSession.topic)
        relayRepository.publish(Topic(proposal.topic), encryptedMessage)
    }

    fun reject(reason: String, topic: String, onResult: (Result<Any>) -> Unit) {
        require(::relayRepository.isInitialized)
        val sessionReject = PreSettlementSession.Reject(id = generateId(), params = Session.Failure(reason = reason))
        val json: String = jsonRpcSerializer.trySerialize(PreSettlementSession.Reject::class.java, sessionReject)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(topic))
        val encryptedMessage: String = codec.encrypt(json, sharedKey as SharedKey, selfPublic as PublicKey)
        observePublishAcknowledgement(onResult, topic)
        observePublishError(onResult)
        relayRepository.publish(Topic(topic), encryptedMessage)
    }

    fun disconnect(topic: String, reason: String, onResult: (Result<Any>) -> Unit) {
        require(::relayRepository.isInitialized)
        val sessionDelete = PostSettlementSession.SessionDelete(id = generateId(), params = Session.DeleteParams(Reason(message = reason)))
        val json = jsonRpcSerializer.trySerialize(PostSettlementSession.SessionDelete::class.java, sessionDelete)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(topic))
        val encryptedMessage: String = codec.encrypt(json, sharedKey as SharedKey, selfPublic as PublicKey)
        observePublishAcknowledgement(onResult, topic)
        observePublishError(onResult)
        //TODO Add subscriptionId from local storage + Delete all data from local storage coupled with given session
        crypto.removeKeys(topic)
        relayRepository.unsubscribe(Topic(topic), SubscriptionId("1"))
        relayRepository.publish(Topic(topic), encryptedMessage)
    }

    private fun observePublishError(onResult: (Result<String>) -> Unit) {
        scope.launch {
            relayRepository.observePublishResponseError
                .catch { exception -> Timber.tag("WalletConnect exception").e(exception) }
                .collect { errorResponse ->
                    onResult(Result.failure(Throwable(errorResponse.error.errorMessage)))
                    cancel()
                }
        }
    }

    private fun observePublishAcknowledgement(onResult: (Result<Any>) -> Unit, result: Any) {
        scope.launch {
            relayRepository.observePublishAcknowledgement
                .catch { exception -> Timber.tag("WalletConnect exception").e(exception) }
                .collect {
                    onResult(Result.success(result))
                    cancel()
                }
        }
    }

    private fun onPairingPayload(json: String, sharedKey: SharedKey, selfPublic: PublicKey) {
        jsonRpcSerializer.tryDeserialize(PostSettlementPairing.PairingPayload::class.java, json)?.let { pairingPayload ->
            val proposal = pairingPayload.payloadParams
            //TODO validate session proposal
            crypto.setEncryptionKeys(sharedKey, selfPublic, proposal.topic)
            val sessionProposal = proposal.toSessionProposal()
            _sequenceEvent.value = SequenceLifecycleEvent.OnSessionProposal(sessionProposal)
        } ?: throw NoSessionProposalException()
    }

    private fun onSessionPayload(json: String, topic: Topic) {
        jsonRpcSerializer.tryDeserialize(PostSettlementSession.SessionPayload::class.java, json)?.let { sessionPayload ->
            val request = sessionPayload.sessionParams
            val chainId = sessionPayload.params.chainId
            val method = sessionPayload.params.request.method
            //TODO Validate session request + add unmarshaling of generic session request payload to the usable generic object
            _sequenceEvent.value =
                SequenceLifecycleEvent.OnSessionRequest(EngineData.SessionRequest(topic.topicValue, request, chainId, method))
        } ?: throw NoSessionRequestPayloadException()
    }

    private fun onSessionDelete(json: String, topic: Topic) {
        jsonRpcSerializer.tryDeserialize(PostSettlementSession.SessionDelete::class.java, json)?.let { sessionDelete ->
            //TODO Add subscriptionId from local storage + Delete all data from local storage coupled with given session
            crypto.removeKeys(topic.topicValue)
            relayRepository.unsubscribe(topic, SubscriptionId("1"))
            val reason = sessionDelete.message
            _sequenceEvent.value = SequenceLifecycleEvent.OnSessionDeleted(topic.topicValue, reason)
        } ?: throw NoSessionDeletePayloadException()
    }


    private fun onUnsupported(rpc: String?) {
        Timber.tag("WalletConnect unsupported RPC").e(rpc)
    }

    private fun settlePairingSequence(
        relay: JSONObject,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        permissions: PairingProposedPermissions?,
        controllerPublicKey: PublicKey,
        expiry: Expiry
    ): SettledPairingSequence {
        //Setting keys on topic B
        val (_, settledTopic) = crypto.generateTopicAndSharedKey(selfPublicKey, peerPublicKey)
        return SettledPairingSequence(
            settledTopic,
            relay,
            selfPublicKey,
            peerPublicKey,
            permissions to controllerPublicKey,
            expiry
        )
    }

    private fun settleSessionSequence(
        relay: RelayProtocolOptions,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        expiry: Expiry,
        sessionState: SessionState
    ): SettledSessionSequence {
        val (sharedKey, topic) = crypto.generateTopicAndSharedKey(selfPublicKey, peerPublicKey)
        return SettledSessionSequence(
            topic,
            relay,
            selfPublicKey,
            peerPublicKey,
            sharedKey,
            expiry,
            sessionState
        )
    }

    data class EngineFactory(
        val useTLs: Boolean = false,
        val hostName: String,
        val apiKey: String,
        val isController: Boolean,
        val application: Application,
        val metaData: AppMetaData
    )
}