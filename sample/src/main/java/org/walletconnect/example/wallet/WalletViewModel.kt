package org.walletconnect.example.wallet

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.walletconnect.example.R

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    private var _uiState = mutableStateOf(WalletUiState())
    val uiState: State<WalletUiState> = _uiState

    fun hideBottomNav() {
        viewModelScope.launch {
            _eventFlow.emit(ToggleBottomNav(false))
        }
    }

    fun showBottomNav() {
        viewModelScope.launch {
            _eventFlow.emit(ToggleBottomNav(true))
        }
    }

    fun pair(uri: String) {
        //call pair(uri) from SDK and setup listener to get SessionProposal, once SessionProposal is received show proposal dialog
        viewModelScope.launch {
            _eventFlow.emit(ShowSessionProposalDialog)
        }

        _uiState.value = _uiState.value.copy(
            proposal = SessionProposal(
                name = "WalletConnect",
                icon = R.drawable.ic_walletconnect_circle_blue,
                uri = "app.walletconnect.org",
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut eu accumsan nunc. Cras luctus, ipsum at tempor vulputate, metus purus mollis ex, ut maximus tellus lectus non nisl. Duis eu diam sollicitudin, bibendum enim ut, elementum erat.",
                chains = listOf("Ethereum Kovan", "BSC Mainnet", "Fantom Opera"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signedTypedData")
            )
        )
    }

    fun approve() {
        //call approve() session method from SDK
        viewModelScope.launch {
            _eventFlow.emit(HideSessionProposalDialog)
        }

        val session = Session(
            name = "WalletConnect",
            uri = "app.walletconnect.org",
            icon = R.drawable.ic_walletconnect_circle_blue
        )

        _uiState.value = _uiState.value.copy(
            sessions = (_uiState.value.sessions + session) as MutableList<Session>
        )
    }

    fun reject() {
        //call reject() session method from SDK
        viewModelScope.launch {
            _eventFlow.emit(HideSessionProposalDialog)
        }
    }

    private var cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()
    val processCameraProvider: LiveData<ProcessCameraProvider>
        get() {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
            cameraProviderFuture.addListener(
                { cameraProviderLiveData.setValue(cameraProviderFuture.get()) },
                ContextCompat.getMainExecutor(getApplication())
            )
            return cameraProviderLiveData
        }
}