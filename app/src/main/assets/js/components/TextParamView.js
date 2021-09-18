"use strict";

const TextParamView = ({paramName,paramValue,editable = true,onSave,validator}) => {
    const [newParamValue, setNewParamValue] = useState(null)
    const [isInvalidValue, setIsInvalidValue] = useState(false)

    async function save() {
        if (!(validator?.(newParamValue)??true)) {
            setIsInvalidValue(true)
        } else {
            await onSave(newParamValue)
            setNewParamValue(null)
            setIsInvalidValue(false)
        }
    }

    function cancel() {
        setNewParamValue(null)
        setIsInvalidValue(false)
    }

    function isEditMode() {
        return newParamValue != null
    }

    function beginEdit() {
        setNewParamValue(paramValue)
    }

    return RE.Container.row.left.center({},{},
        isEditMode()?RE.IconButton({onClick:cancel}, RE.Icon({style:{color:'black'}}, 'highlight_off')):null,
        RE.TextField({
            label:paramName,
            value:newParamValue??paramValue,
            error:isInvalidValue,
            variant:isEditMode()?'outlined':'standard',
            autoFocus:true,
            size:'small',
            disabled: newParamValue == null,
            onChange: event => setNewParamValue(event.nativeEvent.target.value),
            onKeyUp: event =>
                event.nativeEvent.keyCode == 13 ? save()
                    : event.nativeEvent.keyCode == 27 ? cancel()
                        : null,
        }),
        isEditMode()?RE.IconButton({onClick: save}, RE.Icon({style:{color:'black'}}, 'save')):null,
        (!isEditMode() && editable)?RE.IconButton({onClick: beginEdit}, RE.Icon({style:{color:'black'}}, 'edit')):null
    )
}
