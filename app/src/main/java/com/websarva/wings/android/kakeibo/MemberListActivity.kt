package com.websarva.wings.android.kakeibo

import BaseActivity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.MemberListViewModel
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.member.PersonAdapter
import com.websarva.wings.android.kakeibo.room.member.PersonDao

class MemberListActivity : BaseActivity(R.layout.activity_member_list,R.string.title_member_list) {
    private lateinit var viewModel: MemberListViewModel
    private lateinit var personDao: PersonDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var personAdapter: PersonAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        setupDrawerAndToolbar()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        personDao = db.personDao() // DAOのインスタンスを取得

        // ViewModelのインスタンスを生成し、ユーザーIDを渡す
        viewModel = ViewModelProvider(this)[MemberListViewModel::class.java]

        //画面部品の取得
        //メンバー追加ボタン
        val buttonMemberAdd = findViewById<FloatingActionButton>(R.id.buttonMemberAdd)
        //リストを取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        buttonMemberAdd.setOnClickListener {
            val intent = Intent(this, MemberAddActivity::class.java)
            startActivity(intent)
        }

        // メンバーのリストを監視して更新する
        viewModel.getPersons(userID).observe(this) { persons ->
            if (persons != null && persons.isNotEmpty()) {
                personAdapter = PersonAdapter(
                    personList = persons,
                    onUpdateClick = { person ->
                        showUpdateDialog(person)
                    },
                    onDeleteClick = { person ->
                        viewModel.deletePerson(person)
                    }
                )
                recyclerView.adapter = personAdapter
            } else {
                // personsがnullまたは空の場合に適切な処理を追加
                // 例えば、"メンバーがいません"と表示するなど
                personAdapter = PersonAdapter(
                    personList = listOf(), // 空のリストを渡す
                    onUpdateClick = { /* 空のリストなので操作なし */ },
                    onDeleteClick = { /* 空のリストなので操作なし */ }
                )
                recyclerView.adapter = personAdapter
            }
        }
    }

    // 更新用のダイアログを表示
    private fun showUpdateDialog(person: Person) {
        val editText = EditText(this)
        editText.setText(person.memberName)

        AlertDialog.Builder(this)
            .setTitle("メンバーの名前を更新")
            .setView(editText)
            .setPositiveButton("更新") { dialog, _ ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty()) {
                    person.memberName = newName
                    viewModel.updatePerson(person)
                }
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}