package com.websarva.wings.android.kakeibo

import BaseActivity
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import com.websarva.wings.android.kakeibo.room.member.AddPayRecordMemberViewModel
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.payrecord.Payment
import com.websarva.wings.android.kakeibo.room.payrecord.PaymentDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AddPayRecordActivity :
    BaseActivity(R.layout.activity_add_pay_record, R.string.title_add_pay_record) {

    private lateinit var addPayRecordMemberViewModel: AddPayRecordMemberViewModel

    private lateinit var paymentDao: PaymentDao

    private lateinit var validateHelper: ValidateHelper
    private lateinit var dialogHelper: DialogHelper

    private lateinit var spPerson: Spinner
    private lateinit var spPayList: Spinner
    private lateinit var payListError: TextView
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDateEditText: EditText
    private lateinit var payDateError: TextInputLayout
    private lateinit var payDone: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var buttonPayRecordAdd: Button

    private var firstCreate: Boolean = true

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pay_record)

        setupDrawerAndToolbar()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        paymentDao = db.paymentDao() // DAOのインスタンスを取得

        //ライブラリの取得
        validateHelper = ValidateHelper(this)
        dialogHelper = DialogHelper(this)

        //画面部品の取得
        spPerson = findViewById(R.id.spPerson)
        spPayList = findViewById(R.id.spPayList)
        payListError = findViewById(R.id.PayListError)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDateEditText = findViewById(R.id.payDateEditText)
        payDateError = findViewById(R.id.payDate)
        payDone = findViewById(R.id.PayDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        // ViewModelのセットアップ
        addPayRecordMemberViewModel = ViewModelProvider(this)[AddPayRecordMemberViewModel::class.java]


        // Personデータを取得しSpinnerにセット
        addPayRecordMemberViewModel.getPersons(userID).observe(this) { persons ->
            val personNames = persons.map { it.memberName } // Personから名前のリストを作成
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPerson.adapter = adapter
        }

        //バリデーションチェック（フォーカス外れた時の処理）
        spPayList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!firstCreate) {
                    // アイテムが選択されたときの処理
                    val (result, errorMessage) = validateHelper.payListCheck(spPayList)
                    if (!result) {
                        payListError.text = errorMessage
                    } else {
                        payListError.text = null // エラーメッセージをクリア
                    }
                } else {
                    firstCreate = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 何も選択されていない場合の処理（必要に応じて）
            }
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
            val (resultPayList, payListMessage) = validateHelper.payListCheck(spPayList)
            val (resultPayAmount, payAmountMessage) = validateHelper.payAmountCheck(
                payAmountEditText
            )
            val (resultPayDate, payDateMessage) = validateHelper.payDateCheck(payDateEditText)
            if (!(resultPayList && resultPayAmount && resultPayDate)) {
                payListError.text = payListMessage
                payAmountError.error = payAmountMessage
                payDateError.error = payDateMessage
                return@setOnClickListener
            }
            clearErrorMessage()
            // コルーチンを使って非同期にIDを取得
            lifecycleScope.launch {
                val payerId = addPayRecordMemberViewModel.getPersonId(userID, spPerson.selectedItem.toString())
                val purpose = spPayList.selectedItem.toString()
                val paymentDateStr = payDateEditText.text.toString()
                val format = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                val paymentDate = LocalDate.parse(paymentDateStr, format)
                // LocalDateをZonedDateTimeに変換（デフォルトのタイムゾーンを使用）
                val zonedDateTime = paymentDate.atStartOfDay(ZoneId.systemDefault())
                // ZonedDateTimeをUNIXタイムスタンプ（ミリ秒）に変換
                val paymentDateMillis = zonedDateTime.toInstant().toEpochMilli()
                val amountStr = payAmountEditText.text.toString()
                val amount = amountStr.toInt()
                val isReceiptChecked = payDone.isChecked
                val payment = Payment(
                    payerId = payerId,
                    userId = userID,
                    purpose = purpose,
                    paymentDate = paymentDateMillis,
                    amount = amount,
                    isReceiptChecked = isReceiptChecked,
                    notes = noteEditText.text.toString()
                )
                addPayment(payment)
                dialogHelper.dialogOkOnly("","支払い明細が登録されました")
            }
        }
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(payAmountEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spPayList.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spPerson.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDone.windowToken, 0)
        //フォーカスを外す処理
        payAmountEditText.clearFocus()
        spPerson.clearFocus()
        spPayList.clearFocus()
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
                val formattedDate =
                    String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
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

    private fun clearErrorMessage() {
        payAmountError.error = null
        payListError.text = null
        payDateError.error = null
    }

    private fun addPayment(payment:Payment){
        // データベースに登録
        CoroutineScope(Dispatchers.IO).launch {
            paymentDao.insert(payment)
        }
    }

}