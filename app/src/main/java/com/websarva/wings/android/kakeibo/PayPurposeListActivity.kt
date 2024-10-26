package com.websarva.wings.android.kakeibo

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.websarva.wings.android.kakeibo.helper.DialogHelper
import com.websarva.wings.android.kakeibo.room.member.PayPurposeAdapter
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurpose
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurposeViewModel

class PayPurposeListActivity :
    BaseActivity(R.layout.activity_pay_purpose_list, R.string.title_pay_purpose_list) {
    private lateinit var payPurposeViewModel: PayPurposeViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonPayPurposeAdd: FloatingActionButton
    private lateinit var payPurposeAdapter: PayPurposeAdapter
    private lateinit var dialogHelper: DialogHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_list)

        setupDrawerAndToolbar()

        payPurposeViewModel = ViewModelProvider(this)[PayPurposeViewModel::class.java]
        dialogHelper = DialogHelper(this)

        buttonPayPurposeAdd = findViewById(R.id.buttonPayPurposeAdd)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // リストビューに区切り線を入れる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        buttonPayPurposeAdd.setOnClickListener {
            val intent = Intent(this, PayPurposeAddActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        payPurposeViewModel.getPayPurposes(userID).observe(this){payPurposes ->
            if(payPurposes != null && payPurposes.isNotEmpty()){
                payPurposeAdapter = PayPurposeAdapter(
                    payPurposeList = payPurposes,
                    onUpdateClick = { payPurpose ->
                        showUpdateDialog(payPurpose)
                    },
                    onDeleteClick = { payPurpose ->
                        payPurposeViewModel.deletePayPurpose(payPurpose)
                    }
                )
                recyclerView.adapter = payPurposeAdapter
            }else{
                // payPurposesがnullまたは空の場合に適切な処理を追加
                // 例えば、"支払い目的が登録されていません"と表示するなど
                payPurposeAdapter = PayPurposeAdapter(
                    payPurposeList = listOf(), // 空のリストを渡す
                    onUpdateClick = { /* 空のリストなので操作なし */ },
                    onDeleteClick = { /* 空のリストなので操作なし */ }
                )
                recyclerView.adapter = payPurposeAdapter
            }
        }
    }

    // 更新用のダイアログを表示
    private fun showUpdateDialog(payPurpose: PayPurpose) {
        // LinearLayoutを作成
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        // 支払い目的名用のEditText
        val payPurposeNameEditText1 = EditText(this)
        payPurposeNameEditText1.setText(payPurpose.payPurposeName)
        payPurposeNameEditText1.hint = "支払い目的名"

        // LinearLayoutにEditTextを追加
        layout.addView(payPurposeNameEditText1)

        // AlertDialogを構築
        AlertDialog.Builder(this)
            .setTitle("支払い目的を更新")
            .setView(layout) // LinearLayoutをViewとして設定
            .setPositiveButton("更新") { dialog, _ ->
                val newName = payPurposeNameEditText1.text.toString()
                val oldName = payPurpose.payPurposeName // 退避

                if (newName.isNotEmpty()) {
                    payPurpose.payPurposeName = newName
                    payPurposeViewModel.updatePayPurpose(payPurpose) { result ->
                        if (result.success) {
                            dialogHelper.dialogOkOnly("登録成功", result.message)
                        } else {
                            dialogHelper.dialogOkOnly("登録失敗", result.message)
                            payPurpose.payPurposeName = oldName
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

}