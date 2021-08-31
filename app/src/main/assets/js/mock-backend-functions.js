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

function createBeFunctions(...funcs) {
    return funcs.reduce((a,e) => ({...a,[e.name]:promisifyBeFunc(e)}), {})
}

function fillDbWithMockData() {
    saveNewTag({name:'todo'})
    saveNewTag({name:'idea'})
    saveNewTag({name:'buy'})
    saveNewTag({name:'qwe'})
    saveNewTag({name:'rty'})
    saveNewTag({name:'uio'})
    saveNewTag({name:'pas'})
    saveNewTag({name:'dfg'})
}
fillDbWithMockData()

const be = {
    saveNewTag: promisifyBeFunc(saveNewTag),
    getAllTags: promisifyBeFunc(getAllTags),
    deleteTag: promisifyBeFunc(deleteTag),
    updateTag: promisifyBeFunc(updateTag),
}