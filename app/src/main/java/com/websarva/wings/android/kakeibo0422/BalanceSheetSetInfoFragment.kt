package com.websarva.wings.android.kakeibo0422

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
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.util.Calendar
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.websarva.wings.android.kakeibo0422.repository.SettingRepository
import com.websarva.wings.android.kakeibo0422.util.DatePeriodUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BalanceSheetSetInfoFragment : DialogFragment() {
    private lateinit var budgetError:TextInputLayout
    private lateinit var budgetEditText: EditText
    private lateinit var startDateError: TextInputLayout
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateError: TextInputLayout
    private lateinit var finishDateEditText: EditText
    private lateinit var buttonOK: Button

    private lateinit var validateHelper: ValidateHelper

    companion object {
        private const val ARG_INITIAL_BUDGET = "ARG_INITIAL_BUDGET"

        fun newInstance(initialBudget: Int): BalanceSheetSetInfoFragment {
            val fragment = BalanceSheetSetInfoFragment()
            val args = Bundle().apply {
                putInt(ARG_INITIAL_BUDGET, initialBudget)
            }
            fragment.arguments = args
            return fragment
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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

        validateHelper = ValidateHelper(requireContext())

        // 初期予算が渡されていればセットする
        val initialBudget = arguments?.getInt(ARG_INITIAL_BUDGET, 0) ?: 0
        if (initialBudget > 0) {
            budgetEditText.setText(initialBudget.toString())
        }

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

        loadAndSetDefaultPeriod()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadAndSetDefaultPeriod() {
        val settingRepository = SettingRepository()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            val setting = settingRepository.getPeriodSetting()

            // 最新の家計簿の終了日を取得
            val latestFinishDate = getLatestBalanceFinishDate(userId)

            val (startDate, finishDate) = if (!latestFinishDate.isNullOrEmpty()) {
                // 最新家計簿の終了日以降で被らない期間を計算
                DatePeriodUtil.calculateNextPeriodAfter(
                    latestFinishDateStr = latestFinishDate,
                    startDay = setting.startDay,
                    finishDay = setting.finishDay
                )
            } else {
                // 家計簿が存在しない場合は現在日を基準に計算
                DatePeriodUtil.calculateCurrentPeriod(
                    startDay = setting.startDay,
                    finishDay = setting.finishDay
                )
            }

            // 空欄の場合のみデフォルト値をセット（ユーザーが手入力した内容を阻害しない）
            if (startDateEditText.text.isNullOrEmpty()) {
                startDateEditText.setText(startDate)
            }
            if (finishDateEditText.text.isNullOrEmpty()) {
                finishDateEditText.setText(finishDate)
            }
        }
    }

    private suspend fun getLatestBalanceFinishDate(userId: String): String? {
        if (userId.isEmpty()) return null
        return try {
            val snapshot = FirebaseFirestore.getInstance().collection("balance_history")
                .whereEqualTo("user_id", userId)
                .orderBy("start_date", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.first().getString("finish_date")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

}