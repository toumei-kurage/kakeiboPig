package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.helper.DialogHelper

class SecessionActivity : BaseActivity(R.layout.activity_secession,R.string.title_secession) {
    private lateinit var buttonSecessionOK:Button
    private lateinit var buttonSecessionNG:Button

    private lateinit var dialogHelper: DialogHelper

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secession)

        setupDrawerAndToolbar()

        auth = FirebaseAuth.getInstance()
        dialogHelper = DialogHelper(this)

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

    private fun deleteUser() {
        val user = auth.currentUser
        user?.let {
            // ユーザーを削除
            it.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 削除成功後、LoginActivityに遷移
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
}