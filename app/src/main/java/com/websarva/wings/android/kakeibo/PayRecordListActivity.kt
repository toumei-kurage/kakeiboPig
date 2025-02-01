package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class PayRecordListActivity : BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list) {
    // 画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    private var payRecordList: List<PayRecord> = mutableListOf()
    private lateinit var buttonRefinement: Button
    private lateinit var payRecordAdapter: PayRecordAdapter

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

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        payRecordAdapter = PayRecordAdapter(this, payRecordList)
        recyclerView.adapter = payRecordAdapter

        // 初期の絞り込みを適用
        applyRefinement(null, null, null)

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
                applyRefinement(null, null, null)
            }
        }
    }

    // 絞り込み内容を受け取るメソッド
    @SuppressLint("NotifyDataSetChanged")
    fun applyRefinement(memberId: String?, startDate: String?, endDate: String?) {
        // 絞り込み条件のチェックとデータ取得
        val refinedData = when {
            memberId != null && startDate != null && endDate != null -> {
                // memberId, startDate, endDate が全て指定された場合
                getFilteredData(memberId, startDate, endDate)
            }
            memberId != null -> {
                // memberId のみ指定された場合
                getFilteredDataByMember(memberId)
            }
            startDate != null && endDate != null -> {
                // 日付の範囲のみ指定された場合
                getFilteredDataByDateRange(startDate, endDate)
            }
            else -> {
                // 両方指定されていない場合は全データを返す
                loadAllData()
            }
        }

        // 絞り込んだデータをRecyclerViewなどにセット
        payRecordList = refinedData
        payRecordAdapter.updateData(payRecordList)
        payRecordAdapter.notifyDataSetChanged()
    }

    // Firestoreから絞り込んだデータを取得
    @SuppressLint("NotifyDataSetChanged")
    private fun getFilteredData(memberId: String, startDate: String, endDate: String): List<PayRecord> {
        val query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_id", memberId)
            .whereGreaterThanOrEqualTo("payment_date", startDate)
            .whereLessThanOrEqualTo("payment_date", endDate)

        val payRecords = mutableListOf<PayRecord>()
        query.get().addOnSuccessListener { querySnapshot ->
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
                payRecords.add(payRecord)
            }
            payRecordList = payRecords
            payRecordAdapter.updateData(payRecordList)
            payRecordAdapter.notifyDataSetChanged()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "メンバーの検索に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
        return payRecords
    }

    // メンバーのみで絞り込む
    @SuppressLint("NotifyDataSetChanged")
    private fun getFilteredDataByMember(memberId: String): List<PayRecord> {
        val query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_id", memberId)

        val payRecords = mutableListOf<PayRecord>()
        query.get().addOnSuccessListener { querySnapshot ->
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
                payRecords.add(payRecord)
            }
            payRecordList = payRecords
            payRecordAdapter.updateData(payRecordList)
            payRecordAdapter.notifyDataSetChanged()
        }
        return payRecords
    }

    // 日付範囲で絞り込む
    @SuppressLint("NotifyDataSetChanged")
    private fun getFilteredDataByDateRange(startDate: String, endDate: String): List<PayRecord> {
        val query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)
            .whereGreaterThanOrEqualTo("payment_date", startDate)
            .whereLessThanOrEqualTo("payment_date", endDate)

        val payRecords = mutableListOf<PayRecord>()
        query.get().addOnSuccessListener { querySnapshot ->
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
                payRecords.add(payRecord)
            }
            payRecordList = payRecords
            payRecordAdapter.updateData(payRecordList)
            payRecordAdapter.notifyDataSetChanged()
        }.addOnFailureListener { exception ->
                Toast.makeText(this, "メンバーの検索に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
        return payRecords
    }

    // Firestoreからすべてのデータを取得
    @SuppressLint("NotifyDataSetChanged")
    private fun loadAllData(): List<PayRecord> {
        val query = firestore.collection("payment_history")
            .whereEqualTo("user_id", userID)

        val payRecords = mutableListOf<PayRecord>()
        query.get().addOnSuccessListener { querySnapshot ->
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
                payRecords.add(payRecord)
            }
            payRecordList = payRecords
            payRecordAdapter.updateData(payRecordList)
            payRecordAdapter.notifyDataSetChanged()
        }

        return payRecords
    }
}
