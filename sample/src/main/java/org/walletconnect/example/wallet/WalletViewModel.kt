package org.walletconnect.example.wallet

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class WalletViewModel(application: Application) : AndroidViewModel(application) {

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