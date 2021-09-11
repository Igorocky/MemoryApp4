"use strict";

const SharedFileReceiver = ({}) => {
    const {renderMessagePopup, showError, showMessage, showMessageWithProgress} = useMessagePopup()

    const BACKUP = 'BACKUP'
    const KEYSTORE = 'KEYSTORE'

    const [fileName, setFileName] = useState(null)
    const [fileUri, setFileUri] = useState(null)
    const [fileType, setFileType] = useState(BACKUP)

    useEffect(async () => {
        const res = await be.getSharedFileInfo()
        if (res.err) {
            await showError(res.err)
            be.closeSharedFileReceiver()
        } else {
            setFileName(res.data.name)
            setFileUri(res.data.uri)
        }
    }, [])

    async function saveFile() {
        const closeProgressWindow = showMessageWithProgress({text: `Saving ${fileType.toLowerCase()} '${fileName}'....`})
        const res = await be.saveSharedFile({fileUri, fileType, fileName})
        closeProgressWindow()
        if (res.err) {
            await showError(res.err)
        } else {
            await showMessage({text:`${fileType.toLowerCase()} '${fileName}' was saved.`})
        }
        be.closeSharedFileReceiver()
    }

    if (hasValue(fileName)) {
        return RE.Container.col.top.left({},{style:{margin:'10px'}},
            `Saving the file '${fileName}'`,
            RE.FormControl({},
                RE.FormLabel({},'Select file type:'),
                RE.RadioGroup(
                    {
                        value: fileType,
                        onChange: event => {
                            const newValue = event.nativeEvent.target.value
                            setFileType(newValue)
                        }
                    },
                    RE.FormControlLabel({label: BACKUP, value: BACKUP, control:RE.Radio({})}),
                    RE.FormControlLabel({label: KEYSTORE, value: KEYSTORE, control:RE.Radio({})}),
                )
            ),
            RE.Button({variant:'contained', color:'primary', onClick: saveFile}, 'Save'),
            renderMessagePopup()
        )
    } else {
        return "Waiting for the file..."
    }
}