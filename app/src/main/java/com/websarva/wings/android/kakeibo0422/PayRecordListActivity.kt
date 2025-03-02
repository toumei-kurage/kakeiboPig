package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PayRecordListActivity : BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list) {
    // 画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    private lateinit var buttonRefinement: Button
    private lateinit var payRecordAdapter: PayRecordAdapter
    private lateinit var textViewTotalAmount: TextView

    private var payRecordList: List<PayRecord> = mutableListOf()

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        // 画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)
        buttonRefinement = findViewById(R.id.buttonRefinement)
        textViewTotalAmount = findViewById(R.id.textViewTotalAmount)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        payRecordAdapter = PayRecordAdapter(this, payRecordList)
        recyclerView.adapter = payRecordAdapter

        // 初期の絞り込みを適用
        applyRefinement(null, null, null,null,null)

        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, PayRecordAddActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonRefinement.setOnClickListener {
            // フラグメントを表示
            val fragment = PayRecordListRefinementFragment()
            fragment.show(supportFragmentManager, "PayRecordListRefinementFragment")
        }
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("PAY_RECORD_DELETE", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                applyRefinement(null, null, null,null,null)
            }
        }
    }

    // 絞り込み内容を受け取るメソッド
    @SuppressLint("NotifyDataSetChanged")
    fun applyRefinement(memberId: String?, startDate: String?, finishDate: String?,payDone:String?, payPurposeId: String?) {
        // 絞り込み条件のチェックとデータ取得
        val refinedData = getFilterData(memberId,startDate,finishDate,payDone,payPurposeId)

        // 絞り込んだデータをRecyclerViewなどにセット
        payRecordList = refinedData
        payRecordAdapter.updateData(payRecordList)
        payRecordAdapter.notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun getFilterData(memberId: String?, startDate: String?, finishDate: String?, payDoneString: String?, payPurposeId: String?): List<PayRecord>{
        val query = createQuery(memberId,startDate,finishDate,payDoneString,payPurposeId)
        val newPayRecordList = mutableListOf<PayRecord>()
        query.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val payRecord = PayRecord(
                        document.id,
                        document.getString("user_id") ?: "",
                        document.getString("member_id") ?: "",
                        document.getString("pay_purpose_id") ?: "",
                        document.getString("payment_date") ?: "",
                        document.getLong("amount")?.toInt() ?: 0,
                        document.getBoolean("is_recept_checked") == true,
                        document.getString("note") ?: ""
                    )
                    newPayRecordList.add(payRecord)
                }

                // payment_date を Date 型に変換してからソート
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                newPayRecordList.sortByDescending {
                    val dateString = it.payDate
                    try {
                        dateFormat.parse(dateString) ?: Date(0) // 変換できない場合は 1970-01-01 を返す
                    } catch (e: Exception) {
                       Date(0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                // すべてのamountの合計を取得
                textViewTotalAmount.text = "合計金額："+ getString(R.string.formatted_number,newPayRecordList.sumOf { it.payAmount })

                // データが取得できたらRecyclerViewを更新
                if (newPayRecordList.isEmpty()) {
                    Toast.makeText(this, "支払い履歴が登録されていません。", Toast.LENGTH_SHORT).show()
                }

                // 更新したリストをアダプターに渡す
                payRecordList = newPayRecordList
                payRecordAdapter.updateData(payRecordList)
                payRecordAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        return newPayRecordList
    }

    private fun createQuery(memberId: String?, startDate: String?, finishDate: String?, payDoneString: String?, payPurposeId: String?): Query {
        val payDone = if (payDoneString != null) (payDoneString == "領収済み") else null
        var query: Query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)

        if (memberId != null) {
            query = query.whereEqualTo("member_id", memberId)
        }
        if(payPurposeId != null) {
            query = query.whereEqualTo("pay_purpose_id", payPurposeId)
        }
        if (startDate != null && finishDate != null) {
            query = query
                .whereGreaterThanOrEqualTo("payment_date", startDate)
                .whereLessThanOrEqualTo("payment_date", finishDate)
        }
        if (payDone != null) {
            query = query.whereEqualTo("is_recept_checked", payDone)
        }

        return query
    }
}
