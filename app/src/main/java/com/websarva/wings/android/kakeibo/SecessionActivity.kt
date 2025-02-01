package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo.helper.DialogHelper

class SecessionActivity : BaseActivity(R.layout.activity_secession, R.string.title_secession) {
    private lateinit var buttonSecessionOK: Button
    private lateinit var buttonSecessionNG: Button

    private val dialogHelper = DialogHelper(this)
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secession)

        setupDrawerAndToolbar()

        auth = FirebaseAuth.getInstance()

        buttonSecessionOK = findViewById(R.id.secessionOK)
        buttonSecessionNG = findViewById(R.id.secessionNG)

        buttonSecessionOK.setOnClickListener {
            deleteUser()
        }
        buttonSecessionNG.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deleteUserData(userId: String) {
        // Firestoreのコレクション参照を設定
        val paymentHistoryRef = firestore.collection("payment_history")
        val paymentPurposeRef = firestore.collection("payPurposes")
        val balanceHistoryRef = firestore.collection("balance_history")
        val memberRef = firestore.collection("members")

        // トランザクションで複数の削除を行う
        firestore.runTransaction { transaction ->
            // 非同期タスクを同期的に扱うためにTasks.await()を使用
            val paymentHistoryQuery = Tasks.await(paymentHistoryRef.whereEqualTo("user_id", userId).get())
            paymentHistoryQuery.documents.forEach { document ->
                transaction.delete(document.reference)
            }

            val paymentPurposeQuery = Tasks.await(paymentPurposeRef.whereEqualTo("user_id", userId).get())
            paymentPurposeQuery.documents.forEach { document ->
                transaction.delete(document.reference)
            }

            val balanceHistoryQuery = Tasks.await(balanceHistoryRef.whereEqualTo("user_id", userId).get())
            balanceHistoryQuery.documents.forEach { document ->
                transaction.delete(document.reference)
            }

            val memberQuery = Tasks.await(memberRef.whereEqualTo("user_id", userId).get())
            memberQuery.documents.forEach { document ->
                transaction.delete(document.reference)
            }
        }.addOnSuccessListener {
            Toast.makeText(this, "ユーザー情報と関連データが削除されました", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "データ削除に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteUser() {
        val user = auth.currentUser
        user?.let {
            // ユーザーの ID を取得
            val userId = it.uid

            // Firestore のデータ削除処理を実行
            deleteUserData(userId)

            // ユーザーを削除
            it.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 削除成功後、LoginActivity に遷移
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // 現在のアクティビティを終了
                } else {
                    dialogHelper.dialogOkOnly("", "ユーザーの削除に失敗しました")
                }
            }
        }
    }
}
