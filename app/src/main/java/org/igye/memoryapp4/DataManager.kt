package org.igye.memoryapp4

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataManager(context: Context) {
    private val repo = Repository(context)
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
        result
    }

    fun close() = repo.close()
}