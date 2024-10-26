package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.MemberViewModel
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurposeViewModel
import com.websarva.wings.android.kakeibo.room.payRecord.Payment
import com.websarva.wings.android.kakeibo.room.payRecord.PaymentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AddPayRecordActivity :
    BaseActivity(R.layout.activity_add_pay_record, R.string.title_add_pay_record) {

    private lateinit var memberViewModel: MemberViewModel
    private lateinit var payPurposeViewModel: PayPurposeViewModel

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
        memberViewModel = ViewModelProvider(this)[MemberViewModel::class.java]
        payPurposeViewModel = ViewModelProvider(this)[PayPurposeViewModel::class.java]


        // Personデータを取得しSpinnerにセット
        memberViewModel.getPersons(userID).observe(this) { persons ->
            val personNames = persons.map { it.memberName } // Personから名前のリストを作成
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPerson.adapter = adapter
        }

        payPurposeViewModel.getPayPurposes(userID).observe(this){payPurposes ->
            val payPurposeNames = payPurposes.map{it.payPurposeName}
            val adapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,payPurposeNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPayList.adapter = adapter
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
            // コルーチンを使って非同期にIDを取得
            lifecycleScope.launch {
                try {
                    spPerson.selectedItem ?:throw NullPointerException("メンバーが登録されていません")
                    spPayList.selectedItem?: throw NullPointerException("支払い目的が登録されていません")
                    val payerId = memberViewModel.getPersonID(userID, spPerson.selectedItem.toString())
                    val purposeId = payPurposeViewModel.getPayPurposeID(userID, spPayList.selectedItem.toString())
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
                        purposeId = purposeId,
                        paymentDate = paymentDateMillis,
                        amount = amount,
                        isReceiptChecked = isReceiptChecked,
                        notes = noteEditText.text.toString()
                    )
                    addPayment(payment)
                    dialogHelper.dialogOkOnly("", "支払い明細が登録されました")
                } catch (e: NullPointerException) {
                    // 例外メッセージを取り出す
                    val errorMessage = e.message ?: "不明なエラーが発生しました"

                    AlertDialog.Builder(this@AddPayRecordActivity)
                        .setTitle("登録失敗")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK") { _, _ ->
                            // OKボタンを押したら適切なアクティビティに遷移
                            val intent = if (errorMessage == "メンバーが登録されていません")
                                Intent(this@AddPayRecordActivity, MemberAddActivity::class.java)
                            else
                                Intent(this@AddPayRecordActivity, PayPurposeAddActivity::class.java)

                            startActivity(intent)
                            finish()
                        }
                        .setCancelable(false) // ダイアログの外をタップしても閉じないようにする
                        .show()
                }
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

    private fun addPayment(payment: Payment) {
        //データベースへの登録
        lifecycleScope.launch(Dispatchers.IO) {
            paymentDao.insert(payment)
        }
    }

}