package org.igye.taggednotes

import android.content.Context

class MainActivityViewModel(
    appContext: Context,
    dataManager: DataManager,
    httpServerManager: HttpServerManager,
): WebViewViewModel(
    appContext = appContext,
    javascriptInterface = listOf(dataManager, httpServerManager),
    rootReactComponent = "ViewSelector"
)
