'use strict';

const be = {
    doBackup: createBeFunction('doBackup'),
    listAvailableBackups: createBeFunction('listAvailableBackups'),
    restoreFromBackup: createBeFunction('restoreFromBackup'),
    deleteBackup: createBeFunction('deleteBackup'),
    shareBackup: createBeFunction('shareBackup'),

    startHttpServer: createBeFunction('startHttpServer'),

    getSharedFileInfo: createBeFunction('getSharedFileInfo'),
    closeSharedFileReceiver: createBeFunction('closeSharedFileReceiver'),
    saveSharedFile: createBeFunction('saveSharedFile'),

    saveNewTag: createBeFunction('saveNewTag'),
    getAllTags: createBeFunction('getAllTags'),
    updateTag: createBeFunction('updateTag'),
    deleteTag: createBeFunction('deleteTag'),
    saveNewNote: createBeFunction('saveNewNote'),
    getNotes: createBeFunction('getNotes'),
    updateNote: createBeFunction('updateNote'),

}

