package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemberListActivity : BaseActivity(R.layout.activity_member_list, R.string.title_member_list) {
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonMemberAdd: FloatingActionButton
    private var memberList: List<Member> = mutableListOf()
    private lateinit var memberAdapter: MemberAdapter

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        setupDrawerAndToolbar()

        //画面部品の取得
        buttonMemberAdd = findViewById(R.id.buttonMemberAdd)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        memberAdapter = MemberAdapter(this, memberList)
        recyclerView.adapter = memberAdapter

        // Firestoreからmemberのデータを取得してRecyclerViewに表示
        loadMemberList()

        buttonMemberAdd.setOnClickListener {
            val intent = Intent(this, MemberAddActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMemberList() {
        // Firestoreの「members」コレクションからデータを取得
        firestore.collection("members")
            .whereEqualTo("user_id", userID)  // user_idが一致するドキュメントのみ取得
            .get()
            .addOnSuccessListener { querySnapshot ->
                // クエリ結果をリストに変換
                val newMemberList = mutableListOf<Member>()
                for (document in querySnapshot.documents) {
                    val memberName = document.getString("member_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val memberId = document.id  // FirestoreのドキュメントIDを使う（または任意のフィールド）
                    val userId = document.getString("user_id") ?: ""

                    newMemberList.add(Member(memberId, userId, memberName, resistDate))
                }

                //Date 型に変換してからソート
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                newMemberList.sortByDescending {
                    val dateString = it.resistDate
                    try {
                        dateFormat.parse(dateString) ?: Date(0) // 変換できない場合は 1970-01-01 を返す
                    } catch (e: Exception) {
                        Date(0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                // データが取得できたらRecyclerViewを更新
                if (newMemberList.isEmpty()) {
                    Toast.makeText(this, "メンバーが登録されていません。", Toast.LENGTH_SHORT).show()
                }

                // memberListを更新し、アダプターに通知
                memberList = newMemberList
                memberAdapter.updateData(memberList)
                memberAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            // 削除された場合の処理
            if (data?.getBooleanExtra("MEMBER_DELETED", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadMemberList()
            }
        }
    }
}
