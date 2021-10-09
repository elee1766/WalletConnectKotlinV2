package org.walletconnect.example.wallet.ui

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.walletconnect.example.databinding.SessionProposalDialogBinding

class SessionProposalDialog(
    context: Context,
    val approve: () -> Unit,
    val reject: () -> Unit
) : BottomSheetDialog(context) {

    private val binding = SessionProposalDialogBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
    }

    fun setContent(proposal: SessionProposal) = with(binding) {
        icon.setImageDrawable(ContextCompat.getDrawable(context, proposal.icon))
        name.text = proposal.name
        uri.text = proposal.uri
        description.text = proposal.description
        var chainsString = ""
        proposal.chains.forEach {
            chainsString += "$it\n"
        }
        chains.text = chainsString

        var methodsString = ""
        proposal.methods.forEach {
            methodsString += "$it\n"
        }
        methods.text = methodsString

        approve.setOnClickListener {
            approve()
            dismiss()
        }

        reject.setOnClickListener {
            reject()
            dismiss()
        }
    }
}