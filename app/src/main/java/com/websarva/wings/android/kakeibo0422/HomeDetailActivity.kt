package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeDetailActivity : BaseActivity(R.layout.activity_home_detail, R.string.title_total_pay_purpose) {
    private lateinit var linearLayoutContainer: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()
    private var startDateString = ""
    private var finishDateString = ""
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_detail)

        setupDrawerAndToolbar()

        linearLayoutContainer = findViewById(R.id.linearLayoutContainer)
        startDateString = intent.getStringExtra("START_DATE") ?:""
        finishDateString = intent.getStringExtra("FINISH_DATE") ?: ""

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

    // レコードに基づいて動的にLinearLayoutを追加する処理
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun addLayoutsForRecords(records: List<String>) {
        // Firestore から支払い目的ごとの金額を取得
        getAmountByPurposeForUserInDateRange(userID) { payAmountByPurposeList ->
            linearLayoutContainer.removeAllViews()

            for (record in records) {
                val layout = LayoutInflater.from(this).inflate(R.layout.layout_item, null) as LinearLayout
                val payPurposeNameTextView: TextView = layout.findViewById(R.id.payPurposeName)
                val payAmount: TextView = layout.findViewById(R.id.payAmount)

                // 支払い目的と金額を表示
                payPurposeNameTextView.text = getString(R.string.purpose_name, record)
                payAmount.text = getString(R.string.formatted_number,payAmountByPurposeList.getOrDefault(record, 0))

                linearLayoutContainer.addView(layout)
            }
        }
    }

    // 支払い目的ごとの金額を取得
    private fun getAmountByPurposeForUserInDateRange(userID: String, callback: (Map<String, Int>) -> Unit) {
        firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("payment_date", startDateString)
            .whereLessThanOrEqualTo("payment_date", finishDateString)
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
                Toast.makeText(this, "支払い目的の取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(null)  // エラーが発生した場合も null を返す
            }
    }
}