package org.igye.taggednotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : WebViewActivity<MainActivityViewModel>() {
    override val viewModel: MainActivityViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainActivityViewModel(dataManager = (application as TaggedNotesApp).appContainer.dataManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as TaggedNotesApp).appContainer.dataManager.shareFile.set { shareFile(it) }
//        viewModel.startHttpServer(-1)
    }

    override fun onDestroy() {
        (application as TaggedNotesApp).appContainer.dataManager.shareFile.set { null }
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

