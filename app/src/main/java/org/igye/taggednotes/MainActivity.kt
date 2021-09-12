package org.igye.taggednotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels

class MainActivity : WebViewActivity<MainActivityViewModel>() {
    override val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.shareFile = { shareFile(it) }
//        viewModel.startHttpServer(-1)
    }

    override fun onDestroy() {
        viewModel.shareFile = null
        super.onDestroy()
    }

    private fun shareFile(fileUri: Uri) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Send to"))
    }
}

