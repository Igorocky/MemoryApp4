"use strict";

const SearchNotesView = ({query,openView,setPageTitle}) => {

    const [allTags, setAllTags] = useState(null)
    const [editFilterMode, setEditFilterMode] = useState(true)
    const [tagsToInclude, setTagsToInclude] = useState([])
    const [tagsToExclude, setTagsToExclude] = useState([])
    const [searchInDeleted, setSearchInDeleted] = useState(false)

    const [openConfirmActionDialog, closeConfirmActionDialog, renderConfirmActionDialog] = useConfirmActionDialog()

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags()
        setAllTags(allTags)
    }, [])

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

    function doSearch() {
        setEditFilterMode(false)
    }

    function editFilter() {
        setEditFilterMode(true)
    }

    function renderFilter() {
        if (editFilterMode) {
            return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
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
                RE.Button({variant:"contained", color:'primary', disabled:tagsToInclude.length==0, onClick: doSearch}, 'Search'),
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

    if (hasNoValue(allTags)) {
        return "Loading tags..."
    } else {
        return RE.Container.col.top.left({style:{marginTop:'5px'}},{style:{marginTop:'5px'}},
            renderFilter(),
            renderConfirmActionDialog()
        )
    }
}
