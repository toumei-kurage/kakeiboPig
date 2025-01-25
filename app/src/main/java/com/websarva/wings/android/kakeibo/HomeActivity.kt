package com.websarva.wings.android.kakeibo

import android.os.Bundle

class HomeActivity : BaseActivity(R.layout.activity_home,R.string.title_home) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()
    }
}
