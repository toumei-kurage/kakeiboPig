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
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurpose
import com.websarva.wings.android.kakeibo.room.payPurpose.PayPurposeViewModel

class PayPurposeAddActivity :
    BaseActivity(R.layout.activity_pay_purpose_add, R.string.title_pay_purpose_add) {
    private lateinit var payPurposeViewModel: PayPurposeViewModel
    private lateinit var dialogHelper: DialogHelper
    private lateinit var validateHelper: ValidateHelper

    private lateinit var payPurposeNameEditText: EditText
    private lateinit var payPurposeNameError: TextInputLayout
    private lateinit var buttonPayPurposeAdd: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_purpose_add)

        setupDrawerAndToolbar()

        payPurposeViewModel = ViewModelProvider(this)[PayPurposeViewModel::class.java]
        dialogHelper = DialogHelper(this)
        validateHelper = ValidateHelper(this)

        payPurposeNameEditText = findViewById(R.id.payPurposeNameEditText)
        payPurposeNameError = findViewById(R.id.payPurposeName)
        buttonPayPurposeAdd = findViewById(R.id.buttonPayPurposeAdd)

        payPurposeNameEditText.onFocusChangeListener = View.OnFocusChangeListener{_,hasFocus->
            if(!hasFocus){
                //フォーカスが外れた時の処理
                val (result,errorMessage) = validateHelper.payPurposeNameCheck(payPurposeNameEditText)
                if(!result){
                    payPurposeNameError.error = errorMessage
                    return@OnFocusChangeListener
                }
                payPurposeNameError.error = null
            }
        }

        buttonPayPurposeAdd.setOnClickListener{
            clearBordFocus()
            val (resultPayPurposeName: Boolean, payPurposeNameMsg: String) = validateHelper.payPurposeNameCheck(
                payPurposeNameEditText
            )
            if (!resultPayPurposeName) {
                payPurposeNameError.error = payPurposeNameMsg
                return@setOnClickListener
            } else {
                val payPurposeName = payPurposeNameEditText.text.toString()
                val payPurpose = PayPurpose(userID = userID, payPurposeName = payPurposeName)

                // PayPurposeエンティティをデータベースに登録
                // 支払い目的追加処理を呼び出す
                payPurposeViewModel.addPayPurpose(payPurpose) { result ->
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
        inputMethodManager.hideSoftInputFromWindow(payPurposeNameEditText.windowToken, 0)
        //フォーカスを外す処理
        payPurposeNameEditText.clearFocus()
    }
}