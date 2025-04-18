package com.example.cs205.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class OSPointsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "Shop.db"
        private const val DATABASE_VERSION = 3

        // Table for Points
        private const val TABLE_POINTS = "os_points"
        private const val COLUMN_ID_POINTS = "_id"
        private const val COLUMN_POINTS = "points"
        private const val POINTS_ID = 1

        // Table for Active Items
        private const val TABLE_ACTIVE_ITEMS = "active_items"
        private const val COLUMN_ID_ITEMS = "_id"
        private const val COLUMN_ITEM_ID = "item_id"
        private const val COLUMN_ITEM_NAME = "item_name"

        // Shop Items Table
        private const val TABLE_SHOP_ITEMS = "shop_items"
        private const val COLUMN_SHOP_ITEM_ID = "shop_item_id"
        private const val COLUMN_SHOP_ITEM_NAME = "name"
        private const val COLUMN_SHOP_ITEM_DESCRIPTION = "description"
        private const val COLUMN_SHOP_ITEM_COST = "cost"

        private const val PREF_NAME = "app_prefs"
        private const val KEY_INITIAL_DATA_INSERTED = "initial_data_inserted"
    }

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun onCreate(db: SQLiteDatabase) {
        // Create the points table
        val createPointsTable =
            "CREATE TABLE $TABLE_POINTS (" +
                    "$COLUMN_ID_POINTS INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_POINTS INTEGER NOT NULL)"
        db.execSQL(createPointsTable)

        // Create the active items table
        val createActiveItemsTable = """
            CREATE TABLE $TABLE_ACTIVE_ITEMS (
                $COLUMN_ID_ITEMS INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ITEM_ID INTEGER NOT NULL UNIQUE,
                $COLUMN_ITEM_NAME TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createActiveItemsTable)

        val createShopItemsTable =
            "CREATE TABLE $TABLE_SHOP_ITEMS (" +
                    "$COLUMN_SHOP_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_SHOP_ITEM_NAME TEXT NOT NULL, " +
                    "$COLUMN_SHOP_ITEM_DESCRIPTION TEXT NOT NULL, " +
                    "$COLUMN_SHOP_ITEM_COST INTEGER NOT NULL)"
        db.execSQL(createShopItemsTable)

        // Check if initial data has already been inserted
        if (!sharedPreferences.getBoolean(KEY_INITIAL_DATA_INSERTED, false)) {
            initShop(db)
            sharedPreferences.edit().putBoolean(KEY_INITIAL_DATA_INSERTED, true).apply()
        }

        // Init points to 0
        val cursor = db.query(TABLE_POINTS, arrayOf(COLUMN_ID_POINTS), null, null, null, null, null)
        if (cursor.count == 0) {
            val initialPoints = ContentValues().apply {
                put(COLUMN_POINTS, 0)
            }
            db.insert(TABLE_POINTS, null, initialPoints)
        }
        cursor.close()
    }

    private fun initShop(db: SQLiteDatabase) {
        val item1 = ContentValues().apply {
            put(COLUMN_SHOP_ITEM_NAME, "Faster Resources I")
            put(COLUMN_SHOP_ITEM_DESCRIPTION, "Resources are produced 1s faster")
            put(COLUMN_SHOP_ITEM_COST, 500)
        }
        db.insert(TABLE_SHOP_ITEMS, null, item1)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POINTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIVE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SHOP_ITEMS")
        onCreate(db)
    }

    fun getShopItems(): List<ShopItem> {
        val shopItems = mutableListOf<ShopItem>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_SHOP_ITEMS,
            arrayOf(
                COLUMN_SHOP_ITEM_ID,
                COLUMN_SHOP_ITEM_NAME,
                COLUMN_SHOP_ITEM_DESCRIPTION,
                COLUMN_SHOP_ITEM_COST
            ),
            null, null, null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_SHOP_ITEM_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_SHOP_ITEM_NAME))
                val description = it.getString(it.getColumnIndexOrThrow(COLUMN_SHOP_ITEM_DESCRIPTION))
                val cost = it.getInt(it.getColumnIndexOrThrow(COLUMN_SHOP_ITEM_COST))
                shopItems.add(ShopItem(id, name, description, cost))
            }
        }
        db.close()
        return shopItems
    }

    fun getPoints(): Int {
        val db = this.readableDatabase
        var points = 0
        val cursor: Cursor? = try {
            db.query(
                TABLE_POINTS,
                arrayOf(COLUMN_POINTS),
                "$COLUMN_ID_POINTS = ?",
                arrayOf(POINTS_ID.toString()),
                null,
                null,
                null
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            if (it.moveToFirst()) {
                points = it.getInt(it.getColumnIndexOrThrow(COLUMN_POINTS))
            }
        }
        db.close()
        return points
    }

    fun updatePoints(points: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_POINTS, points)
        }
        db.update(
            TABLE_POINTS,
            values,
            "$COLUMN_ID_POINTS = ?",
            arrayOf(POINTS_ID.toString())
        )
        db.close()
    }

    // items here
    fun addActiveItem(itemId: Int, itemName: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ITEM_ID, itemId)
            put(COLUMN_ITEM_NAME, itemName)
            // Add other relevant data from ShopItem if needed
        }
        db.insertWithOnConflict(
            TABLE_ACTIVE_ITEMS,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE // Or CONFLICT_REPLACE if you only want one active instance
        )
        db.close()
    }

    fun removeActiveItem(itemId: Int) {
        val db = this.writableDatabase
        db.delete(
            TABLE_ACTIVE_ITEMS,
            "$COLUMN_ITEM_ID = ?",
            arrayOf(itemId.toString())
        )
        db.close()
    }

    fun getActiveItems(): List<Int> {
        val db = this.readableDatabase
        val activeItemIds = mutableListOf<Int>()
        val cursor: Cursor? = try {
            db.query(
                TABLE_ACTIVE_ITEMS,
                arrayOf(COLUMN_ITEM_ID),
                null,
                null,
                null,
                null,
                null
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            while (it.moveToNext()) {
                activeItemIds.add(it.getInt(it.getColumnIndexOrThrow(COLUMN_ITEM_ID)))
            }
        }
        db.close()
        return activeItemIds
    }

}