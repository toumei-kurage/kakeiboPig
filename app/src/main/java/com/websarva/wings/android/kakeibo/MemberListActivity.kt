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

class MemberListActivity : BaseActivity(R.layout.activity_member_list,R.string.title_member_list) {
    //画面部品の用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonMemberAdd: FloatingActionButton
    private var memberList: List<Member> = mutableListOf()
    private lateinit var memberAdapter: MemberAdapter

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

        // データベースから member のデータを取得して RecyclerView に表示
        loadMemberList()

        buttonMemberAdd.setOnClickListener {
            val intent = Intent(this, MemberAddActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun loadMemberList() {
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM member WHERE user_id = ?", arrayOf(userID))
        val newMemberList = mutableListOf<Member>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex("_id"))
                val userId = cursor.getString(cursor.getColumnIndex("user_id"))
                val memberName = cursor.getString(cursor.getColumnIndex("member_name"))
                newMemberList.add(Member(id, userId, memberName))

            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "メンバーが登録されていません。", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
        db.close()

        // データセットを更新
        memberList = newMemberList
        memberAdapter.updateData(memberList)
        memberAdapter.notifyDataSetChanged()
    }

    // onActivityResultをオーバーライドして削除結果を受け取る
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