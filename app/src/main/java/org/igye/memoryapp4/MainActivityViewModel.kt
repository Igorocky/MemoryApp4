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
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {
    var webView: WebView? = null
    private val gson = Gson()

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

    data class AddArgs(val name:String)
    @JavascriptInterface
    fun add(cbId:Int, a:Int, b:String) = viewModelScope.launch(Dispatchers.Default) {
        val res = a + 3 + gson.fromJson(b,AddArgs::class.java).name.length
        Thread.sleep(1000)
        callFeCallback(cbId,res)
    }

    @JavascriptInterface
    fun update(cbId:Int, addArgs:String) = viewModelScope.launch(Dispatchers.Default) {
        val dto = gson.fromJson(addArgs, AddArgs::class.java)
        val res = dto.copy(name = dto.name + "@@@@@@@@@#")
        Thread.sleep(2000)
        callFeCallbackForDto(cbId,res)
    }

    private fun callFeCallback(callBackId: Int, result: Any) {
        webView!!.post {
            webView!!.loadUrl("javascript:callFeCallback($callBackId, $result)")
        }
    }

    private fun callFeCallbackForDto(callBackId: Int, dto: Any) {
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
