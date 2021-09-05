package org.igye.memoryapp4

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.time.Instant

class Repository(context: Context, dbName: String?) : SQLiteOpenHelper(context, dbName, null, 1) {
    val t = DB_V1
    override fun onCreate(db: SQLiteDatabase) {
        db.transaction {
            execSQL("""
                    CREATE TABLE ${t.tags} (
                        ${t.tags.id} integer primary key,
                        ${t.tags.createdAt} integer not null,
                        ${t.tags.name} text unique not null
                    )
            """)
            execSQL("""
                    CREATE TABLE ${t.notes} (
                        ${t.notes.id} integer primary key,
                        ${t.notes.createdAt} integer not null,
                        ${t.notes.text} text not null
                    )
            """)
            execSQL("""
                    CREATE TABLE ${t.noteToTag} (
                        ${t.noteToTag.noteId} integer references ${t.notes}(${t.notes.id}) on update restrict on delete restrict,
                        ${t.noteToTag.tagId} integer references ${t.tags}(${t.tags.id}) on update restrict on delete restrict
                    )
            """)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db!!.setForeignKeyConstraintsEnabled(true)
    }

    interface InsertTagStmt {fun exec(name: String): Tag} var insertTagStmt: InsertTagStmt? = null
    var updateTagStmt: ((id:Long, name:String) -> Int)? = null

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        insertTagStmt = object : InsertTagStmt {
            val stmt = db!!.compileStatement("insert into ${t.tags} (${t.tags.createdAt},${t.tags.name}) values (?,?)")
            override fun exec(name: String): Tag {
                val createdAt = Instant.now().toEpochMilli()
                stmt.bindLong(1, createdAt)
                stmt.bindString(2, name)
                return Tag(id = stmt.executeInsert(), createdAt = createdAt, name = name)
            }

        }
        updateTagStmt = object : (Long, String) -> Int {
            private val stmt = db!!.compileStatement("update ${t.tags} set ${t.tags.name} = ? where ${t.tags.id} = ?")
            override fun invoke(id: Long, name: String): Int {
                val createdAt = Instant.now().toEpochMilli()
                stmt.bindString(1, name)
                stmt.bindLong(2, createdAt)
                return stmt.executeUpdateDelete()
            }
        }
    }
}

inline fun <T> SQLiteDatabase.doInTransaction(body: SQLiteDatabase.() -> BeRespose<T>): BeRespose<T> {
    beginTransaction()
    try {
        val result = body()
        if (result.err == null) {
            setTransactionSuccessful()
        }
        return result
    } finally {
        endTransaction()
    }
}

object DB_V1 {
    val tags = TagsTable
    object TagsTable {
        override fun toString() = "TAGS"
        val id = "id"
        val name = "name"
        val createdAt = "createdAt"
    }

    val notes = NotesTable
    object NotesTable {
        override fun toString() = "NOTES"
        val id = "id"
        val text = "text"
        val createdAt = "createdAt"
    }

    val noteToTag = NotesToTagsTable
    object NotesToTagsTable {
        override fun toString() = "NOTES_TO_TAGS"
        val noteId = "note_id"
        val tagId = "tag_id"
    }
}