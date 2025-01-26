package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class PayRecordListActivity : BaseActivity(R.layout.activity_pay_record_list, R.string.title_pay_record_list){
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayRecordAdd: FloatingActionButton
    private var payRecordList: List<PayRecord> = mutableListOf()
    private lateinit var buttonRefinement: Button
    private lateinit var payRecordAdapter: PayRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        //画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)
        buttonRefinement = findViewById(R.id.buttonRefinement)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        payRecordAdapter = PayRecordAdapter(this,payRecordList)
        recyclerView.adapter = payRecordAdapter

        applyRefinement(null,null,null)

        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, PayRecordAddActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonRefinement.setOnClickListener{
            // フラグメントを表示
            val fragment = PayRecordListRefinementFragment()
            fragment.show(supportFragmentManager, "PayRecordListRefinementFragment")
        }
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("PAY_RECORD_DELETE", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                applyRefinement(null,null,null)
            }
        }
    }

    // 絞り込み内容を受け取るメソッド
    @SuppressLint("NotifyDataSetChanged")
    fun applyRefinement(memberId: Int?, startDate: String?, endDate: String?) {
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

    //すべてが選択された場合に絞り込むメソッド
    @SuppressLint("Range")
    private fun getFilteredData(memberId: Int, startDate: String, endDate: String): List<PayRecord> {
        // SQLを使って、データベースを絞り込む処理を書く
        val db = DatabaseHelper(this).readableDatabase
        val selection = "user_id = ? AND member_id = ? AND payment_date BETWEEN ? AND ?"
        val selectionArgs = arrayOf(userID, memberId.toString(), startDate, endDate)

        val cursor = db.query(
            "payment_history",
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val payRecords = mutableListOf<PayRecord>()
        while (cursor.moveToNext()) {
            val payRecord = PayRecord(
                cursor.getLong(cursor.getColumnIndex("_id")),
                cursor.getString(cursor.getColumnIndex("user_id")),
                cursor.getLong(cursor.getColumnIndex("member_id")),
                cursor.getLong(cursor.getColumnIndex("purpose_id")),
                cursor.getString(cursor.getColumnIndex("payment_date")),
                cursor.getInt(cursor.getColumnIndex("amount")),
                cursor.getInt(cursor.getColumnIndex("is_recept_checked")) == 1,
                cursor.getString(cursor.getColumnIndex("note"))
            )
            payRecords.add(payRecord)
        }
        cursor.close()
        return payRecords
    }

    //メンバーだけが選択された場合に絞り込むメソッド
    @SuppressLint("Range")
    private fun getFilteredDataByMember(memberId: Int): List<PayRecord> {
        // SQLを使って、memberIdのみでデータを絞り込む処理
        val db = DatabaseHelper(this).readableDatabase
        val selection = "user_id = ? AND member_id = ?"
        val selectionArgs = arrayOf(userID, memberId.toString())

        val cursor = db.query(
            "payment_history",   // テーブル名
            null,                // 取得する列（nullで全列を取得）
            selection,           // 絞り込み条件
            selectionArgs,       // 絞り込み条件の値
            null,                // GROUP BY句（今回は使わない）
            null,                // HAVING句（今回は使わない）
            null                 // ORDER BY句（今回は使わない）
        )

        val payRecords = mutableListOf<PayRecord>()
        while (cursor.moveToNext()) {
            val payRecord = PayRecord(
                cursor.getLong(cursor.getColumnIndex("_id")),
                cursor.getString(cursor.getColumnIndex("user_id")),
                cursor.getLong(cursor.getColumnIndex("member_id")),
                cursor.getLong(cursor.getColumnIndex("purpose_id")),
                cursor.getString(cursor.getColumnIndex("payment_date")),
                cursor.getInt(cursor.getColumnIndex("amount")),
                cursor.getInt(cursor.getColumnIndex("is_recept_checked")) == 1,
                cursor.getString(cursor.getColumnIndex("note"))
            )
            payRecords.add(payRecord)
        }
        cursor.close()
        return payRecords
    }

    @SuppressLint("Range")
    private fun getFilteredDataByDateRange(startDate: String, endDate: String): List<PayRecord> {
        // SQLを使って、日付範囲でデータを絞り込む処理
        val db = DatabaseHelper(this).readableDatabase
        val selection = "user_id = ? AND payment_date BETWEEN ? AND ?"
        val selectionArgs = arrayOf(userID, startDate, endDate)

        val cursor = db.query(
            "payment_history",   // テーブル名
            null,                // 取得する列（nullで全列を取得）
            selection,           // 絞り込み条件
            selectionArgs,       // 絞り込み条件の値
            null,                // GROUP BY句（今回は使わない）
            null,                // HAVING句（今回は使わない）
            null                 // ORDER BY句（今回は使わない）
        )

        val payRecords = mutableListOf<PayRecord>()
        while (cursor.moveToNext()) {
            val payRecord = PayRecord(
                cursor.getLong(cursor.getColumnIndex("_id")),
                cursor.getString(cursor.getColumnIndex("user_id")),
                cursor.getLong(cursor.getColumnIndex("member_id")),
                cursor.getLong(cursor.getColumnIndex("purpose_id")),
                cursor.getString(cursor.getColumnIndex("payment_date")),
                cursor.getInt(cursor.getColumnIndex("amount")),
                cursor.getInt(cursor.getColumnIndex("is_recept_checked")) == 1,
                cursor.getString(cursor.getColumnIndex("note"))
            )
            payRecords.add(payRecord)
        }
        cursor.close()
        return payRecords
    }

    @SuppressLint("Range")
    private fun loadAllData(): List<PayRecord> {
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM payment_history WHERE user_id = ?", arrayOf(userID))
        val payRecords = mutableListOf<PayRecord>()

        if (cursor.moveToFirst()) {
            do {
                val payRecord = PayRecord(
                    cursor.getLong(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("user_id")),
                    cursor.getLong(cursor.getColumnIndex("member_id")),
                    cursor.getLong(cursor.getColumnIndex("purpose_id")),
                    cursor.getString(cursor.getColumnIndex("payment_date")),
                    cursor.getInt(cursor.getColumnIndex("amount")),
                    cursor.getInt(cursor.getColumnIndex("is_recept_checked")) == 1,
                    cursor.getString(cursor.getColumnIndex("note"))
                )
                payRecords.add(payRecord)
            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "支払い履歴が登録されていません。", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
        db.close()

        return payRecords
    }
}