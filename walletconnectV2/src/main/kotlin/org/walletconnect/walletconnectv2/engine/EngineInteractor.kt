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
import org.walletconnect.walletconnectv2.pubsub.RelayProtocolOptions
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

    internal val publishAcknowledgement = relayRepository.publishAcknowledgement
    internal val subscribeAcknowledgement = relayRepository.subscribeAcknowledgement
    val subscriptionRequest = relayRepository.subscriptionRequest

    var pairingPublicKey: PublicKey = PublicKey("")

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

        pairingPublicKey = selfPublicKey

        val preSettlementPairingApprove: PreSettlementPairing.Approve =
            pairingProposal.toApprove(Utils.generateId(), settledSequence.settledTopic, expiry, selfPublicKey)

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
        println("Kobe Session Proposal: $proposal\n")

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

        //SESSION APPROVAL JSON
        println("Kobe Session Approval Json: $sessionApprovalJson\n\n")
        println("Kobe Shared Key: ${settledSession.sharedKey}\n\n")

        val encryptedJson: EncryptionPayload = codec.encrypt(sessionApprovalJson, settledSession.sharedKey, pairingPublicKey)//settledSession.sharedKey, selfPublicKey)
//        val encryptedString: String = relayRepository.getEncryptionPayloadJson(encryptedJson)

//        val encryptedString = "8699fe86f58b8c4424a76231a54ba60f" + "488075efc64f69bff4fddac975136095921cb1b56666edb22874134c1cff9847" + "d2802e50aa3d855bc5819c6fe799baf0ad75b0469d059b126748b87957d4088b" + "f374fc3f7bdaaa9606a7a57e8e2830468470341fb62f5c4752c74a7bee81040f8f6f7d5c5f50b733d13bb1a26f85b2da0d41e07d4db0a8546033df4695aefd1c11c29052e756092d9879a87fc44faef0d6d2fcc4d0daecd6babca426ee94fe374592c364e472b42d69e4737daab9720f9ba7c645897158d077a351b717093311936548d2e5427b9026592dbeda5011c83994ae4a8847420756caf76c50d9f9c044302d4e678eb385663cf4e49b2b989914744f490e2e8246debf836537b3aab4e9136443dae64fc6da674ea434c1081d6e07f7b99e92fb531b9e51a67cc5f3e8c52504f466b2654c139b294aa517b47f390aca7ecf60828179115b5ee1577228b86750edfaf544ae67fef2927a27fa3cc1c2d59bb273bf560d6b99c571e2a388e8ea33c6213e413b7409c2b3eaf530dbbabef015665ff89b837b99bb32c47778a36a83e6ce543605400694a1b0cf7e299356f4a92d050e90478d46f20d8e130055217c5ddd2852e96cfda418c56302028937bb9bde597f888242c4865e585ac22cd8777a6949a7e3439117bbd2ab4054daa8e8ce90c2990df8c57a7d3baaa8fce748a1bf37c1e9de6c8c94e2b9ce8d7c14e8592cc1fc96a5c075463172c5972f21cc85638b943b231a4b7918287c0b16b3f7ed1c5c16551b648fec5d2a95b9bcd7c35180ce1e65d841438ad0e70d36cd"
        val encryptedString = encryptedJson.iv + encryptedJson.publicKey + encryptedJson.mac + encryptedJson.cipherText

        println("Kobe Session Approval Encryped Json: $encryptedJson\n\n")
//        println("Kobe Session Approval Encryped String: $encryptedString\n\n")

        println("publishing session approval on topic C ${proposal.topic}\n\n")
        relayRepository.publishSessionApproval(proposal.topic, encryptedString)

//        relayRepository.eventsStream.start(object : Stream.Observer<WebSocket.Event> {
//            override fun onComplete() {}
//
//            override fun onError(throwable: Throwable) {}
//
//            override fun onNext(data: WebSocket.Event) {
//                if (data is WebSocket.Event.OnConnectionOpened<*>) {
//                    //publishing session approval on topic C
//
//                    println("publishing session approval on topic C")
//
//                    relayRepository.publishSessionApproval(proposal.topic, encryptedString)
//                    //todo subscribe on settledSession.settledTopic - TopicD
//                }
//            }
//        })
    }

    private fun settleSessionSequence(
        relay: RelayProtocolOptions,
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