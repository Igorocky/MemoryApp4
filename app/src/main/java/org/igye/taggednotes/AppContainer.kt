package org.igye.taggednotes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppContainer(private val context: Context) {
    val dataManager = DataManager(context = context)
    val settingsManager = SettingsManager(context = context)
    val httpServerManager = HttpServerManager(context = context, settingsManager = settingsManager)

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
                createMainActivityViewModel() as T
            } else if (modelClass.isAssignableFrom(SharedFileReceiverViewModel::class.java)) {
                createSharedFileReceiverViewModel() as T
            } else {
                null as T
            }
        }
    }

    fun createMainActivityViewModel(): MainActivityViewModel {
        return MainActivityViewModel(
            appContext = context,
            dataManager = dataManager,
            settingsManager = settingsManager,
            httpServerManager = httpServerManager
        )
    }

    fun createSharedFileReceiverViewModel(): SharedFileReceiverViewModel {
        return SharedFileReceiverViewModel(
            appContext = context
        )
    }
}