package com.websarva.wings.android.kakeibo0422

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
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.DialogHelper
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class PayRecordUpdateActivity : BaseActivity(R.layout.activity_pay_record_update, R.string.title_update_pay_record) {
    //画面部品の用意
    private lateinit var spMember: Spinner
    private lateinit var memberListError: TextView
    private lateinit var spPayPurposeList: Spinner
    private lateinit var payPurposeListError: TextView
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDateEditText: EditText
    private lateinit var payDateError: TextInputLayout
    private lateinit var payDone: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var buttonPayRecordUpdate: Button

    //スピナーで選ばれたものを格納する変数
    private lateinit var selectedMemberName: String
    private lateinit var selectedPayPurposeName: String

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)
    private val firestore = FirebaseFirestore.getInstance()
    private val dialogHelper = DialogHelper(this)

    private var payRecordId: String = ""
    private var memberId: String = ""
    private var payPurposeId: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_update)

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
        payDone = findViewById(R.id.payDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonPayRecordUpdate = findViewById(R.id.buttonPayRecordUpdate)

        //前画面からもらった値を取得
        payRecordId = intent.getStringExtra("PAY_RECORD_ID") ?: ""
        memberId = intent.getStringExtra("MEMBER_ID") ?: ""
        val payDate = intent.getStringExtra("PAY_DATE")
        payPurposeId = intent.getStringExtra("PAY_PURPOSE_ID") ?: ""
        val payAmount = intent.getIntExtra("PAY_AMOUNT", -1)
        val isReceptChecked = intent.getBooleanExtra("IS_RECEPT_CHECKED", false)
        val note = intent.getStringExtra("NOTE")

        // 支払い目的データを取得しSpinnerにセット
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val payPurposeNameList = mutableListOf<String>()
                payPurposeNameList.add("支払い目的を" + getString(R.string.un_selected))
                val newPayPurposeList = mutableListOf<PayPurpose>()
                for (document in querySnapshot.documents) {
                    val payPurposeName = document.getString("pay_purpose_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val payPurposeId = document.id
                    val userId = document.getString("user_id") ?: ""

                    newPayPurposeList.add(PayPurpose(payPurposeId, userId, payPurposeName, resistDate))
                }

                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newPayPurposeList.sortBy {
                    val dateString = it.resistDate
                    try {
                        LocalDateTime.parse(dateString, dateFormat) // LocalDateTime に変換
                    } catch (e: Exception) {
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                for(payPurpose in newPayPurposeList){
                    payPurposeNameList.add(payPurpose.payPurposeName)
                }

                val payPurposeArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, payPurposeNameList)
                payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spPayPurposeList.adapter = payPurposeArrayAdapter

                // 前画面からもらった値をもとにデータをセット
                firestore.collection("payPurposes")
                    .document(payPurposeId)
                    .get()
                    .addOnSuccessListener { document ->
                        val payPurposeName = document.getString("pay_purpose_name") ?: ""
                        // Spinnerのadapterが設定されてから選択肢を設定
                        val position = payPurposeArrayAdapter.getPosition(payPurposeName)
                        spPayPurposeList.setSelection(position)
                    }
            }

        // メンバーリストを取得しSpinnerにセット
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val memberNameList = mutableListOf<String>()
                memberNameList.add("メンバーを" + getString(R.string.un_selected))

                // クエリ結果をリストに変換
                val newMemberList = mutableListOf<Member>()
                for (document in querySnapshot.documents) {
                    val memberName = document.getString("member_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val memberId = document.id  // FirestoreのドキュメントIDを使う（または任意のフィールド）
                    val userId = document.getString("user_id") ?: ""

                    newMemberList.add(Member(memberId, userId, memberName, resistDate))
                }

                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newMemberList.sortBy {
                    val dateString = it.resistDate
                    try {
                        LocalDateTime.parse(dateString, dateFormat) // LocalDateTime に変換
                    } catch (e: Exception) {
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                for(member in newMemberList){
                    memberNameList.add(member.memberName)
                }

                val memberArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, memberNameList)
                memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spMember.adapter = memberArrayAdapter

                // 前画面からもらった値をもとにデータをセット
                firestore.collection("members")
                    .document(memberId)
                    .get()
                    .addOnSuccessListener { document ->
                        val memberName = document.getString("member_name") ?: ""
                        val position = memberArrayAdapter.getPosition(memberName)
                        spMember.setSelection(position)
                    }
            }

        payDateEditText.setText(payDate)
        payAmountEditText.setText(payAmount.toString())
        payDone.isChecked = isReceptChecked
        noteEditText.setText(note)

        spPayPurposeList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView: View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedPayPurposeName = parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        spMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView: View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedMemberName = parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {}
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
            val (resultPayAmount, payAmountMessage) = validateHelper.payAmountCheck(payAmountEditText)
            val (resultPayDate, payDateMessage) = validateHelper.payDateCheck(payDateEditText)
            val (resultPayPurpose, payPurposeMessage) = validateHelper.selectedCheck(selectedPayPurposeName)
            val (resultMember, memberMessage) = validateHelper.selectedCheck(selectedMemberName)
            if (!(resultPayAmount && resultPayDate && resultPayPurpose && resultMember)) {
                payAmountError.error = payAmountMessage
                payDateError.error = payDateMessage
                payPurposeListError.text = payPurposeMessage
                memberListError.text = memberMessage
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
        inputMethodManager.hideSoftInputFromWindow(noteEditText.windowToken,0)
        // フォーカスを外す処理
        payAmountEditText.clearFocus()
        spMember.clearFocus()
        spPayPurposeList.clearFocus()
        payDone.clearFocus()
        noteEditText.clearFocus()
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

        datePickerDialog.setOnCancelListener {
            // 必要な処理があればここに記述
        }
        datePickerDialog.show()
    }

    private fun updatePayRecord() {
        // メンバーIDを取得
        getMemberId { memberId ->
            // 支払い目的IDを取得
            getPayPurposeId { payPurposeId ->
                updateData(memberId,payPurposeId)
            }
        }
    }

    private fun updateData(memberId:String,payPurposeId:String){
        try {
            val updatedRecord = hashMapOf(
                "member_id" to memberId,
                "pay_purpose_id" to payPurposeId,
                "payment_date" to payDateEditText.text.toString(),
                "amount" to payAmountEditText.text.toString().toInt(),
                "is_recept_checked" to payDone.isChecked,
                "note" to noteEditText.text.toString()
            )

            // Firestore ドキュメントを更新
            firestore.collection("payment_history")
                .document(payRecordId)
                .update(updatedRecord as Map<String, Any>)
                .addOnSuccessListener {
                    dialogHelper.dialogOkOnly("","支払い記録が更新されました")
                }
                .addOnFailureListener { e ->
                    showToast("更新に失敗しました: ${e.message}")
                }

        } catch (e: Exception) {
            showToast("エラーが発生しました: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        payDateError.error = null
        memberListError.text = null
    }

    // メンバーIDを取得するメソッド
    private fun getMemberId(onSuccess: (String) -> Unit) {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", selectedMemberName)
            .get()
            .addOnSuccessListener { memberQuerySnapshot ->
                if (memberQuerySnapshot.isEmpty) {
                    showToast("メンバーが見つかりません")
                    return@addOnSuccessListener
                }
                val memberId = memberQuerySnapshot.documents.first().id
                onSuccess(memberId)
            }
            .addOnFailureListener { exception ->
                showToast("メンバーデータの取得に失敗しました: ${exception.message}")
            }
    }

    // 支払い目的IDを取得するメソッド
    private fun getPayPurposeId(onSuccess: (String) -> Unit) {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_name", selectedPayPurposeName)
            .get()
            .addOnSuccessListener { payPurposeQuerySnapshot ->
                if (payPurposeQuerySnapshot.isEmpty) {
                    showToast("支払い目的が見つかりません")
                    return@addOnSuccessListener
                }
                val payPurposeId = payPurposeQuerySnapshot.documents.first().id
                onSuccess(payPurposeId)
            }
            .addOnFailureListener { exception ->
                showToast("支払い目的データの取得に失敗しました: ${exception.message}")
            }
    }
}
