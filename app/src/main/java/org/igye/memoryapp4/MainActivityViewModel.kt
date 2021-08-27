package org.igye.memoryapp4

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {
    var webView: WebView? = null

    fun getWebView(appContext: Context?): WebView {
        if (webView == null) {
            val webView = WebView(appContext!!)
            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.i(
                        Constants.LOG_TAG, consoleMessage.message() + " -- From line " +
                                consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                    )
                    return true
                }
            }
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(appContext))
                .build()
            webView.webViewClient = LocalContentWebViewClient(assetLoader)
            webView.addJavascriptInterface(this, "BE")
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            this.webView = webView
        }
        return this.webView!!
    }

    @JavascriptInterface
    fun add(cbId:Int, a:Int) = viewModelScope.launch(Dispatchers.Default) {
        val res = a + 3
        Thread.sleep(3000)
        invokeJs(cbId,res)
    }

    private fun invokeJs(callBackId: Int, data: Any) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $data)")
        }
    }

}

internal class LocalContentWebViewClient(assetLoader: WebViewAssetLoader) : WebViewClientCompat() {
    private val mAssetLoader: WebViewAssetLoader = assetLoader

    @RequiresApi(21)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return mAssetLoader.shouldInterceptRequest(request.url)
    }

    // to support API < 21
    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
        return mAssetLoader.shouldInterceptRequest(Uri.parse(url))
    }
}
