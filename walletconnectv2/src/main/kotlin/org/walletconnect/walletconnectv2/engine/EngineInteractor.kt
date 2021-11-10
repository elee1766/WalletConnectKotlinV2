package org.walletconnect.walletconnectv2.engine

import android.app.Application
import com.tinder.scarlet.WebSocket
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject
import org.walletconnect.walletconnectv2.client.SessionProposal
import org.walletconnect.walletconnectv2.client.SessionRequest
import org.walletconnect.walletconnectv2.client.SettledSession
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
import org.walletconnect.walletconnectv2.crypto.managers.LazySodiumCryptoManager
import org.walletconnect.walletconnectv2.engine.sequence.*
import org.walletconnect.walletconnectv2.errors.NoSessionDeletePayloadException
import org.walletconnect.walletconnectv2.errors.NoSessionProposalException
import org.walletconnect.walletconnectv2.errors.NoSessionRequestPayloadException
import org.walletconnect.walletconnectv2.errors.exception
import org.walletconnect.walletconnectv2.exceptionHandler
import org.walletconnect.walletconnectv2.keyChain
import org.walletconnect.walletconnectv2.relay.WakuRelayRepository
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcError
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_PAIRING_PAYLOAD
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_SESSION_DELETE
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.JsonRpcMethod.WC_SESSION_PAYLOAD
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
    private val crypto: CryptoManager = LazySodiumCryptoManager(keyChain)
    //endregion

    private var metaData: AppMetaData? = null
    private val _sequenceEvent: MutableStateFlow<SequenceLifecycleEvent> = MutableStateFlow(Default)
    val sequenceEvent: StateFlow<SequenceLifecycleEvent> = _sequenceEvent

    fun initialize(engine: EngineFactory) {
        this.metaData = engine.metaData
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
                val message: String = codec.decrypt(encryptionPayload, sharedKey)

                relayRepository.tryDeserialize(Request::class.java, message)?.let { request ->
                    when (val rpc = request.method) {
                        WC_PAIRING_PAYLOAD -> onPairingPayload(message, sharedKey, selfPublic)
                        WC_SESSION_PAYLOAD -> onSessionPayload(message, topic)
                        WC_SESSION_DELETE -> onSessionDelete(message, topic)
                        else -> onUnsupported(rpc)
                    }
                } ?: relayRepository.tryDeserialize(JsonRpcError::class.java, message)?.let { exception ->
                    Timber.tag("WalletConnect exception").e(exception.error.errorMessage)
                }
            }
        }
    }

    fun pair(uri: String, onResult: (Result<String>) -> Unit) {
        require(::relayRepository.isInitialized)
        val pairingProposal = uri.toPairProposal()
        val selfPublicKey = crypto.generateKeyPair()
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

        scope.launch {
            relayRepository.observePublishResponse
                .collect { result ->

                    Timber.tag("kobe").d("RESULT: $result")

                    result.fold(
                        onSuccess = { onResult(Result.success(settledSequence.settledTopic.topicValue)) },
                        onFailure = { error -> onResult(Result.failure(error)) }
                    )
                    cancel()
                }
        }

        relayRepository.eventsFlow
            .filterIsInstance<WebSocket.Event.OnConnectionOpened<*>>()
            .onEach {
                supervisorScope {
                    relayRepository.publishPairingApproval(pairingProposal.topic, preSettlementPairingApprove)
                    relayRepository.subscribe(settledSequence.settledTopic)
                    cancel()
                }
            }
            .launchIn(scope)
    }

    fun approve(proposal: SessionProposal, accounts: List<String>) {
        require(::relayRepository.isInitialized)
        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val peerPublicKey = PublicKey(proposal.proposerPublicKey)
        val sessionState = SessionState(accounts)
        val expiry = Expiry((Calendar.getInstance().timeInMillis / 1000) + proposal.ttl)
        val settledSession: SettledSessionSequence = settleSessionSequence(
            RelayProtocolOptions(),
            selfPublicKey,
            peerPublicKey,
            expiry,
            sessionState
        )

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

        val approvalJson: String = relayRepository.trySerialize(PreSettlementSession.Approve::class.java, sessionApprove)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(proposal.topic))
        val encryptedMessage: String = codec.encrypt(approvalJson, sharedKey, selfPublic)
        relayRepository.subscribe(settledSession.topic)

        //todo success and error
        relayRepository.publish(Topic(proposal.topic), encryptedMessage)

        with(proposal) {
            _sequenceEvent.value = OnSessionSettled(SettledSession(icon, name, url, settledSession.topic.topicValue))
        }
    }

    fun reject(reason: String, topic: String) {
        require(::relayRepository.isInitialized)
        val sessionReject = PreSettlementSession.Reject(id = generateId(), params = Session.Failure(reason = reason))
        val json: String = relayRepository.trySerialize(PreSettlementSession.Reject::class.java, sessionReject)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(topic))
        val encryptedMessage: String = codec.encrypt(json, sharedKey, selfPublic)

        //todo success and error
        relayRepository.publish(Topic(topic), encryptedMessage)
    }

    fun disconnect(topic: String, reason: String) {
        require(::relayRepository.isInitialized)
        val sessionDelete = PostSettlementSession.SessionDelete(id = generateId(), params = Session.DeleteParams(Reason(message = reason)))
        val json = relayRepository.trySerialize(PostSettlementSession.SessionDelete::class.java, sessionDelete)
        val (sharedKey, selfPublic) = crypto.getKeyAgreement(Topic(topic))
        val encryptedMessage: String = codec.encrypt(json, sharedKey, selfPublic)
        //TODO Add subscriptionId from local storage + Delete all data from local storage coupled with given session
        relayRepository.unsubscribe(Topic(topic), SubscriptionId("1"))

        //todo success and error
        relayRepository.publish(Topic(topic), encryptedMessage)
    }

    private fun onPairingPayload(json: String, sharedKey: String, selfPublic: PublicKey) {
        relayRepository.tryDeserialize(PostSettlementPairing.PairingPayload::class.java, json)?.let { pairingPayload ->
            val proposal = pairingPayload.payloadParams
            //TODO validate session proposal
            crypto.setEncryptionKeys(sharedKey, selfPublic, proposal.topic)
            val sessionProposal = proposal.toSessionProposal()
            _sequenceEvent.value = OnSessionProposal(sessionProposal)
        } ?: throw NoSessionProposalException()

    }

    private fun onSessionPayload(json: String, topic: Topic) {
        relayRepository.tryDeserialize(PostSettlementSession.SessionPayload::class.java, json)?.let { sessionPayload ->
            val request = sessionPayload.sessionParams
            val chainId = sessionPayload.params.chainId
            val method = sessionPayload.params.request.method
            //TODO Validate session request + add unmarshaling of generic session request payload to the usable generic object
            _sequenceEvent.value = OnSessionRequest(SessionRequest(topic.topicValue, request, chainId, method))
        } ?: throw NoSessionRequestPayloadException()
    }

    private fun onSessionDelete(json: String, topic: Topic) {
        //TODO Delete all data from local storage coupled with given session
        //TODO unsubscribe
        relayRepository.tryDeserialize(PostSettlementSession.SessionDelete::class.java, json)?.let { sessionDelete ->
            val reason = sessionDelete.message
            _sequenceEvent.value = OnSessionDeleted(topic.topicValue, reason)
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