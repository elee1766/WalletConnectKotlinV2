package org.walletconnect.walletconnectv2.engine

import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientsync.pairing.PreSettlementPairing
import org.walletconnect.walletconnectv2.clientsync.pairing.SettledPairingSequence
import org.walletconnect.walletconnectv2.clientsync.session.PreSettlementSession
import org.walletconnect.walletconnectv2.clientsync.session.SettledSessionSequence
import org.walletconnect.walletconnectv2.common.*
import org.walletconnect.walletconnectv2.common.toApprove
import org.walletconnect.walletconnectv2.common.toPairProposal
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.codec.AuthenticatedEncryptionCodec
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.managers.LazySodiumCryptoManager
import org.walletconnect.walletconnectv2.outofband.pairing.Pairing
import org.walletconnect.walletconnectv2.outofband.pairing.proposal.PairingProposedPermissions
import org.walletconnect.walletconnectv2.pubsub.Session
import org.walletconnect.walletconnectv2.pubsub.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.pubsub.success.SessionState
import org.walletconnect.walletconnectv2.relay.WakuRelayRepository
import org.walletconnect.walletconnectv2.util.Utils
import java.util.*

class EngineInteractor(useTLs: Boolean = false, hostName: String, port: Int = 0) {
    //region provide with DI
    // TODO: add logic to check hostName for ws/wss scheme with and without ://
    private val relayRepository: WakuRelayRepository =
        WakuRelayRepository.initRemote(useTLs = useTLs, hostName = hostName, port = port)
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
    //endregion

    internal val pairingAcknowledgement = relayRepository.publishAcknowledgement
    internal val subscribeAcknowledgement = relayRepository.subscribeAcknowledgement

    fun pair(uri: String) {
        val pairingProposal: Pairing.Proposal = uri.toPairProposal()
        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val expiry =
            Expiry((Calendar.getInstance().timeInMillis / 1000) + pairingProposal.ttl.seconds)

        val peerPublicKey = PublicKey(pairingProposal.pairingProposer.publicKey)
        val controllerPublicKey: PublicKey = if (pairingProposal.pairingProposer.controller) {
            peerPublicKey
        } else {
            selfPublicKey
        }
        val settledSequence: SettledPairingSequence = settle(
            pairingProposal.relay,
            selfPublicKey,
            peerPublicKey,
            pairingProposal.permissions,
            controllerPublicKey,
            expiry
        )
        val preSettlementPairingApprove: PreSettlementPairing.Approve =
            pairingProposal.toApprove(1, settledSequence.settledTopic, expiry, selfPublicKey)

        relayRepository.eventsStream.start(object : Stream.Observer<WebSocket.Event> {
            override fun onComplete() {}

            override fun onError(throwable: Throwable) {}

            override fun onNext(data: WebSocket.Event) {
                if (data is WebSocket.Event.OnConnectionOpened<*>) {
                    relayRepository.publishPairingApproval(
                        pairingProposal.topic,
                        preSettlementPairingApprove
                    )
                    relayRepository.subscribe(settledSequence.settledTopic)
                }
            }
        })
    }

    fun approve(proposal: Session.SessionProposal) {
        val selfPublicKey: PublicKey = crypto.generateKeyPair()
        val peerPublicKey = PublicKey(proposal.proposer.publicKey)
        val sessionState =
            SessionState(listOf("eip155:137:0x022c0c42a80bd19EA4cF0F94c4F9F96645759716")) //pass proper keys from wallet
        val expiry =
            Expiry((Calendar.getInstance().timeInMillis / 1000) + proposal.ttl.seconds)

        val settledSession: SettledSessionSequence = settleSessionSequence(
            proposal.relay,
            selfPublicKey,
            peerPublicKey,
            proposal.permissions,
            expiry,
            sessionState
        )

        val preSettlementSession: PreSettlementSession.Approve = proposal.toApprove(
            Utils.generateId(),
            expiry,
            selfPublicKey,
            settledSession.state,
            settledSession.settledTopic
        )

        val sessionApprovalJson: String =
            relayRepository.getSessionApprovalJson(preSettlementSession)

        val encryptedJson: EncryptionPayload = codec.encrypt(sessionApprovalJson, settledSession.sharedKey)
        val encryptedString: String = relayRepository.getEncryptionPayloadJson(encryptedJson)

        relayRepository.eventsStream.start(object : Stream.Observer<WebSocket.Event> {
            override fun onComplete() {}

            override fun onError(throwable: Throwable) {}

            override fun onNext(data: WebSocket.Event) {
                if (data is WebSocket.Event.OnConnectionOpened<*>) {
                    //publishing session approval on topic C

                    relayRepository.publishSessionApproval(proposal.topic, encryptedString)
                    //todo subscribe on settledSession.settledTopic - TopicD
                }
            }
        })
    }

    private fun settleSessionSequence(
        relay: JSONObject,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        permissions: SessionProposedPermissions,
        expiry: Expiry,
        sessionState: SessionState
    ): SettledSessionSequence {
        val settledTopic: Topic = crypto.generateSettledTopic(selfPublicKey, peerPublicKey)
        val sharedKey: String = crypto.generateSharedKey(selfPublicKey, peerPublicKey)
        return SettledSessionSequence(
            settledTopic,
            relay,
            sharedKey,
            selfPublicKey,
            peerPublicKey,
            permissions,
            expiry,
            sessionState
        )
    }

    private fun settle(
        relay: JSONObject,
        selfPublicKey: PublicKey,
        peerPublicKey: PublicKey,
        permissions: PairingProposedPermissions?,
        controllerPublicKey: PublicKey,
        expiry: Expiry
    ): SettledPairingSequence {
        val settledTopic: Topic = crypto.generateSettledTopic(selfPublicKey, peerPublicKey)
        val sharedKey: String = crypto.generateSharedKey(selfPublicKey, peerPublicKey)
        return SettledPairingSequence(
            settledTopic,
            relay,
            sharedKey,
            selfPublicKey,
            peerPublicKey,
            permissions to controllerPublicKey,
            expiry
        )
    }
}