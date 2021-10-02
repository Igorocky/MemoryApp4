"use strict";

const SearchNotesView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, confirmAction, showError} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)
    const [editFilterMode, setEditFilterMode] = useState(true)
    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [searchInDeleted, setSearchInDeleted] = useState(false)

    const [foundNotes, setFoundNotes] = useState(null)
    const [focusedNote, setFocusedNote] = useState(null)
    const [editNoteMode, setEditNoteMode] = useState(false)

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags({})
        setAllTags(allTags)
    }, [])

    useEffect(() => {
        if (allTags) {
            setAllTagsMap(allTags.reduce((a,t) => ({...a,[t.id]:t}), {}))
        }
    }, [allTags])

    async function doSearch() {
        setEditFilterMode(false)
        setFocusedNote(null)
        let notesResp = await be.getNotes({
            tagIdsToInclude: tagsToInclude.map(t => t.id),
            tagIdsToExclude: tagsToExclude.map(t => t.id),
            searchInDeleted
        })
        console.log('notesResp', notesResp)
        if (notesResp.err) {
            await showError(notesResp.err)
            editFilter()
        } else {
            setFoundNotes(notesResp.data.items)
        }
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
                iconButton({iconName:'tune',onClick:editFilter})
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

    function deleteFocusedNote() {
        updateNote({newNoteAttrs:{isDeleted:!focusedNote.isDeleted}})
    }

    function renderNote({note}) {
        return RE.Paper({},
            RE.Container.row.left.center({},{},
                focusedNote?.id === note.id ? iconButton({iconName:'edit',onClick: () => setEditNoteMode(true)}) : undefined,
                focusedNote?.id === note.id ? iconButton({iconName:searchInDeleted?'restore_from_trash':'delete',onClick: deleteFocusedNote}) : undefined,
                note.text
            )
        )
    }

    async function updateNote({newNoteAttrs}) {
        if (hasValue(newNoteAttrs.isDeleted) && xor(newNoteAttrs.isDeleted, focusedNote.isDeleted??false)) {
            const actionName = newNoteAttrs.isDeleted ? 'deleting' : 'restoring'
            const noteText = newNoteAttrs.text??focusedNote.text
            const lengthToTrim = 10
            const trimmedNoteText = noteText.substring(0, lengthToTrim) + (noteText.length > lengthToTrim ? '...' : '')
            if (!await confirmAction({text: `Confirm ${actionName} note '${trimmedNoteText}'`})) {
                return
            }
        }
        const res = await be.updateNote({id:focusedNote.id,...newNoteAttrs})
        if (!res.err) {
            if (res.data > 0) {
                setEditNoteMode(false)
                if (hasValue(newNoteAttrs.isDeleted) && xor(newNoteAttrs.isDeleted, searchInDeleted)) {
                    setFocusedNote(null)
                    setFoundNotes(prev => prev.filter(n => n.id != focusedNote.id))
                } else {
                    setFocusedNote({...focusedNote, ...newNoteAttrs})
                    setFoundNotes(prev => prev.map(n => n.id != focusedNote.id ? n : {...n, ...newNoteAttrs}))
                }
            } else {
                showError({code:-1, msg:`Internal error when updating a note res.data=${res.data}`})
            }
        } else {
            showError(res.err)
        }
    }

    if (hasNoValue(allTags)) {
        return "Loading tags..."
    } else if (editNoteMode && hasValue(focusedNote)) {
        return RE.Fragment({},
            re(UpdateNoteCmp,{
                allTags,
                allTagsMap,
                note:focusedNote,
                onCancel: () => setEditNoteMode(false),
                onSave: newNoteAttrs => updateNote({newNoteAttrs})
            }),
            renderMessagePopup()
        )
    } else {
        return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
            renderFilter(),
            renderFoundNotes(),
            renderMessagePopup()
        )
    }
}
