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
        val dm = createInmemoryDataManager()
        assertEquals(0, dm.inTestGetAllTags().size)
        val expectedTagName = "test-tag"

        //when
        runBlocking { dm.saveNewTag(expectedTagName) }

        //then
        val tags = dm.inTestGetAllTags()
        assertEquals(1, tags.size)
        assertEquals(expectedTagName, tags[0].name)
    }

    @Test
    fun saveNewTag_fails_when_trying_to_save_tag_with_same_name() {
        //given
        val dm = createInmemoryDataManager()
        val expectedTagName = "test-tag"
        runBlocking { dm.saveNewTag(expectedTagName) }

        //when
        val res = runBlocking { dm.saveNewTag(expectedTagName) }

        //then
        assertEquals(102, res.err!!.code)
        assertEquals("'test-tag' tag already exists.", res.err!!.msg)
    }

    @Test
    fun saveNewNote_saves_new_note_with_few_tags() {
        //given
        val dm = createInmemoryDataManager()
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
        val dm = createInmemoryDataManager()
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
        assertEquals(114,resp.err!!.code)
        assertEquals("SQLiteConstraintException FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)",resp.err!!.msg)
        val allNotes = getAllNotes(dm)
        assertEquals(0, allNotes.size)
    }

    @Test
    fun backup_and_restore_work_correctly() {
        //given
        val dm = DataManager(context = appContext, dbName = "test-backup-and-restore")

        dm.inTestGetAllTags().forEach{dm.inTestDeleteTag(it.id)}

        //when: prepare data before backup
        val tag1Id: Long = dm.inTestSaveTag("tag1").id
        val tag2Id: Long = dm.inTestSaveTag("tag2").id
        //then
        dm.inTestGetAllTags().asSequence().map { it.id }.toSet().also { it.contains(tag1Id) }.also { it.contains(tag2Id) }

        //when:do backup
        val backup = runBlocking { dm.doBackup() }.data!!
        //then
        dm.inTestGetAllTags().asSequence().map { it.id }.toSet().also { it.contains(tag1Id) }.also { it.contains(tag2Id) }

        //when:modify data after backup
        val tag3Id: Long = dm.inTestSaveTag("tag3").id
        dm.inTestDeleteTag(tag1Id)
        //then
        dm.inTestGetAllTags().asSequence().map { it.id }.toSet()
            .also { !it.contains(tag1Id) }
            .also { it.contains(tag2Id) }
            .also { it.contains(tag3Id) }

        //when: restore data from the backup
        runBlocking { dm.restoreFromBackup(backup.name) }
        //then
        dm.inTestGetAllTags().asSequence().map { it.id }.toSet()
            .also { it.contains(tag1Id) }
            .also { it.contains(tag2Id) }
            .also { !it.contains(tag3Id) }
    }

    @Test
    fun getTags_returns_all_tags_when_no_filters_are_specified() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1-abc").id
        val tag2Id = dm.inTestSaveTag("tag2-dEf").id
        val tag3Id = dm.inTestSaveTag("tag3-ghi").id

        //when
        val allTags = runBlocking { dm.getTags() }.data!!

        //then
        assertEquals(3, allTags.size)
        val tagIds = allTags.asSequence().map { it.id }.toSet()
        assertTrue(tagIds.contains(tag1Id))
        assertTrue(tagIds.contains(tag2Id))
        assertTrue(tagIds.contains(tag3Id))

    }

    @Test
    fun getTags_returns_only_specified_in_filter_tags() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1-abc").id
        val tag2Id = dm.inTestSaveTag("tag2-dEf").id
        val tag3Id = dm.inTestSaveTag("tag3-ghi").id

        //when
        val allTags = runBlocking { dm.getTags(nameContains = "De") }.data!!

        //then
        assertEquals(1, allTags.size)
        assertEquals(tag2Id, allTags[0].id)
    }

    private fun createInmemoryDataManager() = DataManager(context = appContext, dbName = null)

    private inline fun DataManager.inTestSaveTag(name:String): Tag = runBlocking { saveNewTag(name) }.data!!
    private inline fun DataManager.inTestGetAllTags(): List<Tag> = runBlocking{ getTags() }.data!!
    private inline fun DataManager.inTestDeleteTag(id:Long) = runBlocking { deleteTag(id) }

    private fun getAllNotes(dm:DataManager): List<Note> {
        val repo = dm.getRepo()
        return repo.select(
            query = "select ${repo.t.notes.id}, ${repo.t.notes.createdAt}, ${repo.t.notes.text} from ${repo.t.notes}",
            columnNames = listOf(repo.t.notes.id, repo.t.notes.createdAt, repo.t.notes.text),
            rowMapper = {Note(
                id = it.getLong(),
                createdAt = it.getLong(),
                text = it.getString(),
                tagIds = emptyList()
            )}
        ).second
    }

    private fun getAllTags(dm:DataManager): List<Tag> {
        val repo = dm.getRepo()
        return repo.select(
            query = "select ${repo.t.tags.id}, ${repo.t.tags.createdAt}, ${repo.t.tags.name} from ${repo.t.tags}",
            columnNames = listOf(repo.t.tags.id, repo.t.tags.createdAt, repo.t.tags.name),
            rowMapper = {Tag(
                id = it.getLong(),
                createdAt = it.getLong(),
                name = it.getString()
            )}
        ).second
    }
}