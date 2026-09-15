package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PhotoManager {

    fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, "member_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveBitmapLocally(context: Context, bitmap: Bitmap, memberId: Long): String {
        val dir = getPhotosDirectory(context)
        val file = File(dir, "photo_${memberId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    fun saveUriLocally(context: Context, uri: Uri, memberId: Long): String {
        val dir = getPhotosDirectory(context)
        val file = File(dir, "photo_${memberId}_${System.currentTimeMillis()}.jpg")

        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            return file.absolutePath
        } else {
            // Fallback direct stream copy
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        }
    }

    fun deletePhotoFile(path: String?) {
        if (!path.isNullOrBlank()) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
