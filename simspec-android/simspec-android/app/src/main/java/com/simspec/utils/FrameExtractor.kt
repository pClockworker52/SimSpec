package com.simspec.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * FrameExtractor - Utility for converting camera frames to bitmaps
 * Handles ImageProxy to Bitmap conversion with proper rotation
 */
object FrameExtractor {
    
    private const val TAG = "FrameExtractor"
    
    /**
     * Convert an ImageProxy from CameraX to a Bitmap
     * Handles rotation and format conversion properly
     * 
     * @param imageProxy The ImageProxy from CameraX
     * @return Bitmap or null if conversion fails
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            Log.d(TAG, "Converting ImageProxy to Bitmap - Format: ${imageProxy.format}, Size: ${imageProxy.width}x${imageProxy.height}")
            
            when (imageProxy.format) {
                ImageFormat.JPEG -> {
                    // For JPEG format (from ImageCapture), extract bytes directly
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap == null) {
                        Log.w(TAG, "Failed to decode JPEG bytes to Bitmap")
                        return null
                    }
                    
                    // Apply rotation to match device orientation
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { 
                            postRotate(rotationDegrees.toFloat()) 
                        }
                        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    
                    bitmap
                }
                
                ImageFormat.YUV_420_888 -> {
                    // For YUV format (from ImageAnalysis), convert via YuvImage
                    val yBuffer = imageProxy.planes[0].buffer
                    val uBuffer = imageProxy.planes[1].buffer
                    val vBuffer = imageProxy.planes[2].buffer

                    val ySize = yBuffer.remaining()
                    val uSize = uBuffer.remaining()
                    val vSize = vBuffer.remaining()

                    val nv21 = ByteArray(ySize + uSize + vSize)
                    
                    yBuffer.get(nv21, 0, ySize)
                    vBuffer.get(nv21, ySize, vSize)
                    uBuffer.get(nv21, ySize + vSize, uSize)

                    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
                    val out = ByteArrayOutputStream()
                    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 75, out)
                    val imageBytes = out.toByteArray()
                    
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap == null) {
                        Log.w(TAG, "Failed to decode YUV to Bitmap")
                        return null
                    }
                    
                    // Apply rotation
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { 
                            postRotate(rotationDegrees.toFloat()) 
                        }
                        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    
                    bitmap
                }
                
                else -> {
                    Log.e(TAG, "Unsupported ImageProxy format: ${imageProxy.format}")
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap: ${e.message}", e)
            null
        }
    }
    
    /**
     * Resize a bitmap to specified dimensions
     * Used for optimizing performance before AI inference
     * 
     * @param bitmap The original bitmap
     * @param width Target width
     * @param height Target height
     * @return Resized bitmap
     */
    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return try {
            Bitmap.createScaledBitmap(bitmap, width, height, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing bitmap: ${e.message}", e)
            bitmap // Return original if resize fails
        }
    }
    
    /**
     * Resize bitmap while maintaining aspect ratio
     * Useful when you want to fit within maximum dimensions
     * 
     * @param bitmap The original bitmap
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Resized bitmap maintaining aspect ratio
     */
    fun resizeBitmapKeepAspectRatio(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            
            val aspectRatio = width.toFloat() / height.toFloat()
            
            val (newWidth, newHeight) = if (aspectRatio > 1) {
                // Landscape
                val newW = minOf(maxWidth, width)
                val newH = (newW / aspectRatio).toInt()
                newW to minOf(newH, maxHeight)
            } else {
                // Portrait or square
                val newH = minOf(maxHeight, height)
                val newW = (newH * aspectRatio).toInt()
                minOf(newW, maxWidth) to newH
            }
            
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing bitmap with aspect ratio: ${e.message}", e)
            bitmap
        }
    }
    
    /**
     * Get bitmap size information for debugging
     */
    fun getBitmapInfo(bitmap: Bitmap): String {
        return "Bitmap: ${bitmap.width}x${bitmap.height}, " +
                "Config: ${bitmap.config}, " +
                "Size: ${bitmap.allocationByteCount / 1024}KB"
    }
}