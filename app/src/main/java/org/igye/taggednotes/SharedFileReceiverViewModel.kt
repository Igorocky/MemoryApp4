package org.igye.taggednotes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SharedFileReceiverViewModel: WebViewViewModel("SharedFileReceiver") {
    @Volatile lateinit var sharedFileUri: String
    @Volatile lateinit var onClose: () -> Unit
    @Volatile lateinit var appContext: Context

    override fun getWebView(appContext: Context): WebView {
        this.appContext = appContext
        return getWebView(appContext, this)
    }

    @JavascriptInterface
    fun closeSharedFileReceiver(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        onClose()
    }

    @JavascriptInterface
    fun getSharedFileInfo(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.IO) {
        val fileName = getFileName(sharedFileUri)
        callFeCallbackForDto(cbId, BeRespose(data = mapOf(
            "uri" to sharedFileUri,
            "name" to fileName,
            "type" to getFileType(fileName),
        )))
    }

    data class SaveSharedFileArgs(val fileUri: String, val fileType: SharedFileType, val fileName: String)
    @JavascriptInterface
    fun saveSharedFile(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.IO) {
        val fileInfo = gson.fromJson(args, SaveSharedFileArgs::class.java)
        if (fileInfo.fileUri != sharedFileUri) {
            callFeCallbackForDto(cbId, BeRespose<Any>(err = BeErr(code = 1, msg = "fileInfo.uri != sharedFileUri")))
        } else {
            callFeCallbackForDto(cbId, BeRespose(
                data = copyFile(fileUri = fileInfo.fileUri, fileName = fileInfo.fileName, fileType = fileInfo.fileType)
            ))
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
