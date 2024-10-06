package com.websarva.wings.android.kakeibo.room.payrecord

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.websarva.wings.android.kakeibo.room.member.Person
import kotlinx.coroutines.launch

class PaymentViewModel(private val paymentDao: PaymentDao) : ViewModel() {

    val allPayments: LiveData<List<Payment>> = paymentDao.getAllPayments()

    fun insert(payment: Payment) {
        viewModelScope.launch {
            paymentDao.insert(payment)
        }
    }
}

