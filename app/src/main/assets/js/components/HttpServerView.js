"use strict";

const HttpServerView = ({}) => {
    const {renderMessagePopup, showError, showMessage, showMessageWithProgress} = useMessagePopup()

    function startHttpServer() {
        be.startHttpServer()
    }

    return RE.Button({variant: 'contained', color: 'primary', onClick: startHttpServer}, "start http server")
}