package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import java.util.Calendar

class UpdatePayRecordActivity : BaseActivity(R.layout.activity_update_pay_record, R.string.title_update_pay_record) {
    //画面部品の用意
    private lateinit var spMember: Spinner
    private lateinit var spPayPurposeList: Spinner
    private lateinit var payPurposeListError: TextView
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDateEditText: EditText
    private lateinit var payDateError: TextInputLayout
    private lateinit var payDone: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var buttonPayRecordAdd: Button

    //スピナーで選ばれたものを格納する変数
    private lateinit var selectedMemberName:String
    private lateinit var selectedPayPurposeName:String

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)
    private val databaseHelper = DatabaseHelper(this)

    private var payRecordId: Int = -1

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pay_record)

        setupDrawerAndToolbar()

        //画面部品の取得
        spMember = findViewById(R.id.spPerson)
        spPayPurposeList = findViewById(R.id.spPayList)
        payPurposeListError = findViewById(R.id.PayListError)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDateEditText = findViewById(R.id.payDateEditText)
        payDateError = findViewById(R.id.payDate)
        payDone = findViewById(R.id.PayDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        //前画面からもらった値を取得
        payRecordId = intent.getIntExtra("PAY_RECORD_ID",-1)
        val memberId = intent.getIntExtra("MEMBER_ID",-1)
        val payDate = intent.getStringExtra("PAY_DATE")
        val payPurposeId = intent.getIntExtra("PAY_PURPOSE_ID",-1)
        val payAmount = intent.getIntExtra("PAY_AMOUNT",-1)
        val isReceptChecked = intent.getBooleanExtra("IS_RECEPT_CHECKED",false)
        val note = intent.getStringExtra("NOTE")

        // PayPurposeデータを取得しSpinnerにセット
        val paymentPurposes = databaseHelper.getPaymentPurposesForUser(userID)
        val payPurposeArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentPurposes)
        payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPayPurposeList.adapter = payPurposeArrayAdapter

        //Memberデータを取得しSpinnerにセット
        val member = databaseHelper.getMemberForUser(userID)
        val memberArrayAdapter = ArrayAdapter(this,android.R.layout.simple_spinner_item, member)
        memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMember.adapter = memberArrayAdapter

        //前画面からもらった値をもとに。データをセット
        val memberList = databaseHelper.getMemberForUser(userID)
        val selectedMemberIndex = memberList.indexOf(databaseHelper.getMemberNameById(memberId))
        spMember.setSelection(selectedMemberIndex)

        val payPurposeList = databaseHelper.getPaymentPurposesForUser(userID)
        val selectedPayPurposeIndex = payPurposeList.indexOf(databaseHelper.getPayPurposeNameById(payPurposeId))
        spPayPurposeList.setSelection(selectedPayPurposeIndex)

        payDateEditText.setText(payDate ?: "")
        payAmountEditText.setText(payAmount.toString())
        payDone.isChecked = isReceptChecked
        noteEditText.setText(note ?: "")


        spPayPurposeList.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView:View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedPayPurposeName= parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>){}
        }

        spMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView:View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedMemberName = parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>){}
        }

        payAmountEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val (result, errorMessage) = validateHelper.payAmountCheck(payAmountEditText)
                if (!result) {
                    payAmountError.error = errorMessage
                    return@OnFocusChangeListener
                }
                payAmountError.error = null
            }
        }

        payDateEditText.setOnClickListener {
            clearBordFocus()
            showDatePickerDialog()
        }

        buttonPayRecordAdd.setOnClickListener {
            clearBordFocus()
            val (resultPayAmount, payAmountMessage) = validateHelper.payAmountCheck(
                payAmountEditText
            )
            val (resultPayDate, payDateMessage) = validateHelper.payDateCheck(payDateEditText)
            if (!(resultPayAmount && resultPayDate)) {
                payAmountError.error = payAmountMessage
                payDateError.error = payDateMessage
                return@setOnClickListener
            }
            clearErrorMessage()
            updatePayRecord()
        }
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(payAmountEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spPayPurposeList.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spMember.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDone.windowToken, 0)
        //フォーカスを外す処理
        payAmountEditText.clearFocus()
        spMember.clearFocus()
        spPayPurposeList.clearFocus()
        payDone.clearFocus()
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // 日付を選択したときの処理
                val formattedDate = String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
                payDateEditText.setText(formattedDate) // EditTextに日付を設定

                // OKボタンが押されたときにバリデーションを行う
                val (result, errorMessage) = validateHelper.payDateCheck(payDateEditText)
                if (!result) {
                    payDateError.error = errorMessage
                } else {
                    payDateError.error = null // エラーメッセージをクリア
                }

            }, year, month, day)

        // ダイアログのキャンセルボタンが押されたときの処理
        datePickerDialog.setOnCancelListener {
            // 必要な処理があればここに記述
        }
        datePickerDialog.show()
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

    private fun updatePayRecord() {
        val memberId = databaseHelper.getMemberId(userID,selectedMemberName)
        val payPurposeId = databaseHelper.getPaymentPurposeId(userID,selectedPayPurposeName)
        val db = DatabaseHelper(this).writableDatabase
        val values = ContentValues().apply {
            put("member_id",memberId)
            put("user_id",userID)
            put("purpose_id",payPurposeId)
            put("payment_date",payDateEditText.text.toString())
            put("amount",payAmountEditText.text.toString().toInt())
            put("is_recept_checked",payDone.isChecked)
            put("note",noteEditText.text.toString())
        }
        val rowsAffected = db.update(
            "payment_history",
            values,
            "_id = ?",
            arrayOf(payRecordId.toString())
        )
        if (rowsAffected > 0) {
            Toast.makeText(this, "更新されました", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "更新できませんでした", Toast.LENGTH_SHORT).show()
        }
        db.close()
    }

    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        payDateError.error = null
    }

}