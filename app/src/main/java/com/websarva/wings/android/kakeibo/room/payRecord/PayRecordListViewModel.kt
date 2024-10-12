package com.websarva.wings.android.kakeibo.room.payRecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.websarva.wings.android.kakeibo.room.AppDatabase

class PayRecordListViewModel(application: Application) : AndroidViewModel(application)  {
    private val paymentDao: PaymentDao = AppDatabase.getDatabase(application).paymentDao()

    //ログイン中のユーザーに紐づく家計簿をすべて取得
    fun getPaymentsByUserId(userId:String): LiveData<List<Payment>>{
        return paymentDao.getPaymentsByUserId(userId)
    }

    //メンバーで検索をかける
    fun getPaymentsByPayerIdAndUserId(payerId: Int, userId: String): LiveData<List<Payment>>{
        return paymentDao.getPaymentsByPayerIdAndUserId(payerId,userId)
    }
}