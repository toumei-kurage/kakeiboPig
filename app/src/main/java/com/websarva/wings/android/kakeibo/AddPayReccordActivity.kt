package com.websarva.wings.android.kakeibo

import BaseActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import com.websarva.wings.android.kakeibo.room.AddPayRecordViewModel

class AddPayReccordActivity :
    BaseActivity(R.layout.activity_add_pay_reccord, R.string.title_add_pay_record) {
    private lateinit var viewModel: AddPayRecordViewModel

    private lateinit var validate:ValidateHelper

    private lateinit var spPerson: Spinner
    private lateinit var spPayList: Spinner
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDone: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pay_reccord)

        setupDrawerAndToolbar()

        //画面部品の取得
        spPerson = findViewById(R.id.spPerson)
        spPayList = findViewById(R.id.spPayList)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDone = findViewById(R.id.PayDone)

        // ViewModelのセットアップ
        viewModel = ViewModelProvider(this).get(AddPayRecordViewModel::class.java)

        // Personデータを取得しSpinnerにセット
        viewModel.getPersons(userID).observe(this) { persons ->
            val personNames = persons.map { it.memberName } // Personから名前のリストを作成
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPerson.adapter = adapter
        }
    }
}