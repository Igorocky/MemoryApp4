package org.igye.taggednotes

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.gson.Gson

abstract class WebViewViewModel: ViewModel() {
    protected var webView: WebView? = null
    protected val gson = Gson()
    private val log = LoggerImpl(this.javaClass.simpleName)

    abstract fun getWebView(appContext: Context): WebView

    fun detachWebView() {
        if (webView != null) {
            (webView!!.parent as ViewGroup).removeView(webView)
        }
    }

    @SuppressLint("JavascriptInterface")
    protected fun getWebView(appContext: Context, javascriptInterface: Any): WebView {
        if (webView == null) {
            val webView = WebView(appContext)
            webView.settings.javaScriptEnabled = true
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    log.info(
                        consoleMessage.message() + " -- From line " +
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
            webView.addJavascriptInterface(javascriptInterface, "BE")
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            this.webView = webView
        }
        return this.webView!!
    }

    protected fun callFeCallback(callBackId: Int, result: Any?) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $result)")
        }
    }

    protected fun callFeCallbackForDto(callBackId: Int, dto: Any) {
        callFeCallback(callBackId, gson.toJson(dto))
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
