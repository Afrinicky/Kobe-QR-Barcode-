package com.kobe.qrbarcode.core.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** CSV/TXT export used by the batch scanner and history. */
object TextExport {

    fun toCsv(rows: List<List<String>>): String = rows.joinToString("\n") { row ->
        row.joinToString(",") { cell ->
            if (cell.any { it == ',' || it == '"' || it == '\n' }) {
                "\"" + cell.replace("\"", "\"\"") + "\""
            } else {
                cell
            }
        }
    }

    fun saveToDownloads(context: Context, fileName: String, content: String, mime: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Kobe QR")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the export file")
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: error("Could not write the export file")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Kobe QR"
            ).apply { mkdirs() }
            val file = File(directory, fileName)
            file.writeText(content)
            Uri.fromFile(file)
        }
    }

    fun shareText(context: Context, fileName: String, content: String, mime: String) {
        val directory = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(directory, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Export list").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
