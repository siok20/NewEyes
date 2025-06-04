package com.neweyes.vision

import android.graphics.ImageFormat
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class FrameAnalyzer(
    private val onObstacleDetected: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val img = image.image
        if (img != null && img.format == ImageFormat.YUV_420_888) {
            val obstacle = detectObstacleInCenter(img)
            onObstacleDetected(obstacle)
        }
        image.close()
    }

    private fun detectObstacleInCenter(image: Image): Boolean {
        val yPlane: ByteBuffer = image.planes[0].buffer
        val width = image.width
        val height = image.height

        val yRowStride = image.planes[0].rowStride

        // Zona central de análisis (por simplicidad: un cuadrado 100x100 px)
        val cx = width / 2
        val cy = height / 2
        var brightPixels = 0

        for (dy in -50..50) {
            for (dx in -50..50) {
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until width && y in 0 until height) {
                    val yIndex = y * yRowStride + x
                    val pixel = yPlane.get(yIndex).toInt() and 0xFF
                    if (pixel < 80) brightPixels++ // píxel oscuro: posible obstáculo
                }
            }
        }

        val total = 101 * 101
        return brightPixels > total * 0.6 // Si más del 60% son oscuros, hay obstáculo
    }
}
