package com.websarva.wings.android.kakeibo.room.member

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PersonDao {
    @Insert
    suspend fun insert(person: Person)

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("SELECT * FROM person_table WHERE userID = :userID")
    fun getAllPersonsByUserId(userID: String): LiveData<List<Person>>
}