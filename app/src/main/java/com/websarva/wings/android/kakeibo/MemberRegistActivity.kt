package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.Person
import com.websarva.wings.android.kakeibo.room.PersonDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MemberRegistActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    private lateinit var auth: FirebaseAuth // Firebase Authentication
    private lateinit var personDao: PersonDao

    private lateinit var memberNameError:TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberAdd:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_regist)

        // Firebase Authenticationのインスタンスを取得
        auth = FirebaseAuth.getInstance()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        personDao = db.personDao() // DAOのインスタンスを取得

        //画面部品取得
        memberNameError = findViewById<TextInputLayout>(R.id.memberName)
        memberNameEditText = findViewById<EditText>(R.id.memberNameEditText)
        buttonMemberAdd = findViewById<Button>(R.id.buttonMemberAdd)

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
                memberNameError.error = memberNameMsg
                return@setOnClickListener
            }
            else{
                val memberName = memberNameEditText.text.toString()
                val userID = auth.currentUser?.uid ?: return@setOnClickListener // ログインしているユーザーのIDを取得
                val person = Person(userID = userID, memberName = memberName)
                if (userID != null) {
                    addPerson(person)
                    // Personエンティティをデータベースに登録
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

    private fun clearBordFocus(){
        val memberNameEditText = findViewById<EditText>(R.id.memberNameEditText)
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(memberNameEditText.windowToken, 0)
        //フォーカスを外す処理
        memberNameEditText.clearFocus()
    }

    private fun addPerson(person:Person) {
        // データベースに登録
        CoroutineScope(Dispatchers.IO).launch {
            personDao.insert(person)
        }
    }

}