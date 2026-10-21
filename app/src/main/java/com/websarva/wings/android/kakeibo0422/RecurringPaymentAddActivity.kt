package com.websarva.wings.android.kakeibo0422

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RecurringPaymentAddActivity : BaseActivity(R.layout.activity_recurring_payment_add, R.string.title_recurring_payment_add) {

    private lateinit var spMember: Spinner
    private lateinit var memberListError: TextView
    private lateinit var spPayPurposeList: Spinner
    private lateinit var payPurposeListError: TextView
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var paymentDayLayout: TextInputLayout
    private lateinit var paymentDayEditText: EditText
    private lateinit var payDone: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var buttonAdd: Button

    private var selectedPaymentDay: Int = 1
    private var selectedMemberName: String = ""
    private var selectedPayPurposeName: String = ""

    private val validateHelper = ValidateHelper(this)
    private val firestore = FirebaseFirestore.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_payment_add)

        setupDrawerAndToolbar()

        spMember = findViewById(R.id.spPerson)
        memberListError = findViewById(R.id.memberListError)
        spPayPurposeList = findViewById(R.id.spPayPurposeList)
        payPurposeListError = findViewById(R.id.payPurposeListError)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        paymentDayLayout = findViewById(R.id.paymentDayLayout)
        paymentDayEditText = findViewById(R.id.paymentDayEditText)
        payDone = findViewById(R.id.payDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonAdd = findViewById(R.id.buttonRecurringPaymentAdd)

        updatePaymentDayText()

        paymentDayEditText.setOnClickListener {
            clearBordFocus()
            showPaymentDayPicker()
        }

        payAmountEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val (result, errorMessage) = validateHelper.payAmountCheck(payAmountEditText)
                if (!result) {
                    payAmountError.error = errorMessage
                    return@OnFocusChangeListener
                }
                payAmountError.error = null
            }
        }

        spMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedMemberName = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spPayPurposeList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedPayPurposeName = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadMembersFromFirestore()
        loadPayPurposesFromFirestore()

        buttonAdd.setOnClickListener {
            clearBordFocus()
            val (resultAmount, amountMsg) = validateHelper.payAmountCheck(payAmountEditText)
            val (resultPurpose, purposeMsg) = validateHelper.selectedCheck(selectedPayPurposeName)
            val (resultMember, memberMsg) = validateHelper.selectedCheck(selectedMemberName)

            if (!(resultAmount && resultPurpose && resultMember)) {
                payAmountError.error = amountMsg
                payPurposeListError.text = purposeMsg
                memberListError.text = memberMsg
                return@setOnClickListener
            }
            clearErrorMessage()
            addRecurringPayment()
        }
    }

    private fun updatePaymentDayText() {
        paymentDayEditText.setText(getString(R.string.recurring_payment_day_format, selectedPaymentDay))
    }

    private fun showPaymentDayPicker() {
        val days = (1..31).map { "${it}日" }.toTypedArray()
        var tempDay = selectedPaymentDay

        AlertDialog.Builder(this)
            .setTitle(R.string.hint_recurring_payment_day)
            .setSingleChoiceItems(days, tempDay - 1) { _, which ->
                tempDay = which + 1
            }
            .setPositiveButton("決定") { _, _ ->
                selectedPaymentDay = tempDay
                updatePaymentDayText()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun addRecurringPayment() {
        getMemberId { mId ->
            getPayPurposeId { pId ->
                val data = hashMapOf(
                    "user_id" to userID,
                    "member_id" to mId,
                    "pay_purpose_id" to pId,
                    "payment_day" to selectedPaymentDay,
                    "amount" to payAmountEditText.text.toString().toInt(),
                    "is_recept_checked" to payDone.isChecked,
                    "note" to noteEditText.text.toString(),
                    "created_at" to FieldValue.serverTimestamp(),
                    "updated_at" to FieldValue.serverTimestamp()
                )

                firestore.collection("recurring_payments")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "固定費を登録しました", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK, Intent().apply { putExtra("IS_DATA_CHANGED", true) })
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "登録に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun getMemberId(onSuccess: (String) -> Unit) {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", selectedMemberName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "メンバーが見つかりません", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                onSuccess(snapshot.documents.first().id)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "メンバーの取得に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getPayPurposeId(onSuccess: (String) -> Unit) {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_name", selectedPayPurposeName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "支払い目的が見つかりません", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                onSuccess(snapshot.documents.first().id)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "支払い目的の取得に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMembersFromFirestore() {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { snapshot ->
                val memberNameList = mutableListOf<String>()
                memberNameList.add("メンバーを" + getString(R.string.un_selected))

                val newMemberList = mutableListOf<Member>()
                for (doc in snapshot.documents) {
                    newMemberList.add(
                        Member(doc.id, doc.getString("user_id") ?: "", doc.getString("member_name") ?: "", doc.getString("resist_date") ?: "")
                    )
                }
                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newMemberList.sortBy {
                    try { LocalDateTime.parse(it.resistDate, dateFormat) } catch (e: Exception) { LocalDateTime.of(1970, 1, 1, 0, 0) }
                }
                for (m in newMemberList) memberNameList.add(m.memberName)

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, memberNameList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spMember.adapter = adapter
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPayPurposesFromFirestore() {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .get()
            .addOnSuccessListener { snapshot ->
                val purposeList = mutableListOf<String>()
                purposeList.add("支払い目的を" + getString(R.string.un_selected))

                val newPurposes = mutableListOf<PayPurpose>()
                for (doc in snapshot.documents) {
                    newPurposes.add(
                        PayPurpose(doc.id, doc.getString("user_id") ?: "", doc.getString("pay_purpose_name") ?: "", doc.getString("resist_date") ?: "")
                    )
                }
                val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                newPurposes.sortBy {
                    try { LocalDateTime.parse(it.resistDate, dateFormat) } catch (e: Exception) { LocalDateTime.of(1970, 1, 1, 0, 0) }
                }
                for (p in newPurposes) purposeList.add(p.payPurposeName)

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, purposeList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spPayPurposeList.adapter = adapter
            }
    }

    private fun clearBordFocus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(payAmountEditText.windowToken, 0)
        imm.hideSoftInputFromWindow(spPayPurposeList.windowToken, 0)
        imm.hideSoftInputFromWindow(spMember.windowToken, 0)
        imm.hideSoftInputFromWindow(noteEditText.windowToken, 0)
        payAmountEditText.clearFocus()
        noteEditText.clearFocus()
    }

    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        memberListError.text = null
    }
}