package org.igye.taggednotes

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class MainActivityViewModel(
    appContext: Context,
    private val dataManager: DataManager
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager),
    rootReactComponent = "ViewSelector"
) {
    private var httpsServer: HttpsServer? = null

    override fun onCleared() {
        log.debug("Clearing")
        super.onCleared()
    }

    @BeMethod
    fun startHttpServer(): Deferred<BeRespose<Boolean>> = viewModelScope.async {
        httpsServer = HttpsServer(applicationContext = appContext, javascriptInterface = listOf(dataManager))
        BeRespose(data = true)
    }
}
