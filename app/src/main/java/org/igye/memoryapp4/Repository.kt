package org.igye.memoryapp4

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction

class Repository(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 1) {
    val t = DB_V1
    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            db.execSQL("""
                    CREATE TABLE ${t.tags.tableName} (
                        ${t.tags.id} integer primary key,
                        ${t.tags.createdAt} integer not null,
                        ${t.tags.name} text unique not null
                    )
            """)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
}

object DB_V1 {
    val tags = TagsTable
    object TagsTable {
        val tableName = "TAGS"
        val id = "id"
        val name = "name"
        val createdAt = "createdAt"
    }
}