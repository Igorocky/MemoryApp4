"use strict";

const UpdateTagCmp = ({tag,onSave,onCancel}) => {
    const [tagName, setTagName] = useState(tag.name)

    return RE.Container.row.left.center({},{},
        RE.IconButton({onClick:onCancel},
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
                onClick: () => onSave({name: tagName})
            },
            RE.Icon({style:{color:'black'}}, 'save')
        )
    )
}
