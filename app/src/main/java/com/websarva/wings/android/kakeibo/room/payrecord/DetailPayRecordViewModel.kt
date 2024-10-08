package com.websarva.wings.android.kakeibo.room.payrecord

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.websarva.wings.android.kakeibo.room.AppDatabase
import com.websarva.wings.android.kakeibo.room.member.Person
import com.websarva.wings.android.kakeibo.room.member.PersonDao

class DetailPayRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val paymentDao: PaymentDao = AppDatabase.getDatabase(application).paymentDao()
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    fun getPayment(id:Int?): LiveData<Payment>{
        return paymentDao.getPayment(id)
    }

    fun getPerson(id:Int): Person {
        return personDao.getPerson(id)
    }
}