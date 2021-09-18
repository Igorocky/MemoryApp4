"use strict";

const HttpServerView = ({}) => {
    const {renderMessagePopup, showError, showMessage, showMessageWithProgress} = useMessagePopup()
    const [httpSettings, setHttpSettings] = useState(null)

    useEffect(async () => {
        const httpSettings = await be.getHttpServerSettings()
        setHttpSettings(httpSettings.data)
    }, [])

    function startHttpServer() {
        be.startHttpServer()
    }

    async function saveSettings(newSettings) {
        const savedSettings = await be.saveHttpServerSettings({...httpSettings, ...newSettings})
        setHttpSettings(savedSettings.data)
    }

    function renderTextSetting({title, attrName, editable = true,validator}) {
        return re(TextParamView,{
            paramName:title,
            paramValue:httpSettings[attrName],
            onSave: newValue => saveSettings({[attrName]: newValue}),
            editable,
            validator
        })
    }

    // return RE.Button({variant: 'contained', color: 'primary', onClick: startHttpServer}, "start http server")
    if (!httpSettings) {
        return "Loading..."
    } else {
        return RE.Container.col.top.left({},{style: {margin:'10px'}},
            renderTextSetting({title:'Key store file',attrName:'keyStoreName',editable:false}),
            renderTextSetting({title:'Key store password',attrName:'keyStorePassword'}),
            renderTextSetting({title:'Key alias',attrName:'keyAlias'}),
            renderTextSetting({title:'Private key password',attrName:'privateKeyPassword'}),
            renderTextSetting({title:'Port',attrName:'port',validator: str => str.match(/^\d+$/)?true:false}),
            renderTextSetting({title:'Server password',attrName:'serverPassword'}),
        )
    }
}