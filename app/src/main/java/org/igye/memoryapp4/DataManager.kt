package org.igye.memoryapp4

import android.content.Context
import android.database.SQLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.collections.ArrayList


class DataManager(private val context: Context, private val dbName: String? = "memory-app-db") {
    private val t = DB_V1
    private var repo = createNewRepo()
    fun getRepo() = repo

    private val ERR_CREATE_TAG_NAME_EMPTY = 101
    private val ERR_CREATE_TAG_NAME_DUPLICATED = 102
    private val ERR_CREATE_TAG_NEGATIVE_NEW_ID = 103
    private val ERR_CREATE_TAG_SQL_EXCEPTION = 104

    private val ERR_CREATE_NOTE_TEXT_EMPTY = 111
    private val ERR_CREATE_NOTE_NEGATIVE_NEW_ID = 112
    private val ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID = 113
    private val ERR_CREATE_NOTE_SQL_EXCEPTION = 114

    private val ERR_UPDATE_TAG_NAME_EMPTY = 301
    private val ERR_UPDATE_TAG_NAME_DUPLICATED = 302
    private val ERR_UPDATE_TAG_SQL_EXCEPTION = 304

    private val ERR_DELETE_TAG_SQL_EXCEPTION = 401

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(ZoneId.from(ZoneOffset.UTC))

    suspend fun saveNewTag(nameArg:String): BeRespose<Tag> = withContext(Dispatchers.IO) {
        val name = nameArg.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_EMPTY, msg = "Name of a new tag should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction {
                try {
                    val newTag = repo.insertTagStmt!!.exec(name = name)
                    if (newTag.id == -1L) {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_NEGATIVE_NEW_ID, msg = "newId == -1"))
                    } else {
                        BeRespose(data = newTag)
                    }
                } catch (ex: SQLException) {
                    if (ex.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else {
                        BeRespose(err = BeErr(code = ERR_CREATE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message ?: "..."}"))
                    }
                }
            }
        }
    }

    suspend fun updateTag(id:Long, nameArg:String): BeRespose<Int> = withContext(Dispatchers.IO) {
        val name = nameArg.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_EMPTY, msg = "Name of a tag should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction {
                try {
                    BeRespose(data = repo.updateTagStmt!!.exec(id=id,name=name))
                } catch (ex: SQLException) {
                    if (ex.message?.contains("UNIQUE constraint failed: ${t.tags}.${t.tags.name}") ?: false) {
                        BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                    } else {
                        BeRespose(err = BeErr(code = ERR_UPDATE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message ?: "..."}"))
                    }
                }
            }
        }
    }

    suspend fun deleteTag(id:Long): BeRespose<Int> = withContext(Dispatchers.IO) {
        repo.writableDatabase.doInTransaction {
            try {
                BeRespose(data = repo.deleteTagStmt!!.exec(id))
            } catch (ex: SQLException) {
                BeRespose(err = BeErr(code = ERR_DELETE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message ?: "..."}"))
            }
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

    suspend fun saveNewNote(textArg:String, tagIds: List<Long>): BeRespose<Note> = withContext(Dispatchers.IO) {
        val text = textArg.replace(" ", "")
        if (text.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_NOTE_TEXT_EMPTY, msg = "Note's content should not be empty."))
        } else {
            repo.writableDatabase.doInTransaction transaction@{
                compileStatement(
                    "INSERT INTO ${t.notes} (${t.notes.createdAt},${t.notes.text}) VALUES (?,?)"
                ).use { stmt ->
                    val createdAt = Instant.now().toEpochMilli()
                    stmt.bindLong(1, createdAt)
                    stmt.bindString(2, text)
                    try {
                        val newId = stmt.executeInsert()
                        if (newId == -1L) {
                            BeRespose(err = BeErr(code = ERR_CREATE_NOTE_NEGATIVE_NEW_ID, "newId == -1"))
                        } else {
                            tagIds.forEach { tagId ->
                                compileStatement(
                                    "INSERT INTO ${t.noteToTag} (${t.noteToTag.noteId},${t.noteToTag.tagId}) VALUES (?,?)"
                                ).use {stmt ->
                                    stmt.bindLong(1, newId)
                                    stmt.bindLong(2, tagId)
                                    val executeInsert = stmt.executeInsert()
                                    if (executeInsert == -1L) {
                                        return@transaction BeRespose<Note>(err = BeErr(code = ERR_CREATE_NOTE_TAG_REF_NEGATIVE_NEW_ID, "noteToTag.newId == -1"))
                                    }
                                }
                            }
                            BeRespose(data = Note(id = newId, createdAt = createdAt, text = text, tagIds = tagIds))
                        }
                    } catch (ex: SQLException) {
                        BeRespose(err = BeErr(code = ERR_CREATE_NOTE_SQL_EXCEPTION, msg = "SQLException: ${ex.message ?: "..."}"))
                    }
                }
            }
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