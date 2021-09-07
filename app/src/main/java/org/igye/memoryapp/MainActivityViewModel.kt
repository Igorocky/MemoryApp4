package org.igye.memoryapp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {
    private var webView: WebView? = null
    private var dataManager: DataManager? = null
    private val gson = Gson()

    fun getWebView(appContext: Context?): WebView {
        if (webView == null) {
            val webView = WebView(appContext!!)
            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.i(
                        Constants.LOG_TAG, consoleMessage.message() + " -- From line " +
                                consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                    )
                    return true
                }
            }
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(appContext))
                .build()
            webView.webViewClient = LocalContentWebViewClient(assetLoader)
            webView.addJavascriptInterface(this, "BE")
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            this.webView = webView

            this.dataManager = DataManager(context = appContext)
        }
        return this.webView!!
    }

    override fun onCleared() {
        dataManager?.close()
    }

    @JavascriptInterface
    fun debug(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        dataManager!!.debug()
        callFeCallback(cbId,null)
    }

    data class SaveNewTagArgs(val name:String)
    @JavascriptInterface
    fun saveNewTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newTag = gson.fromJson(args, SaveNewTagArgs::class.java)
        callFeCallbackForDto(cbId,dataManager!!.saveNewTag(newTag.name))
    }

    @JavascriptInterface
    fun getAllTags(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(cbId,dataManager!!.getTags())
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @JavascriptInterface
    fun updateTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val args = gson.fromJson(args, UpdateTagArgs::class.java)
        callFeCallbackForDto(cbId,dataManager!!.updateTag(id = args.id, nameArg = args.name))
    }

    data class DeleteTagArgs(val id:Long)
    @JavascriptInterface
    fun deleteTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(cbId,dataManager!!.deleteTag(id = gson.fromJson(args, DeleteTagArgs::class.java).id))
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @JavascriptInterface
    fun saveNewNote(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newNote = gson.fromJson(args, SaveNewNoteArgs::class.java)
        callFeCallbackForDto(cbId,dataManager!!.saveNewNote(textArg = newNote.text, tagIds = newNote.tagIds))
    }

    data class GetNotesArgs(
        val tagIdsToInclude: List<Long>? = null,
        val tagIdsToExclude: List<Long>? = null,
        val searchInDeleted: Boolean = false
    )
    @JavascriptInterface
    fun getNotes(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val filter = gson.fromJson(args, GetNotesArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager!!.getNotes(
                rowsMax = 100,
                tagIdsToInclude = filter.tagIdsToInclude,
                tagIdsToExclude = filter.tagIdsToExclude,
                searchInDeleted = filter.searchInDeleted
            ).mapData { it.items }
        )
    }

    data class UpdateNoteArgs(
        val id:Long,
        val text:String? = null,
        val isDeleted: Boolean? = null,
        val tagIds: List<Long>? = null
    )
    @JavascriptInterface
    fun updateNote(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newAttrs = gson.fromJson(args, UpdateNoteArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager!!.updateNote(
                noteId = newAttrs.id,
                textArg = newAttrs.text,
                isDeleted = newAttrs.isDeleted,
                tagIds= newAttrs.tagIds
            )
        )
    }

    @JavascriptInterface
    fun doBackup(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(
            cbId,
            dataManager!!.doBackup()
        )
    }

    @JavascriptInterface
    fun listAvailableBackups(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(
            cbId,
            dataManager!!.listAvailableBackups()
        )
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @JavascriptInterface
    fun restoreFromBackup(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, RestoreFromBackupArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager!!.restoreFromBackup(backupName = argsDto.backupName)
        )
    }

    data class DeleteBackupArgs(val backupName:String)
    @JavascriptInterface
    fun deleteBackup(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, DeleteBackupArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager!!.deleteBackup(backupName = argsDto.backupName)
        )
    }

    private fun callFeCallback(callBackId: Int, result: Any?) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $result)")
        }
    }

    private fun callFeCallbackForDto(callBackId: Int, dto: Any) {
        callFeCallback(callBackId, gson.toJson(dto))
    }

}

internal class LocalContentWebViewClient(assetLoader: WebViewAssetLoader) : WebViewClientCompat() {
    private val mAssetLoader: WebViewAssetLoader = assetLoader

    @RequiresApi(21)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return mAssetLoader.shouldInterceptRequest(request.url)
    }

    // to support API < 21
    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
        return mAssetLoader.shouldInterceptRequest(Uri.parse(url))
    }
}