package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
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
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.MemberViewModel
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.payRecord.Payment
import com.websarva.wings.android.kakeibo.room.payRecord.PaymentDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class UpdatePayRecordActivity :
    BaseActivity(R.layout.activity_update_pay_record, R.string.title_update_pay_record) {
    private lateinit var memberViewModel: MemberViewModel

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
    private lateinit var buttonPayRecordUpdate: Button

    private var firstCreate: Boolean = true

    private lateinit var payment: Payment // 受け取るPaymentオブジェクト

    private var personPosition = -1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_pay_record)

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
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)

        // ViewModelのセットアップ
        memberViewModel =
            ViewModelProvider(this)[MemberViewModel::class.java]

        // IntentからPaymentオブジェクトを受け取る
        payment = intent.getParcelableExtra("支払い明細") ?: throw IllegalArgumentException("Payment data is required")


        // Personデータを取得しSpinnerにセット
        memberViewModel.getPersons(userID).observe(this) { persons ->
            val personNames = persons.map { it.memberName } // Personから名前のリストを作成
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPerson.adapter = adapter

            // 支払い目的のリストを取得しSpinnerにセット
            val payList = resources.getStringArray(R.array.pay_list).toList() // XMLから配列を取得
            val payListAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, payList)
            payListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPayList.adapter = payListAdapter
            personPosition = getPersonPosition(payment.payerId,persons)
            // Paymentデータに基づいてフォームを設定
            setupFormWithPayment(payment)
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

        buttonPayRecordUpdate.setOnClickListener {
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
            // 更新処理
            lifecycleScope.launch {
                updatePayment()
                AlertDialog.Builder(this@UpdatePayRecordActivity)
                    .setTitle("")
                    .setMessage("支払い明細が更新されました")
                    .setPositiveButton("OK"){_,_->
                        val intent = Intent(this@UpdatePayRecordActivity,PayRecordListActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .setCancelable(false)
                    .show()
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

        // ダイアログのキャンセルfボタンが押されたときの処理
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

    private fun setupFormWithPayment(payment: Payment) {
        // フォームの各フィールドにPaymentのデータを設定
        // 例えば
        spPerson.setSelection(personPosition) // 支払者のIDから位置を取得
        spPayList.setSelection(getPayPurposePosition(payment.purpose)) // 支払い目的の位置を取得
        payAmountEditText.setText(payment.amount.toString())
        payDateEditText.setText(formatLongToDateString(payment.paymentDate))
        payDone.isChecked = payment.isReceiptChecked
        noteEditText.setText(payment.notes)
    }

    private fun formatLongToDateString(timestamp: Long): String {
        // Long型のtimestampをDate型に変換
        val date = Date(timestamp)
        // yyyy/MM/ddフォーマットのSimpleDateFormatを作成
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        // Dateをフォーマットして文字列を返す
        return format.format(date)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updatePayment() {
        lifecycleScope.launch {
            val payerId = memberViewModel.getPersonId(userID, spPerson.selectedItem.toString())
            val purpose = spPayList.selectedItem.toString()
            // 更新用のPaymentオブジェクトを作成
            val updatedPayment = payment.copy(
                payerId = payerId,
                purpose = purpose,
                amount = payAmountEditText.text.toString().toInt(),
                paymentDate = parseDate(payDateEditText.text.toString()),
                isReceiptChecked = payDone.isChecked,
                notes = noteEditText.text.toString()
            )
            paymentDao.updatePayment(updatedPayment) // DAOの更新メソッドを呼び出す
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDate(dateStr: String): Long {
        // 日付文字列をミリ秒に変換
        val format = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val paymentDate = LocalDate.parse(dateStr, format)
        return paymentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // getPersonPositionメソッド
    private fun getPersonPosition(payerId: Int, persons: List<Person>): Int {
        for (i in persons.indices) {
            if (persons[i].id == payerId) {
                return i // インデックスを返す
            }
        }
        return 0 // 一致するものがない場合はデフォルトのインデックスを返す
    }

    private fun getPayPurposePosition(purpose: String): Int {
        // Spinnerのアダプタを取得
        val adapter = spPayList.adapter as ArrayAdapter<*>

        // 各目的を比較してインデックスを返す
        for (i in 0 until adapter.count) {
            val item = adapter.getItem(i) // アイテムを取得
            if (item == purpose) { // 目的が一致する場合
                return i // インデックスを返す
            }
        }
        return 0 // 一致するものがない場合はデフォルトのインデックスを返す
    }

}