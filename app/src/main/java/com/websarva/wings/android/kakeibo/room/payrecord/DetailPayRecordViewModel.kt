package com.websarva.wings.android.kakeibo.room.payrecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.member.PersonDao
import kotlinx.coroutines.launch

class DetailPayRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val paymentDao: PaymentDao = AppDatabase.getDatabase(application).paymentDao()
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    // メンバーリストを保持するLiveData
    private val _paymentList = MutableLiveData<List<Payment>>()

    fun getPayment(id:Int?): LiveData<Payment>{
        return paymentDao.getPayment(id)
    }

    fun getPerson(id:Int): Person {
        return personDao.getPerson(id)
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