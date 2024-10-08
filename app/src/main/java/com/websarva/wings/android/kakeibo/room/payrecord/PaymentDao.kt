package com.websarva.wings.android.kakeibo.room.payrecord

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("SELECT * FROM payment WHERE payerId = :payerId AND userId = :userId")
    fun getPaymentsByPayerIdAndUserId(payerId: Int, userId: String): LiveData<List<Payment>>

    @Query("SELECT * FROM payment WHERE userId = :userId ORDER BY paymentDate")
    fun getPaymentsByUserId(userId: String): LiveData<List<Payment>>

    @Query("SELECT * FROM payment")
    fun getAllPayments(): LiveData<List<Payment>>

    @Query("SELECT * FROM payment WHERE id = :id")
    fun getPayment(id:Int?):LiveData<Payment>
}
