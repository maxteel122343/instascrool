package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {
    private const val TAG = "ImageDownloader"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun downloadPhoto(context: Context, photoUrl: String, title: String? = null) {
        if (photoUrl.isBlank()) {
            showToast(context, "URL da imagem inválida")
            return
        }

        showToast(context, "Iniciando download...")

        if (photoUrl.startsWith("data:image")) {
            // Base64 encoded image
            scope.launch {
                saveBase64Image(context, photoUrl, title)
            }
        } else if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            // Standard HTTP/HTTPS image
            scope.launch {
                downloadHttpImage(context, photoUrl, title)
            }
        } else {
            showToast(context, "Formato de imagem não suportado")
        }
    }

    private suspend fun downloadHttpImage(context: Context, photoUrl: String, title: String?) {
        try {
            // Download bytes directly to ensure high resolution and support all image hosts
            withContext(Dispatchers.IO) {
                val url = URL(photoUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/122.0.0.0 Mobile Safari/537.36"
                )
                connection.connect()

                if (connection.responseCode in 200..299) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        saveBitmapToGallery(context, bitmap, title ?: "scrollsearch")
                    } else {
                        // Fallback to DownloadManager
                        enqueueDownloadManager(context, photoUrl, title)
                    }
                } else {
                    enqueueDownloadManager(context, photoUrl, title)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct download failed, falling back to DownloadManager: ${e.message}")
            enqueueDownloadManager(context, photoUrl, title)
        }
    }

    private fun enqueueDownloadManager(context: Context, photoUrl: String, title: String?) {
        try {
            val fileName = "scrollsearch_${System.currentTimeMillis()}.jpg"
            val request = DownloadManager.Request(Uri.parse(photoUrl)).apply {
                setTitle(title ?: "ScrollSearch Imagem")
                setDescription("Baixando foto em alta qualidade")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "ScrollSearch/$fileName")
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)
            showToast(context, "Download agendado na barra de notificações!")
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed: ${e.message}")
            showToast(context, "Erro ao baixar imagem: ${e.message}")
        }
    }

    private fun saveBase64Image(context: Context, dataUrl: String, title: String?) {
        try {
            val base64Data = dataUrl.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                saveBitmapToGallery(context, bitmap, title ?: "scrollsearch_base64")
            } else {
                showToast(context, "Erro ao decodificar imagem base64")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64: ${e.message}")
            showToast(context, "Erro ao salvar imagem: ${e.message}")
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String) {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScrollSearch")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val scrollSearchDir = File(picturesDir, "ScrollSearch")
                if (!scrollSearchDir.exists()) scrollSearchDir.mkdirs()
                val imageFile = File(scrollSearchDir, fileName)
                outputStream = FileOutputStream(imageFile)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.flush()
                outputStream.close()
                showToast(context, "Foto salva na pasta Imagens/ScrollSearch!")
            } else {
                showToast(context, "Não foi possível abrir destino para salvar")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: ${e.message}")
            showToast(context, "Erro ao salvar na galeria: ${e.message}")
        }
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
