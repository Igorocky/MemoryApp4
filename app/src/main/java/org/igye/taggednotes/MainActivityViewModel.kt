package org.igye.taggednotes

import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class MainActivityViewModel: WebViewViewModel("ViewSelector") {
    private val self = this
    @Volatile var shareFile: ((Uri) -> Unit)? = null
    private lateinit var appContext: Context
    private lateinit var dataManager: DataManager
    private var httpsServer: HttpsServer? = null

    override fun onCleared() {
        log.debug("Clearing")
        dataManager?.close()
        super.onCleared()
    }

    override fun getWebView(appContext: Context): WebView {
        this.appContext = appContext
        dataManager = DataManager(context = appContext, shareFile = {uri -> shareFile?.invoke(uri)?:Unit})
        return getWebView(appContext, this)
    }

    data class SaveNewTagArgs(val name:String)
    @JavascriptInterface
    fun saveNewTag(cbId:Long, args:String): Deferred<BeRespose<Tag>> = viewModelScope.async(Dispatchers.Default) {
        val newTag = gson.fromJson(args, SaveNewTagArgs::class.java)
        returnDtoToFrontend(cbId,dataManager.saveNewTag(newTag.name))
    }

    @JavascriptInterface
    fun getAllTags(cbId:Long, args:String): Deferred<BeRespose<List<Tag>>> = viewModelScope.async(Dispatchers.Default) {
        returnDtoToFrontend(cbId,dataManager.getTags())
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @JavascriptInterface
    fun updateTag(cbId:Long, args:String): Deferred<BeRespose<Int>> = viewModelScope.async(Dispatchers.Default) {
        val args = gson.fromJson(args, UpdateTagArgs::class.java)
        returnDtoToFrontend(cbId,dataManager.updateTag(id = args.id, nameArg = args.name))
    }

    data class DeleteTagArgs(val id:Long)
    @JavascriptInterface
    fun deleteTag(cbId:Long, args:String): Deferred<BeRespose<Int>> = viewModelScope.async(Dispatchers.Default) {
        returnDtoToFrontend(cbId,dataManager.deleteTag(id = gson.fromJson(args, DeleteTagArgs::class.java).id))
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @JavascriptInterface
    fun saveNewNote(cbId:Long, args:String): Deferred<BeRespose<Note>> = viewModelScope.async(Dispatchers.Default) {
        val newNote = gson.fromJson(args, SaveNewNoteArgs::class.java)
        returnDtoToFrontend(cbId,dataManager.saveNewNote(textArg = newNote.text, tagIds = newNote.tagIds))
    }

    data class GetNotesArgs(val tagIdsToInclude: List<Long>? = null, val tagIdsToExclude: List<Long>? = null, val searchInDeleted: Boolean = false)
    @JavascriptInterface
    fun getNotes(cbId:Long, args:String): Deferred<BeRespose<List<Note>>> = viewModelScope.async(Dispatchers.Default) {
        val filter = gson.fromJson(args, GetNotesArgs::class.java)
        returnDtoToFrontend(
            cbId,
            dataManager.getNotes(
                rowsMax = 100,
                tagIdsToInclude = filter.tagIdsToInclude,
                tagIdsToExclude = filter.tagIdsToExclude,
                searchInDeleted = filter.searchInDeleted
            ).mapData { it.items }
        )
    }

    data class UpdateNoteArgs(val id:Long, val text:String? = null, val isDeleted: Boolean? = null, val tagIds: List<Long>? = null)
    @JavascriptInterface
    fun updateNote(cbId:Long, args:String): Deferred<BeRespose<Int>> = viewModelScope.async(Dispatchers.Default) {
        val newAttrs = gson.fromJson(args, UpdateNoteArgs::class.java)
        returnDtoToFrontend(
            cbId,
            dataManager.updateNote(
                noteId = newAttrs.id,
                textArg = newAttrs.text,
                isDeleted = newAttrs.isDeleted,
                tagIds= newAttrs.tagIds
            )
        )
    }

    @JavascriptInterface
    fun doBackup(cbId:Long, args:String): Deferred<BeRespose<Backup>> = viewModelScope.async(Dispatchers.Default) {
        returnDtoToFrontend(
            cbId,
            dataManager.doBackup()
        )
    }

    @JavascriptInterface
    fun listAvailableBackups(cbId:Long, args:String): Deferred<BeRespose<List<Backup>>> = viewModelScope.async(Dispatchers.Default) {
        returnDtoToFrontend(
            cbId,
            dataManager.listAvailableBackups()
        )
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @JavascriptInterface
    fun restoreFromBackup(cbId:Long, args:String): Deferred<BeRespose<String>> = viewModelScope.async(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, RestoreFromBackupArgs::class.java)
        returnDtoToFrontend(
            cbId,
            dataManager.restoreFromBackup(backupName = argsDto.backupName)
        )
    }

    data class DeleteBackupArgs(val backupName:String)
    @JavascriptInterface
    fun deleteBackup(cbId:Long, args:String): Deferred<BeRespose<List<Backup>>> = viewModelScope.async(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, DeleteBackupArgs::class.java)
        returnDtoToFrontend(
            cbId,
            dataManager.deleteBackup(backupName = argsDto.backupName)
        )
    }

    data class ShareBackupArgs(val backupName:String)
    @JavascriptInterface
    fun shareBackup(cbId:Long, args:String): Deferred<BeRespose<Unit>> = viewModelScope.async(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, ShareBackupArgs::class.java)
        returnDtoToFrontend(
            cbId,
            dataManager.shareBackup(backupName = argsDto.backupName)
        )
    }

    @JavascriptInterface
    fun startHttpServer(cbId:Long, args:String = ""): Deferred<BeRespose<Boolean>> = viewModelScope.async(Dispatchers.Default) {
        httpsServer = HttpsServer(applicationContext = appContext, javascriptInterface = self)
        returnDtoToFrontend(
            cbId,
            BeRespose(data = true)
        )
    }
}
