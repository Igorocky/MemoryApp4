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

abstract class WebViewViewModel(private val rootReactComponent: String): ViewModel() {
    protected var webView: WebView? = null
    protected val gson = Gson()
    protected val log = LoggerImpl(this.javaClass.simpleName)

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
                .addPathHandler(
                    "/assets/",
                    CustomAssetsPathHandler(
                        appContext = appContext,
                        rootReactComponent = rootReactComponent,
                        feBeBridge = "js/android-fe-be-bridge.js"
                    )
                )
                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(appContext))
                .build()
            webView.webViewClient = LocalContentWebViewClient(assetLoader)
            webView.addJavascriptInterface(javascriptInterface, "BE")
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            this.webView = webView
        }
        return this.webView!!
    }

    private fun callFeCallback(callBackId: Long, result: Any?) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $result)")
        }
    }

    protected fun <T> returnDtoToFrontend(callBackId: Long, dto: BeRespose<T>): BeRespose<T> {
        if (callBackId >= 0) {
            val dtoStr = gson.toJson(dto)
            callFeCallback(callBackId, dtoStr)
        }
        return dto
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
