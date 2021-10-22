package org.walletconnect.walletconnectv2.engine

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.clientcomm.PreSettlementSession
import org.walletconnect.walletconnectv2.clientcomm.pairing.PairingPayload
import org.walletconnect.walletconnectv2.clientcomm.pairing.SettledPairingSequence
import org.walletconnect.walletconnectv2.clientcomm.pairing.proposal.PairingProposedPermissions
import org.walletconnect.walletconnectv2.clientcomm.session.RelayProtocolOptions
import org.walletconnect.walletconnectv2.clientcomm.session.Session
import org.walletconnect.walletconnectv2.clientcomm.session.SettledSessionSequence
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.clientcomm.session.success.SessionState
import org.walletconnect.walletconnectv2.common.*
import org.walletconnect.walletconnectv2.common.network.adapters.*
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.codec.AuthenticatedEncryptionCodec
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.managers.LazySodiumCryptoManager
import org.walletconnect.walletconnectv2.relay.WakuRelayRepository
import org.walletconnect.walletconnectv2.util.generateId
import java.util.*

class EngineInteractor {
    //region provide with DI
    // TODO: add logic to check hostName for ws/wss scheme with and without ://
    private lateinit var relayRepository: WakuRelayRepository
    private val keyChain = object : KeyChain {
        val mapOfKeys = mutableMapOf<String, String>()

        override fun setKey(key: String, value: String) {
            mapOfKeys[key] = value
        }

        override fun getKey(key: String): String {
            return mapOfKeys[key]!!
        }
    }
    private val crypto: CryptoManager = LazySodiumCryptoManager(keyChain)
    private val codec: AuthenticatedEncryptionCodec = AuthenticatedEncryptionCodec()
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
    //endregion

    private val _sessionProposal: MutableStateFlow<Session.Proposal?> = MutableStateFlow(null)
    val sessionProposal: StateFlow<Session.Proposal?> = _sessionProposal

    private var pairingPublicKey = PublicKey("")
    private var peerPublicKey = PublicKey("")

    fun initialize(engineFactory: EngineFactory) {
        relayRepository = WakuRelayRepository.initRemote(engineFactory.useTLs, engineFactory.hostName, engineFactory.port)

        WalletConnectClient.scope.launch {
            relayRepository.subscriptionRequest.collect {
                val pairingPayloadJson = codec.decrypt(it.params.subscriptionData.message.toEncryptionPayload(), crypto.getSharedKey(pairingPublicKey, peerPublicKey))
                val pairingPayload = moshi.adapter(PairingPayload::class.java).fromJson(pairingPayloadJson)
                val sessionProposal = pairingPayload?.params?.request?.params.also { session -> Log.e("TalhaEngine", session.toString()) } // Log if null
                _sessionProposal.value = sessionProposal
            }
        }
    }

    fun pair(uri: String) {
        require(::relayRepository.isInitialized)

        val pairingProposal = uri.toPairProposal()
        val selfPublicKey = crypto.generateKeyPair().also { pairingPublicKey = it }
        val expiry = Expiry((Calendar.getInstance().timeInMillis / 1000) + pairingProposal.ttl.seconds)
        val peerPublicKey = PublicKey(pairingProposal.pairingProposer.publicKey).also { peerPublicKey = it }
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
        val preSettlementPairingApprove =
            pairingProposal.toApprove(
                generateId(),
                settledSequence.settledTopic,
                expiry,
                selfPublicKey
            )

        relayRepository.eventsStream.start(object : Stream.Observer<WebSocket.Event> {
            override fun onComplete() {}

            override fun onError(throwable: Throwable) {}

            override fun onNext(data: WebSocket.Event) {
                if (data is WebSocket.Event.OnConnectionOpened<*>) {
                    relayRepository.subscribe(settledSequence.settledTopic)
                    relayRepository.publishPairingApproval(
                        pairingProposal.topic,
                        preSettlementPairingApprove
                    )
                }
            }
        })
    }

    private fun settlePairingSequence(
        relay: JSONObject,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        permissions: PairingProposedPermissions?,
        controllerPublicKey: PublicKey,
        expiry: Expiry
    ): SettledPairingSequence {
        require(::relayRepository.isInitialized)

        val settledTopic: Topic = crypto.generateSharedKey(selfPublicKey, peerPublicKey)

        return SettledPairingSequence(
            settledTopic,
            relay,
            selfPublicKey,
            peerPublicKey,
            permissions to controllerPublicKey,
            expiry
        )
    }

    fun approve(proposal: Session.Proposal) {
        require(::relayRepository.isInitialized)

        println("Kobe Session Proposal: $proposal\n")

        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val peerPublicKey = PublicKey(proposal.proposer.publicKey)
        val sessionState = SessionState(listOf("eip155:137:0x022c0c42a80bd19EA4cF0F94c4F9F96645759716")) //pass proper keys from wallet
        val expiry = Expiry((Calendar.getInstance().timeInMillis / 1000) + proposal.ttl.seconds)

        val settledSession: SettledSessionSequence = settleSessionSequence(
            proposal.relay,
            selfPublicKey,
            peerPublicKey,
            proposal.permissions,
            expiry,
            sessionState
        )

        val preSettlementSession: PreSettlementSession.Approve = proposal.toApprove(
            generateId(),
            expiry,
            selfPublicKey,
            settledSession.state
        )

        val sessionApprovalJson: String =
            relayRepository.getSessionApprovalJson(preSettlementSession)

        //SESSION APPROVAL JSON
        println("Kobe Session Approval Json: $sessionApprovalJson\n\n")
//        println("Kobe Shared Key: ${settledSession.sharedKey}\n\n")

        val encryptedJson: EncryptionPayload = codec.encrypt(
            sessionApprovalJson,
            "",
//            settledSession.sharedKey,
            selfPublicKey
            // should be pairingPublicKey
        )

        val encryptedString = encryptedJson.iv + encryptedJson.publicKey + encryptedJson.mac + encryptedJson.cipherText

        println("Kobe Session Approval Encryped Json: $encryptedJson\n\n")

        relayRepository.eventsStream.start(object : Stream.Observer<WebSocket.Event> {
            override fun onComplete() {}

            override fun onError(throwable: Throwable) {}

            override fun onNext(data: WebSocket.Event) {
                if (data is WebSocket.Event.OnConnectionOpened<*>) {
                    println("publishing session approval on topic C ${proposal.topic}\n\n")
                    relayRepository.publishSessionApproval(proposal.topic, encryptedString)

                    //todo subscribe on settledSession.settledTopic - TopicD
                }
            }
        })
    }

    private fun settleSessionSequence(
        relay: RelayProtocolOptions,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        permissions: SessionProposedPermissions,
        expiry: Expiry,
        sessionState: SessionState
    ): SettledSessionSequence {
        val settledTopic: Topic = crypto.generateSharedKey(selfPublicKey, peerPublicKey)

        return SettledSessionSequence(
            settledTopic,
            relay,
            selfPublicKey,
            peerPublicKey,
            permissions,
            expiry,
            sessionState
        )
    }

    private fun String.toEncryptionPayload(): EncryptionPayload {
        val pubKeyStartIndex = EncryptionPayload.ivLength
        val macStartIndex = pubKeyStartIndex + EncryptionPayload.publicKeyLength
        val cipherTextStartIndex = macStartIndex + EncryptionPayload.macLength

        val iv = this.substring(0, pubKeyStartIndex)
        val publicKey = this.substring(pubKeyStartIndex, macStartIndex)
        val mac = this.substring(macStartIndex, cipherTextStartIndex)
        val cipherText = this.substring(cipherTextStartIndex, this.length)

        return EncryptionPayload(iv, publicKey, mac, cipherText)
    }

    data class EngineFactory(val useTLs: Boolean = false, val hostName: String, val port: Int = 0)
}