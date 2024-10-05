package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.Person
import com.websarva.wings.android.kakeibo.room.PersonAdapter
import com.websarva.wings.android.kakeibo.room.PersonDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberListActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var personDao: PersonDao
    private lateinit var currentUserId: String // ログインユーザーのIDを格納する変数
    private lateinit var recyclerView: RecyclerView
    private lateinit var personAdapter: PersonAdapter
    private lateinit var personList: List<Person> // 登録した人名のリスト


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)
        //ログイン中のユーザーIDを取得
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        personDao = db.personDao() // DAOのインスタンスを取得

        //リストを取得
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // デフォルトのDividerItemDecorationを使う場合
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        //画面部品の取得
        val buttonMemberAdd = findViewById<FloatingActionButton>(R.id.buttonMemberAdd)

        // DrawerLayoutとNavigationViewのセットアップ
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_member_list)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarのハンバーガーメニューアイコン設定
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションメニューアイテムのクリックリスナー
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    //ホーム画面に遷移
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_member_list -> {
                    // メンバー登録画面に遷移
                    val intent = Intent(this, MemberListActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawers() // メニューを閉じる
            true
        }

        // アイコンのクリックリスナーを設定
        toolbar.setNavigationIcon(R.drawable.ic_hamberger_menu) // ハンバーガーアイコンを表示

        buttonMemberAdd.setOnClickListener {
            val intent = Intent(this, MemberRegistActivity::class.java)
            startActivity(intent)
        }

        loadPersons()

    }

    // ハンバーガーメニューのクリック対応
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadPersons() {
        CoroutineScope(Dispatchers.IO).launch {
            personList = personDao.getAllPersonsByUserId(currentUserId)

            withContext(Dispatchers.Main) {
                // RecyclerViewにアダプターを設定
                personAdapter = PersonAdapter(personList)
                recyclerView.adapter = personAdapter
            }
        }
    }
}