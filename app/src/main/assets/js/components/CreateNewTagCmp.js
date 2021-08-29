"use strict";

const CreateNewTagCmp = ({onSave}) => {
    const [expanded, setExpanded] = useState(false)
    const [tagName, setTagName] = useState('')

    if (expanded) {
        return RE.Container.row.left.center({},{},
            RE.IconButton({onClick:()=>setExpanded(false)},
                RE.Icon({style:{color:'black'}}, 'highlight_off')
            ),
            RE.TextField({
                value:tagName,
                variant:'outlined',
                autoFocus:true,
                size:'small',
                onChange: event => {
                    const newName = event.nativeEvent.target.value.replaceAll(' ', '')
                    if (newName != tagName) {
                        setTagName(newName)
                    }
                }
            }),
            RE.IconButton(
                {
                    onClick: async () => {
                        const result = await onSave({name: tagName})
                        if (!result.err) {
                            setTagName('')
                            setExpanded(false)
                        }
                    }
                },
                RE.Icon({style:{color:'black'}}, 'save')
            )
        )
    } else {
        return RE.IconButton({onClick:()=>setExpanded(true)},
            RE.Icon({style:{color:'black'}}, 'add')
        )
    }
}
