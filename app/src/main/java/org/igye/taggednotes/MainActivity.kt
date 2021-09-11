package org.igye.taggednotes

import android.os.Bundle
import androidx.activity.viewModels

class MainActivity : WebViewActivity<MainActivityViewModel>() {
    override val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.debug("Starting")
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(viewModel.getWebView(applicationContext))
    }
}

