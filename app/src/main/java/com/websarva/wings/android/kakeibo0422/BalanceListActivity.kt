package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BalanceListActivity : BaseActivity(R.layout.activity_balance_list, R.string.title_balance_list) {
    // 画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private var balanceList: List<Balance> = mutableListOf()
    private lateinit var balanceAdapter: BalanceAdapter

    private val firestore = FirebaseFirestore.getInstance()

    // ✅ Activity Result API を使ってコールバックを登録
    private val editBalanceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            // 削除された場合の処理
            if (data?.getBooleanExtra("BALANCE_DELETE", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadBalanceList()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_list)

        setupDrawerAndToolbar()

        // 画面部品の取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        balanceAdapter = BalanceAdapter(this, balanceList)
        recyclerView.adapter = balanceAdapter

        loadBalanceList()
    }

    // Firestore から家計簿リストを読み込む
    @SuppressLint("NotifyDataSetChanged")
    private fun loadBalanceList() :List<Balance>{
        val query = firestore.collection("balance_history")
            .whereEqualTo("user_id", userID)

        val newBalanceList = mutableListOf<Balance>()
        query.get().addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val balance = Balance(
                        document.id,
                        document.getString("user_id") ?: "",
                        document.getString("start_date") ?: "",
                        document.getString("finish_date") ?: "",
                        document.getLong("budget")?.toInt() ?: 0,
                        document.getLong("actual_balance")?.toInt() ?: 0
                    )
                    newBalanceList.add(balance)
                }

            // payment_date を Date 型に変換してからソート
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            newBalanceList.sortByDescending {
                val dateString = it.startDate
                try {
                    dateFormat.parse(dateString) ?: Date(0) // 変換できない場合は 1970-01-01 を返す
                } catch (e: Exception) {
                    Date(0) // 変換エラー時には 1970-01-01 を返す
                }
            }

            // データが取得できたらRecyclerViewを更新
            if (newBalanceList.isEmpty()) {
                Toast.makeText(this, "家計簿履歴が登録されていません。", Toast.LENGTH_SHORT).show()
            }

            // データセットを更新
            balanceList = newBalanceList
            balanceAdapter.updateData(balanceList)
            balanceAdapter.notifyDataSetChanged()
        }

        return newBalanceList
    }

    // ✅ 他クラスから Activity を起動するためのヘルパー
    fun launchEditBalance(intent: Intent) {
        editBalanceLauncher.launch(intent)
    }
}