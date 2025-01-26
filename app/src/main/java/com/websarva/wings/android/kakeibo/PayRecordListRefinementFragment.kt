package com.websarva.wings.android.kakeibo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.websarva.wings.android.kakeibo.helper.DatabaseHelper
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import java.util.Calendar
import android.view.ViewGroup as ViewGroup1

class PayRecordListRefinementFragment : DialogFragment() {
    //画面部品の用意
    private lateinit var spinnerMember: Spinner
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateEditText: EditText
    private lateinit var buttonOK: Button
    private lateinit var databaseHelper: DatabaseHelper

    //ヘルパークラス
    private lateinit var validateHelper:ValidateHelper

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup1?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pay_record_list_refinement, container, false)

        //ログイン中のユーザーIDを取得
        val userID = FirebaseAuth.getInstance().currentUser?.uid.toString()

        //画面部品の取得
        spinnerMember = view.findViewById(R.id.spinnerMember)
        startDateEditText = view.findViewById(R.id.startDateEditText)
        finishDateEditText = view.findViewById(R.id.finishDateEditText)
        buttonOK = view.findViewById(R.id.buttonOK)

        databaseHelper = DatabaseHelper(requireContext())
        validateHelper = ValidateHelper(requireContext())

        //Memberデータを取得しSpinnerにセット
        val member = arrayOf(getString(R.string.un_selected)) + databaseHelper.getMemberForUser(userID)
        val memberArrayAdapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item, member)
        memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMember.adapter = memberArrayAdapter

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

        //OKボタンが押された時の処理
        buttonOK.setOnClickListener {
            // メンバー名の選択
            val selectedMemberName = spinnerMember.selectedItem?.toString()

            // メンバーIDが選択されたか確認
            val memberId = if (selectedMemberName != getString(R.string.un_selected)) {
                databaseHelper.getMemberId(userID, selectedMemberName!!)
            } else {
                null
            }

            // 日付の範囲の入力値を取得
            val startDate = startDateEditText.text.toString().takeIf { it.isNotEmpty() }
            val finishDate = finishDateEditText.text.toString().takeIf { it.isNotEmpty() }

            // 日付の入力チェック
            if ((startDate != null && finishDate == null) || (startDate == null && finishDate != null)) {
                // 両方の入力がある場合にのみ処理を進める
                Toast.makeText(context, "開始日と終了日は両方入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 入力された絞り込み内容をアクティビティに渡す
            (activity as? PayRecordListActivity)?.applyRefinement(
                memberId?.toInt(),
                startDate,
                finishDate
            )

            dismiss()  // フラグメントを閉じる
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // ダイアログのサイズを設定
        dialog?.window?.setLayout(
            ViewGroup1.LayoutParams.MATCH_PARENT, // 横幅
            ViewGroup1.LayoutParams.WRAP_CONTENT // 縦幅（内容に応じて自動調整）
        )
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // 日付を選択したときの処理
                val formattedDate = String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
                editText.setText(formattedDate) // EditTextに日付を設定

                // OKボタンが押されたときにバリデーションを行う
                val (result, errorMessage) = validateHelper.payDateCheck(editText)
                if (!result) {
                    editText.error = errorMessage
                } else {
                    editText.error = null // エラーメッセージをクリア
                }

            }, year, month, day)

        // ダイアログのキャンセルボタンが押されたときの処理
        datePickerDialog.setOnCancelListener {
            // 必要な処理があればここに記述
        }
        datePickerDialog.show()
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(startDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(finishDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spinnerMember.windowToken, 0)
        //フォーカスを外す処理
        startDateEditText.clearFocus()
        finishDateEditText.clearFocus()
        spinnerMember.clearFocus()
    }
}
