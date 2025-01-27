package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper

class BalanceListActivity : BaseActivity(R.layout.activity_balance_list, R.string.title_balance_list){
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private var balanceList: List<Balance> = mutableListOf()
    private lateinit var balanceAdapter: BalanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_list)

        setupDrawerAndToolbar()

        //画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        balanceAdapter = BalanceAdapter(this,balanceList)
        recyclerView.adapter = balanceAdapter

        loadBalanceList()
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("BALANCE_DELETE", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadBalanceList()
            }
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun loadBalanceList() {
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM balance_history WHERE user_id = ?", arrayOf(userID))
        val newBalanceList = mutableListOf<Balance>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex("_id"))
                val userId = cursor.getString(cursor.getColumnIndex("user_id"))
                val startDate = cursor.getString(cursor.getColumnIndex("start_date"))
                val finishDate = cursor.getString(cursor.getColumnIndex("finish_date"))
                val budget = cursor.getInt(cursor.getColumnIndex("budget"))
                newBalanceList.add(Balance(id, userId, startDate,finishDate,budget))

            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "家計簿が登録されていません。", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
        db.close()

        // データセットを更新
        balanceList = newBalanceList
        balanceAdapter.updateData(balanceList)
        balanceAdapter.notifyDataSetChanged()
    }
}