package com.websarva.wings.android.kakeibo

import BaseActivity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu

class HomeActivity : BaseActivity(R.layout.activity_home,R.string.title_home) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupDrawerAndToolbar()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    // メニューをインフレートする
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logout, menu)
        return true
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
