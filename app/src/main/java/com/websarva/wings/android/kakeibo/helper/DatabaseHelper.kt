package com.websarva.wings.android.kakeibo.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context): SQLiteOpenHelper(context,
    DATABASE_NAME,null,
    DATABASE_VERSION
) {
    companion object{
        private const val DATABASE_NAME = "balance.db"
        private const val DATABASE_VERSION = 1
    }
    override fun onCreate(db:SQLiteDatabase){
        // member テーブルの作成
        val memberTable = """
            CREATE TABLE member (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT,
                member_name TEXT,
                CONSTRAINT unique_user_member UNIQUE (user_id, member_name)
            );
        """
        db.execSQL(memberTable)

        // payment_history テーブルの作成
        val paymentHistoryTable = """
            CREATE TABLE payment_history (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER,
                user_id TEXT,
                purpose_id INTEGER,
                payment_date DATE,
                amount INTEGER,
                is_recept_checked BOOLEAN,
                note TEXT,
                FOREIGN KEY (member_id) REFERENCES member(_id) ON DELETE RESTRICT,
                FOREIGN KEY (purpose_id) REFERENCES payment_purpose(_id) ON DELETE RESTRICT
            );
        """
        db.execSQL(paymentHistoryTable)

        // payment_purpose テーブルの作成
        val paymentPurposeTable = """
            CREATE TABLE payment_purpose (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT,
                pay_purpose_name TEXT,
                CONSTRAINT unique_user_pay_purpose UNIQUE (user_id, pay_purpose_name)
            );
        """
        db.execSQL(paymentPurposeTable)
    }

    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS member")
            db.execSQL("DROP TABLE IF EXISTS payment_history")
            db.execSQL("DROP TABLE IF EXISTS payment_purpose")
            onCreate(db)
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // 外部キー制約を有効にする
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    //userIDに紐づいた支払い目的の名前をリスト型の変数で返すメソッド
    @SuppressLint("Range")
    fun getPaymentPurposesForUser(userId: String): List<String> {
        val paymentPurposes = mutableListOf<String>()
        val db = this.readableDatabase

        // user_id に基づいてデータを絞り込むクエリ
        val cursor = db.rawQuery(
            "SELECT pay_purpose_name FROM payment_purpose WHERE user_id = ?",
            arrayOf(userId)  // プレースホルダ ? に userId をセット
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val payPurposeName = cursor.getString(cursor.getColumnIndex("pay_purpose_name"))
                paymentPurposes.add(payPurposeName)
            }
        }
        cursor.close()
        db.close()
        return paymentPurposes
    }

    @SuppressLint("Range")
    fun getPayPurposeNameById(payPurposeId: Int):String{
        // データベースを取得
        val db: SQLiteDatabase = this.readableDatabase

        // クエリを実行して member_name を取得
        val cursor = db.query(
            "payment_purpose", // テーブル名
            arrayOf("pay_purpose_name"), // 取得するカラム
            "_id = ?", // WHERE句
            arrayOf(payPurposeId.toString()), // ? に渡す引数
            null, // GROUP BY句
            null, // HAVING句
            null // ORDER BY句
        )

        var payPurposeName = ""

        // クエリ結果からデータを取得
        if (cursor.moveToFirst()) {
            payPurposeName = cursor.getString(cursor.getColumnIndex("pay_purpose_name"))
        }
        cursor.close()
        db.close()

        return payPurposeName
    }

    @SuppressLint("Range")
    fun getPaymentPurposeId(userId: String, payPurposeName: String): Long? {
        // データベースを取得
        val db: SQLiteDatabase = this.readableDatabase

        // クエリを実行して _id を取得
        val cursor: Cursor = db.query(
            "payment_purpose", // テーブル名
            arrayOf("_id"), // 取得するカラム（_idのみ）
            "user_id = ? AND pay_purpose_name = ?", // WHERE句
            arrayOf(userId, payPurposeName), // WHERE句に渡す引数
            null, // GROUP BY句
            null, // HAVING句
            null // ORDER BY句
        )

        // クエリ結果から _id を取得
        var paymentPurposeId: Long? = null
        if (cursor.moveToFirst()) {
            paymentPurposeId = cursor.getLong(cursor.getColumnIndex("_id"))
        }

        cursor.close()
        db.close()

        return paymentPurposeId
    }

    //userIDに紐づいたメンバーの名前をリスト型の変数で返すメソッド
    @SuppressLint("Range")
    fun getMemberForUser(userId: String): List<String> {
        val memberList = mutableListOf<String>()
        // データベースを取得
        val db: SQLiteDatabase = this.readableDatabase

        // user_id に基づいてデータを絞り込むクエリ
        val cursor = db.rawQuery(
            "SELECT member_name FROM member WHERE user_id = ?",
            arrayOf(userId)  // プレースホルダ ? に userId をセット
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val memberName = cursor.getString(cursor.getColumnIndex("member_name"))
                memberList.add(memberName)
            }
        }
        cursor.close()
        db.close()
        return memberList
    }

    @SuppressLint("Range")
    fun getMemberNameById(memberId: Int):String{
        // データベースを取得
        val db: SQLiteDatabase = this.readableDatabase

        // クエリを実行して member_name を取得
        val cursor = db.query(
            "member", // テーブル名
            arrayOf("member_name"), // 取得するカラム
            "_id = ?", // WHERE句
            arrayOf(memberId.toString()), // ? に渡す引数
            null, // GROUP BY句
            null, // HAVING句
            null // ORDER BY句
        )

        var memberName = ""

        // クエリ結果からデータを取得
        if (cursor.moveToFirst()) {
            // member_name を取得
            memberName = cursor.getString(cursor.getColumnIndex("member_name"))
        }
        cursor.close()
        db.close()

        return memberName
    }

    @SuppressLint("Range")
    fun getMemberId(userId: String, memberName: String): Long? {
        // データベースを取得
        val db: SQLiteDatabase = this.readableDatabase

        // クエリを実行して _id を取得
        val cursor: Cursor = db.query(
            "member", // テーブル名
            arrayOf("_id"), // 取得するカラム（_idのみ）
            "user_id = ? AND member_name = ?", // WHERE句
            arrayOf(userId, memberName), // WHERE句に渡す引数
            null, // GROUP BY句
            null, // HAVING句
            null // ORDER BY句
        )

        // クエリ結果から _id を取得
        var memberId: Long? = null
        if (cursor.moveToFirst()) {
            memberId = cursor.getLong(cursor.getColumnIndex("_id"))
        }
        cursor.close()
        db.close()

        return memberId
    }
}