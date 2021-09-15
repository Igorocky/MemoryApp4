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
    private lateinit var beFuncs: Map<String, (String) -> String>
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
            beFuncs = Utils.createMethodMap(javascriptInterface)
            webView.addJavascriptInterface(this, "BE")
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
            this.webView = webView
        }
        return this.webView!!
    }

    @JavascriptInterface
    fun invokeBeMethod(cbId:Long, methodName:String, args:String) {
        if (!beFuncs.containsKey(methodName)) {
            returnDtoToFrontend(cbId, BeRespose<Any>(err = BeErr(code = 1000, msg = "backend method '$methodName' was not found")))
        } else {
            callFeCallback(cbId, beFuncs[methodName]!!.invoke(args))
        }
    }

    private fun callFeCallback(callBackId: Long, dtoStr: String) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $dtoStr)")
        }
    }

    private fun returnDtoToFrontend(callBackId: Long, dto: Any) {
        val dtoStr = gson.toJson(dto)
        callFeCallback(callBackId, dtoStr)
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
