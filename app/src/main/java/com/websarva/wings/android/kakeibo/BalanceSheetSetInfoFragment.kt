package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import java.util.Calendar
import android.view.ViewGroup as ViewGroup1

class BalanceSheetSetInfoFragment : DialogFragment() {
    private lateinit var budgetError:TextInputLayout
    private lateinit var budgetEditText: EditText
    private lateinit var startDateError: TextInputLayout
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateError: TextInputLayout
    private lateinit var finishDateEditText: EditText
    private lateinit var buttonOK: Button

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var validateHelper: ValidateHelper

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup1?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_balance_sheet_set_info, container, false)
        budgetError = view.findViewById(R.id.budget)
        budgetEditText = view.findViewById(R.id.budgetEditText)
        startDateError = view.findViewById(R.id.startDate)
        startDateEditText = view.findViewById(R.id.startDateEditText)
        finishDateError = view.findViewById(R.id.finishDate)
        finishDateEditText = view.findViewById(R.id.finishDateEditText)
        buttonOK = view.findViewById(R.id.buttonOK)

        databaseHelper = DatabaseHelper(requireContext())
        validateHelper = ValidateHelper(requireContext())

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
            val budget = budgetEditText.text.toString().takeIf { it.isNotEmpty() }
            val startDate = startDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val finishDate = finishDateEditText.text.toString().takeIf { it.isNotEmpty() }

            val (resultBudget, budgetMsg) = validateHelper.payAmountCheck(budgetEditText)
            val (resultStartDate,startDateMsg) = validateHelper.dateCheck(startDateEditText)
            val (resultFinishDate,finishDateMsg) = validateHelper.dateCheck(finishDateEditText)

            budgetError.error = if(!resultBudget) budgetMsg else null
            startDateError.error = if(!resultStartDate) startDateMsg else null
            finishDateError.error = if(!resultFinishDate) finishDateMsg else null

            if(!(resultBudget && resultStartDate && resultFinishDate)){
                return@setOnClickListener
            }

            // 入力された内容をアクティビティに渡す
            (activity as? HomeActivity)?.setInfo(budget!!.toInt(), startDate!!, finishDate!!)

            dismiss()
        }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup1.LayoutParams.MATCH_PARENT, ViewGroup1.LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
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
        val inputMethodManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(startDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(finishDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(budgetEditText.windowToken, 0)
        startDateEditText.clearFocus()
        finishDateEditText.clearFocus()
        budgetEditText.clearFocus()
    }
}
