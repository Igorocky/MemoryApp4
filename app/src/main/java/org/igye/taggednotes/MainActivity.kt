package org.igye.taggednotes

import androidx.activity.viewModels

class MainActivity : WebViewActivity<MainActivityViewModel>() {
    override val viewModel: MainActivityViewModel by viewModels()
}

