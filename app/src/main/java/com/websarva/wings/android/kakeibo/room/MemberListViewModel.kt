package com.websarva.wings.android.kakeibo.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MemberListViewModel(application: Application) : AndroidViewModel(application) {
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    // メンバーリストを保持するLiveData
    private val _personList = MutableLiveData<List<Person>>()
    val personList: LiveData<List<Person>> = _personList

    // メンバーリストを取得するメソッド
    fun getPersons(userId: String): LiveData<List<Person>> {
        return personDao.getAllPersonsByUserId(userId)
    }

    // 更新メソッド
    fun updatePerson(person: Person) {
        viewModelScope.launch {
            personDao.updatePerson(person)
            // 更新後にリストを再取得
            _personList.value = personDao.getAllPersonsByUserId(person.userID).value
        }
    }

    // 削除メソッド
    fun deletePerson(person: Person) {
        viewModelScope.launch {
            personDao.deletePerson(person)
            // 削除後にリストを再取得
            _personList.value = personDao.getAllPersonsByUserId(person.userID).value
        }
    }
}
