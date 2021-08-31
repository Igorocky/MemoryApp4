"use strict";

const TagsView = ({query,openView,setPageTitle}) => {

    const [allTags, setAllTags] = useState(null)
    const [focusedTagId, setFocusedTagId] = useState(null)
    const [editMode, setEditMode] = useState(false)

    const [openConfirmActionDialog, closeConfirmActionDialog, renderConfirmActionDialog] = useConfirmActionDialog()

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags()
        setAllTags(allTags)
    }, [])

    function iconButton({iconName,onClick}) {
        return RE.IconButton({onClick}, RE.Icon({style:{color:'black'}}, iconName))
    }

    function renderAllTags() {
        if (hasNoValue(allTags)) {
            return 'Loading tags...'
        } else if (allTags.length == 0) {
            return 'There are no tags.'
        } else {
            return RE.table({},
                RE.tbody({},
                    allTags.map(tag =>
                        RE.tr({key:tag.id, onClick: () => setFocusedTagId(tag.id), style:{backgroundColor: focusedTagId === tag.id && !editMode ? 'lightgrey' : undefined}},
                            RE.td({}, renderTag({tag})),
                            RE.td({}, focusedTagId === tag.id && !editMode ? iconButton({iconName:'edit', onClick:() => setEditMode(true)}) : null),
                            RE.td({}, focusedTagId === tag.id && !editMode ? iconButton({iconName:'delete', onClick:() => deleteTag({tag})}) : null),
                        )
                    )
                )
            )
        }
    }

    function renderTag({tag}) {
        if (focusedTagId === tag.id && editMode) {
            return re(UpdateTagCmp,{
                tag,
                onCancel: () => setEditMode(false),
                onSave: async ({name}) => {
                    const res = await be.updateTag({id: tag.id, name})
                    if (!res.err) {
                        if (res.data > 0) {
                            setAllTags(prev => prev.map(t=>t.id!=tag.id?t:{...t,name:name}))
                        }
                        setEditMode(false)
                    } else {
                        showErrorMessage(res.err.msg)
                    }
                },
            })
        } else {
            return tag.name
        }
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

    function deleteTag({tag}) {
        openConfirmActionDialog({
            confirmText: `Confirm deleting tag '${tag.name}'`,
            onCancel: () => {
                closeConfirmActionDialog()
            },
            startActionBtnText: 'Delete',
            startAction: async ({updateInProgressText, onDone}) => {
                const res = await be.deleteTag({id: tag.id})
                if (res.data??0 > 0) {
                    setAllTags(prev => prev.filter(t=>t.id!=tag.id))
                }
                closeConfirmActionDialog()
            },
        })
    }

    return RE.Container.col.top.left({style:{marginTop:'5px'}},{},
        re(CreateNewTagCmp,{
            onSave: async newTag => {
                const resp = await be.saveNewTag(newTag)
                if (resp.err) {
                    await showErrorMessage(resp.err.msg)
                    return {err:true}
                } else {
                    console.log('resp.data', resp.data)
                    setAllTags(prev => [resp.data, ...prev])
                    return {}
                }
            }
        }),
        renderAllTags(),
        renderConfirmActionDialog()
    )
}
