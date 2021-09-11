package org.igye.taggednotes

import androidx.activity.viewModels

class SharedFileReceiverActivity : WebViewActivity<SharedFileReceiverViewModel>() {
    override val viewModel: SharedFileReceiverViewModel by viewModels()
}

