package com.websarva.wings.android.kakeibo0422.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class PeriodSetting(
    val startDay: Int = 1,
    val finishDay: Int = 31,
    val isFinishNextMonth: Boolean = false
)

class SettingRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun getSettingsDocRef() = auth.currentUser?.uid?.let { uid ->
        db.collection("user_settings").document(uid)
    }

    suspend fun getPeriodSetting(): PeriodSetting {
        val docRef = getSettingsDocRef() ?: return PeriodSetting()
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val startDay = snapshot.getLong("period_start_day")?.toInt() ?: 1
                val finishDay = snapshot.getLong("period_finish_day")?.toInt() ?: 31
                // 保存されていない既存データの場合は startDay > finishDay で自動補完
                val isFinishNextMonth = snapshot.getBoolean("is_finish_next_month") ?: (startDay > finishDay)
                PeriodSetting(startDay, finishDay, isFinishNextMonth)
            } else {
                PeriodSetting()
            }
        } catch (e: Exception) {
            PeriodSetting()
        }
    }

    suspend fun savePeriodSetting(startDay: Int, finishDay: Int, isFinishNextMonth: Boolean): Boolean {
        val docRef = getSettingsDocRef() ?: return false
        val data = hashMapOf<String, Any>(
            "period_start_day" to startDay.coerceIn(1, 31),
            "period_finish_day" to finishDay.coerceIn(1, 31),
            "is_finish_next_month" to isFinishNextMonth
        )
        return try {
            docRef.set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}