package com.websarva.wings.android.kakeibo

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
        loadPayPurposesFromFirestore()

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
            val (resultPayAmount, payAmountMessage) = validateHelper.payAmountCheck(payAmountEditText)
            val (resultPayDate, payDateMessage) = validateHelper.payDateCheck(payDateEditText)
            val (resultPayPurpose,payPurposeMessage) = validateHelper.selectedCheck(selectedPayPurposeName)
            val (resultMember,memberMessage) = validateHelper.selectedCheck(selectedMemberName)
            if(!(resultPayAmount && resultPayDate && resultPayPurpose && resultMember)){
                payAmountError.error = payAmountMessage
                payDateError.error = payDateMessage
                payPurposeListError.text = payPurposeMessage
                memberListError.text = memberMessage
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
    private fun onSaveButtonClick() {
        // メンバーIDを取得
        getMemberId { memberId ->
            // 支払い目的IDを取得
            getPayPurposeId { payPurposeId ->
                // 支払い履歴をFirestoreに追加
                addPaymentHistory(memberId, payPurposeId)
            }
        }
    }

    // メンバーIDを取得するメソッド
    private fun getMemberId(onSuccess: (String) -> Unit) {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", selectedMemberName)
            .get()
            .addOnSuccessListener { memberQuerySnapshot ->
                if (memberQuerySnapshot.isEmpty) {
                    showError("メンバーが見つかりません")
                    return@addOnSuccessListener
                }
                val memberId = memberQuerySnapshot.documents.first().id
                onSuccess(memberId)
            }
            .addOnFailureListener { exception ->
                showError("メンバーデータの取得に失敗しました: ${exception.message}")
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
                    showError("支払い目的が見つかりません")
                    return@addOnSuccessListener
                }
                val payPurposeId = payPurposeQuerySnapshot.documents.first().id
                onSuccess(payPurposeId)
            }
            .addOnFailureListener { exception ->
                showError("支払い目的データの取得に失敗しました: ${exception.message}")
            }
    }

    // 支払い履歴をFirestoreに追加するメソッド
    private fun addPaymentHistory(memberId: String, payPurposeId: String) {
        val paymentHistory = hashMapOf(
            "member_id" to memberId,
            "user_id" to userID,
            "pay_purpose_id" to payPurposeId,
            "payment_date" to payDateEditText.text.toString(),
            "amount" to payAmountEditText.text.toString().toInt(),
            "is_recept_checked" to payDone.isChecked,
            "note" to noteEditText.text.toString()
        )

        firestore.collection("payment_history")
            .add(paymentHistory)
            .addOnSuccessListener {
                Toast.makeText(this, "支払い履歴が追加されました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                showError("支払い履歴の追加に失敗しました: ${exception.message}")
            }
    }

    // エラーメッセージを表示する共通メソッド
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        payDateError.error = null
        memberListError.text = null
    }

    // Firestoreからメンバーを取得してSpinnerにセットするメソッド
    private fun loadMembersFromFirestore() {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)  // user_idに紐づくメンバーを取得
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

    // Firestoreから支払い目的を取得してSpinnerにセットするメソッド
    private fun loadPayPurposesFromFirestore() {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)  // user_idに紐づくメンバーを取得
            .get()
            .addOnSuccessListener { querySnapshot ->
                // 成功した場合
                val payPurposeList = mutableListOf<String>()
                // 支払い目的をリストに追加（最初に「選択してください」の項目を追加）
                payPurposeList.add(getString(R.string.un_selected))

                for (document in querySnapshot.documents) {
                    val payPurposeName = document.getString("pay_purpose_name") ?: ""
                    payPurposeList.add(payPurposeName) // pay_purpose_nameをリストに追加
                }

                // Spinnerにセットする
                val payPurposeArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, payPurposeList)
                payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spPayPurposeList.adapter = payPurposeArrayAdapter

                // リストが空の場合の処理
                if (payPurposeList.size == 1) {
                    payPurposeListError.text = "支払い目的が登録されていません。"
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}