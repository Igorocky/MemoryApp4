package org.igye.memoryapp4

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.igye.memoryapp4.Utils.isNotEmpty
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class DataManager(private val context: Context, private val dbName: String? = "memory-app-db") {
    private val t = DB_V1
    private var repo = createNewRepo()
    fun getRepo() = repo

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

    private val ERR_DELETE_TAG = 401

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))

    suspend fun saveNewTag(nameArg:String): BeRespose<Tag> = withContext(Dispatchers.IO) {
        val name = nameArg.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_EMPTY, msg = "Name of a new tag should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_CREATE_TAG
            ) {
                val newTag = repo.insertTagStmt!!.exec(name = name)
                if (newTag.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_TAG_NEGATIVE_NEW_ID, msg = "newId == -1"))
                } else {
                    BeRespose(data = newTag)
                }
            }
        }
    }

    suspend fun updateTag(id:Long, nameArg:String): BeRespose<Int> = withContext(Dispatchers.IO) {
        val name = nameArg.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_EMPTY, msg = "Name of a tag should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction(
                exceptionHandler = {
                    if (it.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else null
                },
                errCode = ERR_UPDATE_TAG
            ) {
                BeRespose(data = repo.updateTagStmt!!.exec(id=id,name=name))
            }
        }
    }

    suspend fun deleteTag(id:Long): BeRespose<Int> = withContext(Dispatchers.IO) {
        repo.writableDatabase.doInTransaction(errCode = ERR_DELETE_TAG) {
            BeRespose(data = repo.deleteTagStmt!!.exec(id))
        }
    }

    suspend fun getTags(nameContains:String? = null): BeRespose<List<Tag>> = withContext(Dispatchers.IO) {
        val query = StringBuilder()
        val args = ArrayList<String>()
        query.append("select ${t.tags.id}, ${t.tags.createdAt}, ${t.tags.name} from ${t.tags}")
        if (nameContains != null) {
            query.append(" where lower(${t.tags.name}) like ?")
            args.add("%${nameContains.lowercase()}%")
        }
        query.append(" order by ${t.tags.name}")
        repo.readableDatabase.doInTransaction(errCode = ERR_GET_TAGS) {
            BeRespose(data = repo.select(
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

    suspend fun saveNewNote(textArg:String, tagIds: List<Long>): BeRespose<Note> = withContext(Dispatchers.IO) {
        val text = textArg.replace(" ", "")
        if (text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_NOTE_TEXT_EMPTY, msg = "Note's content should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction(errCode = ERR_CREATE_NOTE) transaction@{
                val newNote = repo.insertNoteStmt!!.exec(text)
                if (newNote.id == -1L) {
                    BeRespose(err = BeErr(code = ERR_CREATE_NOTE_NEGATIVE_NEW_ID, "newId == -1"))
                } else {
                    tagIds.forEach { tagId ->
                        if (repo.insertNoteToTagStmt!!.exec(newNote.id,tagId) == -1L) {
                            return@transaction BeRespose<Note>(err = BeErr(code = ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                        }
                    }
                    BeRespose(data = newNote.copy(tagIds = tagIds))
                }
            }
        }
    }

    suspend fun getNotes(
        tagIdsToInclude:List<Long>? = null,
        tagIdsToExclude:List<Long>? = null,
        searchInDeleted:Boolean = false
    ): BeRespose<ListOfItems<Note>> = withContext(Dispatchers.IO) {
        val query = StringBuilder(
            """select n.${t.notes.id}, 
                max(n.${t.notes.createdAt}) as ${t.notes.createdAt}, 
                max(n.${t.notes.isDeleted}) as ${t.notes.isDeleted}, 
                max(n.${t.notes.text}) as ${t.notes.text},
                (select group_concat(t.${t.noteToTag.tagId}) from ${t.noteToTag} t where t.${t.noteToTag.noteId} = n.${t.notes.id}) as tagIds"""
        )
        val searchFilters = ArrayList<String>()
        if (isNotEmpty(tagIdsToInclude)) {
            query.append(" from ${t.noteToTag} nt inner join ${t.notes} n on nt.${t.noteToTag.noteId} = n.${t.notes.id}")
            searchFilters.add(" nt.${t.noteToTag.tagId} in (${tagIdsToInclude!!.joinToString(separator = ",")})")
        } else {
            query.append(" from ${t.notes} n")
            if (isNotEmpty(tagIdsToExclude)) {
                query.append(" inner join ${t.noteToTag} nt on n.${t.notes.id} = nt.${t.noteToTag.noteId}")
            }
        }
        searchFilters.add("n.${t.notes.isDeleted} ${if(searchInDeleted)  "!= 0" else "= 0"}")
        query.append(searchFilters.joinToString(prefix = " where ", separator = " and "))
        query.append(" group by n.${t.notes.id}")
        if (isNotEmpty(tagIdsToExclude)) {
            fun excludeTagId(tagId:Long) = " group_concat(':'||nt.${t.noteToTag.tagId}||':') not like '%:'||nt.${tagId}||':%'"
            query.append(" having ${tagIdsToExclude?.asSequence()?.map { excludeTagId(it) }?.joinToString(separator = " and ")}")
        }
        repo.readableDatabase.doInTransaction(errCode = ERR_GET_NOTES) {
            val (allRowsFetched, result) = repo.select(
                query = query.toString(),
                rowsMax = 100,
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
            BeRespose(data = ListOfItems(partialResult = !allRowsFetched, items = result))
        }
    }

    suspend fun doBackup(): BeRespose<Backup> = withContext(Dispatchers.IO) {
        val databasePath: File = context.getDatabasePath(dbName)
        val backupFileName = createBackupFileName(databasePath)
        val backupPath = File(backupDir, backupFileName)
        try {
            repo.close()
            databasePath.copyTo(
                target = backupPath,
                overwrite = true
            )
            BeRespose(data = Backup(name = backupFileName, size = backupPath.length()))
        } finally {
            repo = createNewRepo()
        }
    }

    suspend fun restoreFromBackup(backupName:String): BeRespose<String> = withContext(Dispatchers.IO) {
        val databasePath: File = context.getDatabasePath(dbName)
        val backupPath = File(backupDir, backupName)
        try {
            repo.close()
            backupPath.copyTo(
                target = databasePath,
                overwrite = true
            )
            BeRespose(data = "The database was restored from the backup $backupName")
        } finally {
            repo = createNewRepo()
        }
    }

    suspend fun listAvailableBackups(): BeRespose<List<Backup>> = withContext(Dispatchers.IO) {
        BeRespose(data = backupDir.listFiles().map { Backup(name = it.name, size = it.length()) })
    }

    suspend fun removeBackup(backupName:String): BeRespose<List<Backup>> = withContext(Dispatchers.IO) {
        File(backupDir, backupName).delete()
        listAvailableBackups()
    }

    fun close() = repo.close()

    suspend fun debug() {
        doBackup()
    }

    private val backupDir = File(context.getExternalFilesDir(null)!!.absolutePath + "/backup")

    private fun createNewRepo() = Repository(context, dbName)

    private fun createBackupFileName(dbPath: File): String {
        return "${dbPath.name}-backup-${dateTimeFormatter.format(Instant.now()).replace(":","-")}"
    }
}