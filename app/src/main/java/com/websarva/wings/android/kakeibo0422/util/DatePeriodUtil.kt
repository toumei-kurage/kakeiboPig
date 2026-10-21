package com.websarva.wings.android.kakeibo0422.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

object DatePeriodUtil {
    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    /**
     * 開始日(1〜31)、終了日(1〜31)、および終了月が翌月かどうかに基づき期間を算出する
     */
    fun calculateCurrentPeriod(
        startDay: Int,
        finishDay: Int,
        isFinishNextMonth: Boolean = (startDay > finishDay),
        baseDate: LocalDate = LocalDate.now()
    ): Pair<String, String> {
        val safeStartDay = startDay.coerceIn(1, 31)
        val safeFinishDay = finishDay.coerceIn(1, 31)

        val startDate: LocalDate
        val finishDate: LocalDate

        if (isFinishNextMonth) {
            // パターンA: 翌月終了（例: 10/27 〜 11/28、または 10/25 〜 11/24）
            if (baseDate.dayOfMonth >= safeStartDay) {
                // 当月開始 〜 翌月終了
                val validStart = min(safeStartDay, baseDate.lengthOfMonth())
                startDate = baseDate.withDayOfMonth(validStart)

                val nextMonth = baseDate.plusMonths(1)
                val validFinish = min(safeFinishDay, nextMonth.lengthOfMonth())
                finishDate = nextMonth.withDayOfMonth(validFinish)
            } else {
                // 前月開始 〜 当月終了
                val prevMonth = baseDate.minusMonths(1)
                val validStart = min(safeStartDay, prevMonth.lengthOfMonth())
                startDate = prevMonth.withDayOfMonth(validStart)

                val validFinish = min(safeFinishDay, baseDate.lengthOfMonth())
                finishDate = baseDate.withDayOfMonth(validFinish)
            }
        } else {
            // パターンB: 同月内終了（例: 10/01 〜 10/31）
            val validStart = min(safeStartDay, baseDate.lengthOfMonth())
            val validFinish = min(safeFinishDay, baseDate.lengthOfMonth())

            startDate = baseDate.withDayOfMonth(validStart)
            finishDate = baseDate.withDayOfMonth(validFinish)
        }

        return Pair(startDate.format(formatter), finishDate.format(formatter))
    }

    /**
     * 最新の家計簿終了日の翌日以降で、重複しない次の期間を算出する
     */
    fun calculateNextPeriodAfter(
        latestFinishDateStr: String,
        startDay: Int,
        finishDay: Int,
        isFinishNextMonth: Boolean = (startDay > finishDay)
    ): Pair<String, String> {
        return try {
            val latestFinishDate = LocalDate.parse(latestFinishDateStr, formatter)
            val nextDay = latestFinishDate.plusDays(1)

            var period = calculateCurrentPeriod(startDay, finishDay, isFinishNextMonth, nextDay)
            val periodStartDate = LocalDate.parse(period.first, formatter)

            // 算出した開始日が最新終了日以前（被っている）場合は1ヶ月進める
            if (!periodStartDate.isAfter(latestFinishDate)) {
                val safeStartDay = startDay.coerceIn(1, 31)
                val nextMonth = periodStartDate.plusMonths(1)
                val validStart = min(safeStartDay, nextMonth.lengthOfMonth())
                val adjustedBaseDate = nextMonth.withDayOfMonth(validStart)
                period = calculateCurrentPeriod(startDay, finishDay, isFinishNextMonth, adjustedBaseDate)
            }
            period
        } catch (e: Exception) {
            calculateCurrentPeriod(startDay, finishDay, isFinishNextMonth)
        }
    }
}