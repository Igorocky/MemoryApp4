package org.igye.taggednotes

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class MainActivityViewModel(private val dataManager: DataManager): WebViewViewModel("ViewSelector") {
    private val self = this
    private lateinit var appContext: Context
    private var httpsServer: HttpsServer? = null

    override fun onCleared() {
        log.debug("Clearing")
        super.onCleared()
    }

    override fun getWebView(appContext: Context): WebView {
        this.appContext = appContext
        return getWebView(appContext, listOf(this, dataManager))
    }

    @BeMethod
    fun startHttpServer(): Deferred<BeRespose<Boolean>> = viewModelScope.async {
        httpsServer = HttpsServer(applicationContext = appContext, javascriptInterface = listOf(dataManager))
        BeRespose(data = true)
    }
}
