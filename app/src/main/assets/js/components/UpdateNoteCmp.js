"use strict";

const UpdateNoteCmp = ({allTags,allTagsMap,note,onSave,onCancel}) => {
    const [text, setText] = useState(note.text)
    const [tags, setTags] = useState(note.tagIds.map(id=>allTagsMap[id]))

    function save() {
        onSave({text, tagIds: tags.map(t=>t.id)})
    }

    function renderSaveButton() {
        return RE.Button({variant:'contained', color:'primary', disabled:text.trim().length==0, onClick: save}, 'Save')
    }

    function renderCancelButton() {
        return RE.Button({variant:'contained', color:'default', onClick: onCancel}, 'Cancel')
    }

    function renderButtons() {
        return RE.Container.row.left.center({},{style:{margin:'3px'}},
            renderCancelButton(),
            renderSaveButton()
        )
    }

    return RE.Container.col.top.left({},{},
        renderButtons(),
        RE.TextField({
            value:text,
            variant:'outlined',
            autoFocus:true,
            multiline: true,
            maxRows: 10,
            size:'small',
            onChange: event => {
                const newText = event.nativeEvent.target.value.trim()
                if (newText != text) {
                    setText(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == 27 ? onCancel() : null,
        }),
        RE.Paper({},
            re(TagSelector,{
                allTags,
                selectedTags: tags,
                onTagRemoved:tag=>setTags(prev=>prev.filter(t=>t.id!=tag.id)),
                onTagSelected:tag=>setTags(prev=>[...prev,tag]),
                label: 'Tag',
                color:'primary',
            })
        ),
        renderButtons(),
    )
}
