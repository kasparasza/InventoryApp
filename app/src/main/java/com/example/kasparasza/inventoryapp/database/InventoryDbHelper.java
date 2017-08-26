package com.example.kasparasza.inventoryapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

/**
 * Class that implements a set of APIs that will be used in interacting with db
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    // Logcat tag String
    private static final String LOG_TAG = InventoryDbHelper.class.getSimpleName();


    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Inventory.db";

    /**
     * String constants - Table Create Statements.
     */
    // Statement to create Inventory table - FruitList
    private static final String SQL_CREATE_INVENTORY_TABLE =
            "CREATE TABLE " + FruitEntry.TABLE_NAME + " (" +
                    FruitEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FruitEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                    FruitEntry.COLUMN_QUANTITY + " INTEGER NOT NULL," +
                    FruitEntry.COLUMN_PRICE + " REAL NOT NULL," +
                    FruitEntry.COLUMN_IMAGE + " TEXT," +
                    FruitEntry.COLUMN_DESCRIPTION + " TEXT," +
                    FruitEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL," +
                    FruitEntry.COLUMN_SUPPLIER_EMAIL + " TEXT," +
                    FruitEntry.COLUMN_SUPPLIER_PHONE + " TEXT);";

    // Constructor
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method that is called when the db is created for the 1st time
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create db tables
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    // Method that is called when the db is upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // so far the method body is left blank;
        // the default implementation could have this logic:
/*        onUpgrade(db, oldVersion, newVersion);*/
    }
}
