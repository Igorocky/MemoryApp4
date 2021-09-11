package org.igye.taggednotes

import android.content.Context
import android.webkit.WebView

class SharedFileReceiverViewModel: WebViewViewModel("SharedFileReceiver") {

    override fun getWebView(appContext: Context): WebView {
        return getWebView(appContext, this)
    }

}
