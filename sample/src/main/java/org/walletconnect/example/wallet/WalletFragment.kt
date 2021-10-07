package org.walletconnect.example.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.walletconnect.example.R
import org.walletconnect.example.databinding.WalletFragmentBinding
import org.walletconnect.example.wallet.WalletViewModel

class WalletFragment : Fragment(R.layout.wallet_fragment) {

    private lateinit var binding: WalletFragmentBinding
    private val viewModel: WalletViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = WalletFragmentBinding.bind(view)

//        binding.composeView.setContent {
//
//            MaterialTheme() {
//                Scaffold(
//                    scaffoldState = scaffoldState,
//                    backgroundColor = Color.White,
//                ) {
//                    Column() {
//
//                        Button(
//                            onClick = {  },
//                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue)
//                        ) {
//                            Icon(
//                                painter = painterResource(id = R.drawable.ic_qr_code_scanner),
//                                contentDescription = null,
//                                tint = Color.White
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(text = "Scan QR code", color = Color.White)
//                        }
//                    }
//                }
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }
}