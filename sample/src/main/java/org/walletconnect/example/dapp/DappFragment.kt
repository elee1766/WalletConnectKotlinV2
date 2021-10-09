package org.walletconnect.example.dapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.walletconnect.example.R
import org.walletconnect.example.databinding.DappFragmentBinding

class DappFragment : Fragment(R.layout.dapp_fragment) {

    private lateinit var binding: DappFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = DappFragmentBinding.bind(view)
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }
}