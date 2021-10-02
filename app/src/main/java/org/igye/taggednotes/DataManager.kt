package org.igye.taggednotes

import android.content.Context
import android.database.sqlite.SQLiteStatement
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import org.igye.taggednotes.Utils.isNotEmpty
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class DataManager(
    private val context: Context,
    private val dbName: String? = "tagged-notes-db",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val shareFile: AtomicReference<((Uri) -> Unit)?> = AtomicReference(null)
    private val t = DB_V1
    private val repo: AtomicReference<Repository> = AtomicReference(createNewRepo())
    fun getRepo() = repo.get()

    private val ERR_CREATE_TAG_NAME_EMPTY = 101
    private val ERR_CREATE_TAG_NAME_DUPLICATED = 102
    private val ERR_CREATE_TAG_NEGATIVE_NEW_ID = 103
    private val ERR_CREATE_TAG = 104

    private val ERR_CREATE_NOTE_TEXT_EMPTY = 111
    private val ERR_CREATE_NOTE_NEGATIVE_NEW_ID = 112
    private val ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID = 113
    private val ERR_CREATE_NOTE = 114

    private val ERR_GET_TAGS = 201
    private val ERR_GET_NOTES = 202

    private val ERR_UPDATE_TAG_NAME_EMPTY = 301
    private val ERR_UPDATE_TAG_NAME_DUPLICATED = 302
    private val ERR_UPDATE_TAG = 304

    private val ERR_UPDATE_NOTE = 305
    private val ERR_UPDATE_NOTE_TEXT_EMPTY = 306
    private val ERR_UPDATE_NOTE_CNT_IS_NOT_ONE = 307
    private val ERR_UPDATE_NOTE_TAG_REF_NEGATIVE_NEW_ID = 308

    private val ERR_DELETE_TAG = 401

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))

    data class SaveNewTagArgs(val name:String)
    @BeMethod
    fun saveNewTag(args:SaveNewTagArgs): Deferred<BeRespose<Tag>> = CoroutineScope(ioDispatcher).async {
        val name = args.name.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_EMPTY, msg = "Name of a new tag should not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_CREATE_TAG
            ) {
                val newTag = getRepo().insertTagStmt!!.exec(name = name)
                if (newTag.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_TAG_NEGATIVE_NEW_ID, msg = "newId == -1"))
                } else {
                    BeRespose(data = newTag)
                }
            }
        }
    }

    data class UpdateTagArgs(val id:Long, val name: String)
    @BeMethod
    fun updateTag(args:UpdateTagArgs): Deferred<BeRespose<Int>> = CoroutineScope(ioDispatcher).async {
        val name = args.name.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_EMPTY, msg = "Name of a tag must not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_UPDATE_TAG
            ) {
                BeRespose(data = getRepo().updateTagStmt!!.exec(id=args.id,name=name))
            }
        }
    }

    data class DeleteTagArgs(val id:Long)
    @BeMethod
    fun deleteTag(args:DeleteTagArgs): Deferred<BeRespose<Int>> = CoroutineScope(ioDispatcher).async {
        getRepo().writableDatabase.doInTransaction(errCode = ERR_DELETE_TAG) {
            BeRespose(data = getRepo().deleteTagStmt!!.exec(args.id))
        }
    }

    data class GetTagsArgs(val nameContains:String? = null)
    @BeMethod
    fun getTags(params:GetTagsArgs): Deferred<BeRespose<List<Tag>>> = CoroutineScope(Dispatchers.IO).async {
        val query = StringBuilder()
        val args = ArrayList<String>()
        query.append("select ${t.tags.id}, ${t.tags.createdAt}, ${t.tags.name} from ${t.tags}")
        if (params.nameContains != null) {
            query.append(" where lower(${t.tags.name}) like ?")
            args.add("%${params.nameContains.lowercase()}%")
        }
        query.append(" order by ${t.tags.name}")
        getRepo().readableDatabase.doInTransaction(errCode = ERR_GET_TAGS) {
            BeRespose(data = getRepo().select(
                query = query.toString(),
                args = args.toTypedArray(),
                columnNames = listOf(t.tags.id, t.tags.createdAt, t.tags.name),
                rowMapper = {Tag(
                    id = it.getLong(),
                    createdAt = it.getLong(),
                    name = it.getString(),
                )}
            ).second)
        }
    }

    data class SaveNewNoteArgs(val text:String, val tagIds: List<Long>)
    @BeMethod
    fun saveNewNote(args:SaveNewNoteArgs): Deferred<BeRespose<Note>> = CoroutineScope(ioDispatcher).async {
        if (args.text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_NOTE_TEXT_EMPTY, msg = "Note's content should not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(errCode = ERR_CREATE_NOTE) transaction@{
                val newNote = getRepo().insertNoteStmt!!.exec(args.text)
                if (newNote.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_NOTE_NEGATIVE_NEW_ID, "newId == -1"))
                } else {
                    args.tagIds.forEach { tagId ->
                        if (getRepo().insertNoteToTagStmt!!.exec(newNote.id,tagId) == -1L) {
                            return@transaction BeRespose<Note>(err = BeErr(code = ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                        }
                    }
                    BeRespose(data = newNote.copy(tagIds = args.tagIds))
                }
            }
        }
    }

    data class GetNotesArgs(val tagIdsToInclude: List<Long>? = null, val tagIdsToExclude: List<Long>? = null, val searchInDeleted: Boolean = false, val rowsMax: Long = 100)
    @BeMethod
    fun getNotes(args:GetNotesArgs): Deferred<BeRespose<ListOfItems<Note>>> = CoroutineScope(ioDispatcher).async {
        val query = StringBuilder(
            """select n.${t.notes.id}, 
                max(n.${t.notes.createdAt}) as ${t.notes.createdAt}, 
                max(n.${t.notes.isDeleted}) as ${t.notes.isDeleted}, 
                max(n.${t.notes.text}) as ${t.notes.text},
                (select group_concat(t.${t.noteToTag.tagId}) from ${t.noteToTag} t where t.${t.noteToTag.noteId} = n.${t.notes.id}) as tagIds"""
        )
        val searchFilters = ArrayList<String>()
        if (isNotEmpty(args.tagIdsToInclude)) {
            query.append(" from ${t.noteToTag} nt inner join ${t.notes} n on nt.${t.noteToTag.noteId} = n.${t.notes.id}")
            searchFilters.add(" nt.${t.noteToTag.tagId} in (${args.tagIdsToInclude!!.joinToString(separator = ",")})")
        } else {
            query.append(" from ${t.notes} n")
        }
        if (isNotEmpty(args.tagIdsToExclude)) {
            query.append(" inner join ${t.noteToTag} nte on n.${t.notes.id} = nte.${t.noteToTag.noteId}")
        }
        searchFilters.add("n.${t.notes.isDeleted} ${if(args.searchInDeleted)  "!= 0" else "= 0"}")
        query.append(searchFilters.joinToString(prefix = " where ", separator = " and "))
        query.append(" group by n.${t.notes.id}")
        if (isNotEmpty(args.tagIdsToExclude)) {
            fun excludeTagId(tagId:Long) = " group_concat(':'||nte.${t.noteToTag.tagId}||':') not like '%:'||${tagId}||':%'"
            query.append(" having ${args.tagIdsToExclude?.asSequence()?.map { excludeTagId(it) }?.joinToString(separator = " and ")}")
        }
        getRepo().readableDatabase.doInTransaction(errCode = ERR_GET_NOTES) {
            val (allRowsFetched, result) = getRepo().select(
                query = query.toString(),
                rowsMax = args.rowsMax,
                columnNames = listOf(t.notes.id, t.notes.createdAt, t.notes.isDeleted, t.notes.text, "tagIds"),
                rowMapper = {
                    Note(
                        id = it.getLong(),
                        createdAt = it.getLong(),
                        isDeleted = if (it.getLong() == 0L) false else true,
                        text = it.getString(),
                        tagIds = it.getString().splitToSequence(",").map { it.toLong() }.toList()
                    )
                }
            )
            BeRespose(data = ListOfItems(complete = allRowsFetched, items = result))
        }
    }

    data class UpdateNoteArgs(val id:Long, val text:String? = null, val isDeleted: Boolean? = null, val tagIds: List<Long>? = null)
    @BeMethod
    fun updateNote(params:UpdateNoteArgs): Deferred<BeRespose<Int>> = CoroutineScope(ioDispatcher).async {
        if (params.text == null && params.isDeleted == null && params.tagIds == null) {
            BeRespose(data = 0)
        } else if (params.text != null && params.text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_TEXT_EMPTY, msg = "Content of a note must not be empty."))
        } else {
            getRepo().writableDatabase.doInTransaction(errCode = ERR_UPDATE_NOTE) transaction@{
                if (params.text != null || params.isDeleted != null) {
                    val text = params.text?.trim()
                    val query = StringBuilder("update ${t.notes} set ")
                    val updateParts = ArrayList<String>()
                    val args = ArrayList<(SQLiteStatement,Int) -> Unit>()
                    if (text != null) {
                        updateParts.add("${t.notes.text} = ?")
                        args.add { stmt, idx -> stmt.bindString(idx, text) }
                    }
                    if (params.isDeleted != null) {
                        updateParts.add("${t.notes.isDeleted} = ?")
                        args.add { stmt, idx -> stmt.bindLong(idx, if (params.isDeleted) 1 else 0) }
                    }
                    query.append(updateParts.joinToString(separator = ", "))
                    query.append(" where id = ?")
                    args.add { stmt, idx -> stmt.bindLong(idx, params.id) }
                    val updatedCnt = compileStatement(query.toString()).use { stmt: SQLiteStatement ->
                        args.forEachIndexed { index, binder -> binder(stmt,index+1) }
                        stmt.executeUpdateDelete()
                    }
                    if (updatedCnt != 1) {
                        return@transaction BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_CNT_IS_NOT_ONE, msg = "updatedCnt != 1"))
                    }
                }
                if (params.tagIds != null) {
                    getRepo().deleteNoteToTagStmt!!.exec(params.id)
                    params.tagIds.forEach { tagId ->
                        if (getRepo().insertNoteToTagStmt!!.exec(params.id,tagId) == -1L) {
                            return@transaction BeRespose(err = BeErr(code = ERR_UPDATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                        }
                    }
                }
                BeRespose(data = 1)
            }
        }
    }

    @BeMethod
    fun doBackup(): Deferred<BeRespose<Backup>> = CoroutineScope(ioDispatcher).async {
        try {
            getRepo().close()
            val databasePath: File = context.getDatabasePath(dbName)
            val backupFileName = createBackupFileName(databasePath)
            val backupFile = File(backupDir, backupFileName + ".zip")

            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                val backupZipEntry = ZipEntry(backupFileName)
                zipOut.putNextEntry(backupZipEntry)
                databasePath.inputStream().use { dbData ->
                    dbData.copyTo(zipOut)
                }
                zipOut.closeEntry()
                BeRespose(data = Backup(name = backupFile.name, size = backupFile.length()))
            }
        } finally {
            repo.set(createNewRepo())
        }
    }

    @BeMethod
    fun listAvailableBackups(): Deferred<BeRespose<List<Backup>>> = CoroutineScope(ioDispatcher).async {
        if (!backupDir.exists()) {
            BeRespose(data = emptyList())
        } else {
            BeRespose(
                data = backupDir.listFiles().asSequence()
                    .sortedBy { -it.lastModified() }
                    .map { Backup(name = it.name, size = it.length()) }
                    .toList()
            )
        }
    }

    data class RestoreFromBackupArgs(val backupName:String)
    @BeMethod
    fun restoreFromBackup(args:RestoreFromBackupArgs): Deferred<BeRespose<String>> = CoroutineScope(ioDispatcher).async {
        val databasePath: File = context.getDatabasePath(dbName)
        val backupFile = File(backupDir, args.backupName)
        try {
            getRepo().close()
            val zipFile = ZipFile(backupFile)
            val entries = zipFile.entries()
            val entry = entries.nextElement()
            zipFile.getInputStream(entry).use { inp ->
                FileOutputStream(databasePath).use { out ->
                    inp.copyTo(out)
                }
            }
            BeRespose(data = "The database was restored from the backup ${args.backupName}")
        } finally {
            repo.set(createNewRepo())
        }
    }

    data class DeleteBackupArgs(val backupName:String)
    @BeMethod
    fun deleteBackup(args:DeleteBackupArgs): Deferred<BeRespose<List<Backup>>> = CoroutineScope(ioDispatcher).async {
        File(backupDir, args.backupName).delete()
        listAvailableBackups().await()
    }

    data class ShareBackupArgs(val backupName:String)
    @BeMethod
    fun shareBackup(args:ShareBackupArgs): Deferred<BeRespose<Unit>> = CoroutineScope(ioDispatcher).async {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "org.igye.taggednotes.fileprovider",
            File(backupDir, args.backupName)
        )
        shareFile.get()?.invoke(fileUri)
        BeRespose(data = Unit)
    }

    fun close() = getRepo().close()

    private val backupDir = Utils.getBackupsDir(context)

    private fun createNewRepo() = Repository(context, dbName)

    private fun createBackupFileName(dbPath: File): String {
        return "${dbPath.name}-backup-${dateTimeFormatter.format(Instant.now()).replace(":","-")}"
    }
}