package org.igye.memoryapp

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
        assertEquals("org.igye.memoryapp.dev", appContext.packageName)
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
        assertEquals(0, dm.inTestGetAllTags().size)
        assertEquals(0, getAllNotes(dm).size)
        val expectedNoteText = "text-test-345683462354"
        runBlocking { dm.saveNewTag("111") }
        runBlocking { dm.saveNewTag("222") }
        runBlocking { dm.saveNewTag("333") }
        val allTags = dm.inTestGetAllTags()
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
        assertEquals(0, dm.inTestGetAllTags().size)
        assertEquals(0, getAllNotes(dm).size)
        val expectedNoteText = "text-test-345683462354"
        runBlocking { dm.saveNewTag("111") }
        runBlocking { dm.saveNewTag("222") }
        runBlocking { dm.saveNewTag("333") }
        val allTags = dm.inTestGetAllTags()
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
    fun saveNewNote_doesnt_save_new_note_when_there_is_duplication_in_tags() {
        //given
        val dm = createInmemoryDataManager()
        assertEquals(0, dm.inTestGetAllTags().size)
        assertEquals(0, getAllNotes(dm).size)
        val expectedNoteText = "text-test-345683462354"
        val tag = dm.inTestSaveTag("111")

        //when
        val resp: BeRespose<Note> = runBlocking { dm.saveNewNote(textArg = expectedNoteText, tagIds = listOf(tag.id,tag.id)) }

        //then
        assertEquals(114,resp.err!!.code)
        assertEquals(
            "SQLiteConstraintException UNIQUE constraint failed: " +
                "NOTES_TO_TAGS.tag_id, NOTES_TO_TAGS.note_id (code 2067 SQLITE_CONSTRAINT_UNIQUE)",
            resp.err!!.msg
        )
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

    @Test
    fun getNotes_returns_all_notes_if_no_filters_were_specified() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id

        //when
        val notesResp = runBlocking { dm.getNotes() }

        //then
        val notes = notesResp.data!!.items
        assertEquals(2, notes.size)
        assertTrue(notesResp.data!!.complete)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.put(note.id, note)
            map
        }
        assertEquals(note1TextExpected, notesMap[note1Id]?.text)
        assertEquals(setOf(tag1Id, tag2Id), notesMap[note1Id]?.tagIds?.toSet())
        assertEquals(note2TextExpected, notesMap[note2Id]?.text)
        assertEquals(setOf(tag2Id, tag3Id), notesMap[note2Id]?.tagIds?.toSet())
    }

    @Test
    fun getNotes_searches_by_tags_to_include_only() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val notesResp = runBlocking { dm.getNotes(tagIdsToInclude = listOf(tag3Id)) }

        //then
        val notes = notesResp.data!!.items
        assertEquals(2, notes.size)
        assertTrue(notesResp.data!!.complete)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.put(note.id, note)
            map
        }
        assertEquals(note2TextExpected, notesMap[note2Id]?.text)
        assertEquals(setOf(tag2Id, tag3Id), notesMap[note2Id]?.tagIds?.toSet())
        assertEquals(note3TextExpected, notesMap[note3Id]?.text)
        assertEquals(setOf(tag3Id, tag4Id), notesMap[note3Id]?.tagIds?.toSet())
    }

    @Test
    fun getNotes_searches_by_tags_to_exclude_only() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val notesResp = runBlocking { dm.getNotes(tagIdsToExclude = listOf(tag1Id,tag4Id)) }

        //then
        val notes = notesResp.data!!.items
        assertEquals(1, notes.size)
        assertTrue(notesResp.data!!.complete)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.put(note.id, note)
            map
        }
        assertEquals(note2TextExpected, notesMap[note2Id]?.text)
        assertEquals(setOf(tag2Id, tag3Id), notesMap[note2Id]?.tagIds?.toSet())
    }

    @Test
    fun getNotes_searches_by_both_tags_to_include_and_exclude() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val tag5Id = dm.inTestSaveTag("tag5").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note4TextExpected = "note4"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id
        val note4Id = runBlocking { dm.saveNewNote(note4TextExpected, listOf(tag4Id, tag5Id)) }.data!!.id

        //when
        val notesResp = runBlocking { dm.getNotes(tagIdsToInclude = listOf(tag3Id), tagIdsToExclude = listOf(tag2Id)) }

        //then
        val notes = notesResp.data!!.items
        assertEquals(1, notes.size)
        assertTrue(notesResp.data!!.complete)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.put(note.id, note)
            map
        }
        assertEquals(note3TextExpected, notesMap[note3Id]?.text)
        assertEquals(setOf(tag3Id, tag4Id), notesMap[note3Id]?.tagIds?.toSet())
    }

    @Test
    fun getNotes_returns_maximum_specified_number_of_items() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        runBlocking { dm.saveNewNote("note1", listOf(tag1Id)) }
        runBlocking { dm.saveNewNote("note1", listOf(tag1Id)) }
        runBlocking { dm.saveNewNote("note1", listOf(tag1Id)) }
        runBlocking { dm.saveNewNote("note1", listOf(tag1Id)) }
        runBlocking { dm.saveNewNote("note1", listOf(tag1Id)) }

        //when
        val notesResp = runBlocking { dm.getNotes(rowsMax = 4) }

        //then
        val notes = notesResp.data!!.items
        assertEquals(4, notes.size)
        assertFalse(notesResp.data!!.complete)
    }

    @Test
    fun updateNote_updates_only_text() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2-updated"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote("note2", listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, textArg = note2TextExpected) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes() }.data!!.items
        assertEquals(3, notes.size)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.also { it.put(note.id, note) }
        }
        var note = notesMap[note1Id]
        assertEquals(note1TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag1Id, tag2Id), note?.tagIds?.toSet())
        note = notesMap[note2Id]
        assertEquals(note2TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag3Id), note?.tagIds?.toSet())
        note = notesMap[note3Id]
        assertEquals(note3TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag3Id, tag4Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_only_tags() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, tagIds = listOf(tag2Id, tag4Id)) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes() }.data!!.items
        assertEquals(3, notes.size)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.also { it.put(note.id, note) }
        }
        var note = notesMap[note1Id]
        assertEquals(note1TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag1Id, tag2Id), note?.tagIds?.toSet())
        note = notesMap[note2Id]
        assertEquals(note2TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag4Id), note?.tagIds?.toSet())
        note = notesMap[note3Id]
        assertEquals(note3TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag3Id, tag4Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_only_isDeleted() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, isDeleted = true) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes(searchInDeleted = true) }.data!!.items
        assertEquals(1, notes.size)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.also { it.put(note.id, note) }
        }
        val note = notes[0]
        assertEquals(note2TextExpected, note?.text)
        assertTrue(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag3Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_text_and_isDeleted() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2-updated"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote("note2", listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, textArg = note2TextExpected, isDeleted = true) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes(searchInDeleted = true) }.data!!.items
        assertEquals(1, notes.size)
        val note = notes[0]
        assertEquals(note2TextExpected, note?.text)
        assertTrue(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag3Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_text_and_tags() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2-updated"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote("note2", listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, textArg = note2TextExpected, tagIds = listOf(tag2Id, tag4Id)) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes() }.data!!.items
        assertEquals(3, notes.size)
        val notesMap = notes.fold(HashMap<Long,Note>()) { map, note ->
            map.also { it.put(note.id, note) }
        }
        var note = notesMap[note1Id]
        assertEquals(note1TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag1Id, tag2Id), note?.tagIds?.toSet())
        note = notesMap[note2Id]
        assertEquals(note2TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag4Id), note?.tagIds?.toSet())
        note = notesMap[note3Id]
        assertEquals(note3TextExpected, note?.text)
        assertFalse(note?.isDeleted!!)
        assertEquals(setOf(tag3Id, tag4Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_isDeleted_and_tags() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote(note2TextExpected, listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, isDeleted = true, tagIds = listOf(tag2Id,tag4Id)) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes(searchInDeleted = true) }.data!!.items
        assertEquals(1, notes.size)
        val note = notes[0]
        assertEquals(note2TextExpected, note?.text)
        assertTrue(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag4Id), note?.tagIds?.toSet())
    }

    @Test
    fun updateNote_updates_text_isDeleted_and_tags() {
        //given
        val dm = createInmemoryDataManager()
        val tag1Id = dm.inTestSaveTag("tag1").id
        val tag2Id = dm.inTestSaveTag("tag2").id
        val tag3Id = dm.inTestSaveTag("tag3").id
        val tag4Id = dm.inTestSaveTag("tag4").id
        val note1TextExpected = "note1"
        val note2TextExpected = "note2-updated"
        val note3TextExpected = "note3"
        val note1Id = runBlocking { dm.saveNewNote(note1TextExpected, listOf(tag1Id, tag2Id)) }.data!!.id
        val note2Id = runBlocking { dm.saveNewNote("note2", listOf(tag2Id, tag3Id)) }.data!!.id
        val note3Id = runBlocking { dm.saveNewNote(note3TextExpected, listOf(tag3Id, tag4Id)) }.data!!.id

        //when
        val updRes = runBlocking { dm.updateNote(noteId = note2Id, textArg = note2TextExpected, isDeleted = true, tagIds = listOf(tag2Id,tag4Id)) }

        //then
        assertNull(updRes.err)
        val notes = runBlocking { dm.getNotes(searchInDeleted = true) }.data!!.items
        assertEquals(1, notes.size)
        val note = notes[0]
        assertEquals(note2TextExpected, note?.text)
        assertTrue(note?.isDeleted!!)
        assertEquals(setOf(tag2Id, tag4Id), note?.tagIds?.toSet())
    }

    private fun createInmemoryDataManager() = DataManager(context = appContext, dbName = null)

    private inline fun DataManager.inTestSaveTag(name:String): Tag = runBlocking { saveNewTag(name) }.data!!
    private inline fun DataManager.inTestGetAllTags(): List<Tag> {
        val repo = getRepo()
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
}