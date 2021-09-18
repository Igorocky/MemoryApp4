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
    private val settingsFile = File(context.filesDir, applicationSettingsFileName)

    fun getApplicationSettings(): AppSettings {
        if (!settingsFile.exists()) {
            saveApplicationSettings(AppSettings(httpServerSettings = HttpServerSettings()))
        }
        return Utils.strToObj(settingsFile.readText(), AppSettings::class.java)
    }

    fun saveApplicationSettings(appSettings: AppSettings) {
        FileOutputStream(settingsFile).use {
            it.write(Utils.objToStr(appSettings).toByteArray())
        }
    }

    fun getKeyStorName(): String {
        var result: String? = null
        for (keyStor in Utils.getKeystoreDir(context).listFiles()) {
            if (result == null) {
                result = keyStor.name
            } else {
                keyStor.delete()
            }
        }
        return result?:""
    }

    fun getHttpServerSettings(): HttpServerSettings {
        val appSettings = getApplicationSettings()
        return appSettings.httpServerSettings.copy(keyStoreName = getKeyStorName())
    }

    fun saveHttpServerSettings(httpServerSettings: HttpServerSettings): HttpServerSettings {
        val appSettings = getApplicationSettings()
        saveApplicationSettings(appSettings.copy(httpServerSettings = httpServerSettings))
        return getHttpServerSettings()
    }
}