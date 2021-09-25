package org.igye.taggednotes

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

class HttpsServerManager(
    private val appContext: Context,
    private val javascriptInterface: List<Any>,
    private val settingsManager: SettingsManager,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val self = this
    private val httpsServer: AtomicReference<HttpsServer> = AtomicReference(null)

    @BeMethod
    fun getHttpServerState(): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        val settings = settingsManager.getHttpServerSettings()
        BeRespose(data = HttpServerState(
            isRunning = httpsServer.get() != null,
            url = "https://${Utils.getIpAddress()}:${settings.port}",
            settings = settings
        ))
    }

    @BeMethod
    fun saveHttpServerSettings(httpServerSettings: HttpServerSettings): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        settingsManager.saveHttpServerSettings(httpServerSettings)
        getHttpServerState().await()
    }

    @BeMethod
    fun startHttpServer(): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        val keyStorFile = settingsManager.getKeyStorFile()
        if (keyStorFile == null) {
            BeRespose(err = BeErr(code = 1, msg = "Key store is not defined."))
        } else if (httpsServer.get() != null) {
            BeRespose(err = BeErr(code = 2, msg = "Http server is already running."))
        } else {
            Intent(appContext, HttpsServerService::class.java).also { intent ->
                appContext.startService(intent)
            }
            val serverSettings = settingsManager.getHttpServerSettings()
            try {
                httpsServer.set(HttpsServer(
                    appContext = appContext,
                    keyStoreFile = keyStorFile!!,
                    keyStorePassword = serverSettings.keyStorePassword,
                    keyAlias = serverSettings.keyAlias,
                    privateKeyPassword = serverSettings.privateKeyPassword,
                    portNum = serverSettings.port,
                    serverPassword = serverSettings.serverPassword,
                    javascriptInterface = javascriptInterface + self
                ))
            } catch (ex: Exception) {
                stopHttpServer().await()
                return@async BeRespose(err = BeErr(code = 2, msg = ex.message?:ex.javaClass.name))
            }
            getHttpServerState().await()
        }
    }

    @BeMethod
    fun stopHttpServer(): Deferred<BeRespose<HttpServerState>> = CoroutineScope(defaultDispatcher).async {
        httpsServer.get()?.stop()
        httpsServer.set(null)
        Intent(appContext, HttpsServerService::class.java).also { intent ->
            appContext.stopService(intent)
        }
        getHttpServerState().await()
    }
}