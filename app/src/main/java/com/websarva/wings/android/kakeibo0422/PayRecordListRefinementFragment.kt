package com.websarva.wings.android.kakeibo0422

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PayRecordListRefinementFragment : DialogFragment() {
    private lateinit var spinnerMember: Spinner
    private lateinit var memberListError: TextView
    private lateinit var spinnerPayPurpose: Spinner
    private lateinit var payPurposeListError: TextView
    private lateinit var startDateEditText: EditText
    private lateinit var finishDateEditText: EditText
    private lateinit var spinnerPayDone: Spinner
    private lateinit var buttonOK: Button
    private lateinit var validateHelper: ValidateHelper
    private val firestore = FirebaseFirestore.getInstance()

    private var memberId: String? = null
    private var payPurposeId: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pay_record_list_refinement, container, false)

        setupUI(view)
        setupSpinners()
        setupDatePickers()
        buttonOK.setOnClickListener { handleOkButtonClick() }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupUI(view: View) {
        spinnerMember = view.findViewById(R.id.spinnerMember)
        memberListError = view.findViewById(R.id.memberListError)
        spinnerPayPurpose = view.findViewById(R.id.spinnerPayPurpose)
        payPurposeListError = view.findViewById(R.id.payPurposeListError)
        startDateEditText = view.findViewById(R.id.startDateEditText)
        finishDateEditText = view.findViewById(R.id.finishDateEditText)
        spinnerPayDone = view.findViewById(R.id.spinnerPayDone)
        buttonOK = view.findViewById(R.id.buttonOK)
        validateHelper = ValidateHelper(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSpinners() {
        val payDoneList = listOf(
            "領収状態を" + getString(R.string.un_selected),
            "領収済み",
            "未受領"
        )
        spinnerPayDone.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, payDoneList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        loadMembersFromFirestore()
        loadPayPurposesFromFirestore()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDatePickers() {
        val datePickerListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                (v as? EditText)?.let { showDatePickerDialog(it) }
            }
        }
        startDateEditText.onFocusChangeListener = datePickerListener
        finishDateEditText.onFocusChangeListener = datePickerListener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth)
                editText.setText(formattedDate)
                if(!validateHelper.dateCheck(editText).first) editText.error = validateHelper.dateCheck(editText).second
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun handleOkButtonClick() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val selectedMemberName = spinnerMember.selectedItem?.toString()
        val selectedPayPurposeName = spinnerPayPurpose.selectedItem?.toString()
        val startDate = startDateEditText.text.toString().takeIf { it.isNotEmpty() }
        val finishDate = finishDateEditText.text.toString().takeIf { it.isNotEmpty() }
        val selectedPayDone = spinnerPayDone.selectedItem.toString().takeIf { !it.contains(getString(R.string.un_selected)) }

        // 全ての選択が「未選択」の場合はフィルタなしで適用
        if (startDate == null && finishDate == null &&
            selectedMemberName?.contains(getString(R.string.un_selected)) == true &&
            selectedPayPurposeName?.contains(getString(R.string.un_selected)) == true) {
            applyRefinementAndDismiss(null, null, selectedPayDone)
            return
        }

        // 開始日・終了日のどちらか一方のみが入力されている場合、エラーを表示
        if ((startDate == null) != (finishDate == null)) {
            Toast.makeText(context, "開始日と終了日は両方入力してください。", Toast.LENGTH_SHORT).show()
            return
        }

        // メンバーが未選択でない場合、FirestoreでIDを取得
        if (!selectedMemberName!!.contains(getString(R.string.un_selected))) {
            getMemberDocumentId(userID, selectedMemberName) { memberDocId ->
                memberId = memberDocId
                if (!selectedPayPurposeName!!.contains(getString(R.string.un_selected))) {
                    getPayPurposeDocumentId(userID, selectedPayPurposeName) { payPurposeDocId ->
                        payPurposeId = payPurposeDocId
                        applyRefinementAndDismiss(startDate, finishDate, selectedPayDone)
                    }
                } else {
                    applyRefinementAndDismiss(startDate, finishDate, selectedPayDone)
                }
            }
        } else if (!selectedPayPurposeName!!.contains(getString(R.string.un_selected))) {
            getPayPurposeDocumentId(userID, selectedPayPurposeName) { payPurposeDocId ->
                payPurposeId = payPurposeDocId
                applyRefinementAndDismiss(startDate, finishDate, selectedPayDone)
            }
        } else {
            applyRefinementAndDismiss(startDate, finishDate, selectedPayDone)
        }
    }


    private fun applyRefinementAndDismiss(startDate: String?, finishDate: String?, payDone: String?) {
        (activity as? PayRecordListActivity)?.applyRefinement(memberId, startDate, finishDate, payDone, payPurposeId)
        dismiss()
    }

    // Firestoreからメンバーを取得してSpinnerにセットするメソッド
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMembersFromFirestore() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("members")
            .whereEqualTo("user_id", userID)  // user_idに紐づくメンバーを取得
            .get()
            .addOnSuccessListener { querySnapshot ->

                val memberNameList = mutableListOf<String>()
                // メンバー名をリストに追加（最初に「選択してください」の項目を追加）
                memberNameList.add("メンバーを" + getString(R.string.un_selected))

                // クエリ結果をリストに変換
                val newMemberList = mutableListOf<Member>()
                for (document in querySnapshot.documents) {
                    val memberName = document.getString("member_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val memberId = document.id  // FirestoreのドキュメントIDを使う（または任意のフィールド）
                    val userId = document.getString("user_id") ?: ""

                    newMemberList.add(Member(memberId, userId, memberName, resistDate))
                }

                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newMemberList.sortBy {
                    val dateString = it.resistDate
                    try {
                        LocalDateTime.parse(dateString, dateFormat) // LocalDateTime に変換
                    } catch (e: Exception) {
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                for(member in newMemberList){
                    memberNameList.add(member.memberName)
                }

                // Spinnerにセットする
                val memberArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, memberNameList)
                memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerMember.adapter = memberArrayAdapter

                // リストが空の場合の処理
                if (memberNameList.size == 1) {
                    memberListError.text = getString(R.string.error_un_member_resisted)
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(requireContext(), "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestoreから支払い目的を取得してSpinnerにセットするメソッド
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPayPurposesFromFirestore() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)  // user_idに紐づくメンバーを取得
            .get()
            .addOnSuccessListener { querySnapshot ->
                val payPurposeNameList = mutableListOf<String>()
                val newPayPurposeList = mutableListOf<PayPurpose>()

                // 支払い目的をリストに追加（最初に「選択してください」の項目を追加）
                payPurposeNameList.add("支払い目的を" + getString(R.string.un_selected))

                for (document in querySnapshot.documents) {
                    val payPurposeName = document.getString("pay_purpose_name") ?: ""
                    val resistDate = document.getString("resist_date") ?: ""
                    val payPurposeId = document.id
                    val userId = document.getString("user_id") ?: ""

                    newPayPurposeList.add(PayPurpose(payPurposeId, userId, payPurposeName, resistDate))
                }

                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newPayPurposeList.sortBy {
                    val dateString = it.resistDate
                    try {
                        LocalDateTime.parse(dateString, dateFormat) // LocalDateTime に変換
                    } catch (e: Exception) {
                        LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0) // 変換エラー時には 1970-01-01 を返す
                    }
                }

                for(payPurpose in newPayPurposeList){
                    payPurposeNameList.add(payPurpose.payPurposeName)
                }

                // Spinnerにセットする
                val payPurposeArrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, payPurposeNameList)
                payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPayPurpose.adapter = payPurposeArrayAdapter

                // リストが空の場合の処理
                if (payPurposeNameList.size == 1) {
                    payPurposeListError.text = getString(R.string.error_un_pay_purpose_resisted)
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(requireContext(), "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getMemberDocumentId(userID: String, memberName: String, callback: (String?) -> Unit) {
        firestore.collection("members").whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", memberName).get()
            .addOnSuccessListener { querySnapshot ->
                callback(querySnapshot.documents.firstOrNull()?.id)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "メンバーの検索に失敗しました", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    private fun getPayPurposeDocumentId(userID: String, payPurposeName: String, callback: (String?) -> Unit) {
        firestore.collection("payPurposes").whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_name", payPurposeName).get()
            .addOnSuccessListener { querySnapshot ->
                callback(querySnapshot.documents.firstOrNull()?.id)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "支払い目的の検索に失敗しました", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }
}
