package com.websarva.wings.android.kakeibo0422.helper

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReceiptParseResult(
    val date: String?,
    val amount: Int?,
    val storeName: String?
)

object ReceiptParser {

    /**
     * Gemini API の API キーは、Google 公式の開発者向けツール 「Google AI Studio」 から無料で即座に取得できます。クレジットカードの登録も不要です。
     *
     * API キーの取得手順（3ステップ）
     * Google AI Studio にアクセス
     *
     * ブラウザで https://aistudio.google.com/ を開き、お使いの Google アカウントでログインします。
     * 初回アクセス時、利用規約の同意画面が表示されたら内容を確認して同意します。
     * API キーの作成画面へ進む
     *
     * 画面左上のメニュー、または画面内にある 「Get API key」 ボタンをクリックします。
     * キーを発行・コピー
     *
     * 「Create API key」（API キーを作成）をクリックします。
     * プロジェクトの選択肢が出ます：
     * すでに GCP（Google Cloud Platform）プロジェクトがある場合はそれを選択
     * ない場合（または分ける場合）は 「Create API key in new project」（新規プロジェクトで作成）を選択
     * 画面に AIzaSy... から始まる API キーが表示されるのでコピーします。
     * 安全な管理方法（推奨設定）
     * コード内に直接キーを書き込む（ハードコードする）と、GitHub などの公開リポジトリへ誤ってコミットしてしまう危険があります。Android 開発では以下の手順で local.properties を経由させて扱うのが一般的です。
     *
     * 1. local.properties に記述（Git 管理対象外のファイル）
     * プロジェクト直下の local.properties に追記します。
     *
     * properties
     *
     * GEMINI_API_KEY=AIzaSyあなたのAPIキー
     * 2. app/build.gradle で読み込み設定
     * buildFeatures で buildConfig を有効化し、buildConfigField で注入します。
     *
     * groovy
     *
     * android {
     *     ...
     *     defaultConfig {
     *         ...
     *         // local.properties から読み込んで BuildConfig に定義
     *         Properties properties = new Properties()
     *         properties.load(project.rootProject.file('local.properties').newDataInputStream())
     *         buildConfigField "String", "GEMINI_API_KEY", "\"${properties.getProperty('GEMINI_API_KEY')}\""
     *     }
     *
     *     buildFeatures {
     *         buildConfig true
     *     }
     * }
     * 3. コード内での参照 (ReceiptParser.kt)
     * ハードコードではなく BuildConfig から取得します。
     *
     * kotlin
     *
     * // ReceiptParser.kt
     * private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
     */
    private const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = GEMINI_API_KEY,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    private val promptTemplate = """
        以下のテキストはレシートをOCRで読み取った結果です。
        内容を解析し、以下の項目を抽出して指定のJSON形式のみで出力してください。
        
        【抽出項目】
        - storeName: 店舗名・会社名（見当たらない場合は null）
        - date: 支払日・取引日。必ず「yyyy/MM/dd」形式に変換してください（見当たらない場合は null）
        - amount: 支払合計金額・請求金額（お釣りや預かり金ではなく、実際に支払った総計額の整数値。見当たらない場合は null）

        【出力JSONフォーマット】
        {
          "storeName": "店舗名文字列またはnull",
          "date": "yyyy/MM/dd文字列またはnull",
          "amount": 0
        }

        【レシートOCRテキスト】
    """.trimIndent()

    suspend fun parseWithAi(ocrText: String): ReceiptParseResult = withContext(Dispatchers.IO) {
        if (ocrText.isBlank()) {
            return@withContext ReceiptParseResult(null, null, null)
        }

        try {
            val response = generativeModel.generateContent("$promptTemplate\n$ocrText")
            val responseText = response.text ?: return@withContext ReceiptParseResult(null, null, null)

            val json = JSONObject(responseText)
            val storeName = if (json.isNull("storeName")) null else json.optString("storeName").takeIf { it.isNotBlank() }
            val date = if (json.isNull("date")) null else json.optString("date").takeIf { it.isNotBlank() }
            val amount = if (json.isNull("amount")) null else json.optInt("amount").takeIf { it > 0 }

            ReceiptParseResult(
                date = date,
                amount = amount,
                storeName = storeName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ReceiptParseResult(null, null, null)
        }
    }
}