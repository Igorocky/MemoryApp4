package org.igye.memoryapp4

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

class Repository(context: Context) : SQLiteOpenHelper(context, "memory-app-4-db", null, 1) {
    val t = DB_V1
    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            db.execSQL("""
                    CREATE TABLE ${t.notes.tableName} (
                        ${t.notes.id} INTEGER PRIMARY KEY,
                        ${t.notes.text} TEXT
                    )
            """)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}

object DB_V1 {
    val notes = NotesTable
    object NotesTable {
        val tableName = "NOTES"
        val id = "id"
        val text = "text"
    }
}