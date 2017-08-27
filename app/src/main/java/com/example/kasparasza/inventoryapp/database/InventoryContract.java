package com.example.kasparasza.inventoryapp.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A contract class that specifies the layout of Inventory db schema
 */

public class InventoryContract {

    // Uri objects that hold Uri for communication with content provider
    public static final String CONTENT_AUTHORITY = "com.example.kasparasza.inventoryapp.database";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FRUIT = FruitEntry.TABLE_NAME;
    // Logcat tag String
    private static final String LOG_TAG = InventoryContract.class.getSimpleName();

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private InventoryContract() {
    }

    /* Inner class that defines the table contents */
    public static class FruitEntry implements BaseColumns {

        // The content URI to access the fruit data in the provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_FRUIT);

        /**
         * The MIME type for a list of fruits.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRUIT;

        /**
         * The MIME type of a single fruit.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FRUIT;

        /**
         * Name of the db table
         */
        public static final String TABLE_NAME = "FruitList";

        /*
        * Unique ID number for each inventory list item (only for use in the database table)
        *
        * Type: INTEGER
        **/
        public static final String _ID = BaseColumns._ID;

        /*
        * Inventory item name
        *
        * Type: TEXT
        **/
        public static final String COLUMN_ITEM_NAME = "item_name";

        /*
        * Inventory item quantity currently held in stock (quantity can only be denoted as whole numbers)
        *
        * Type: INTEGER
        **/
        public static final String COLUMN_QUANTITY = "quantity";

        /*
        * Inventory item price
        *
        * Type: REAL
        **/
        public static final String COLUMN_PRICE = "price";

        /*
        * Inventory item image (URI of the image resource held as String)
        *
        * Type: TEXT
        **/
        public static final String COLUMN_IMAGE = "image";

        /*
        * Inventory item description
        *
        * Type: TEXT
        **/
        public static final String COLUMN_DESCRIPTION = "description";

        /*
        * Name / company name of the supplier
        *
        * Type: TEXT
        **/
        public static final String COLUMN_SUPPLIER_NAME = "supplier_name";

        /*
        * Email contact address of the supplier
        *
        * Type: TEXT
        **/
        public static final String COLUMN_SUPPLIER_EMAIL = "supplier_email";

        /*
        * Phone contact number of the supplier (held as String)
        *
        * Type: TEXT
        **/
        public static final String COLUMN_SUPPLIER_PHONE = "supplier_phone";
    }
}
