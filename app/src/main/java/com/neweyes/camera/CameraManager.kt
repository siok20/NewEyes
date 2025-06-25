package com.neweyes.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.camera.view.PreviewView
import com.neweyes.camera.service.ImageAnalyzeResponse
import com.neweyes.camera.service.analyzeApi
import com.neweyes.vision.FrameAnalyzer
import com.neweyes.voice.TextToSpeechHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    val textToSpeechHelper: TextToSpeechHelper
) {
    private lateinit var cameraExecutor: ExecutorService
    private var lastObstacleTime = 0L
    private val obstacleCooldown = 10000L

    fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                cameraExecutor,
                FrameAnalyzer { obstacleDetected, imageProxy ->
                    val now = System.currentTimeMillis()
                    if (obstacleDetected && now - lastObstacleTime > obstacleCooldown) {
                        //capturarYAnalizarImagen(imageProxy)
                        val jpegBytes = imageProxyToJpeg(imageProxy)
                        val file = File(context.cacheDir, "image.jpg")
                        file.outputStream().use { output ->
                            output.write(jpegBytes)
                        }
                        analizarImagen(file)
                        Log.d("CAMERA_VIEWER", "Encontrado")
                        lastObstacleTime = now
                    } else {
                        Log.d("CAMERA_VIEWER", "Analizando")
                        imageProxy.close()
                    }
                }
            )

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun analizarImagen(imageFile: File) {
        val requestFile = RequestBody.create("image/jpg".toMediaTypeOrNull(), imageFile)
        val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        analyzeApi.sendImage(body).enqueue(object : Callback<ImageAnalyzeResponse> {
            override fun onResponse(call: Call<ImageAnalyzeResponse>, response: Response<ImageAnalyzeResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("CAMERA_VIEWER", "✅ Resultado: ${result?.response}")
                    textToSpeechHelper.speak(result?.response.toString())

                } else {
                    Log.e("CAMERA_VIEWER", "❌ Error en la respuesta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ImageAnalyzeResponse>, t: Throwable) {
                Log.e("CAMERA_VIEWER", "❌ Error en la solicitud: ${t.message}")
            }
        })
    }

    fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray {
        val planes = imageProxy.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copiamos Y
        yBuffer.get(nv21, 0, ySize)

        // Copiamos UV
        val uvPixelStride = planes[1].pixelStride
        val uvRowStride = planes[1].rowStride
        var offset = ySize
        val uBytes = ByteArray(uSize)
        val vBytes = ByteArray(vSize)

        uBuffer.get(uBytes, 0, uSize)
        vBuffer.get(vBytes, 0, vSize)

        if (uvPixelStride == 2) {
            for (i in 0 until vSize) {
                nv21[offset++] = vBytes[i]
                nv21[offset++] = uBytes[i]
            }
        } else {
            var uIndex = 0
            var vIndex = 0
            for (i in 0 until uSize) {
                nv21[offset++] = vBytes[vIndex++]
                nv21[offset++] = uBytes[uIndex++]
            }
        }

        // Ahora comprimimos a JPEG
        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            output
        )
        return output.toByteArray()
    }



    fun stopCamera() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}


