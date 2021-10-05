package org.walletconnect.example.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.walletconnect.example.R
import org.walletconnect.example.databinding.ScannerFragmentBinding
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ScannerFragment : Fragment(R.layout.scanner_fragment) {

    private lateinit var binding: ScannerFragmentBinding
    private lateinit var viewModel: ScannerViewModel

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private val screenAspectRatio: Int
        get() {
            // Get screen metrics used to setup camera for full screen resolution
            val metrics = DisplayMetrics().also { binding.previewView.display?.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ScannerFragmentBinding.bind(view)
        viewModel = ViewModelProvider(this).get(ScannerViewModel::class.java)
        activity?.invalidateOptionsMenu()
        setupCamera()
    }

    private fun setupCamera() {
        previewView = binding.previewView
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(ScannerViewModel::class.java)
            .processCameraProvider
            .observe(viewLifecycleOwner, { provider: ProcessCameraProvider? ->
                cameraProvider = provider

                if (isCameraPermissionGranted()) {
                    bindCameraUseCases()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA_REQUEST)
                }
            }
            )
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(previewView!!.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
        } catch (illegalStateException: IllegalStateException) {
            Log.e("kobe", illegalStateException.message ?: "")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("kobe", illegalArgumentException.message ?: "")
        }
    }

    private fun bindAnalyseUseCase() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build();
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()


        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(previewView!!.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(cameraExecutor,
            ImageAnalysis.Analyzer { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
        } catch (illegalStateException: IllegalStateException) {
            Log.e("kobe", illegalStateException.message ?: "")
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e("kobe", illegalArgumentException.message ?: "")
        }
    }

    var shouldScan: Boolean = true

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && shouldScan){
                    shouldScan = false

                    AlertDialog.Builder(requireContext())
                        .setMessage(barcodes.first().rawValue)
                        .setPositiveButton("Ok"
                        ) { dialog, which ->
                            shouldScan = true
                            dialog?.dismiss()
                        }
                        .show()
                }
            }
            .addOnFailureListener { Log.e("kobe", it.message ?: "") }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (isCameraPermissionGranted()) {
                Log.d("kobe", "permissions granded")
                bindCameraUseCases()
            } else {
                Log.e("kobe", "no camera permission")
            }
        }
    }


    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}