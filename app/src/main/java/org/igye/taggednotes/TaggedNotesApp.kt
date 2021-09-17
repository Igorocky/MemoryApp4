package org.igye.taggednotes

import android.app.Application

class TaggedNotesApp: Application() {
    private val log = LoggerImpl("TaggedNotesApp")
    val appContainer by lazy { AppContainer(context = applicationContext) }

    override fun onTerminate() {
        log.debug("Terminating.")
        appContainer.dataManager.close()
        super.onTerminate()
    }
}