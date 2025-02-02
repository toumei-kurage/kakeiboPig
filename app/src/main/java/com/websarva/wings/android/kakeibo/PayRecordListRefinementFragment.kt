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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo.helper.ValidateHelper
import java.util.Calendar
import android.view.ViewGroup as ViewGroup1

class PayRecordListRefinementFragment : DialogFragment() {
    private lateinit var spinnerMember: Spinner
    private lateinit var memberListError: TextView
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateEditText: EditText
    private lateinit var buttonOK: Button
    private lateinit var validateHelper: ValidateHelper
    private val firestore = FirebaseFirestore.getInstance()

    private var memberId: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup1?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pay_record_list_refinement, container, false)

        val userID = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // 画面部品の取得
        spinnerMember = view.findViewById(R.id.spinnerMember)
        startDateEditText = view.findViewById(R.id.startDateEditText)
        finishDateEditText = view.findViewById(R.id.finishDateEditText)
        buttonOK = view.findViewById(R.id.buttonOK)
        memberListError = view.findViewById(R.id.memberListError)

        validateHelper = ValidateHelper(requireContext())

        // Memberデータを取得しSpinnerにセット
        loadMembersFromFirestore()

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
            handleOkButtonClick(userID)
        }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup1.LayoutParams.MATCH_PARENT, ViewGroup1.LayoutParams.WRAP_CONTENT)
    }

    private fun clearBordFocus() {
        val inputMethodManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(startDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(finishDateEditText.windowToken, 0)
        startDateEditText.clearFocus()
        finishDateEditText.clearFocus()
    }

    @SuppressLint("DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
                editText.setText(formattedDate)

                val (result, errorMessage) = validateHelper.payDateCheck(editText)
                editText.error = if (result) null else errorMessage
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun handleOkButtonClick(userID: String) {
        val selectedMemberName = spinnerMember.selectedItem?.toString()
        val startDate = startDateEditText.text.toString().takeIf { it.isNotEmpty() }
        val finishDate = finishDateEditText.text.toString().takeIf { it.isNotEmpty() }

        if ((startDate != null && finishDate == null) || (startDate == null && finishDate != null)) {
            Toast.makeText(context, "開始日と終了日は両方入力してください。", Toast.LENGTH_SHORT).show()
            return
        }

        if (!selectedMemberName!!.contains(requireContext().getString(R.string.un_selected))) {
            getMemberDocumentId(userID, selectedMemberName) { memberDocId ->
                if (memberDocId != null) {
                    memberId = memberDocId
                    // 取得が完了した後にapplyRefinementを呼び出す
                    applyRefinementAndDismiss(startDate, finishDate)
                } else {
                    Toast.makeText(requireContext(), "該当するメンバーが見つかりません", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // メンバーが選択されていない場合
            applyRefinementAndDismiss(startDate, finishDate)
        }
    }

    private fun applyRefinementAndDismiss(startDate: String?, finishDate: String?) {
        (activity as? PayRecordListActivity)?.applyRefinement(memberId, startDate, finishDate)
        dismiss()  // フラグメントを閉じる
    }

    private fun loadMembersFromFirestore() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid.toString()

        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val memberList = mutableListOf<String>().apply {
                    add(getString(R.string.un_selected))
                    querySnapshot.documents.forEach { document ->
                        val memberName = document.getString("member_name") ?: ""
                        add(memberName)
                    }
                }

                val memberArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, memberList)
                memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerMember.adapter = memberArrayAdapter

                if (memberList.size == 1) {
                    memberListError.text = "メンバーが登録されていません。"
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getMemberDocumentId(userID: String, memberName: String, callback: (String?) -> Unit) {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", memberName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback(null)
                } else {
                    val memberDocId = querySnapshot.documents.first().id
                    callback(memberDocId)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "メンバーの検索に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }
}

