package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class HomeActivity : BaseActivity(R.layout.activity_home, R.string.title_home) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var leftoverTextView: TextView
    private lateinit var buttonSetInfo: Button
    private lateinit var buttonBalanceSheetAdd: Button

    // Fragmentからもらう値の用意
    private var budgetSet: String = "0"
    private var startDateString: String = ""
    private var finishDateString: String = ""

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()

        // 画面部品の取得
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        budgetTextView = findViewById(R.id.budgetTextView)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        leftoverTextView = findViewById(R.id.leftoverTextView)
        buttonSetInfo = findViewById(R.id.buttonSetInfo)
        buttonBalanceSheetAdd = findViewById(R.id.buttonBalanceSheetAdd)

        loadLatestBalanceHistory()

        // 「設定」ボタンのクリックリスナー
        buttonSetInfo.setOnClickListener {
            val fragment = BalanceSheetSetInfoFragment()
            fragment.show(supportFragmentManager, "BalanceSheetSetInfoFragment")
        }

        buttonBalanceSheetAdd.setOnClickListener {
            if (budgetSet != "0" && startDateString != "" && finishDateString != "") {
                saveBalanceHistory()
            } else {
                Toast.makeText(this, "予算と期間を設定してください。", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    // Fragmentから情報を受け取る
    fun setInfo(budget: Int, startDate: String, finishDate: String) {
        budgetSet = budget.toString()
        this.startDateString = startDate
        this.finishDateString = finishDate

        // 新しいデータで画面を再描画
        updateUI()
    }

    // UIを更新するメソッド
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        dateRangeTextView.text = if (startDateString != "" && finishDateString != "") {
            getString(R.string.date_range_set, startDateString, finishDateString)
        } else {
            getString(R.string.date_range_set, "未設定", "未設定")
        }

        budgetTextView.text = if (budgetSet != "0") {
            "${budgetSet}円"
        } else {
            "未設定"
        }

        // Firestore から合計額を取得
        getTotalExpenditureInDateRange { sumExpenditure ->
            sumExpenditureTextView.text = "${sumExpenditure}円"
            leftoverTextView.text = "${budgetSet.toInt() - sumExpenditure}円"
        }

        // 支払い目的リストを取得して表示
        loadPaymentPurposes()
    }

    // 支払い目的リストの取得
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPaymentPurposes() {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val payPurposeNameList = mutableListOf<String>()
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
                addLayoutsForRecords(payPurposeNameList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "支払い目的の取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
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

    // レコードに基づいて動的にLinearLayoutを追加する処理
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun addLayoutsForRecords(records: List<String>) {
        // Firestore から支払い目的ごとの金額を取得
        getAmountByPurposeForUserInDateRange(userID, startDateString, finishDateString) { payAmountByPurposeList ->
            linearLayoutContainer.removeAllViews()

            for (record in records) {
                val layout = LayoutInflater.from(this).inflate(R.layout.layout_item, null) as LinearLayout
                val payPurposeNameTextView: TextView = layout.findViewById(R.id.payPurposeName)
                val payAmount: TextView = layout.findViewById(R.id.payAmount)

                // 支払い目的と金額を表示
                payPurposeNameTextView.text = getString(R.string.purpose_name, record)
                payAmount.text = "${payAmountByPurposeList.getOrDefault(record, 0)}円"

                linearLayoutContainer.addView(layout)
            }
        }
    }

    // Firestore に家計簿データを保存
    private fun saveBalanceHistory() {
        val balanceHistory = hashMapOf(
            "user_id" to userID,
            "start_date" to startDateString,
            "finish_date" to finishDateString,
            "budget" to budgetSet.toInt()
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

    // 支払い目的ごとの金額を取得
    private fun getAmountByPurposeForUserInDateRange(userID: String, startDate: String, finishDate: String, callback: (Map<String, Int>) -> Unit) {
        firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("payment_date", startDate)
            .whereLessThanOrEqualTo("payment_date", finishDate)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val payAmountByPurposeList = mutableMapOf<String, Int>()
                val totalDocuments = querySnapshot.documents.size
                var processedDocuments = 0

                if (totalDocuments == 0) {
                    // 支払い履歴が存在しない場合
                    callback(payAmountByPurposeList)
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val payPurposeId = document.getString("pay_purpose_id") ?: ""
                    val amount = document.getLong("amount")?.toInt() ?: 0

                    // 支払い目的名を取得
                    getPayPurposeNameFromDocumentId(payPurposeId) { payPurposeName ->
                        if (payPurposeName != null) {
                            // 支払い目的ごとに金額を集計
                            payAmountByPurposeList[payPurposeName] = payAmountByPurposeList.getOrDefault(payPurposeName, 0) + amount
                        }

                        // 最後のドキュメント処理が完了したらコールバックを呼ぶ
                        processedDocuments++
                        if (processedDocuments == totalDocuments) {
                            callback(payAmountByPurposeList)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "支払い目的ごとの金額取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(emptyMap())  // エラーが発生した場合は空のマップを返す
            }
    }


    // Firestore から payPurposes コレクションの指定されたドキュメントの pay_purpose_name を取得するメソッド
    private fun getPayPurposeNameFromDocumentId(documentId: String, callback: (String?) -> Unit) {
        firestore.collection("payPurposes")
            .document(documentId)  // ドキュメントIDを指定
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val payPurposeName = documentSnapshot.getString("pay_purpose_name")  // フィールド名を指定
                    callback(payPurposeName)
                } else {
                    callback(null)  // ドキュメントが存在しない場合は null を返す
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "pay_purpose_name の取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(null)  // エラーが発生した場合も null を返す
            }
    }
}
