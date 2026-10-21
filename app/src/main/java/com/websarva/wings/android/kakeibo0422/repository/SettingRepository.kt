package com.websarva.wings.android.kakeibo0422.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class PeriodSetting(
    val startDay: Int = 1,
    val finishDay: Int = 31
)

class SettingRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ルールに合わせて /user_settings/{userId} を直接参照
    private fun getSettingsDocRef() = auth.currentUser?.uid?.let { uid ->
        db.collection("user_settings").document(uid)
    }

    /**
     * 家計簿の期間設定（開始日・終了日）を取得する。
     * 未設定の場合はデフォルト（1日〜31日）を返却。
     */
    suspend fun getPeriodSetting(): PeriodSetting {
        val docRef = getSettingsDocRef() ?: return PeriodSetting()
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val startDay = snapshot.getLong("period_start_day")?.toInt() ?: 1
                val finishDay = snapshot.getLong("period_finish_day")?.toInt() ?: 31
                PeriodSetting(startDay, finishDay)
            } else {
                PeriodSetting()
            }
        } catch (e: Exception) {
            PeriodSetting()
        }
    }

    /**
     * 家計簿の期間設定（開始日・終了日）を保存する。
     * SetOptions.merge() により既存の仮予算等のフィールドを保持したまま更新。
     */
    suspend fun savePeriodSetting(startDay: Int, finishDay: Int): Boolean {
        val docRef = getSettingsDocRef() ?: return false
        val data = hashMapOf<String, Any>(
            "period_start_day" to startDay.coerceIn(1, 31),
            "period_finish_day" to finishDay.coerceIn(1, 31)
        )
        return try {
            docRef.set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}