package com.websarva.wings.android.kakeibo0422

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingActivity : BaseActivity(R.layout.activity_setting, R.string.title_setting) {

    private lateinit var provisionalBudgetLayout: TextInputLayout
    private lateinit var provisionalBudgetEditText: TextInputEditText
    private lateinit var switchRoundDown: SwitchMaterial
    private lateinit var buttonSaveSetting: Button

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        setupDrawerAndToolbar()

        provisionalBudgetLayout = findViewById(R.id.provisionalBudgetLayout)
        provisionalBudgetEditText = findViewById(R.id.provisionalBudgetEditText)
        switchRoundDown = findViewById(R.id.switchRoundDown)
        buttonSaveSetting = findViewById(R.id.buttonSaveSetting)

        loadUserSettings()

        buttonSaveSetting.setOnClickListener {
            clearKeyboardFocus()
            saveUserSettings()
        }
    }

    private fun loadUserSettings() {
        if (userID.isEmpty()) return

        firestore.collection("user_settings")
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val budget = document.getLong("provisional_budget") ?: 0
                    val roundDown = document.getBoolean("round_down_fraction") ?: false
                    provisionalBudgetEditText.setText(budget.toString())
                    switchRoundDown.isChecked = roundDown
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の読み込みに失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserSettings() {
        val budgetText = provisionalBudgetEditText.text.toString().trim()
        if (budgetText.isEmpty()) {
            provisionalBudgetLayout.error = "仮予算を入力してください。"
            return
        }
        provisionalBudgetLayout.error = null

        val budget = budgetText.toIntOrNull() ?: 0
        val roundDown = switchRoundDown.isChecked

        val settingData = hashMapOf(
            "user_id" to userID,
            "provisional_budget" to budget,
            "round_down_fraction" to roundDown,
            "updated_at" to FieldValue.serverTimestamp()
        )

        firestore.collection("user_settings")
            .document(userID)
            .set(settingData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "設定の保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearKeyboardFocus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(provisionalBudgetEditText.windowToken, 0)
        provisionalBudgetEditText.clearFocus()
    }
}