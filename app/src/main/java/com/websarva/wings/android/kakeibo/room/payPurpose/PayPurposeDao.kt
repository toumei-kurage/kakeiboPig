package com.websarva.wings.android.kakeibo.room.payPurpose

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

data class PayPurposeResultCountAndId(val count: Int, val id: Int)

@Dao
interface PayPurposeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payPurpose: PayPurpose)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun updatePayPurpose(payPurpose: PayPurpose)

    @Delete
    suspend fun deletePayPurpose(payPurpose: PayPurpose)

    @Query("SELECT * FROM payPurpose_table WHERE userID = :userID")
    fun getAllPayPurposeByUserID(userID: String): LiveData<List<PayPurpose>>

    @Query("SELECT id FROM payPurpose_table WHERE userID = :userID AND payPurposeName = :payPurposeName")
    suspend fun getPayPurposeID(userID: String, payPurposeName: String): Int

    @Query("SELECT COUNT(*) as count, id FROM payPurpose_table WHERE userID = :userID AND payPurposeName = :payPurposeName")
    suspend fun countByUserIdAndPurposeName(userID: String, payPurposeName: String): List<PayPurposeResultCountAndId>

    @Query("SELECT * FROM payPurpose_table WHERE id = :id")
    fun getPayPurpose(id: Int):PayPurpose
}
