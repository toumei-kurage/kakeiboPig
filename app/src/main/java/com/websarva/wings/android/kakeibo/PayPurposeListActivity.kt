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

class PayPurposeListActivity : BaseActivity(R.layout.activity_pay_purpose_list, R.string.title_pay_purpose_list) {
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayPurposeAdd: FloatingActionButton
    private var payPurposeList: List<PayPurpose> = mutableListOf()
    private lateinit var payPurposeAdapter: PayPurposeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_list)

        setupDrawerAndToolbar()

        //画面部品の取得
        buttonPayPurposeAdd = findViewById(R.id.buttonPayPurposeAdd)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        payPurposeAdapter = PayPurposeAdapter(this, payPurposeList)
        recyclerView.adapter = payPurposeAdapter

        // データベースから member のデータを取得して RecyclerView に表示
        loadPayPurposeList()

        buttonPayPurposeAdd.setOnClickListener {
            val intent = Intent(this, PayPurposeAddActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun loadPayPurposeList() {
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM payment_purpose WHERE user_id = ?", arrayOf(userID))
        val newPayPurposeList = mutableListOf<PayPurpose>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex("_id"))
                val userId = cursor.getString(cursor.getColumnIndex("user_id"))
                val payPurposeName = cursor.getString(cursor.getColumnIndex("pay_purpose_name"))
                newPayPurposeList.add(PayPurpose(id, userId, payPurposeName))

            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "支払い目的が登録されていません。", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
        db.close()

        // データセットを更新
        payPurposeList = newPayPurposeList
        payPurposeAdapter.updateData(payPurposeList)
        payPurposeAdapter.notifyDataSetChanged()
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("PAY_PURPOSE_DELETED", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadPayPurposeList()
            }
        }
    }
}