package com.websarva.wings.android.kakeibo

import BaseActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import com.websarva.wings.android.kakeibo.room.AddPayRecordViewModel

class AddPayRecordActivity :
    BaseActivity(R.layout.activity_add_pay_record, R.string.title_add_pay_record) {
    private lateinit var viewModel: AddPayRecordViewModel

    private lateinit var validateHelper: ValidateHelper

    private lateinit var spPerson: Spinner
    private lateinit var spPayList: Spinner
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDone: CheckBox
    private lateinit var buttonPayRecordAdd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pay_record)

        setupDrawerAndToolbar()

        //画面部品の取得
        spPerson = findViewById(R.id.spPerson)
        spPayList = findViewById(R.id.spPayList)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDone = findViewById(R.id.PayDone)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

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