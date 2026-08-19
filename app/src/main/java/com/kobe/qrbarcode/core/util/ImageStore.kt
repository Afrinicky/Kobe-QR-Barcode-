package com.kobe.qrbarcode.core.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saving, sharing and printing generated images — all local operations. */
object ImageStore {

    private const val ALBUM = "Kobe QR"

    fun defaultFileName(prefix: String = "kobe"): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_$stamp"
    }

    /** Writes the bitmap into the device gallery. Returns the new content uri. */
    fun saveToGallery(context: Context, bitmap: Bitmap, name: String): Uri {
        val fileName = "$name.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create a file in the gallery")
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: error("Could not write the image")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                ALBUM
            )
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            Uri.fromFile(file)
        }
    }

    /** Copies the bitmap into the shareable cache folder and returns a FileProvider uri. */
    fun cacheForSharing(context: Context, bitmap: Bitmap, name: String): Uri {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }
            ?.forEach { it.delete() }
        val file = File(directory, "$name.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareImage(context: Context, bitmap: Bitmap, name: String, caption: String = "") {
        val uri = cacheForSharing(context, bitmap, name)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share code").also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun print(context: Context, bitmap: Bitmap, jobName: String) {
        PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            colorMode = PrintHelper.COLOR_MODE_COLOR
        }.printBitmap(jobName, bitmap)
    }
}
