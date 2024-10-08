package com.websarva.wings.android.kakeibo

import BaseActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class UpdatePayRecordActivity : BaseActivity(R.layout.activity_update_pay_record,R.string.title_update_pay_record) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_pay_record)

        setupDrawerAndToolbar()
    }
}