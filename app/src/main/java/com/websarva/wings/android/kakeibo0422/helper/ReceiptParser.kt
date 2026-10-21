package com.websarva.wings.android.kakeibo0422.helper

import com.google.mlkit.vision.text.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class ReceiptParseResult(
    val date: String?,
    val amount: Int?,
    val candidateAmounts: List<Int>
)

object ReceiptParser {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    // 日付判定用正規表現 (例: 2026/04/22, 2026年4月22日, 2026-04-22, 26/4/22)
    private val dateRegexList = listOf(
        Regex("""(20\d{2})[/\-.年](0?[1-9]|1[0-2])[/\-.月](0?[1-9]|[12]\d|3[01])"""),
        Regex("""(\d{2})[/\-.年](0?[1-9]|1[0-2])[/\-.月](0?[1-9]|[12]\d|3[01])""") // 西暦下2桁
    )

    // 金額抽出用（カンマ区切り数値、¥記号対応）
    private val amountRegex = Regex("""[¥￥]?\s*([1-9]\d{0,2}(?:,\d{3})+|[1-9]\d{1,6})\s*円?""")

    // 合計を示すキーワード
    private val totalKeywords = listOf("総合計", "合計", "お買上計", "小計", "支払計", "決済額")

    // 除外キーワード（合計金額と誤認しやすいノイズ）
    private val excludeKeywords = listOf("お預", "預り", "お釣", "釣", "対象額", "内税", "外税", "消費税", "ポイント", "値引", "割引")

    fun parse(text: Text): ReceiptParseResult {
        var detectedDate: String? = null
        val detectedAmounts = mutableListOf<Pair<Int, Text.Line>>()

        val allLines = text.textBlocks.flatMap { it.lines }

        // --- 1. 日付の抽出 ---
        for (line in allLines) {
            val lineText = line.text.replace(" ", "")
            for (regex in dateRegexList) {
                val match = regex.find(lineText)
                if (match != null) {
                    val groups = match.groupValues
                    val rawYear = groups[1].toInt()
                    val year = if (rawYear < 100) 2000 + rawYear else rawYear
                    val month = groups[2].toInt()
                    val day = groups[3].toInt()

                    try {
                        val parsed = LocalDate.of(year, month, day)
                        // 未来すぎる日付や10年以上過去の日付は除外
                        if (!parsed.isAfter(LocalDate.now().plusDays(1)) && parsed.year >= 2015) {
                            detectedDate = parsed.format(dateFormatter)
                            break
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            if (detectedDate != null) break
        }

        // --- 2. 金額候補の収集 ---
        for (line in allLines) {
            val lineText = line.text.replace(" ", "")
            // ノイズキーワードが含まれる行はスキップ
            if (excludeKeywords.any { lineText.contains(it) }) continue

            val matches = amountRegex.findAll(lineText)
            for (m in matches) {
                val rawValue = m.groupValues[1].replace(",", "")
                val value = rawValue.toIntOrNull()
                if (value != null && value in 10..9999999) {
                    detectedAmounts.add(Pair(value, line))
                }
            }
        }

        // --- 3. 「合計」キーワードに最も近接する金額の特定 ---
        var bestAmount: Int? = null
        var totalLine: Text.Line? = null

        // 最もフッター（下側）にある「合計」キーワード行を探す
        for (line in allLines.reversed()) {
            if (totalKeywords.any { line.text.contains(it) }) {
                totalLine = line
                break
            }
        }

        if (totalLine != null) {
            val totalBox = totalLine.boundingBox
            if (totalBox != null) {
                // 「合計」行と同一水平線上（Y座標が近い）または直下にある数値を優先
                val bestMatch = detectedAmounts
                    .filter { (_, line) ->
                        val box = line.boundingBox ?: return@filter false
                        // 「合計」の右側、または直下500px以内
                        (box.bottom >= totalBox.top - 20) && (box.top <= totalBox.bottom + 150)
                    }
                    .minByOrNull { (_, line) ->
                        val box = line.boundingBox!!
                        abs(box.centerY() - totalBox.centerY())
                    }

                bestAmount = bestMatch?.first
            }
        }

        // 「合計」で見つからなかった場合は、検出された中で最も大きい金額をフォールバックとして採用
        if (bestAmount == null && detectedAmounts.isNotEmpty()) {
            bestAmount = detectedAmounts.maxOf { it.first }
        }

        val candidateList = detectedAmounts.map { it.first }.distinct()

        return ReceiptParseResult(
            date = detectedDate,
            amount = bestAmount,
            candidateAmounts = candidateList
        )
    }
}