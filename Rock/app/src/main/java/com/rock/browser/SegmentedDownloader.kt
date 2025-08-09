package com.rock.browser

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

class SegmentedDownloader(private val context: Context, private val segments: Int = 4) {
    private val client = OkHttpClient()

    fun download(url: String, fileName: String, onDone: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val headReq = Request.Builder().url(url).head().build()
                val headResp = client.newCall(headReq).execute()
                val length = headResp.header("Content-Length")?.toLongOrNull()
                if (length == null) {
                    singleStream(url, fileName)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                    return@launch
                }

                val partSize = length / segments
                val jobs = (0 until segments).map { idx ->
                    async {
                        val start = idx * partSize
                        val end = if (idx == segments - 1) length - 1 else (start + partSize - 1)
                        val req = Request.Builder().url(url).addHeader("Range", "bytes=$start-$end").build()
                        client.newCall(req).execute().use { resp -> resp.body?.bytes() ?: ByteArray(0) }
                    }
                }

                val parts = jobs.awaitAll()
                saveToDownloads(fileName, parts)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_SHORT).show()
                    onDone()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun singleStream(url: String, fileName: String) {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            saveToDownloads(fileName, listOf(bytes))
        }
    }

    private fun saveToDownloads(fileName: String, parts: List<ByteArray>) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        }
        val externalContentUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val itemUri = resolver.insert(externalContentUri, values) ?: return
        resolver.openOutputStream(itemUri, "w")?.use { os ->
            parts.forEach { os.write(it) }
            os.flush()
        }
    }
}
