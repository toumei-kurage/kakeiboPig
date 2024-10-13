package com.websarva.wings.android.kakeibo.room.payRecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.websarva.wings.android.kakeibo.room.AppDatabase
import kotlinx.coroutines.launch

class PayRecordViewModel(application: Application) : AndroidViewModel(application)  {
    private val paymentDao: PaymentDao = AppDatabase.getDatabase(application).paymentDao()

    // メンバーリストを保持するLiveData
    private val _paymentList = MutableLiveData<List<Payment>>()

    fun getPayment(id:Int?): LiveData<Payment>{
        return paymentDao.getPayment(id)
    }

    //ログイン中のユーザーに紐づく家計簿をすべて取得
    fun getPaymentsByUserId(userId:String): LiveData<List<Payment>>{
        return paymentDao.getPaymentsByUserId(userId)
    }

    //メンバーで検索をかける
    fun getPaymentsByPayerIdAndUserId(payerId: Int, userId: String): LiveData<List<Payment>>{
        return paymentDao.getPaymentsByPayerIdAndUserId(payerId,userId)
    }

    // 削除メソッド
    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            paymentDao.deletePayment(payment)
            // 削除後にリストを再取得
            _paymentList.value = paymentDao.getPaymentsByUserId(payment.userId).value
        }
    }
}