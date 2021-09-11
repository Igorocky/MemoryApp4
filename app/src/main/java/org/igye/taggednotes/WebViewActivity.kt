package org.igye.taggednotes

import androidx.appcompat.app.AppCompatActivity

abstract class WebViewActivity<T : WebViewViewModel> : AppCompatActivity() {
    private val log = LoggerImpl(this.javaClass.simpleName)
    protected abstract val viewModel: T

    override fun onDestroy() {
        viewModel.detachWebView()
        super.onDestroy()
    }
}

