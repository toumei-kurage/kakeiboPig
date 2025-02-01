package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class BalanceListActivity : BaseActivity(R.layout.activity_balance_list, R.string.title_balance_list) {
    // 画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private var balanceList: List<Balance> = mutableListOf()
    private lateinit var balanceAdapter: BalanceAdapter

    private val firestore = FirebaseFirestore.getInstance()

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

    // onActivityResultをオーバーライドして削除結果を受け取る
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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
                        document.getLong("budget")?.toInt() ?: 0
                    )
                    newBalanceList.add(balance)
                }
                // データセットを更新
                balanceList = newBalanceList
                balanceAdapter.updateData(balanceList)
                balanceAdapter.notifyDataSetChanged()
        }

        return newBalanceList
    }
}
