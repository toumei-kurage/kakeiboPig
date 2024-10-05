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
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.MemberListViewModel
import com.websarva.wings.android.kakeibo.room.Person
import com.websarva.wings.android.kakeibo.room.PersonAdapter
import com.websarva.wings.android.kakeibo.room.PersonDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberListActivity : BaseActivity(R.layout.activity_member_list,R.string.title_member_list) {
    private lateinit var viewModel: MemberListViewModel
    private lateinit var personDao: PersonDao
    private lateinit var currentUserId: String // ログインユーザーのIDを格納する変数
    private lateinit var recyclerView: RecyclerView
    private lateinit var personAdapter: PersonAdapter
    private lateinit var personList: List<Person> // 登録した人名のリスト


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        setupDrawerAndToolbar()

        //ログイン中のユーザーIDを取得
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        personDao = db.personDao() // DAOのインスタンスを取得

        // ViewModelのインスタンスを生成し、ユーザーIDを渡す
        viewModel = ViewModelProvider(this).get(MemberListViewModel::class.java)



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

        loadPersons()

        // メンバーのリストを監視して更新する
        viewModel.personList.observe(this) { persons ->
            val adapter = PersonAdapter(persons,
                onUpdateClick = { person ->
                    showUpdateDialog(person)
                },
                onDeleteClick = { person ->
                    viewModel.deletePerson(person)
                }
            )
            recyclerView.adapter = adapter
        }

    }

    private fun loadPersons() {
        CoroutineScope(Dispatchers.IO).launch {
            personList = personDao.getAllPersonsByUserId(currentUserId)

            withContext(Dispatchers.Main) {
                // RecyclerViewにアダプターを設定
                personAdapter = PersonAdapter(
                    personList = personList, // メンバーのリストを渡す
                    onUpdateClick = { person ->
                        showUpdateDialog(person) // 更新処理を定義
                    },
                    onDeleteClick = { person ->
                        viewModel.deletePerson(person) // 削除処理を定義
                    }
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