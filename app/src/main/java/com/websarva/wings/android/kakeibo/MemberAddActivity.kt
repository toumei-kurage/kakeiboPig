package com.websarva.wings.android.kakeibo

import BaseActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
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

class MemberAddActivity : BaseActivity(R.layout.activity_member_add,R.string.title_member_add) {
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    private lateinit var auth: FirebaseAuth // Firebase Authentication
    private lateinit var personDao: PersonDao

    private lateinit var memberNameError:TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberAdd:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_add)

        setupDrawerAndToolbar()

        // データベースのインスタンスを取得
        val db = AppDatabase.getDatabase(applicationContext)
        personDao = db.personDao() // DAOのインスタンスを取得

        //画面部品取得
        memberNameError = findViewById(R.id.memberName)
        memberNameEditText = findViewById(R.id.memberNameEditText)
        buttonMemberAdd = findViewById(R.id.buttonMemberAdd)

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
                val person = Person(userID = userID, memberName = memberName)
                // Personエンティティをデータベースに登録
                addPerson(person)
                dialogHelper.dialogOkOnly("","メンバーが登録されました")
            }
        }

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