package org.igye.taggednotes

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
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
    @BeMethod
    fun saveNewTag(args:SaveNewTagArgs): Deferred<BeRespose<Tag>> = viewModelScope.async {
        dataManager.saveNewTag(args.name)
    }

    @BeMethod
    fun getAllTags(): Deferred<BeRespose<List<Tag>>> = viewModelScope.async {
        dataManager.getTags()
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @BeMethod
    fun updateTag(cbId:Long, args:UpdateTagArgs): Deferred<BeRespose<Int>> = viewModelScope.async {
        dataManager.updateTag(id = args.id, nameArg = args.name)
    }

    data class DeleteTagArgs(val id:Long)
    @BeMethod
    fun deleteTag(args:DeleteTagArgs): Deferred<BeRespose<Int>> = viewModelScope.async {
        dataManager.deleteTag(id = args.id)
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @BeMethod
    fun saveNewNote(args:SaveNewNoteArgs): Deferred<BeRespose<Note>> = viewModelScope.async {
        dataManager.saveNewNote(textArg = args.text, tagIds = args.tagIds)
    }

    data class GetNotesArgs(val tagIdsToInclude: List<Long>? = null, val tagIdsToExclude: List<Long>? = null, val searchInDeleted: Boolean = false)
    @BeMethod
    fun getNotes(args:GetNotesArgs): Deferred<BeRespose<List<Note>>> = viewModelScope.async {
        dataManager.getNotes(
            rowsMax = 100,
            tagIdsToInclude = args.tagIdsToInclude,
            tagIdsToExclude = args.tagIdsToExclude,
            searchInDeleted = args.searchInDeleted
        ).mapData { it.items }
    }

    data class UpdateNoteArgs(val id:Long, val text:String? = null, val isDeleted: Boolean? = null, val tagIds: List<Long>? = null)
    @BeMethod
    fun updateNote(cbId:Long, args:UpdateNoteArgs): Deferred<BeRespose<Int>> = viewModelScope.async {
        dataManager.updateNote(
            noteId = args.id,
            textArg = args.text,
            isDeleted = args.isDeleted,
            tagIds= args.tagIds
        )
    }

    @BeMethod
    fun doBackup(): Deferred<BeRespose<Backup>> = viewModelScope.async {
        dataManager.doBackup()
    }

    @BeMethod
    fun listAvailableBackups(): Deferred<BeRespose<List<Backup>>> = viewModelScope.async {
        dataManager.listAvailableBackups()
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @BeMethod
    fun restoreFromBackup(args:RestoreFromBackupArgs): Deferred<BeRespose<String>> = viewModelScope.async {
        dataManager.restoreFromBackup(backupName = args.backupName)
    }

    data class DeleteBackupArgs(val backupName:String)
    @BeMethod
    fun deleteBackup(args:DeleteBackupArgs): Deferred<BeRespose<List<Backup>>> = viewModelScope.async {
        dataManager.deleteBackup(backupName = args.backupName)
    }

    data class ShareBackupArgs(val backupName:String)
    @BeMethod
    fun shareBackup(args:ShareBackupArgs): Deferred<BeRespose<Unit>> = viewModelScope.async {
        dataManager.shareBackup(backupName = args.backupName)
    }

    @BeMethod
    fun startHttpServer(): Deferred<BeRespose<Boolean>> = viewModelScope.async {
        httpsServer = HttpsServer(applicationContext = appContext, javascriptInterface = self)
        BeRespose(data = true)
    }
}
