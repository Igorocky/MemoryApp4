package org.igye.memoryapp4

import android.content.Context
import android.database.SQLException
import androidx.core.database.sqlite.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant


class DataManager(private val context: Context) {
    private val dbName = "memory-app-4-db"
    private val repo = Repository(context, dbName)
    private val t = DB_V1

    private val ERR_CREATE_TAG_NAME_EMPTY = 101
    private val ERR_CREATE_TAG_NAME_DUPLICATED = 102
    private val ERR_CREATE_TAG_NEGATIVE_NEW_ID = 103
    private val ERR_CREATE_TAG_SQL_EXCEPTION = 104

    private val ERR_UPDATE_TAG_NAME_EMPTY = 301
    private val ERR_UPDATE_TAG_NAME_DUPLICATED = 302
    private val ERR_UPDATE_TAG_SQL_EXCEPTION = 304

    private val ERR_DELETE_TAG_SQL_EXCEPTION = 401

    suspend fun saveNewTag(nameArg:String): BeRespose<Tag> = withContext(Dispatchers.IO) {
        val name = nameArg.replace(" ", "")
        if (name.isBlank()) {
            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_EMPTY, msg = "Name of a new tag should not be empty."))
        } else {
            val db = repo.writableDatabase
            db.transaction {
                db.compileStatement("""
                    INSERT INTO ${t.tags.tableName} (${t.tags.createdAt},${t.tags.name}) VALUES (?,?)
                """).use { stmt ->
                    val createdAt = Instant.now().toEpochMilli()
                    stmt.bindLong(1, createdAt)
                    stmt.bindString(2, name)
                    try {
                        val newId = stmt.executeInsert()
                        if (newId != -1L) {
                            BeRespose(data = Tag(newId,createdAt,name))
                        } else {
                            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NEGATIVE_NEW_ID, "newId == -1"))
                        }
                    } catch (ex: SQLException) {
                        if (ex.message?.contains("UNIQUE constraint failed: ${t.tags.tableName}.${t.tags.name}")?:false) {
                            BeRespose(err = BeErr(code = ERR_CREATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                        } else {
                            BeRespose(err = BeErr(code = ERR_CREATE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message?:"..."}"))
                        }
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
            val db = repo.writableDatabase
            db.transaction {
                db.compileStatement("""
                    update ${t.tags.tableName} set ${t.tags.name} = ? where ${t.tags.id} = ?
                """).use { stmt ->
                    stmt.bindString(1, name)
                    stmt.bindLong(2, id)
                    try {
                        BeRespose(data = stmt.executeUpdateDelete())
                    } catch (ex: SQLException) {
                        if (ex.message?.contains("UNIQUE constraint failed: ${t.tags.tableName}.${t.tags.name}")?:false) {
                            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_NAME_DUPLICATED, msg = "'${name}' tag already exists."))
                        } else {
                            BeRespose(err = BeErr(code = ERR_UPDATE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message ?: "..."}"))
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteTag(id:Long): BeRespose<Int> = withContext(Dispatchers.IO) {
        val db = repo.writableDatabase
        db.transaction {
            db.compileStatement("""
                delete from ${t.tags.tableName} where ${t.tags.id} = ?
            """).use { stmt ->
                stmt.bindLong(1, id)
                try {
                    val numOfRowsDeleted = stmt.executeUpdateDelete()
                    BeRespose(data = numOfRowsDeleted)
                } catch (ex: SQLException) {
                    BeRespose(err = BeErr(code = ERR_DELETE_TAG_SQL_EXCEPTION, msg = "SQLException: ${ex.message?:"..."}"))
                }
            }
        }
    }

    suspend fun getAllTags(): BeRespose<List<Tag>> = withContext(Dispatchers.IO) {
        val db = repo.readableDatabase
        db.transaction {
            db.rawQuery(
                "select ${t.tags.id}, ${t.tags.createdAt}, ${t.tags.name} from ${t.tags.tableName}",
                null
            ).use { cursor ->
                val result = ArrayList<Tag>()
                if (cursor.moveToFirst()) {
                    val idColumnIndex = cursor.getColumnIndex(t.tags.id)
                    val createdAtColumnIndex = cursor.getColumnIndex(t.tags.createdAt)
                    val nameColumnIndex = cursor.getColumnIndex(t.tags.name)
                    while (!cursor.isAfterLast) {
                        result.add(Tag(
                            id = cursor.getLong(idColumnIndex),
                            createdAt = cursor.getLong(createdAtColumnIndex),
                            name = cursor.getString(nameColumnIndex),
                        ))
                        cursor.moveToNext()
                    }
                }
                BeRespose(data = result)
            }
        }
    }

    suspend fun doBackup() = withContext(Dispatchers.IO) {
        context.getDatabasePath(dbName).copyTo(
            target = File(context.getExternalFilesDir(null)!!.absolutePath + "/$dbName-backup"),
            overwrite = true
        )
    }

    fun close() = repo.close()

    suspend fun debug() {
        doBackup()
    }
}