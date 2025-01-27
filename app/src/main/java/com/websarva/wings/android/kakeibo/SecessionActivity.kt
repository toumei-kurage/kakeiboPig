package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.DialogHelper

class SecessionActivity : BaseActivity(R.layout.activity_secession,R.string.title_secession) {
    private lateinit var buttonSecessionOK:Button
    private lateinit var buttonSecessionNG:Button

    private val dialogHelper = DialogHelper(this)
    private val databaseHelper = DatabaseHelper(this)

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secession)

        setupDrawerAndToolbar()

        auth = FirebaseAuth.getInstance()

        buttonSecessionOK = findViewById(R.id.secessionOK)
        buttonSecessionNG = findViewById(R.id.secessionNG)

        buttonSecessionOK.setOnClickListener{
            deleteUser()
        }
        buttonSecessionNG.setOnClickListener{
            val intent = Intent(this,HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deleteUserData(userId: String) {
        val db = databaseHelper.writableDatabase

        // トランザクション開始
        db.beginTransaction()

        try {
            // 1. payment_history テーブルから該当する user_id のデータを削除
            db.execSQL("DELETE FROM payment_history WHERE user_id = ?", arrayOf(userId))

            // 2. payment_purpose テーブルから該当する user_id のデータを削除
            db.execSQL("DELETE FROM payment_purpose WHERE user_id = ?", arrayOf(userId))

            // 3. balance_history テーブルから該当する user_id のデータを削除
            db.execSQL("DELETE FROM balance_history WHERE user_id = ?", arrayOf(userId))

            // 4. member テーブルから該当する user_id のデータを削除
            db.execSQL("DELETE FROM member WHERE user_id = ?", arrayOf(userId))

            // トランザクションをコミット
            db.setTransactionSuccessful()

            // 成功した場合はトースト等で通知
            Toast.makeText(this, "ユーザー情報と関連データが削除されました", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // 例外発生時にはロールバック
            Toast.makeText(this, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            // トランザクション終了
            db.endTransaction()
        }
    }

    private fun deleteUser() {
        val user = auth.currentUser
        deleteUserData(userID)
        user?.let {
            // ユーザーを削除
            it.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 削除成功後、ユーザーに紐づくデータベースのレコードを削除し、LoginActivityに遷移
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // 現在のアクティビティを終了
                } else {
                    dialogHelper.dialogOkOnly("","ユーザーの削除に失敗しました")
                }
            }
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}