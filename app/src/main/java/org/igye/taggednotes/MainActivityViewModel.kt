package org.igye.taggednotes

import android.content.Context

class MainActivityViewModel(
    appContext: Context,
    dataManager: DataManager,
    httpsServerManager: HttpsServerManager,
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager, httpsServerManager),
    rootReactComponent = "ViewSelector"
)
