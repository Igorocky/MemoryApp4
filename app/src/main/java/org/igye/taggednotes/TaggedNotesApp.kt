package org.igye.taggednotes

import android.app.Application

class TaggedNotesApp: Application() {
    val appContainer by lazy { AppContainer(context = applicationContext) }
}