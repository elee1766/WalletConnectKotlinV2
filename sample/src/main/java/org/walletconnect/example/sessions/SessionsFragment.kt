package org.walletconnect.example.sessions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.walletconnect.example.R
import org.walletconnect.example.databinding.SessionsFragmentBinding

class SessionsFragment : Fragment(R.layout.sessions_fragment) {

    private lateinit var binding: SessionsFragmentBinding
    private lateinit var viewModel: SessionsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = SessionsFragmentBinding.bind(view)
        viewModel = ViewModelProvider(this).get(SessionsViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }
}