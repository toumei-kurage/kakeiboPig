package com.websarva.wings.android.kakeibo.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class AddPayRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    fun getPersons(userId: String): LiveData<List<Person>> {
        return personDao.getAllPersonsByUserId(userId)
    }
}

