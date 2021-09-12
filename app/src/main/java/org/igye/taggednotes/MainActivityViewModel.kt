package org.igye.taggednotes

import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel: WebViewViewModel("ViewSelector") {
    @Volatile var shareFile: ((Uri) -> Unit)? = null
    private lateinit var dataManager: DataManager

    override fun onCleared() {
        log.debug("Clearing")
        dataManager?.close()
        super.onCleared()
    }

    override fun getWebView(appContext: Context): WebView {
        dataManager = DataManager(context = appContext, shareFile = {uri -> shareFile?.invoke(uri)?:Unit})
        return getWebView(appContext, this)
    }

    @JavascriptInterface
    fun debug(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        dataManager.debug()
        callFeCallback(cbId,null)
    }

    data class SaveNewTagArgs(val name:String)
    @JavascriptInterface
    fun saveNewTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newTag = gson.fromJson(args, SaveNewTagArgs::class.java)
        callFeCallbackForDto(cbId,dataManager.saveNewTag(newTag.name))
    }

    @JavascriptInterface
    fun getAllTags(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(cbId,dataManager.getTags())
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @JavascriptInterface
    fun updateTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val args = gson.fromJson(args, UpdateTagArgs::class.java)
        callFeCallbackForDto(cbId,dataManager.updateTag(id = args.id, nameArg = args.name))
    }

    data class DeleteTagArgs(val id:Long)
    @JavascriptInterface
    fun deleteTag(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(cbId,dataManager.deleteTag(id = gson.fromJson(args, DeleteTagArgs::class.java).id))
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @JavascriptInterface
    fun saveNewNote(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newNote = gson.fromJson(args, SaveNewNoteArgs::class.java)
        callFeCallbackForDto(cbId,dataManager.saveNewNote(textArg = newNote.text, tagIds = newNote.tagIds))
    }

    data class GetNotesArgs(val tagIdsToInclude: List<Long>? = null, val tagIdsToExclude: List<Long>? = null, val searchInDeleted: Boolean = false)
    @JavascriptInterface
    fun getNotes(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val filter = gson.fromJson(args, GetNotesArgs::class.java)
        callFeCallbackForDto(
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
    fun updateNote(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val newAttrs = gson.fromJson(args, UpdateNoteArgs::class.java)
        callFeCallbackForDto(
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
    fun doBackup(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(
            cbId,
            dataManager.doBackup()
        )
    }

    @JavascriptInterface
    fun listAvailableBackups(cbId:Int) = viewModelScope.launch(Dispatchers.Default) {
        callFeCallbackForDto(
            cbId,
            dataManager.listAvailableBackups()
        )
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @JavascriptInterface
    fun restoreFromBackup(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, RestoreFromBackupArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager.restoreFromBackup(backupName = argsDto.backupName)
        )
    }

    data class DeleteBackupArgs(val backupName:String)
    @JavascriptInterface
    fun deleteBackup(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, DeleteBackupArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager.deleteBackup(backupName = argsDto.backupName)
        )
    }

    data class ShareBackupArgs(val backupName:String)
    @JavascriptInterface
    fun shareBackup(cbId:Int, args:String) = viewModelScope.launch(Dispatchers.Default) {
        val argsDto = gson.fromJson(args, ShareBackupArgs::class.java)
        callFeCallbackForDto(
            cbId,
            dataManager.shareBackup(backupName = argsDto.backupName)
        )
    }
}
