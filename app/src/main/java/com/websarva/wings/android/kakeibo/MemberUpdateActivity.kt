package com.websarva.wings.android.kakeibo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.websarva.wings.android.kakeibo.helper.ValidateHelper

class MemberUpdateActivity : BaseActivity(R.layout.activity_member_update, R.string.title_member_update) {

    // 画面部品の用意
    private lateinit var memberNameError: TextInputLayout
    private lateinit var memberNameEditText: EditText
    private lateinit var buttonMemberUpdate: Button

    // Firestoreインスタンス
    private val firestore = FirebaseFirestore.getInstance()

    // メンバーID
    private var memberId: String = ""

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_update)

        setupDrawerAndToolbar()

        // 画面部品取得
        memberNameError = findViewById(R.id.memberName)
        memberNameEditText = findViewById(R.id.memberNameEditText)
        buttonMemberUpdate = findViewById(R.id.buttonMemberUpdate)

        // 渡されたデータを受け取る
        memberId = intent.getStringExtra("MEMBER_ID") ?: ""
        val memberName = intent.getStringExtra("MEMBER_NAME")

        memberNameEditText.setText(memberName)

        // メンバーネームのフォーカスが外れた時のバリデーションチェック
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

        buttonMemberUpdate.setOnClickListener {
            clearBordFocus()
            val (resultMemberName: Boolean, memberNameMsg: String) = validateHelper.usernameCheck(
                memberNameEditText
            )
            if (!resultMemberName) {
                memberNameError.error = memberNameMsg
                return@setOnClickListener
            }

            clearErrorMessage()
            checkForDuplicateAndUpdate()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()  // 削除確認ダイアログを表示
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // メニュー（ActionBar）の作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(memberNameEditText.windowToken, 0)
        // フォーカスを外す処理
        memberNameEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        memberNameError.error = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // 重複チェックと更新処理
    private fun checkForDuplicateAndUpdate() {
        val newMemberName = memberNameEditText.text.toString()

        // 重複をチェックするクエリを作成
        val query: Query = firestore.collection("members")
            .whereEqualTo("user_id", userID)  // user_idを基に検索
            .whereEqualTo("member_name", newMemberName)  // member_nameが一致するものを検索

        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // 重複がない場合、更新処理を実行
                    updateMember(newMemberName)
                } else {
                    // 重複がある場合
                    Toast.makeText(this, "このメンバー名はすでに存在します", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "重複チェックに失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // メンバーの更新
    private fun updateMember(newMemberName: String) {
        val memberRef = firestore.collection("members").document(memberId)

        val updatedData = hashMapOf(
            "member_name" to newMemberName,
            "user_id" to userID
        )

        memberRef.set(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "更新されました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "更新できませんでした: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 削除確認ダイアログを表示
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("本当に削除しますか？")
            .setCancelable(false)
            .setPositiveButton("YES") { _, _ -> deleteMember() }
            .setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }

        val alert = builder.create()
        alert.show()
    }

    // メンバーを削除する処理
    private fun deleteMember() {
        val memberRef = firestore.collection("members").document(memberId)

        memberRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "削除されました", Toast.LENGTH_SHORT).show()
                // 削除成功した場合、親Activityに通知する
                val resultIntent = Intent()
                resultIntent.putExtra("MEMBER_DELETED", true)  // 削除フラグを渡す
                setResult(RESULT_OK, resultIntent)  // 削除成功の結果を返す
                finish()  // アクティビティを終了し、前の画面に戻る
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "削除できませんでした: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
