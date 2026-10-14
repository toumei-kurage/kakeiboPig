package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity(private val layoutResId: Int, private val titleResId: Int) :
    AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    protected lateinit var userID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)

        setupDrawerAndToolbar()

        // 端末の戻るボタン（ジェスチャー含む）を完全に無効化
        onBackPressedDispatcher.addCallback(this) {
            // 何も実行しないことで戻る操作を消費・無効化
        }

        userID = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    }

    /**
     * 追加・更新などのサブ画面かどうかを判定
     */
    private fun isSubActivity(): Boolean {
        return when (this) {
            is PayRecordAddActivity,
            is PayRecordUpdateActivity,
            is MemberAddActivity,
            is MemberUpdateActivity,
            is PayPurposeAddActivity,
            is PayPurposeUpdateActivity,
            is BalanceUpdateActivity,
            is BalanceDetailActivity-> true
            else -> false
        }
    }

    protected fun setupDrawerAndToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(titleResId)
        toolbar.setTitle(titleResId)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        if (isSubActivity()) {
            // 【サブ画面（追加・更新）】
            // 1. 戻るアイコンを表示
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            // 2. タップ時はドロワーを開かず、前画面に戻る
            toolbar.setNavigationOnClickListener {
                finish() // 画面上部の戻る矢印はアプリUIのため、画面を閉じる処理を直接呼ぶ
            }
            // 3. 誤スワイプでドロワーが開かないようにロック
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            // 【メイン画面（一覧・ホーム等）】
            // 1. ハンバーガーアイコンを表示
            toolbar.setNavigationIcon(R.drawable.ic_hamburger_menu)
            // 2. タップ時はドロワーメニューを開く
            toolbar.setNavigationOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            // 3. ドロワーのスワイプ操作を有効化
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        navView.setNavigationItemSelectedListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isSubActivity()) {
                    finish()
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (isCurrentActivity(item.itemId)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        val targetClass = when (item.itemId) {
            R.id.nav_home -> HomeActivity::class.java
            R.id.nav_member_list -> MemberListActivity::class.java
            R.id.nav_pay_record_list -> PayRecordListActivity::class.java
            R.id.nav_balance_list -> BalanceListActivity::class.java
            R.id.nav_user_update -> UserUpdateActivity::class.java
            R.id.nav_setting -> SettingActivity::class.java
            R.id.nav_pay_purpose_list -> PayPurposeListActivity::class.java
            R.id.nav_secession -> SecessionActivity::class.java
            R.id.nav_logout -> {
                logout()
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_license -> {
                OssLicensesMenuActivity.setActivityTitle("ライセンス")
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            else -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                return false
            }
        }

        val intent = Intent(this, targetClass)
        if (targetClass == HomeActivity::class.java) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun isCurrentActivity(itemId: Int): Boolean {
        return when (itemId) {
            R.id.nav_home -> this is HomeActivity
            R.id.nav_member_list -> this is MemberListActivity
            R.id.nav_pay_record_list -> this is PayRecordListActivity
            R.id.nav_balance_list -> this is BalanceListActivity
            R.id.nav_user_update -> this is UserUpdateActivity
            R.id.nav_setting -> this is SettingActivity
            R.id.nav_pay_purpose_list -> this is PayPurposeListActivity
            R.id.nav_secession -> this is SecessionActivity
            else -> false
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}