package org.igye.memoryapp4

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import androidx.webkit.WebViewClientCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "MemoryApp4-WebViewActivity"
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Starting WebViewActivity")
        super.onCreate(savedInstanceState)
        // TODO: 8/25/2021 log warning if supportActionBar == null
        supportActionBar?.hide()

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.i(
                    TAG, consoleMessage.message() + " -- From line " +
                            consoleMessage.lineNumber() + " of " + consoleMessage.sourceId()
                )
                return true
            }
        }
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .build()
        webView.webViewClient = LocalContentWebViewClient(assetLoader)
        webView.addJavascriptInterface(viewModel, "BE")

        viewModel.fe = object : FrontEnd {
            override fun invokeJs(callBackId: Int, data: Any) {
                webView.post {
                    webView.loadUrl("javascript:callFeCallback($callBackId, $data)")
                }
            }

        }

        setContentView(webView)
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onDestroy() {
        viewModel.fe=null
        super.onDestroy()
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
