package org.igye.taggednotes

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val log = LoggerImpl(this.javaClass.simpleName)
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.debug("Starting")
        super.onCreate(savedInstanceState)
        // TODO: 8/25/2021 log warning if supportActionBar == null
        supportActionBar?.hide()

        setContentView(viewModel.getWebView(applicationContext))
    }

    override fun onDestroy() {
        viewModel.detachWebView()
        super.onDestroy()
    }
}

