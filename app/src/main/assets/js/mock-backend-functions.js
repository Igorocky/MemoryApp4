'use strict';

function okResponse(data) {
    return {data}
}

function errResponse(errCode, msg) {
    return {err: {code:errCode,msg}}
}

function promisifyBeFunc(func) {
    return function (...args) {
        return new Promise((resolve,reject) => {
            try {
                resolve(func(...args))
            } catch (ex) {
                reject(ex)
            }
        })
    }
}

const TAGS = []
const NOTES = []
const NOTES_TO_TAGS = []

function saveNewTag({name}) {
    if (TAGS.find(t=>t.name==name)) {
        return errResponse(1,`'${name}' tag already exists.`)
    } else {
        const id = (TAGS.map(t=>t.id).max()??0)+1
        const newTag = {id,name,createdAt:new Date().getTime()}
        TAGS.push(newTag)
        return okResponse(newTag)
    }
}

function getAllTags() {
    return okResponse(TAGS.map(t => ({...t})))
}

function updateTag({id,name}) {
    const tagsToUpdate = TAGS.filter(t=>t.id==id)
    for (const tag of tagsToUpdate) {
        if (TAGS.find(t=> t.name==name && t.id != id)) {
            return errResponse(1, `'${name}' tag already exists.`)
        } else {
            tag.name = name
        }
    }
    return okResponse(tagsToUpdate.length)
}

function deleteTag({id}) {
    // return errResponse(2,'Error while deleting a tag.')
    return okResponse(removeIf(TAGS,t => t.id==id))
}

function saveNewNote({text, tagIds}) {
    const id = (NOTES.map(n=>n.id).max()??0)+1
    const newNote = {id,text,createdAt:new Date().getTime()}
    NOTES.push(newNote)
    for (let tagId of tagIds) {
        NOTES_TO_TAGS.push({noteId:id,tagId})
    }
    return okResponse(newNote)
}

function getNotes({tagIdsToInclude=[],tagIdsToExclude=[],searchInDeleted = false}) {
    function getAllTagIdsOfNote({noteId}) {
        return NOTES_TO_TAGS
            .filter(({noteId:id,tagId})=>noteId==id)
            .map(({tagId})=>tagId)
    }
    function hasTags({noteId,tagIds,atLeastOne = false}) {
        let noteTagIds = getAllTagIdsOfNote({noteId})
        if (atLeastOne) {
            return hasValue(tagIds.find(id => noteTagIds.includes(id)))
        } else {
            return tagIds.length && tagIds.every(id => noteTagIds.includes(id))
        }
    }
    let result = NOTES
        .filter(note => searchInDeleted && note.isDeleted || !searchInDeleted && !note.isDeleted)
        .filter(note => hasTags({noteId:note.id,tagIds:tagIdsToInclude}))
        .filter(note => !hasTags({noteId:note.id, tagIds:tagIdsToExclude, atLeastOne:true}))
        .map(note => ({...note, tagIds:getAllTagIdsOfNote({noteId:note.id})}))
    return okResponse(result)
}

function updateNote({id,text,tagIds,isDeleted}) {
    const notesToUpdate = NOTES.filter(n=>n.id==id)
    for (const note of notesToUpdate) {
        if (hasValue(text)) {
            note.text = text
        }
        if (hasValue(tagIds)) {
            removeIf(NOTES_TO_TAGS, ({noteId}) => noteId == id)
            for (let tagId of tagIds) {
                NOTES_TO_TAGS.push({noteId:note.id,tagId})
            }
        }
        if (hasValue(isDeleted)) {
            note.isDeleted = isDeleted
        }
    }
    return okResponse(notesToUpdate.length)
}

function createBeFunctions(...funcs) {
    return funcs.reduce((a,e) => ({...a,[e.name]:promisifyBeFunc(e)}), {})
}

function fillDbWithMockData() {
    const numOfTags = 30
    const tags = ints(1,numOfTags)
        .map(i=>randomAlphaNumString({minLength:3,maxLength:5}))
        .map(s=>saveNewTag({name:s}))
        .map(({data:tag}) => tag)

    function getRandomTagIds() {
        let numOfTags = randomInt(1,5)
        let result = []
        while (result.length < numOfTags) {
            let newId = tags[randomInt(0,tags.length-1)].id
            if (!result.includes(newId)) {
                result.push(newId)
            }
        }
        return result
    }

    const numOfNotes = 500
    const notes = ints(1,numOfNotes)
        .map(i=>randomSentence({}))
        .map(s=>saveNewNote({text:s, tagIds:getRandomTagIds()}))
        .map(({data:note}) => note)
}
fillDbWithMockData()
// console.log('TAGS', TAGS)
// console.log('NOTES', NOTES)
// console.log('NOTES_TO_TAGS', NOTES_TO_TAGS)

const be = {
    saveNewTag: promisifyBeFunc(saveNewTag),
    getAllTags: promisifyBeFunc(getAllTags),
    deleteTag: promisifyBeFunc(deleteTag),
    updateTag: promisifyBeFunc(updateTag),
    saveNewNote: promisifyBeFunc(saveNewNote),
    getNotes: promisifyBeFunc(getNotes),
    updateNote: promisifyBeFunc(updateNote),
}