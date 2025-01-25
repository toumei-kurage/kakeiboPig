package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
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
    private lateinit var payRecordAdapter: PayRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_list)

        setupDrawerAndToolbar()

        //画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        payRecordAdapter = PayRecordAdapter(this,payRecordList)
        recyclerView.adapter = payRecordAdapter

        loadPayRecordList()

        buttonPayRecordAdd.setOnClickListener {
            val intent = Intent(this, AddPayRecordActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun loadPayRecordList() {
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM payment_history WHERE user_id = ?", arrayOf(userID))
        val newPayRecordList = mutableListOf<PayRecord>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex("_id"))
                val userId = cursor.getString(cursor.getColumnIndex("user_id"))
                val memberId = cursor.getLong(cursor.getColumnIndex("member_id"))
                val payPurposeId = cursor.getLong(cursor.getColumnIndex("purpose_id"))
                val payDate = cursor.getString(cursor.getColumnIndex("payment_date"))
                val payAmount = cursor.getInt(cursor.getColumnIndex("amount"))
                val isReceptChecked = cursor.getInt(cursor.getColumnIndex("is_recept_checked")) == 1
                val note = cursor.getString(cursor.getColumnIndex("note"))
                newPayRecordList.add(PayRecord(id, userId, memberId,payPurposeId,payDate,payAmount,isReceptChecked,note))

            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "支払い履歴が登録されていません。", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
        db.close()

        // データセットを更新
        payRecordList = newPayRecordList
        payRecordAdapter.updateData(payRecordList)
        payRecordAdapter.notifyDataSetChanged()
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("PAY_RECORD_DELETE", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadPayRecordList()
            }
        }
    }
}