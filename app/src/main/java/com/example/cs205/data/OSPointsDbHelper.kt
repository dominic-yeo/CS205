package com.example.cs205.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OSPointsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "Points.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "os_points"
        private const val COLUMN_POINTS = "points"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_POINTS INTEGER PRIMARY KEY
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun getPoints(points: Int): Int {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_POINTS),
            "$COLUMN_POINTS = ?",
            arrayOf(points.toString()),
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

    fun updatePoints(points: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_POINTS, points)
        }

        db.insertWithOnConflict(
            TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

}