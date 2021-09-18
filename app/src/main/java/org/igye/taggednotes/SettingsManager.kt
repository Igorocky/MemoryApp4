package org.igye.taggednotes

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class SettingsManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val applicationSettingsFileName = "settings.json"

    fun getApplicationSettings(): Deferred<AppSettings> = CoroutineScope(defaultDispatcher).async {
        Utils.strToObj(
            File(context.filesDir, applicationSettingsFileName).readText(),
            AppSettings::class.java
        )
    }

    fun saveApplicationSettings(appSettings: AppSettings): Deferred<Unit> = CoroutineScope(defaultDispatcher).async {
        FileOutputStream(File(context.filesDir, applicationSettingsFileName)).use {
            it.write(Utils.objToStr(appSettings).toByteArray())
        }
    }

    fun getKeyStorName(): Deferred<String?> = CoroutineScope(defaultDispatcher).async {
        var result: String? = null
        for (keyStor in Utils.getKeystoreDir(context).listFiles()) {
            if (result == null) {
                result = keyStor.name
            } else {
                keyStor.delete()
            }
        }
        result
    }

    @BeMethod
    fun getHttpServerSettings(): Deferred<BeRespose<HttpServerSettings>> = CoroutineScope(defaultDispatcher).async {
        val appSettings = getApplicationSettings().await()
        BeRespose(data = appSettings.httpServerSettings.copy(keyStoreName = getKeyStorName().await()))
    }

    @BeMethod
    fun saveHttpServerSettings(httpServerSettings: HttpServerSettings): Deferred<BeRespose<HttpServerSettings>> = CoroutineScope(defaultDispatcher).async {
        val appSettings = getApplicationSettings().await()
        saveApplicationSettings(appSettings.copy(httpServerSettings = httpServerSettings))
        getHttpServerSettings().await()
    }
}