package com.websarva.wings.android.kakeibo

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo.helper.ValidateHelper

class MemberAddActivity : BaseActivity(R.layout.activity_member_add, R.string.title_member_add) {
    //画面部品の用意
    private lateinit var memberNameError: TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberAdd: Button

    // ヘルパークラス
    private val validateHelper = ValidateHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_add)

        setupDrawerAndToolbar()

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
                memberNameError.error = null
            }
        }

        buttonMemberAdd.setOnClickListener {
            clearBordFocus()
            val (resultMemberName: Boolean, memberNameMsg: String) = validateHelper.usernameCheck(memberNameEditText)
            if (!resultMemberName) {
                memberNameError.error = memberNameMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            onSaveButtonClick()
        }

    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(memberNameEditText.windowToken, 0)
        //フォーカスを外す処理
        memberNameEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        memberNameError.error = null
    }

    private fun onSaveButtonClick() {
        val firestore = FirebaseFirestore.getInstance()

        // 現在ログインしているユーザーのIDを取得 (Firebase Authenticationを使用している場合)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "ユーザーがログインしていません", Toast.LENGTH_SHORT).show()
            return
        }

        // メンバー名とユーザーIDを取得
        val memberName = memberNameEditText.text.toString()

        // 「members」コレクションから、user_idとmember_nameの組み合わせで既に存在するかチェック
        val query = firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", memberName)

        query.get()
            .addOnSuccessListener { querySnapshot ->
                // クエリ結果が空ならば新しいメンバーを追加
                if (querySnapshot.isEmpty) {
                    // Firestoreに追加するデータ
                    val memberData = hashMapOf(
                        "user_id" to userID,
                        "member_name" to memberName
                    )

                    // Firestoreの「members」コレクションにデータを追加
                    firestore.collection("members")
                        .add(memberData)
                        .addOnSuccessListener {
                            // 成功した場合の処理
                            Toast.makeText(this, "メンバーが追加されました", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // エラーが発生した場合の処理
                            Toast.makeText(this, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // 既に同じ組み合わせのデータが存在する場合
                    Toast.makeText(this, "このメンバーは既に存在します", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // クエリ実行時のエラー処理
                Toast.makeText(this, "データベースの読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}