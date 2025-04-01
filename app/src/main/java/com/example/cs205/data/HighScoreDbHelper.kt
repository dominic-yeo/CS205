package com.example.cs205.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HighScoreDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "HighScore.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "high_scores"
        private const val COLUMN_LEVEL = "level"
        private const val COLUMN_SCORE = "score"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_LEVEL INTEGER PRIMARY KEY,
                $COLUMN_SCORE INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getHighScore(level: Int): Int {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_SCORE),
            "$COLUMN_LEVEL = ?",
            arrayOf(level.toString()),
            null,
            null,
            null
        )
        
        return if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else {
            0
        }.also {
            cursor.close()
        }
    }

    fun updateHighScore(level: Int, score: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LEVEL, level)
            put(COLUMN_SCORE, score)
        }

        db.insertWithOnConflict(
            TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun resetAllHighScores() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
} 