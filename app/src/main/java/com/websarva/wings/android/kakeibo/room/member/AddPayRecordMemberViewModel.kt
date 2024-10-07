package com.websarva.wings.android.kakeibo.room.member

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.websarva.wings.android.kakeibo.room.AppDatabase

class AddPayRecordMemberViewModel(application: Application) : AndroidViewModel(application) {
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    fun getPersons(userId: String): LiveData<List<Person>> {
        return personDao.getAllPersonsByUserId(userId)
    }

    // 特定のメンバーを取得するメソッド
    suspend fun getPersonId(userId: String, memberName: String): Int {
        return personDao.getPersonId(userId, memberName)
    }

}

