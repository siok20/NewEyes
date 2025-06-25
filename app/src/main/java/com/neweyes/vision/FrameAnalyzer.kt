package com.neweyes.vision

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class FrameAnalyzer(
    private val onObstacleDetected: (Boolean, ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        Log.e("CAMERA_DEBUG", "Analyze llamado para imagen ${image.image?.width}x${image.image?.height}")
        val img = image.image
        if (img != null && img.format == ImageFormat.YUV_420_888) {
            val obstacle = detectObstacleInCenter(img)
            onObstacleDetected(obstacle, image)
        }
        image.close()
    }

    private fun detectObstacleInCenter(image: Image): Boolean {
        val yPlane = image.planes[0].buffer
        val width = image.width
        val height = image.height
        val yRowStride = image.planes[0].rowStride

        val sampleSize = 101
        val half = sampleSize / 2
        val cx = width / 2
        val cy = height / 2

        var edgePixels = 0
        var totalPixels = 0
        val start = System.currentTimeMillis()
        for (dy in -half until half) {
            for (dx in -half until half) {
                val x = cx + dx
                val y = cy + dy
                if (x in 1 until width - 1 && y in 1 until height - 1) {
                    val index = y * yRowStride + x
                    if (index in 0 until yPlane.limit()) {
                        val pixel = yPlane.get(index).toInt() and 0xFF
                        val leftPixel = yPlane.get(index - 1).toInt() and 0xFF
                        val rightPixel = yPlane.get(index + 1).toInt() and 0xFF
                        val topPixel = yPlane.get(index - yRowStride).toInt() and 0xFF
                        val bottomPixel = yPlane.get(index + yRowStride).toInt() and 0xFF

                        val diffH = Math.abs(leftPixel - rightPixel)
                        val diffV = Math.abs(topPixel - bottomPixel)

                        // Si hay un contraste significativo en esta posición,
                        // asumimos que es un borde de un posible obstáculo
                        if (diffH > 30 || diffV > 30) {
                            edgePixels++
                        }
                        totalPixels++
                    }
                }
            }
        }

        // Si más del 15% de los píxeles analizados presentan borde significativo,
        // asumimos que hay un obstáculo enfrente
        Log.d("CAMERA_VIEWER", "${System.currentTimeMillis() - start}")
        return edgePixels > totalPixels * 0.15
    }

}
