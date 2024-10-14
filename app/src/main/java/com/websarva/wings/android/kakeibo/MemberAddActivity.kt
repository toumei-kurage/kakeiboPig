package com.websarva.wings.android.kakeibo

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.MemberViewModel
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.member.PersonDao

class MemberAddActivity : BaseActivity(R.layout.activity_member_add, R.string.title_member_add) {
    private val validateHelper = ValidateHelper(this)
    private val dialogHelper = DialogHelper(this)

    private lateinit var personDao: PersonDao

    private lateinit var memberViewModel: MemberViewModel

    private lateinit var memberNameError: TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberAdd: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_add)

        setupDrawerAndToolbar()

        memberViewModel = ViewModelProvider(this)[MemberViewModel::class.java]

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

        buttonMemberAdd.setOnClickListener {
            clearBordFocus()
            val (resultMemberName: Boolean, memberNameMsg: String) = validateHelper.usernameCheck(
                memberNameEditText
            )
            if (!resultMemberName) {
                memberNameError.error = memberNameMsg
                return@setOnClickListener
            } else {
                val memberName = memberNameEditText.text.toString()
                val person = Person(userID = userID, memberName = memberName)

                // Personエンティティをデータベースに登録
                // メンバー追加処理を呼び出す
                memberViewModel.addPerson(person) { result ->
                    if (result.success) {
                        dialogHelper.dialogOkOnly("登録成功", result.message)
                    } else {
                        dialogHelper.dialogOkOnly("登録失敗", result.message)
                    }

                }
            }

        }

    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(memberNameEditText.windowToken, 0)
        //フォーカスを外す処理
        memberNameEditText.clearFocus()
    }
}