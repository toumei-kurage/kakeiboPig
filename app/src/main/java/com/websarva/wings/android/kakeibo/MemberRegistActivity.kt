package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MemberRegistActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_regist)

        // Roomデータベースのインスタンスを取得


        //画面部品取得
        val memberNameError = findViewById<TextInputLayout>(R.id.memberName)
        val memberNameEditText = findViewById<EditText>(R.id.memberNameEditText)
        val buttonMemberAdd = findViewById<Button>(R.id.buttonMemberAdd)

        // DrawerLayoutとNavigationViewのセットアップ
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)


        // Toolbarを設定
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.title_member_regist)
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

        // アイコンのクリックリスナーを設定
        toolbar.setNavigationIcon(R.drawable.ic_hamberger_menu) // ハンバーガーアイコンを表示

        //メンバーネームのフォーカスが外れた時のバリデーションチェック
        memberNameEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたときの処理
                val (result: Boolean, errorMsg: String) = validateHelper.usernameCheck(memberNameEditText)
                if (!result) {
                    memberNameError.error = errorMsg
                    return@OnFocusChangeListener
                }
                memberNameError.error = ""
            }
        }

        buttonMemberAdd.setOnClickListener{
            clearBordFocus()
            val(resultMemberName:Boolean,memberNameMsg:String) = validateHelper.usernameCheck(memberNameEditText)
            if(!resultMemberName){
                return@setOnClickListener
            }
            else{
                val memberName = memberNameEditText.text.toString()
                val userID = FirebaseAuth.getInstance().currentUser?.uid

                if (memberName.isNotEmpty() && userID != null) {
                    // Personエンティティをデータベースに登録
                    registerPerson(memberName, userID)
                    dialogHelper.dialogOkOnly("","メンバーが登録されました")
                }
            }
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