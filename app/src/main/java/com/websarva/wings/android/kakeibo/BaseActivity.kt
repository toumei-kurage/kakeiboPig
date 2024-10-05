package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class BaseActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        // DrawerLayoutとNavigationViewのセットアップ
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.app_name)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarのハンバーガーメニューアイコン設定
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションメニューアイテムのクリックリスナー
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home ->{
                    //ホーム画面に遷移
                    val intent = Intent(this,HomeActivity::class.java)
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
    }

    // ハンバーガーメニューのクリック対応
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}