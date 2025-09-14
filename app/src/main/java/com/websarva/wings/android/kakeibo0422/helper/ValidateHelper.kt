package com.websarva.wings.android.kakeibo0422.helper

import android.content.Context
import android.os.Build
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.websarva.wings.android.kakeibo0422.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 入力フォームのバリデーションチェック
 */
class ValidateHelper(private val context: Context) {
    //未入力チェック(空白の時false)
    private fun emptyCheck(text: String): Boolean {
        return text != ""
    }

    //電子メールの形式チェック(@が含まれていればtrue)
    private fun emailFormatCheck(email: String): Boolean {
        return email.contains("@")
    }

    /**
     * 桁数チェック
     * @param[text] 桁数チェックをしたい文字列
     */
    private fun lengthCheck(text: String): Boolean {
        return text.length >= 6
    }

    //半角数字チェック
    private fun numberFormatCheck(text: String): Boolean {
        val regex = Regex("^[0-9]+$")
        return regex.matches(text)
    }

    /**
     * 電子メールのバリデーションチェック
     */
    fun emailCheck(editTextEmail: EditText): Pair<Boolean, String> {
        val email = editTextEmail.text.toString()
        if (!emptyCheck(email)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        if (!emailFormatCheck(email)) {
            return Pair(false, context.getString(R.string.error_email))
        }
        return Pair(true, "")
    }

    /**
     * パスワードのバリデーションチェック
     */
    fun passwordCheck(editTextPassword: EditText): Pair<Boolean, String> {
        val password = editTextPassword.text.toString()
        if (!emptyCheck(password)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        if (!lengthCheck(password)) {
            return Pair(false, context.getString(R.string.error_digit_6))
        }
        return Pair(true, "")
    }

    /**
     * ユーザー名のバリデーションチェック
     */
    fun usernameCheck(editTextUsername: EditText): Pair<Boolean, String> {
        val username = editTextUsername.text.toString()
        if (!emptyCheck(username)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        return Pair(true, "")
    }

    /**
     * 支払金額のバリデーションチェック
     */
    fun payAmountCheck(payAmountEditText: EditText): Pair<Boolean, String> {
        val payAmount = payAmountEditText.text.toString()
        if (!emptyCheck(payAmount)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        if (!numberFormatCheck(payAmount)) {
            return Pair(false, context.getString(R.string.error_number_format))
        }
        if (payAmount.toInt() !in 1..1000000) {
            return Pair(false, context.getString(R.string.error_range_pay_amount,1,1000000))
        }
        return Pair(true, "")
    }

    /**
     * 実際残高のバリデーションチェック
     */
    fun actualBalanceCheck(payAmountEditText: EditText): Pair<Boolean, String> {
        val payAmount = payAmountEditText.text.toString()
        if (!emptyCheck(payAmount)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        if (!numberFormatCheck(payAmount)) {
            return Pair(false, context.getString(R.string.error_number_format))
        }
        if (payAmount.toInt() !in 0..1000000) {
            return Pair(false, context.getString(R.string.error_range_pay_amount,0,1000000))
        }
        return Pair(true, "")
    }

    /**
     * 支払日のバリデーションチェック
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun payDateCheck(payDateEditText: EditText): Pair<Boolean, String> {
        val payDateStr = payDateEditText.text.toString()
        if (!emptyCheck(payDateStr)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        val maxDate = LocalDate.now()
        val minDate = LocalDate.of(1900, 1, 1)
        val format = DateTimeFormatter.ofPattern("yyy/MM/dd")
        val payDate = LocalDate.parse(payDateStr, format)
        if (!((payDate.isEqual(maxDate) || payDate.isBefore(maxDate)) && payDate.isAfter(minDate))) {
            return Pair(false, context.getString(R.string.error_range_pay_date))
        }
        return Pair(true, "")
    }

    //日付型のバリデーションチェック
    @RequiresApi(Build.VERSION_CODES.O)
    fun dateCheck(dateEditText: EditText): Pair<Boolean, String> {
        val payDateStr = dateEditText.text.toString()
        if (!emptyCheck(payDateStr)) {
            return Pair(false, context.getString(R.string.error_empty))
        }
        val minDate = LocalDate.of(1900, 1, 1)
        val format = DateTimeFormatter.ofPattern("yyy/MM/dd")
        val payDate = LocalDate.parse(payDateStr, format)
        if (!payDate.isAfter(minDate)) {
            return Pair(false, context.getString(R.string.error_range_date))
        }
        return Pair(true, "")
    }


    /**
     * 支払い目的名のバリデーションチェック
     */
    fun payPurposeNameCheck(payPurposeNameEditText: EditText):Pair<Boolean,String>{
        val payPurposeName = payPurposeNameEditText.text.toString()
        if(!emptyCheck(payPurposeName)){
            return Pair(false,context.getString(R.string.error_empty))
        }
        return Pair(true,"")
    }

    /**
     * Spinnerのバリデーションチェック
     */
    fun selectedCheck(selectItem:String):Pair<Boolean,String>{
        if(selectItem.contains(context.getString(R.string.un_selected))){
            return Pair(false,context.getString(R.string.error_selected))
        }
        return Pair(true,"")
    }
}