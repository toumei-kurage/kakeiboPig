package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.util.Calendar

class BalanceUpdateActivity : BaseActivity(R.layout.activity_balance_update,R.string.title_balance_update) {
    private lateinit var budgetError: TextInputLayout
    private lateinit var budgetEditText: EditText
    private lateinit var startDateError: TextInputLayout
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateError: TextInputLayout
    private lateinit var finishDateEditText: EditText
    private lateinit var buttonOK: Button

    private val validateHelper = ValidateHelper(this)
    private val firestore = FirebaseFirestore.getInstance()

    // 前画面からもらう値の用意
    private var balanceId = ""
    private var budgetSet: String = "0"
    private var startDate: String = ""
    private var finishDate: String = ""
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_update)

        setupDrawerAndToolbar()

        // 取得したデータをフィールドにセット
        balanceId = intent.getStringExtra("BALANCE_ID")?:""
        budgetSet = intent.getStringExtra("BUDGET").toString()
        startDate = intent.getStringExtra("START_DATE").toString()
        finishDate = intent.getStringExtra("FINISH_DATE").toString()

        budgetError = findViewById(R.id.budget)
        budgetEditText = findViewById(R.id.budgetEditText)
        startDateError = findViewById(R.id.startDate)
        startDateEditText = findViewById(R.id.startDateEditText)
        finishDateError = findViewById(R.id.finishDate)
        finishDateEditText = findViewById(R.id.finishDateEditText)
        buttonOK = findViewById(R.id.buttonUpdate)

        budgetEditText.setText(budgetSet)
        startDateEditText.setText(startDate)
        finishDateEditText.setText(finishDate)

        budgetEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val (result, errorMessage) = validateHelper.payAmountCheck(budgetEditText)
                if (!result) {
                    budgetError.error = errorMessage
                    return@OnFocusChangeListener
                }
            }
        }

        startDateEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearBordFocus()
                showDatePickerDialog(startDateEditText)
            }
        }

        finishDateEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearBordFocus()
                showDatePickerDialog(finishDateEditText)
            }
        }

        buttonOK.setOnClickListener {
            val (resultBudget, budgetMsg) = validateHelper.payAmountCheck(budgetEditText)
            val (resultStartDate,startDateMsg) = validateHelper.dateCheck(startDateEditText)
            val (resultFinishDate,finishDateMsg) = validateHelper.dateCheck(finishDateEditText)

            budgetError.error = if(!resultBudget) budgetMsg else null
            startDateError.error = if(!resultStartDate) startDateMsg else null
            finishDateError.error = if(!resultFinishDate) finishDateMsg else null

            if(!(resultBudget && resultStartDate && resultFinishDate)){
                return@setOnClickListener
            }
            updateBalance()
        }
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
            editText.setText(formattedDate)

            val (result, errorMessage) = validateHelper.dateCheck(editText)
            if (!result) {
                editText.error = errorMessage
            } else {
                editText.error = null
            }

        }, year, month, day)

        datePickerDialog.setOnCancelListener { }
        datePickerDialog.show()
    }

    private fun clearBordFocus() {
        val inputMethodManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(startDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(finishDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(budgetEditText.windowToken, 0)
        startDateEditText.clearFocus()
        finishDateEditText.clearFocus()
        budgetEditText.clearFocus()
    }

    private fun updateBalance() {
        try {
            val updatedRecord = hashMapOf(
                "start_date" to startDateEditText.text.toString(),
                "finish_date" to finishDateEditText.text.toString(),
                "budget" to budgetEditText.text.toString().toInt(),
            )

            // Firestore ドキュメントを更新
            firestore.collection("balance_history")
                .document(balanceId)
                .update(updatedRecord as Map<String, Any>)
                .addOnSuccessListener {
                    showToast("家計簿記録が更新されました")
                }
                .addOnFailureListener { e ->
                    showToast("更新に失敗しました: ${e.message}")
                }

        } catch (e: Exception) {
            showToast("エラーが発生しました: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}