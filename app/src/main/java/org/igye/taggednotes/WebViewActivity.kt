package org.igye.taggednotes

import androidx.appcompat.app.AppCompatActivity

abstract class WebViewActivity<T : WebViewViewModel> : AppCompatActivity() {
    protected val log = LoggerImpl(this.javaClass.simpleName)
    protected abstract val viewModel: T

    override fun onDestroy() {
        log.debug("Destroying")
        viewModel.detachWebView()
        super.onDestroy()
    }
}

