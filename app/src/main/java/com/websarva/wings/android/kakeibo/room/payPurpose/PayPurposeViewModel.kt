package com.websarva.wings.android.kakeibo.room.payPurpose

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.websarva.wings.android.kakeibo.room.AppDatabase
import kotlinx.coroutines.launch

data class PayPurposeResult(val success: Boolean, val message: String)

class PayPurposeViewModel(application: Application) : AndroidViewModel(application) {
    private val payPurposeDao = AppDatabase.getDatabase(application).payPurposeDao()
    // 支払い目的リストを保持するLiveData
    private val _payPurposeList = MutableLiveData<List<PayPurpose>>()

    // 支払い目的リストを取得するメソッド
    fun getPayPurposes(userID: String): LiveData<List<PayPurpose>> {
        return payPurposeDao.getAllPayPurposeByUserID(userID)
    }

    //支払い目的のIDを取得するメソッド
    suspend fun getPayPurposeID(userID:String,payPurposeName:String):Int{
        return payPurposeDao.getPayPurposeID(userID = userID,payPurposeName=payPurposeName)
    }

    fun getPayPurpose(id:Int):PayPurpose{
        return payPurposeDao.getPayPurpose(id)
    }

    // 支払い目的を追加するメソッド
    fun addPayPurpose(payPurpose: PayPurpose, onResult: (PayPurposeResult) -> Unit) {
        viewModelScope.launch {
            val data = payPurposeDao.countByUserIdAndPurposeName(payPurpose.userID, payPurpose.payPurposeName)
            if (data.isNotEmpty() && data[0].count == 0) {
                payPurposeDao.insert(payPurpose)
                onResult(PayPurposeResult(true, "支払い目的が追加されました"))
            } else {
                // ユーザーに重複エラーを通知する処理
                onResult(PayPurposeResult(false, "あなたのアカウントですでに同じ名前の支払い目的が存在します"))
            }
        }
    }

    // 支払い目的を更新するメソッド
    fun updatePayPurpose(payPurpose: PayPurpose, onResult: (PayPurposeResult) -> Unit) {
        viewModelScope.launch {
            val result = payPurposeDao.countByUserIdAndPurposeName(payPurpose.userID, payPurpose.payPurposeName)
            if (result.isNotEmpty()) {
                val (count, id) = result[0]
                if (count == 0 || (count == 1 && payPurpose.id == id)) {
                    payPurposeDao.updatePayPurpose(payPurpose)
                    // 支払い目的の再取得
                    _payPurposeList.value = payPurposeDao.getAllPayPurposeByUserID(payPurpose.userID).value
                    onResult(PayPurposeResult(true, "支払い目的の名前が更新されました"))
                } else {
                    // ユーザーに重複エラーを通知する処理
                    onResult(PayPurposeResult(false, "あなたのアカウントで既に同じ名前の支払い目的が存在します"))
                }
            } else {
                // カウントが取得できなかった場合の処理（必要に応じて）
                onResult(PayPurposeResult(false, "エラーが発生しました"))
            }
        }
    }

    // 削除メソッド
    fun deletePayPurpose(payPurpose:PayPurpose) {
        viewModelScope.launch {
            payPurposeDao.deletePayPurpose(payPurpose)
            // 削除後にリストを再取得
            _payPurposeList.value = payPurposeDao.getAllPayPurposeByUserID(payPurpose.userID).value
        }
    }

}