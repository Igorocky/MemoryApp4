package org.igye.taggednotes

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(Constants.LOG_TAG, "Starting WebViewActivity")
        super.onCreate(savedInstanceState)
        // TODO: 8/25/2021 log warning if supportActionBar == null
        supportActionBar?.hide()

        setContentView(viewModel.getWebView(applicationContext))
    }

    override fun onDestroy() {
        val webView = viewModel.getWebView(null)
        (webView.parent as ViewGroup).removeView(webView)
        super.onDestroy()
    }
}

