package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MemberListActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)
        //ログイン中のユーザーIDを取得
        val userId = FirebaseAuth.getInstance().currentUser?.uid

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
                R.id.nav_member_list -> {
                    // メンバー一覧画面に遷移
                    val intent = Intent(this, MemberListActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawers() // メニューを閉じる
            true
        }

        // アイコンのクリックリスナーを設定
        toolbar.setNavigationIcon(R.drawable.ic_hamberger_menu) // ハンバーガーアイコンを表示

        buttonMemberAdd.setOnClickListener{
            val intent = Intent(this,MemberRegistActivity::class.java)
            startActivity(intent)
        }

    }
    // ハンバーガーメニューのクリック対応
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}