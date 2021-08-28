package org.igye.memoryapp4

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DataManager(private val context: Context) {
    private val dbName = "memory-app-4-db"
    private val repo = Repository(context, dbName)
    private val t = DB_V1


    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val cursor = repo.writableDatabase.rawQuery(
            "select ${t.notes.id}, ${t.notes.text} from ${t.notes.tableName}",
            null
        )
        val result = ArrayList<Note>()
        if (cursor.moveToFirst()) {
            val idColumnIndex = cursor.getColumnIndex(t.notes.id)
            val textColumnIndex = cursor.getColumnIndex(t.notes.text)
            while (!cursor.isAfterLast()) {
                result.add(Note(
                    id = cursor.getInt(idColumnIndex),
                    text = cursor.getString(textColumnIndex),
                ))
            }
        }
        cursor.close()
        result
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