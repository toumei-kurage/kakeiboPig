package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MemberListActivity : BaseActivity(R.layout.activity_member_list, R.string.title_member_list) {
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonMemberAdd: FloatingActionButton
    private var memberList: List<Member> = mutableListOf()
    private lateinit var memberAdapter: MemberAdapter

    private val firestore = FirebaseFirestore.getInstance()

    // ✅ Activity Result API を使ってコールバックを登録
    @RequiresApi(Build.VERSION_CODES.O)
    private val editMemberLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            // 削除された場合の処理
            if (data?.getBooleanExtra("MEMBER_DELETED", false) == true) {
                // 削除後にデータを再読み込みしてRecyclerViewを更新
                loadMemberList()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
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

                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newMemberList.sortBy {
                    val dateString = it.resistDate
                    try {
                        LocalDateTime.parse(dateString, dateFormat) // LocalDateTime に変換
                    } catch (e: Exception) {
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0) // 変換エラー時には 1970-01-01 を返す
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


    // ✅ 他クラスから Activity を起動するためのヘルパー
    @RequiresApi(Build.VERSION_CODES.O)
    fun launchEditMember(intent: Intent) {
        editMemberLauncher.launch(intent)
    }
}
