package com.websarva.wings.android.kakeibo0422

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.DialogHelper

class SecessionActivity : BaseActivity(R.layout.activity_secession, R.string.title_secession) {
    private lateinit var buttonSecessionOK: Button
    private lateinit var buttonSecessionNG: Button

    private val dialogHelper = DialogHelper(this)
    private lateinit var auth: FirebaseAuth

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

    private fun deleteUserData(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        // 削除対象のコレクション
        val collections = listOf("members", "payment_history", "payPurposes", "balance_history")

        val tasks = collections.map { collectionName ->
            db.collection(collectionName).whereEqualTo("user_id", userId).get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        batch.delete(document.reference)
                    }
                }
        }

        // すべてのデータ取得が完了したら削除を実行
        Tasks.whenAllSuccess<Any>(tasks).addOnSuccessListener {
            batch.commit().addOnSuccessListener {
                onSuccess() // すべてのデータ削除が成功したらユーザー削除へ
            }.addOnFailureListener(onFailure)
        }.addOnFailureListener(onFailure)
    }


    private fun deleteUser() {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid

            // Firestoreのデータ削除を実行
            deleteUserData(userId,
                onSuccess = {
                    // Firestoreのデータ削除が完了したらユーザーアカウント削除を実行
                    it.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this,"ユーザーの削除に成功しました",Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            dialogHelper.dialogOkOnly("", "ユーザーの削除に失敗しました")
                        }
                    }
                },
                onFailure = { e ->
                    Toast.makeText(this, "データ削除に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }


}
