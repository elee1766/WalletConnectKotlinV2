package org.walletconnect.example.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.walletconnect.example.R
import org.walletconnect.example.databinding.WalletFragmentBinding
import org.walletconnect.example.wallet.ui.SessionsAdapter
import org.walletconnect.example.wallet.ui.UpdateActiveSessions
import org.walletconnect.example.wallet.ui.sessionList

class WalletFragment : Fragment(R.layout.wallet_fragment) {

    private val viewModel: WalletViewModel by activityViewModels()
    private lateinit var binding: WalletFragmentBinding
    private val sessionAdapter = SessionsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = WalletFragmentBinding.bind(view)
        viewModel.showBottomNav()
        binding.sessions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sessionAdapter
        }

        if (viewModel.activeSessions.isEmpty()){
            sessionAdapter.updateList(sessionList)
        } else {
            sessionAdapter.updateList(viewModel.activeSessions)
        }

        viewModel.eventFlow.observe(viewLifecycleOwner, { event ->
            (event as? UpdateActiveSessions)?.apply {
                sessionAdapter.updateList(sessions)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }
}