package com.websarva.wings.android.kakeibo.room.payrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PaymentViewModelFactory(private val paymentDao: PaymentDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentViewModel(paymentDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
