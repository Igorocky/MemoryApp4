package org.igye.memoryapp4

import android.webkit.JavascriptInterface
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    var fe: FrontEnd? = null

    @JavascriptInterface
    fun add(a:Int, cbId:Int) {
        val res = a + 3
        Thread.sleep(3000)
        fe?.invokeJs(cbId,res)
    }

}

interface FrontEnd {
    fun invokeJs(callBackId:Int,data:Any)
}