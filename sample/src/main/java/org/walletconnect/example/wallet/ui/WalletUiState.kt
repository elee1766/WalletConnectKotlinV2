package org.walletconnect.example.wallet.ui

import org.walletconnect.example.R
import org.walletconnect.walletconnectv2.client.SessionProposal

sealed class WalletUiEvent
data class ShowSessionProposalDialog(val proposal: SessionProposal) : WalletUiEvent()
data class UpdateActiveSessions(val sessions: List<Session>) : WalletUiEvent()

data class Session(
    var icon: Int = R.drawable.ic_walletconnect_circle_blue,
    var name: String = "",
    var uri: String = ""
)