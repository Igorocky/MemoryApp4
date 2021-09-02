'use strict';

const TagSelector = ({allTags, selectedTags, onTagSelected, onTagRemoved, label, color}) => {

    const [filterText, setFilterText] = useState('')
    let selectedTagIds = selectedTags.map(t=>t.id)

    function renderSelectedTags() {
        return RE.Fragment({},
            selectedTags.map(tag => RE.Chip({
                style: {marginRight:'10px', marginBottom:'5px'},
                key:tag.id,
                variant:'outlined',
                size:'small',
                onDelete: () => onTagRemoved(tag),
                label: tag.name,
                color:color??'default',
            }))
        )
    }

    function renderTagFilter() {
        return RE.TextField(
            {
                variant: 'outlined',
                style: {width: 200},
                size: 'small',
                label,
                onChange: event => setFilterText(event.nativeEvent.target.value.trim().toLowerCase()),
                value: filterText
            }
        )
    }

    function renderFilteredTags() {
        let notSelectedTags = allTags.filter(t => !selectedTagIds.includes(t.id))
        let filteredTags = filterText.length == 0 ? notSelectedTags : notSelectedTags.filter(tag => tag.name.toLowerCase().indexOf(filterText) >= 0)
        if (filteredTags.length == 0) {
            return 'No tags match the search criteria'
        } else {
            return RE.Container.row.left.center(
                {style:{maxHeight:'250px', overflow:'auto'}},
                {style:{margin:'3px'}},
                filteredTags.map(tag => RE.Chip({
                    style: {marginRight:'3px'},
                    variant:'outlined',
                    size:'small',
                    onClick: () => {
                        setFilterText('')
                        onTagSelected(tag)
                    },
                    label: tag.name,
                }))
            )
        }
    }

    return RE.Container.col.top.left({},{style:{margin:'3px'}},
        renderSelectedTags(),
        renderTagFilter(),
        renderFilteredTags()
    )
}