package org.walletconnect.example.scanner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.walletconnect.example.R
import org.walletconnect.example.databinding.ScannerFragmentBinding

class ScannerFragment : Fragment(R.layout.scanner_fragment) {

    private lateinit var binding: ScannerFragmentBinding
    private lateinit var viewModel: ScannerViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ScannerFragmentBinding.bind(view)
        viewModel = ViewModelProvider(this).get(ScannerViewModel::class.java)
        activity?.invalidateOptionsMenu()
    }
}