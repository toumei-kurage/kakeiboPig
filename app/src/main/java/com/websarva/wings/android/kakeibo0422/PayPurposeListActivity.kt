package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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

class PayPurposeListActivity : BaseActivity(R.layout.activity_pay_purpose_list, R.string.title_pay_purpose_list) {
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayPurposeAdd: FloatingActionButton
    private var payPurposeList: List<PayPurpose> = mutableListOf()
    private lateinit var payPurposeAdapter: PayPurposeAdapter

    private val firestore = FirebaseFirestore.getInstance()

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
        // Firestoreの「members」コレクションからデータを取得
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)  // user_idが一致するドキュメントのみ取得
            .get()
            .addOnSuccessListener { querySnapshot ->
                // クエリ結果をリストに変換
                val newPayPurposeList = mutableListOf<PayPurpose>()
                for (document in querySnapshot.documents) {
                    val payPurposeName = document.getString("pay_purpose_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val payPurposeId = document.id  // FirestoreのドキュメントIDを使う（または任意のフィールド）
                    val userId = document.getString("user_id") ?: ""

                    newPayPurposeList.add(PayPurpose(payPurposeId, userId, payPurposeName, resistDate))
                }

                //Date 型に変換してからソート
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                newPayPurposeList.sortByDescending {
                    val dateString = it.resistDate
                    try {
                        dateFormat.parse(dateString) ?: Date(0) // 変換できない場合は 1970-01-01 を返す
                    } catch (e: Exception) {
                        Date(0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                // データが取得できたらRecyclerViewを更新
                if (newPayPurposeList.isEmpty()) {
                    Toast.makeText(this, "支払い目的が登録されていません。", Toast.LENGTH_SHORT).show()
                }

                // memberListを更新し、アダプターに通知
                payPurposeList = newPayPurposeList
                payPurposeAdapter.updateData(payPurposeList)
                payPurposeAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
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