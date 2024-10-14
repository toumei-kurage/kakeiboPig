package com.websarva.wings.android.kakeibo.room.member

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.websarva.wings.android.kakeibo.room.AppDatabase
import kotlinx.coroutines.launch

data class MemberResult(val success: Boolean, val message: String)

class MemberViewModel(application: Application) : AndroidViewModel(application) {
    private val personDao: PersonDao = AppDatabase.getDatabase(application).personDao()

    // メンバーリストを保持するLiveData
    private val _personList = MutableLiveData<List<Person>>()

    // メンバーリストを取得するメソッド
    fun getPersons(userId: String): LiveData<List<Person>> {
        return personDao.getAllPersonsByUserId(userId)
    }

    // 削除メソッド
    fun deletePerson(person: Person) {
        viewModelScope.launch {
            personDao.deletePerson(person)
            // 削除後にリストを再取得
            _personList.value = personDao.getAllPersonsByUserId(person.userID).value
        }
    }

    fun getPerson(id: Int): Person {
        return personDao.getPerson(id)
    }

    // 特定のメンバーのIDを取得するメソッド
    suspend fun getPersonId(userId: String, memberName: String): Int {
        return personDao.getPersonId(userId, memberName)
    }

    //メンバー追加の処理
    fun addPerson(person: Person, onResult: (MemberResult) -> Unit) {
        viewModelScope.launch {
            val count = personDao.countByUserIdAndMemberName(person.userID, person.memberName)
            if (count == 0) {
                personDao.insert(person)
                onResult(MemberResult(true, "メンバーが登録されました"))
            } else {
                // ユーザーに重複エラーを通知する処理を追加
                onResult(MemberResult(false, "あなたのアカウントですでに同じ名前のメンバーが存在します"))
            }
        }
    }

    fun updatePerson(person: Person, onResult: (MemberResult) -> Unit) {
        viewModelScope.launch {
            val count = personDao.countByUserIdAndMemberName(person.userID, person.memberName)
            if (count == 0) {
                personDao.updatePerson(person)
                // 更新後にリストを再取得
                _personList.value = personDao.getAllPersonsByUserId(person.userID).value
                onResult(MemberResult(true, "メンバー名前が更新されました"))
            } else {
                // ユーザーに重複エラーを通知する処理を追加
                onResult(MemberResult(false, "あなたのアカウントですでに同じ名前のメンバーが存在します"))
            }
        }
    }
}
