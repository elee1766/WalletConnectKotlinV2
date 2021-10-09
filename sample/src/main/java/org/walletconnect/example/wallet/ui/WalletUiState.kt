package org.walletconnect.example.wallet.ui

import org.walletconnect.example.R

sealed class WalletUiEvent
data class ToggleBottomNav(val shouldShown: Boolean) : WalletUiEvent()
data class ShowSessionProposalDialog(val proposal: SessionProposal) : WalletUiEvent()
data class UpdateActiveSessions(val sessions: List<Session>) : WalletUiEvent()

data class SessionProposal(
    var icon: Int = R.drawable.ic_walletconnect_circle_blue,
    var name: String = "",
    var uri: String = "",
    var description: String = "",
    var chains: List<String> = emptyList(),
    var methods: List<String> = emptyList()
)

data class Session(
    var icon: Int = R.drawable.ic_walletconnect_circle_blue,
    var name: String = "",
    var uri: String = ""
)

val sessionList = listOf(
    Session(
        name = "UniSwap",
        uri = "app.uniswap.org",
        icon = R.drawable.ic_uniswap
    ),
    Session(
        name = "PancakeSwap",
        uri = "app.pancake.org",
        icon = R.drawable.ic_pancakeswap
    ),
    Session(
        name = "SushiSwap",
        uri = "app.sushiswap.org",
        icon = R.drawable.ic_sushiswap
    )
)