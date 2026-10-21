package com.websarva.wings.android.kakeibo0422

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.websarva.wings.android.kakeibo0422.helper.ReceiptParser
import com.websarva.wings.android.kakeibo0422.helper.ValidateHelper
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class PayRecordAddActivity : BaseActivity(R.layout.activity_pay_record_add, R.string.title_add_pay_record) {
    //画面部品の用意
    private lateinit var spMember: Spinner
    private lateinit var memberListError:TextView
    private lateinit var spPayPurposeList: Spinner
    private lateinit var payPurposeListError: TextView
    private lateinit var payAmountEditText: EditText
    private lateinit var payAmountError: TextInputLayout
    private lateinit var payDateEditText: EditText
    private lateinit var payDateError: TextInputLayout
    private lateinit var payDone: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var buttonPayRecordAdd: Button

    //スピナーで選ばれたものを格納する変数
    private lateinit var selectedMemberName:String
    private lateinit var selectedPayPurposeName:String

    //ヘルパークラス
    private val validateHelper = ValidateHelper(this)
    private val firestore = FirebaseFirestore.getInstance()

    // ML Kit テキスト認識クライアント（日本語）
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    // カメラ撮影用の画像URI保持用
    private var cameraImageUri: Uri? = null

    // 写真ピッカーのランチャー
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processReceiptImage(uri)
        }
    }

    // カメラ撮影のランチャー
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            cameraImageUri?.let { processReceiptImage(it) }
        }
    }

    // onCreate 内に追加
    private fun setupReceiptScanButton() {
        val buttonScanReceipt = findViewById<View>(R.id.buttonScanReceipt)
        buttonScanReceipt.setOnClickListener {
            clearBordFocus()
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("カメラで撮影", "アルバムから選択")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("レシートの読み取り方法を選択")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()
    }

    private fun launchCamera() {
        val tempFile = File.createTempFile("receipt_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            tempFile
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun processReceiptImage(imageUri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, imageUri)
        } catch (e: Exception) {
            Toast.makeText(this, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "レシートを解析中...", Toast.LENGTH_SHORT).show()

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val result = ReceiptParser.parse(visionText)

                var updated = false

                // 日付が検出されたらセット
                if (!result.date.isNullOrEmpty()) {
                    payDateEditText.setText(result.date)
                    payDateError.error = null
                    updated = true
                }

                // 金額が検出されたらセット
                if (result.amount != null) {
                    payAmountEditText.setText(result.amount.toString())
                    payAmountError.error = null
                    updated = true
                }

                if (updated) {
                    Toast.makeText(this, "レシートの情報を反映しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "レシートから日付・金額を検出できませんでした", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "文字認識に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_record_add)

        setupDrawerAndToolbar()

        //画面部品の取得
        spMember = findViewById(R.id.spPerson)
        memberListError = findViewById(R.id.memberListError)
        spPayPurposeList = findViewById(R.id.spPayList)
        payPurposeListError = findViewById(R.id.PayListError)
        payAmountEditText = findViewById(R.id.payAmountEditText)
        payAmountError = findViewById(R.id.payAmount)
        payDateEditText = findViewById(R.id.payDateEditText)
        payDateError = findViewById(R.id.payDate)
        payDone = findViewById(R.id.payDone)
        noteEditText = findViewById(R.id.payNoteEditText)
        buttonPayRecordAdd = findViewById(R.id.buttonPayRecordAdd)

        setupReceiptScanButton()

        // PayPurposeデータを取得しSpinnerにセット
        loadPayPurposesFromFirestore()

        //Memberデータを取得しSpinnerにセット
        loadMembersFromFirestore()

        spPayPurposeList.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView:View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedPayPurposeName= parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>){}
        }

        spMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedView:View?, position: Int, id: Long) {
                // 選ばれた項目を取得
                selectedMemberName = parentView.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parentView: AdapterView<*>){}
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

        payDateEditText.setOnClickListener {
            clearBordFocus()
            showDatePickerDialog()
        }

        buttonPayRecordAdd.setOnClickListener {
            clearBordFocus()
            val (resultPayAmount, payAmountMessage) = validateHelper.payAmountCheck(payAmountEditText)
            val (resultPayDate, payDateMessage) = validateHelper.dateCheck(payDateEditText)
            val (resultPayPurpose,payPurposeMessage) = validateHelper.selectedCheck(selectedPayPurposeName)
            val (resultMember,memberMessage) = validateHelper.selectedCheck(selectedMemberName)
            if(!(resultPayAmount && resultPayDate && resultPayPurpose && resultMember)){
                payAmountError.error = payAmountMessage
                payDateError.error = payDateMessage
                payPurposeListError.text = payPurposeMessage
                memberListError.text = memberMessage
                return@setOnClickListener
            }
            clearErrorMessage()
            onSaveButtonClick()
        }
    }

    private fun clearBordFocus() {
        // キーボードを閉じる処理
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(payAmountEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDateEditText.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spPayPurposeList.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(spMember.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(payDone.windowToken, 0)
        inputMethodManager.hideSoftInputFromWindow(noteEditText.windowToken, 0)
        //フォーカスを外す処理
        payAmountEditText.clearFocus()
        spMember.clearFocus()
        spPayPurposeList.clearFocus()
        payDone.clearFocus()
        noteEditText.clearFocus()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                // 日付を選択したときの処理
                val formattedDate = String.format(Locale.ROOT, "%04d/%02d/%02d", selectedYear, selectedMonth + 1, selectedDay)
                payDateEditText.setText(formattedDate) // EditTextに日付を設定

                // OKボタンが押されたときにバリデーションを行う
                val (result, errorMessage) = validateHelper.dateCheck(payDateEditText)
                if (!result) {
                    payDateError.error = errorMessage
                } else {
                    payDateError.error = null // エラーメッセージをクリア
                }

            }, year, month, day)

        // ダイアログのキャンセルボタンが押されたときの処理
        datePickerDialog.setOnCancelListener {
            // 必要な処理があればここに記述
        }
        datePickerDialog.show()
    }

    private fun onSaveButtonClick() {
        // メンバーIDを取得
        getMemberId { memberId ->
            // 支払い目的IDを取得
            getPayPurposeId { payPurposeId ->
                // 支払い履歴をFirestoreに追加
                addPaymentHistory(memberId, payPurposeId)
            }
        }
    }

    // メンバーIDを取得するメソッド
    private fun getMemberId(onSuccess: (String) -> Unit) {
        firestore.collection("members")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("member_name", selectedMemberName)
            .get()
            .addOnSuccessListener { memberQuerySnapshot ->
                if (memberQuerySnapshot.isEmpty) {
                    showError("メンバーが見つかりません")
                    return@addOnSuccessListener
                }
                val memberId = memberQuerySnapshot.documents.first().id
                onSuccess(memberId)
            }
            .addOnFailureListener { exception ->
                showError("メンバーデータの取得に失敗しました: ${exception.message}")
            }
    }

    // 支払い目的IDを取得するメソッド
    private fun getPayPurposeId(onSuccess: (String) -> Unit) {
        firestore.collection("payPurposes")
            .whereEqualTo("user_id", userID)
            .whereEqualTo("pay_purpose_name", selectedPayPurposeName)
            .get()
            .addOnSuccessListener { payPurposeQuerySnapshot ->
                if (payPurposeQuerySnapshot.isEmpty) {
                    showError("支払い目的が見つかりません")
                    return@addOnSuccessListener
                }
                val payPurposeId = payPurposeQuerySnapshot.documents.first().id
                onSuccess(payPurposeId)
            }
            .addOnFailureListener { exception ->
                showError("支払い目的データの取得に失敗しました: ${exception.message}")
            }
    }

    // 支払い履歴をFirestoreに追加するメソッド
    private fun addPaymentHistory(memberId: String, payPurposeId: String) {
        val paymentHistory = hashMapOf(
            "member_id" to memberId,
            "user_id" to userID,
            "pay_purpose_id" to payPurposeId,
            "payment_date" to payDateEditText.text.toString(),
            "amount" to payAmountEditText.text.toString().toInt(),
            "is_recept_checked" to payDone.isChecked,
            "note" to noteEditText.text.toString()
        )

        firestore.collection("payment_history")
            .add(paymentHistory)
            .addOnSuccessListener {
                Toast.makeText(this, "支払い履歴が追加されました", Toast.LENGTH_SHORT).show()
                val resultIntent = Intent().apply {
                    putExtra("IS_DATA_CHANGED", true)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { exception ->
                showError("支払い履歴の追加に失敗しました: ${exception.message}")
            }
    }

    // エラーメッセージを表示する共通メソッド
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun clearErrorMessage() {
        payAmountError.error = null
        payPurposeListError.text = null
        payDateError.error = null
        memberListError.text = null
    }

    // Firestoreからメンバーを取得してSpinnerにセットするメソッド
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMembersFromFirestore() {
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
                val memberArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, memberNameList)
                memberArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spMember.adapter = memberArrayAdapter

                // リストが空の場合の処理
                if (memberNameList.size == 1) {
                    memberListError.text = getString(R.string.error_un_member_resisted)
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestoreから支払い目的を取得してSpinnerにセットするメソッド
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadPayPurposesFromFirestore() {
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
                val payPurposeArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, payPurposeNameList)
                payPurposeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spPayPurposeList.adapter = payPurposeArrayAdapter

                // リストが空の場合の処理
                if (payPurposeNameList.size == 1) {
                    payPurposeListError.text = getString(R.string.error_un_pay_purpose_resisted)
                }
            }
            .addOnFailureListener { exception ->
                // エラーハンドリング
                Toast.makeText(this, "データ取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}