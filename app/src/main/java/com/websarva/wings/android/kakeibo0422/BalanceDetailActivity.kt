package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BalanceDetailActivity : BaseActivity(R.layout.activity_balance_detail,R.string.title_balance_detail) {
    // 画面部品の用意
    private lateinit var dateRangeTextView: TextView
    private lateinit var linearLayoutContainer: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var sumExpenditureTextView: TextView
    private lateinit var leftoverTextView: TextView
    private lateinit var buttonBalanceSheetUpdate: Button

    private val firestore = FirebaseFirestore.getInstance()

    // 前画面からもらう値の用意
    private var balanceId: String =""
    private var budgetSet: String = "0"
    private var startDate: String = ""
    private var finishDate: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_detail)

        setupDrawerAndToolbar()

        // 取得したデータをフィールドにセット
        balanceId = intent.getStringExtra("BALANCE_ID")?:""
        budgetSet = intent.getIntExtra("BUDGET",-1).toString()
        startDate = intent.getStringExtra("START_DATE").toString()
        finishDate = intent.getStringExtra("FINISH_DATE").toString()

        // 画面部品の取得
        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        dateRangeTextView = findViewById(R.id.dateRangeTextView)
        budgetTextView = findViewById(R.id.budgetTextView)
        sumExpenditureTextView = findViewById(R.id.sumExpenditureTextView)
        leftoverTextView = findViewById(R.id.leftoverTextView)
        buttonBalanceSheetUpdate = findViewById(R.id.buttonBalanceSheetUpdate)

        // 期間のセット
        dateRangeTextView.text = getString(R.string.date_range_set, startDate, finishDate)

        // 予算額のセット
        budgetTextView.text = "${budgetSet}円"

        // 合計額のセット
        getTotalExpenditureInDateRange { sumExpenditure ->
            sumExpenditureTextView.text = "${sumExpenditure}円"
            leftoverTextView.text = "${budgetSet.toInt() - sumExpenditure}円"

            // 繰越額のセット
            leftoverTextView.text = "${budgetSet.toInt() - sumExpenditure}円"
        }

        // 支払い目的リストを取得して表示
        loadPaymentPurposes()

        buttonBalanceSheetUpdate.setOnClickListener{
            val intent = Intent(this,BalanceUpdateActivity::class.java)
            // 編集するデータを渡す
            intent.putExtra("BALANCE_ID", balanceId)
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("FINISH_DATE",finishDate)
            intent.putExtra("BUDGET",budgetSet)
            startActivity(intent)
            finish()
        }
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

    // メニュー（ActionBar）の作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()  // 削除確認ダイアログを表示
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // レコードに基づいて動的にLinearLayoutを追加する処理
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun addLayoutsForRecords(records: List<String>) {
        // Firestore から支払い目的ごとの金額を取得
        getAmountByPurposeForUserInDateRange(userID, startDate, finishDate) { payAmountByPurposeList ->
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

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ ->
                deleteBalance()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    // 家計簿を削除する処理
    private fun deleteBalance() {
        firestore.collection("balance_history")
            .document(balanceId)  // payRecordIdでドキュメントを指定
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
                // 削除成功した場合、親Activityに通知する
                val resultIntent = Intent()
                resultIntent.putExtra("BALANCE_DELETE", true)  // 削除フラグを渡す
                setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
                finish()  // アクティビティを終了し、前の画面に戻る
            }
            .addOnFailureListener {
                Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show()
            }
    }

    // 合計支出を取得する
    private fun getTotalExpenditureInDateRange(callback: (Int) -> Unit) {
        firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("payment_date", startDate)
            .whereLessThanOrEqualTo("payment_date", finishDate)
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
}