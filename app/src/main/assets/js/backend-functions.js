'use strict';

const FE_CALLBACKS = []
let FE_CALLBACK_CNT = 0

function createFeCallback(resultHandler) {
    let id = FE_CALLBACK_CNT++
    FE_CALLBACKS.push({
        id,resultHandler
    })
    return id
}

function callFeCallback(cbId,result) {
    const idx = FE_CALLBACKS.findIndex(cb => cb.id === cbId)
    if (idx >= 0) {
        let callback = FE_CALLBACKS[idx]
        removeAtIdx(FE_CALLBACKS, idx)
        callback.resultHandler(result)
    }
}

function createBePromise(functionName, ...args) {
    return new Promise((resolve, reject) => {
        BE[functionName](createFeCallback(resolve), ...args)
    })
}

function createSingleDtoArgBeFunction(functionName) {
    return async dto => createBePromise(functionName, JSON.stringify(dto))
}

function createBeFunction(functionName) {
    return async function(...args) {
        return createBePromise(functionName, ...args)
    }
}

const be = {
    add: async (a,b) => createBePromise('add', a, JSON.stringify(b)),

    saveNewTag: createSingleDtoArgBeFunction('saveNewTag'),
    getAllTags: createBeFunction('getAllTags'),
    updateTag: createSingleDtoArgBeFunction('updateTag'),
    deleteTag: createSingleDtoArgBeFunction('deleteTag'),
    saveNewNote: createSingleDtoArgBeFunction('saveNewNote'),

    debug: createBeFunction('debug'),
}

