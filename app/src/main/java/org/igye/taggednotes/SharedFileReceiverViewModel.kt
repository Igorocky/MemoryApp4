package org.igye.taggednotes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SharedFileReceiverViewModel: WebViewViewModel("SharedFileReceiver") {
    @Volatile lateinit var sharedFileUri: String
    @Volatile lateinit var onClose: () -> Unit
    @Volatile lateinit var appContext: Context

    override fun getWebView(appContext: Context): WebView {
        this.appContext = appContext
        return getWebView(appContext, listOf(this))
    }

    @BeMethod
    fun closeSharedFileReceiver(): Deferred<BeRespose<Boolean>> = viewModelScope.async {
        onClose()
        BeRespose(data = true)
    }

    @BeMethod
    fun getSharedFileInfo(): Deferred<BeRespose<Map<String, Any>>> = viewModelScope.async {
        val fileName = getFileName(sharedFileUri)
        BeRespose(data = mapOf(
            "uri" to sharedFileUri,
            "name" to fileName,
            "type" to getFileType(fileName),
        ))
    }

    data class SaveSharedFileArgs(val fileUri: String, val fileType: SharedFileType, val fileName: String)
    @BeMethod
    fun saveSharedFile(args:SaveSharedFileArgs): Deferred<BeRespose<Any>> = viewModelScope.async {
        if (args.fileUri != sharedFileUri) {
            BeRespose<Any>(err = BeErr(code = 1, msg = "fileInfo.uri != sharedFileUri"))
        } else {
            BeRespose(data = copyFile(fileUri = args.fileUri, fileName = args.fileName, fileType = args.fileType))
        }
    }

    private fun getFileName(uri: String): String {
        appContext.contentResolver.query(Uri.parse(uri), null, null, null, null)!!.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
    }

    private fun getFileType(fileName: String): SharedFileType {
        return if (fileName.endsWith(".bks")) SharedFileType.KEYSTORE else SharedFileType.BACKUP
    }

    private fun copyFile(fileUri: String, fileType: SharedFileType, fileName: String): Long {
        val destinationDir = when(fileType) {
            SharedFileType.BACKUP -> Utils.getBackupsDir(appContext)
            SharedFileType.KEYSTORE -> Utils.getKeystoreDir(appContext)
        }
        FileInputStream(appContext.contentResolver.openFileDescriptor(Uri.parse(fileUri), "r")!!.fileDescriptor).use{ inp ->
            FileOutputStream(File(destinationDir, fileName)).use { out ->
                return inp.copyTo(out)
            }
        }
    }

}