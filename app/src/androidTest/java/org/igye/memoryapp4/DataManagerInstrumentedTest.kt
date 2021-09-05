package org.igye.memoryapp4

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataManagerInstrumentedTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.igye.memoryapp4", appContext.packageName)
    }

    @Test
    fun saveNewTag_saves_new_tag() {
        //given
        val dm = DataManager(context = appContext, dbName = null)
        assertEquals(0, (runBlocking{ dm.getAllTags() }.data as List<Tag>).size)
        val expectedTagName = "test-tag"

        //when
        runBlocking { dm.saveNewTag(expectedTagName) }

        //then
        val tags = runBlocking { dm.getAllTags() }.data as List<Tag>
        assertEquals(1, tags.size)
        assertEquals(expectedTagName, tags[0].name)
    }

    @Test
    fun saveNewNote_saves_new_note_with_few_tags() {
        //given
        val dm = DataManager(context = appContext, dbName = null)
        assertEquals(0, getAllTags(dm).size)
        assertEquals(0, getAllNotes(dm).size)
        val expectedNoteText = "text-test-345683462354"
        runBlocking { dm.saveNewTag("111") }
        runBlocking { dm.saveNewTag("222") }
        runBlocking { dm.saveNewTag("333") }
        val allTags = getAllTags(dm)
        assertEquals(3, allTags.size)

        //when
        runBlocking { dm.saveNewNote(textArg = expectedNoteText, tagIds = allTags.map { it.id }) }

        //then
        val allNotes = getAllNotes(dm)
        assertEquals(1, allNotes.size)
        assertEquals(expectedNoteText, allNotes[0].text)
    }

    @Test
    fun saveNewNote_doesnt_save_new_note_when_nonexistent_tag_id_is_provided() {
        //given
        val dm = DataManager(context = appContext, dbName = null)
        assertEquals(0, getAllTags(dm).size)
        assertEquals(0, getAllNotes(dm).size)
        val expectedNoteText = "text-test-345683462354"
        runBlocking { dm.saveNewTag("111") }
        runBlocking { dm.saveNewTag("222") }
        runBlocking { dm.saveNewTag("333") }
        val allTags = getAllTags(dm)
        assertEquals(3, allTags.size)
        val nonExistentTagId = 100L
        assertTrue(nonExistentTagId != allTags[0].id && nonExistentTagId != allTags[1].id && nonExistentTagId != allTags[2].id)

        //when
        val resp: BeRespose<Note> = runBlocking { dm.saveNewNote(textArg = expectedNoteText, tagIds = listOf(allTags[0].id, allTags[1].id, nonExistentTagId)) }

        //then
        assertNotNull(resp.err)
        val allNotes = getAllNotes(dm)
        assertEquals(0, allNotes.size)
    }

    @Test
    fun backup_and_restore_work_correctly() {
        //given
        val dm = DataManager(context = appContext, dbName = "test-backup-and-restore")
        fun getAllTags() = runBlocking{ dm.getAllTags() }.data!!
        fun saveTag(name:String) = runBlocking { dm.saveNewTag(name) }.data!!
        fun deleteTag(id:Long) = runBlocking { dm.deleteTag(id) }

        getAllTags().forEach{deleteTag(it.id)}

        //when: prepare data before backup
        val tag1Id: Long = saveTag("tag1").id
        val tag2Id: Long = saveTag("tag2").id
        //then
        getAllTags().asSequence().map { it.id }.toSet().also { it.contains(tag1Id) }.also { it.contains(tag2Id) }

        //when:do backup
        val backup = runBlocking { dm.doBackup() }.data!!
        //then
        getAllTags().asSequence().map { it.id }.toSet().also { it.contains(tag1Id) }.also { it.contains(tag2Id) }

        //when:modify data after backup
        val tag3Id: Long = saveTag("tag3").id
        deleteTag(tag1Id)
        //then
        getAllTags().asSequence().map { it.id }.toSet()
            .also { !it.contains(tag1Id) }
            .also { it.contains(tag2Id) }
            .also { it.contains(tag3Id) }

        //when: restore data from the backup
        runBlocking { dm.restoreFromBackup(backup.name) }
        //then
        getAllTags().asSequence().map { it.id }.toSet()
            .also { it.contains(tag1Id) }
            .also { it.contains(tag2Id) }
            .also { !it.contains(tag3Id) }
    }

    private fun getAllNotes(dm:DataManager): List<Note> {
        val repo = dm.getRepo()
        return repo.readableDatabase.rawQuery("select ${repo.t.notes.id}, ${repo.t.notes.createdAt}, ${repo.t.notes.text} from ${repo.t.notes}", null).use { cursor ->
            val result = ArrayList<Note>()
            if (cursor.moveToFirst()) {
                val idColumnIndex = cursor.getColumnIndex(repo.t.notes.id)
                val createdAtColumnIndex = cursor.getColumnIndex(repo.t.notes.createdAt)
                val textColumnIndex = cursor.getColumnIndex(repo.t.notes.text)
                while (!cursor.isAfterLast) {
                    result.add(
                        Note(
                            id = cursor.getLong(idColumnIndex),
                            createdAt = cursor.getLong(createdAtColumnIndex),
                            text = cursor.getString(textColumnIndex),
                            tagIds = emptyList()
                        )
                    )
                    cursor.moveToNext()
                }
            }
            result
        }
    }

    private fun getAllTags(dm:DataManager): List<Tag> {
        val repo = dm.getRepo()
        return repo.readableDatabase.rawQuery("select ${repo.t.tags.id}, ${repo.t.tags.createdAt}, ${repo.t.tags.name} from ${repo.t.tags}", null).use { cursor ->
            val result = ArrayList<Tag>()
            if (cursor.moveToFirst()) {
                val idColumnIndex = cursor.getColumnIndex(repo.t.tags.id)
                val createdAtColumnIndex = cursor.getColumnIndex(repo.t.tags.createdAt)
                val nameColumnIndex = cursor.getColumnIndex(repo.t.tags.name)
                while (!cursor.isAfterLast) {
                    result.add(
                        Tag(
                            id = cursor.getLong(idColumnIndex),
                            createdAt = cursor.getLong(createdAtColumnIndex),
                            name = cursor.getString(nameColumnIndex),
                        )
                    )
                    cursor.moveToNext()
                }
            }
            result
        }
    }
}