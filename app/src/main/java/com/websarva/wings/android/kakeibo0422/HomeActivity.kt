package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper

class HomeActivity : BaseActivity(R.layout.activity_home, R.string.title_home) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var dateRangeError: TextView
    private lateinit var budgetTextView: TextView
    private lateinit var budgetError: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var bookBalanceTextView: TextView
    private lateinit var actualBalanceEditText: EditText
    private lateinit var actualBalanceError: TextInputLayout
    private lateinit var differenceTextView: TextView
    private lateinit var buttonSetInfo: Button
    private lateinit var buttonSetActualBalance: Button
    private lateinit var buttonBalanceSheetAdd: Button
    private lateinit var buttonBalanceDetail: Button

    // Fragmentからもらう値の用意
    private var budgetSet: String = "0"
    private var startDateString: String = ""
    private var finishDateString: String = ""

    private val firestore = FirebaseFirestore.getInstance()
    private val validateHelper = ValidateHelper(this)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()

        // 画面部品の取得
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        dateRangeError = findViewById(R.id.dateRangeError)
        budgetTextView = findViewById(R.id.budgetTextView)
        budgetError = findViewById(R.id.budgetError)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        bookBalanceTextView = findViewById(R.id.bookBalanceTextView)
        actualBalanceEditText = findViewById(R.id.actualBalanceEditText)
        actualBalanceError = findViewById(R.id.actualBalanceError)
        differenceTextView = findViewById(R.id.differenceTextView)
        buttonSetInfo = findViewById(R.id.buttonSetInfo)
        buttonSetActualBalance = findViewById(R.id.buttonSetActualBalance)
        buttonBalanceSheetAdd = findViewById(R.id.buttonBalanceSheetAdd)
        buttonBalanceDetail = findViewById(R.id.buttonBalanceDetail)

        loadLatestBalanceHistory()

        // 「設定」ボタンのクリックリスナー
        buttonSetInfo.setOnClickListener {
            val fragment = BalanceSheetSetInfoFragment()
            fragment.show(supportFragmentManager, "BalanceSheetSetInfoFragment")
        }

        buttonBalanceSheetAdd.setOnClickListener {
            clearBordFocus()
            actualBalanceError.error = null
            val (resultDateRange: Boolean, dateRangeMsg: String) = Pair(startDateString != "" && finishDateString != "", if(!(startDateString != "" && finishDateString != "")) "期間を設定してください。" else "")
            val (resultBudgetSet: Boolean, budgetSetMsg: String) = Pair(budgetSet != "0", if(budgetSet == "0") "予算を設定してください。" else "")
            if(!(resultDateRange && resultBudgetSet)){
                dateRangeError.text = dateRangeMsg
                budgetError.text = budgetSetMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            saveBalanceHistory()
        }

        buttonSetActualBalance.setOnClickListener{
            clearBordFocus()
            val (resultActualBalance: Boolean, actualBalanceMsg: String) = validateHelper.actualBalanceCheck(actualBalanceEditText)
            val (resultDateRange: Boolean, dateRangeMsg: String) = Pair(startDateString != "" && finishDateString != "", if(!(startDateString != "" && finishDateString != "")) "期間を設定してください。" else "")
            val (resultBudgetSet: Boolean, budgetSetMsg: String) = Pair(budgetSet != "0", if(budgetSet == "0") "予算を設定してください。" else "")
            if(!(resultActualBalance && resultDateRange && resultBudgetSet)){
                actualBalanceError.error = actualBalanceMsg
                dateRangeError.text = dateRangeMsg
                budgetError.text = budgetSetMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            val bookBalance = bookBalanceTextView.text.toString().replace("円","").replace(",","").toInt()
            val actualBalance = actualBalanceEditText.text.toString().toInt()
            if(bookBalance == actualBalance){
                differenceTextView.text = getString(R.string.text_just)
            }
            else if(bookBalance > actualBalance){
                differenceTextView.text = getString(R.string.text_difference,getString(R.string.formatted_number,bookBalance - actualBalance) + "少ないです")
            }
            else{
                differenceTextView.text = getString(R.string.text_difference,getString(R.string.formatted_number,actualBalance - bookBalance) + "多いです")
            }
            getBalanceId { balanceId ->
                updateBalance(balanceId)
            }
        }

        buttonBalanceDetail.setOnClickListener{
            val intent = Intent(this,HomeDetailActivity::class.java)
            intent.putExtra("START_DATE",startDateString)
            intent.putExtra("FINISH_DATE",finishDateString)
            startActivity(intent)
            finish()
        }
    }

    // Fragmentから情報を受け取る
    @RequiresApi(Build.VERSION_CODES.O)
    fun setInfo(budget: Int, startDate: String, finishDate: String) {
        budgetSet = budget.toString()
        this.startDateString = startDate
        this.finishDateString = finishDate

        // 新しいデータで画面を再描画
        updateUI()
    }

    // UIを更新するメソッド
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        dateRangeTextView.text = if (startDateString != "" && finishDateString != "") {
            getString(R.string.date_range_set, startDateString, finishDateString)
        } else {
            getString(R.string.date_range_set, "未設定", "未設定")
        }

        budgetTextView.text = if (budgetSet != "0") {
            getString(R.string.formatted_number,budgetSet.toInt())
        } else {
            "未設定"
        }

        // Firestore から合計額を取得
        getTotalExpenditureInDateRange { sumExpenditure ->
            sumExpenditureTextView.text = getString(R.string.formatted_number,sumExpenditure)
            bookBalanceTextView.text = getString(R.string.formatted_number,budgetSet.toInt() - sumExpenditure)
        }
    }


    // 合計支出を取得する
    private fun getTotalExpenditureInDateRange(callback: (Int) -> Unit) {
        firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("payment_date", startDateString)
            .whereLessThanOrEqualTo("payment_date", finishDateString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val sumExpenditure = querySnapshot.documents.sumOf { it.getLong("amount")?.toInt() ?: 0 }
                callback(sumExpenditure)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "支出の合計取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(0)  // エラーが発生した場合は0円として返す
            }
    }

    // Firestore に家計簿データを保存
    private fun saveBalanceHistory() {
        val actualBalance = if(validateHelper.actualBalanceCheck(actualBalanceEditText).first) actualBalanceEditText.text.toString().toInt() else 0
        val balanceHistory = hashMapOf(
            "user_id" to userID,
            "start_date" to startDateString,
            "finish_date" to finishDateString,
            "budget" to budgetSet.toInt(),
            "actual_balance" to actualBalance
        )

        // Firestore で既に同じ期間のデータが存在するか確認
        firestore.collection("balance_history")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("start_date", startDateString)
            .whereEqualTo("finish_date", finishDateString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    firestore.collection("balance_history")
                        .add(balanceHistory)
                        .addOnSuccessListener {
                            Toast.makeText(this, "家計簿が追加されました", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "データベースに挿入できませんでした: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "この期間は既に存在します", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データベースの確認に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestore から最新の家計簿情報を取得
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLatestBalanceHistory() {
        firestore.collection("balance_history")
            .whereEqualTo("user_id", userID)
            .orderBy("start_date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val document = querySnapshot.documents.first()
                    budgetSet = document.getLong("budget")?.toString() ?: "0"
                    startDateString = document.getString("start_date") ?: ""
                    finishDateString = document.getString("finish_date") ?: ""
                    val actualBalance = document.getLong("actual_balance") ?: 0
                    actualBalanceEditText.setText("$actualBalance")
                } else {
                    budgetSet = "0"
                    startDateString = ""
                    finishDateString = ""
                }
                updateUI()  // 取得したデータをUIに反映
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データの取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(actualBalanceEditText.windowToken, 0)
        //フォーカスを外す処理
        actualBalanceEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        actualBalanceError.error = null
        dateRangeError.error = null
        budgetError.error = null
    }

    // 家計簿IDを取得するメソッド
    private fun getBalanceId(onSuccess: (String) -> Unit) {
        firestore.collection("balance_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("start_date", startDateString)
            .whereLessThanOrEqualTo("finish_date", finishDateString)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this,"家計簿が見つかりません。",Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val balanceId = querySnapshot.documents.first().id
                onSuccess(balanceId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this,"家計簿データの取得に失敗しました: ${exception.message}",Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBalance(balanceId:String) {
        try {
            val updatedRecord = hashMapOf(
                "start_date" to startDateString,
                "finish_date" to finishDateString,
                "budget" to budgetSet.toInt(),
                "actual_balance" to actualBalanceEditText.text.toString().toInt(),
                "user_id" to userID
            )

            // Firestore ドキュメントを更新
            firestore.collection("balance_history")
                .document(balanceId)
                .update(updatedRecord as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this,"家計簿記録が更新されました",Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,"更新に失敗しました: ${e.message}",Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this,"エラーが発生しました: ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }
}
