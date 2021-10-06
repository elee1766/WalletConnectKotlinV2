package org.walletconnect.example.scanner

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutionException

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
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