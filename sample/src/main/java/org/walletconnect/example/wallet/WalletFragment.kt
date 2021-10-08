package org.walletconnect.example.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.walletconnect.example.R

class WalletFragment : Fragment() {

    private val viewModel: WalletViewModel by activityViewModels()


    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.showBottomNav()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val modalBottomSheetState =
                    rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
                val scope = rememberCoroutineScope()
                val uiState: WalletUiState = viewModel.uiState.value
                observeUiEvents(scope, modalBottomSheetState)
                BottomSheetModal(uiState, modalBottomSheetState)
            }
        }
    }

    @ExperimentalMaterialApi
    private fun observeUiEvents(
        scope: CoroutineScope,
        modalBottomSheetState: ModalBottomSheetState
    ) {
        viewModel.eventFlow.observe(viewLifecycleOwner, { state ->
            when (state) {
                is ShowSessionProposalDialog -> {
                    scope.launch {
                        modalBottomSheetState.show()
                    }
                }
                is HideSessionProposalDialog -> {
                    scope.launch {
                        modalBottomSheetState.hide()
                    }
                }
                else -> {
                }
            }
        })
    }

    @ExperimentalMaterialApi
    @Composable
    private fun ComposeView.BottomSheetModal(
        uiState: WalletUiState,
        modalBottomSheetState: ModalBottomSheetState
    ) {
        ModalBottomSheetLayout(sheetContent = {
            Column(
                modifier = Modifier
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = uiState.proposal.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.proposal.name)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.proposal.uri)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = uiState.proposal.description, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.chains),
                    fontWeight = FontWeight.Bold
                )
                uiState.proposal.chains.forEach { chain ->
                    Text(text = chain)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.methods),
                    fontWeight = FontWeight.Bold
                )
                uiState.proposal.methods.forEach { chain ->
                    Text(text = chain)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(
                        onClick = { viewModel.reject() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                    ) {
                        Text(text = context.getString(R.string.reject), color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.approve() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue)
                    ) {
                        Text(
                            text = context.getString(R.string.approve),
                            color = Color.White
                        )
                    }
                }

            }
        }, sheetState = modalBottomSheetState) { ShowSessionsView(uiState) }
    }

    @Composable
    private fun ShowSessionsView(uiState: WalletUiState) {
        MaterialTheme {
            Scaffold(backgroundColor = Color.White) {

                LazyColumn {
                    if (uiState.sessions.isEmpty()) {
                        items(sessionList) { session ->
                            SessionRow(session = session)
                        }
                    } else {
                        items(uiState.sessions) { session ->
                            SessionRow(session = session)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SessionRow(session: Session) {
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(session.icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(text = session.name)
                    Text(text = session.uri, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.invalidateOptionsMenu()
    }
}