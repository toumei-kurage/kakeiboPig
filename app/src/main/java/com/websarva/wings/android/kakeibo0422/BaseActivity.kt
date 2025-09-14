package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity(private val layoutResId: Int, private val title: Int) : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    lateinit var toolbar: Toolbar
    lateinit var userID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId) // 各ActivityでsetContentViewを上書きするため、この行は必要に応じて変更

        // 戻るボタンを無効化
        onBackPressedDispatcher.addCallback(this) {
            // ここに何も書かない or 任意の処理を書く
        }

        // DrawerLayoutとNavigationViewのセットアップ
        setupDrawerAndToolbar()

        //ログイン中のユーザーIDを取得
        userID = FirebaseAuth.getInstance().currentUser?.uid.toString()
    }

    // DrawerLayoutとNavigationViewのセットアップを共通化
    fun setupDrawerAndToolbar() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Toolbarを設定
        toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(title)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        // ActionBarのハンバーガーメニューアイコン設定
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションメニューアイテムのクリックリスナー設定
        navView.setNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem.itemId)
            drawerLayout.closeDrawers() // メニューを閉じる
            true
        }

        // アイコンのクリックリスナーを設定
        toolbar.setNavigationIcon(R.drawable.ic_hamburger_menu) // ハンバーガーアイコンを表示
    }

    // 各Activityでメニューアイテムの動作をオーバーライド可能に
    open fun onNavigationItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }

            R.id.nav_member_list -> {
                val intent = Intent(this, MemberListActivity::class.java)
                startActivity(intent)
                finish()
            }

            R.id.nav_pay_record_list -> {
                val intent = Intent(this, PayRecordListActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_balance_list ->{
                val intent = Intent(this,BalanceListActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_user_update -> {
                val intent = Intent(this,UserUpdateActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_logout ->{
                logout()
            }
            R.id.nav_secession ->{
                val intent = Intent(this,SecessionActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_pay_purpose_list ->{
                val intent = Intent(this,PayPurposeListActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_license ->{
                OssLicensesMenuActivity.setActivityTitle("ライセンス")
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            }
            // 他のメニューアイテムを追加する場合はここに追加
        }
    }

    // ハンバーガーメニューのクリック対応
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        // SharedPreferencesを使用してセッションをクリアする
        val sharedPreferences: SharedPreferences = getSharedPreferences("userSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // セッション情報をクリア
        editor.apply()

        // LoginActivityへ遷移
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

        // HomeActivityを終了して戻れないようにする
        finish()
    }
}
