package org.igye.taggednotes

import android.content.Context
import kotlinx.coroutines.*

class HttpServerManager(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @BeMethod
    fun getHttpServerState(): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        BeRespose(data = HttpServerState(
            isRunning = false,
            url = null,
            settings = settingsManager.getHttpServerSettings()
        ))
    }

    @BeMethod
    fun saveHttpServerSettings(httpServerSettings: HttpServerSettings): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        settingsManager.saveHttpServerSettings(httpServerSettings)
        getHttpServerState().await()
    }
}