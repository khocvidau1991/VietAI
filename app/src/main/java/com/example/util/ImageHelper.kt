package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object ImageHelper {
    private const val MAX_DIM = 1024      // Giới hạn chiều dài lớn nhất (px)
    private const val JPEG_QUALITY = 75   // Chất lượng JPEG

    /**
     * Đọc Uri → downscale → base64. Chạy trên IO dispatcher.
     * Dùng inSampleSize để tránh OOM: đọc kích thước trước, giảm mẫu,
     * rồi mới decode bitmap thật.
     */
    suspend fun uriToBase64(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                // ===== Bước 1: Đọc kích thước ảnh (không load bitmap) =====
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
                val srcW = options.outWidth
                val srcH = options.outHeight

                if (srcW <= 0 || srcH <= 0) {
                    LogRepository.log("ImageHelper", "[ERROR] Không đọc được kích thước ảnh")
                    return@withContext null
                }

                // ===== Bước 2: Tính inSampleSize (chia 2 liên tiếp) =====
                var sampleSize = 1
                var halfW = srcW / 2
                var halfH = srcH / 2
                val maxSide = maxOf(srcW, srcH)
                while (halfW / sampleSize > MAX_DIM * 2 && halfH / sampleSize > MAX_DIM * 2) {
                    sampleSize *= 2
                }

                // ===== Bước 3: Decode bitmap đã giảm mẫu =====
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565  // 2 byte/px thay vì 4
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: run {
                    LogRepository.log("ImageHelper", "[ERROR] decodeStream trả null")
                    return@withContext null
                }

                // ===== Bước 4: Scale chính xác về MAX_DIM =====
                val scaled = downscale(bitmap, MAX_DIM)
                if (scaled !== bitmap) bitmap.recycle()

                // ===== Bước 5: Nén JPEG → base64 =====
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                scaled.recycle()

                val result = out.toByteArray()
                out.close()

                LogRepository.log("ImageHelper",
                    "Ảnh gốc ${srcW}x${srcH}, sample=$sampleSize → " +
                        "base64 ${result.size / 1024}KB")

                Base64.encodeToString(result, Base64.NO_WRAP)
            } catch (e: OutOfMemoryError) {
                LogRepository.log("ImageHelper", "[OOM] Ảnh quá lớn: ${e.message}")
                null
            } catch (e: Exception) {
                LogRepository.log("ImageHelper", "[ERROR] uriToBase64: ${e.message}")
                null
            }
        }

    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: OutOfMemoryError) {
            LogRepository.log("ImageHelper", "[OOM] base64ToBitmap: ${e.message}")
            null
        } catch (e: Exception) {
            LogRepository.log("ImageHelper", "[ERROR] base64ToBitmap: ${e.message}")
            null
        }
    }

    private fun downscale(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxDim) return src
        val ratio = maxDim.toFloat() / maxSide
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    /**
     * Tạo Uri cho camera ghi ảnh vào cache.
     * Dùng FileProvider với authority = "${'$'}{packageName}.fileprovider".
     */
    fun createCameraOutputUri(context: Context): Uri? {
        return try {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            if (!dir.exists()) {
                LogRepository.log("ImageHelper", "[ERROR] Không tạo được thư mục cache")
                return null
            }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")

            // Đảm bảo file cha tồn tại
            file.parentFile?.mkdirs()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            LogRepository.log("ImageHelper", "[ERROR] createCameraOutputUri: ${e.message}")
            null
        }
    }
}
