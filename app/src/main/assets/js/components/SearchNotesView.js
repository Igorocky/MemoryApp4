"use strict";

const SearchNotesView = ({query,openView,setPageTitle}) => {

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [editFilterMode, setEditFilterMode] = useState(true)
    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [searchInDeleted, setSearchInDeleted] = useState(false)

    const [foundNotes, setFoundNotes] = useState(null)
    const [focusedNote, setFocusedNote] = useState(null)
    const [editNoteMode, setEditNoteMode] = useState(false)

    const [openConfirmActionDialog, closeConfirmActionDialog, renderConfirmActionDialog] = useConfirmActionDialog()

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags()
        setAllTags(allTags)
    }, [])

    useEffect(() => {
        if (allTags) {
            setAllTagsMap(allTags.reduce((a,t) => ({...a,[t.id]:t}), {}))
        }
    }, [allTags])

    function iconButton({iconName,onClick}) {
        return RE.IconButton({onClick}, RE.Icon({style:{color:'black'}}, iconName))
    }

    function showErrorMessage(msg) {
        return new Promise(resolve => {
            openConfirmActionDialog({
                confirmText: `Error: ${msg}`,
                startActionBtnText: 'OK',
                startAction: ({updateInProgressText,onDone}) => {
                    closeConfirmActionDialog()
                    resolve()
                },
            })
        })
    }

    async function doSearch() {
        setEditFilterMode(false)
        let notesResp = await be.getNotes({
            tagIdsToInclude: tagsToInclude.map(t => t.id),
            tagIdsToExclude: tagsToExclude.map(t => t.id),
            searchInDeleted
        })
        setFoundNotes(notesResp.data)
    }

    function editFilter() {
        setEditFilterMode(true)
    }

    function renderSearchButton() {
        return RE.Button({variant:"contained", color:'primary', disabled:tagsToInclude.length==0, onClick: doSearch}, 'Search')
    }

    function renderFilter() {
        if (editFilterMode) {
            return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
                renderSearchButton(),
                RE.Paper({},
                    re(TagSelector,{
                        allTags: allTags,
                        selectedTags: tagsToInclude,
                        onTagRemoved:tag=>setTagsToInclude(prev=>prev.filter(t=>t.id!=tag.id)),
                        onTagSelected:tag=>setTagsToInclude(prev=>[...prev,tag]),
                        label: 'Include',
                        color:'primary',
                    })
                ),
                RE.Paper({},
                    re(TagSelector,{
                        allTags: allTags,
                        selectedTags: tagsToExclude,
                        onTagRemoved:tag=>setTagsToExclude(prev=>prev.filter(t=>t.id!=tag.id)),
                        onTagSelected:tag=>setTagsToExclude(prev=>[...prev,tag]),
                        label: 'Exclude',
                        color:'secondary',
                    })
                ),
                RE.FormControlLabel({
                    control:RE.Checkbox({
                        checked:searchInDeleted,
                        onChange:() => setSearchInDeleted(prev=>!prev),
                        color:'primary'
                    }),
                    label:'search in deleted'
                }),
                renderSearchButton(),
            )
        } else {
            return RE.Container.row.left.center({},{style: {marginRight:'10px', marginBottom:'5px'}},
                searchInDeleted?RE.Icon({style:{color:'blue',}}, 'delete'):null,
                tagsToInclude.map(tag => RE.Chip({
                    // style: {marginRight:'10px', marginBottom:'5px'},
                    key:tag.id,
                    variant:'outlined',
                    size:'small',
                    label: tag.name,
                    color:'primary',
                })),
                tagsToExclude.map(tag => RE.Chip({
                    // style: {marginRight:'10px', marginBottom:'5px'},
                    key:tag.id,
                    variant:'outlined',
                    size:'small',
                    label: tag.name,
                    color:'secondary',
                })),
                iconButton({iconName:'edit',onClick:editFilter})
            )
        }
    }

    function renderFoundNotes() {
        if (editFilterMode) {
            return
        } else if (hasNoValue(foundNotes)) {
            return 'Searching...'
        } else if (foundNotes.length==0) {
            return 'No notes match search criteria.'
        } else {
            return RE.table({},
                RE.tbody({},
                    foundNotes.map(note =>
                        RE.tr(
                            {
                                key:note.id,
                                onClick: () => setFocusedNote(prev=> prev?.id==note.id?null:note),
                            },
                            RE.td({}, renderNote({note})),
                        )
                    )
                )
            )
        }
    }

    function renderNote({note}) {
        return RE.Paper({},
            RE.Container.row.left.center({},{},
                focusedNote?.id === note.id ? iconButton({iconName:'edit',onClick:e=> {e.stopPropagation();setEditNoteMode(true)}}) : undefined,
                focusedNote?.id === note.id ? iconButton({iconName:'delete',onClick:e=> {e.stopPropagation();deleteNote({})}}) : undefined,
                note.text
            )
        )
    }

    function showErrorMessage(msg) {
        return new Promise(resolve => {
            openConfirmActionDialog({
                confirmText: `Error: ${msg}`,
                startActionBtnText: 'OK',
                startAction: ({updateInProgressText,onDone}) => {
                    closeConfirmActionDialog()
                    resolve()
                },
            })
        })
    }

    async function deleteNote({getNote}) {
        const noteToDelete = getNote ? await getNote() : focusedNote
        if (noteToDelete) {
            let lengthToTrim = 10
            openConfirmActionDialog({
                confirmText: `Confirm deleting note '${noteToDelete.text.substring(0, lengthToTrim)}${noteToDelete.text.length > lengthToTrim ? '...' : ''}'`,
                onCancel: () => {
                    closeConfirmActionDialog()
                },
                startActionBtnText: 'Delete',
                startAction: async ({updateInProgressText, onDone}) => {
                    let res = await be.deleteNote({id: noteToDelete.id})
                    if (res.data ?? 0 > 0) {
                        setFoundNotes(prev => prev.filter(n => n.id != noteToDelete.id))
                        setFocusedNote(null)
                    }
                    closeConfirmActionDialog()
                },
            })
        }
    }

    async function updateNote({newNoteAttrs}) {
        const res = await be.updateNote({id:focusedNote.id,...newNoteAttrs})
        if (!res.err) {
            if (res.data > 0) {
                const updatedNote = {...focusedNote,...newNoteAttrs}
                setFocusedNote(updatedNote)
                setFoundNotes(prev => prev.map(n => n.id != focusedNote.id ? n : {...n, ...newNoteAttrs}))
                setEditNoteMode(false)
                return updatedNote
            } else {
                await showErrorMessage(`Internal error: res.data=${res.data}`)
                return null
            }
        } else {
            await showErrorMessage(res.err.msg)
            return null
        }
    }

    if (hasNoValue(allTags)) {
        return "Loading tags..."
    } else if (editNoteMode && hasValue(focusedNote)) {
        return re(UpdateNoteCmp,{
            allTags,
            allTagsMap,
            note:focusedNote,
            onCancel: () => setEditNoteMode(false),
            onSave: async ({text, tagIds, isDeleted}) => {
                if (isDeleted && !focusedNote.isDeleted) {
                    if (focusedNote.text == text && arraysAreEqualAsSets(focusedNote.tagIds, tagIds)) {
                        setEditNoteMode(false)
                        deleteNote({})
                    } else {
                        deleteNote({getNote: async () => {
                                return await updateNote({newNoteAttrs: {text, tagIds, isDeleted}})
                        }})
                    }
                } else {
                    updateNote({newNoteAttrs: {text, tagIds, isDeleted}})
                }
            }
        })
    } else {
        return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
            renderFilter(),
            renderFoundNotes(),
            renderConfirmActionDialog()
        )
    }
}
