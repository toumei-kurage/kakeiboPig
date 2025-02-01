package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import java.util.Calendar

class PayRecordAddActivity : BaseActivity(R.layout.activity_pay_record_add, R.string.title_add_pay_record) {
    //画面部品の用意
    private lateinit var spMember: Spinner
    private lateinit var memberListError:TextView
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
    private val firestore = FirebaseFirestore.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_add)

        setupDrawerAndToolbar()

        //画面部品の取得
        spMember = findViewById(R.id.spPerson)
        memberListError = findViewById(R.id.memberListError)
        spPayPurposeList = findViewById(R.id.spPayList)
        payPurposeListError = findViewById(R.id.PayListError)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDateEditText = findViewById(R.id.payDateEditText)
        payDateError = findViewById(R.id.payDate)
        payDone = findViewById(R.id.PayDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        // PayPurposeデータを取得しSpinnerにセット
        val paymentPurposes = arrayOf(getString(R.string.un_selected)) + databaseHelper.getPaymentPurposesForUser(userID)
        val payPurposeArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentPurposes)
        payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPayPurposeList.adapter = payPurposeArrayAdapter

        //Memberデータを取得しSpinnerにセット
        loadMembersFromFirestore()

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
            if(!resultPayAmount){
                payAmountError.error = payAmountMessage
            }
            val (resultPayDate, payDateMessage) = validateHelper.payDateCheck(payDateEditText)
            if (!resultPayDate) {
                payDateError.error = payDateMessage
            }
            val (resultPayPurpose,payPurposeMessage) = validateHelper.selectedCheck(selectedPayPurposeName)
            if(!resultPayPurpose){
                payPurposeListError.text = payPurposeMessage
            }
            val (resultMember,memberMessage) = validateHelper.selectedCheck(selectedMemberName)
            if(!resultMember){
                memberListError.text = memberMessage
            }
            if(!(resultPayAmount && resultPayDate && resultPayPurpose && resultMember)){
                return@setOnClickListener
            }
            clearErrorMessage()
            onSaveButtonClick()
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

    @SuppressLint("SimpleDateFormat")
    private fun onSaveButtonClick(){
        val memberId = databaseHelper.getMemberId(userID,selectedMemberName)
        val payPurposeId = databaseHelper.getPaymentPurposeId(userID,selectedPayPurposeName)
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put("member_id",memberId)
            put("user_id",userID)
            put("purpose_id",payPurposeId)
            put("payment_date",payDateEditText.text.toString())
            put("amount",payAmountEditText.text.toString().toInt())
            put("is_recept_checked",payDone.isChecked)
            put("note",noteEditText.text.toString())
        }

        // 挿入処理
        try {
            val newRowId = db.insert("payment_history", null, values)
            if (newRowId != -1L) {
                Toast.makeText(this, "支払い履歴が追加されました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "データベースに挿入できませんでした", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // エラーハンドリング
            Toast.makeText(this, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // 操作が終わった後でデータベースを閉じる
            db.close()
        }
    }

    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        payDateError.error = null
        memberListError.text = null
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

    // Firestoreからメンバーを取得してSpinnerにセットするメソッド
    private fun loadMembersFromFirestore() {
        val userId = userID // 現在ログインしているユーザーIDを取得

        firestore.collection("members")
            .whereEqualTo("user_id", userId)  // user_idに紐づくメンバーを取得
            .get()
            .addOnSuccessListener { querySnapshot ->
                // 成功した場合
                val memberList = mutableListOf<String>()
                // メンバー名をリストに追加（最初に「選択してください」の項目を追加）
                memberList.add(getString(R.string.un_selected))

                for (document in querySnapshot.documents) {
                    val memberName = document.getString("member_name") ?: ""
                    memberList.add(memberName) // member_nameをリストに追加
                }

                // Spinnerにセットする
                val memberArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, memberList)
                memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spMember.adapter = memberArrayAdapter

                // リストが空の場合の処理
                if (memberList.size == 1) {
                    memberListError.text = "メンバーが登録されていません。"
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}