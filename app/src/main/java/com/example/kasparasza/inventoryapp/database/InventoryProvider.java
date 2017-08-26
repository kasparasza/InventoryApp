package com.example.kasparasza.inventoryapp.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.example.kasparasza.inventoryapp.R;
import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

import java.util.ArrayList;

/**
 * A subclass of the ContentProvider that will be used to interact with {@link InventoryContract} database
 */

public class InventoryProvider extends ContentProvider {

    /** Declaration of global constants */
    // Logcat tag String
    private static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    // URI matcher code for the content URI for the fruits table
    public static final int FRUITS = 100;
    // URI matcher code for the content URI for the specific row from fruit table
    public static final int FRUIT_ID = 101;

    /** Declaration of global variables & objectss */

    // Helper object that will perform communication with the db
    private InventoryDbHelper mDbHelper = null;

    /** URI matcher object to match a context URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // This URI is used to provide access to MULTIPLE rows the fruits table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_FRUIT, FRUITS);

        // This URI is used to provide access to ONE single row of the fruits table.
        // the "#" wildcard is used where "#" can be substituted for an integer.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_FRUIT + "/#", FRUIT_ID);
    }

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // Initialisation of DbHelper object to gain access to the fruits database
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     * @return Cursor object with that contains the results of query
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor object will hold the result of the query
        Cursor cursor = null;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);

        try{
            switch (match) {
                case FRUITS:
                    // For the FRUITS code, query the fruits table directly with the given
                    // projection, selection, selection arguments, and sort order. The cursor
                    // could contain multiple rows of the fruits table.
                    //
                    // this will perform the query on fruits table and will return all the rows of the table,
                    // the particular columns returned by the query will depend upon projection input parameter
                    cursor = database.query(FruitEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                case FRUIT_ID:
                    // For the FRUIT_ID code, extract out the ID from the URI.
                    //
                    // For every "?" in the selection, we need to have an element in the selection
                    // arguments that will fill in the "?". Since we have 1 question mark in the
                    // selection, we have 1 String in the selection arguments' String array.
                    selection = FruitEntry._ID + "=?";
                    selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                    // This will perform a query on the fruits table where the _id equals # to return a
                    // Cursor containing that row of the table.
                    cursor = database.query(FruitEntry.TABLE_NAME, projection, selection, selectionArgs,
                            null, null, sortOrder);
                    break;
                default:
                    throw new IllegalArgumentException("Cannot query unknown URI " + uri);
            }
        } catch (IllegalArgumentException exception_IA_02){
            Log.e(LOG_TAG, "Illegal argument exception was encountered " + exception_IA_02);
        }

        // Register to watch a content URI for changes
        // this watch needs to be set up before the final resulting cursor object is returned
        // if the data at this uri changes, we know that we have to update the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Return content type for the Uri that is supported by the Provider
     * @param uri URI of the db item
     * @return String that describes content type
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);

        try{
            switch (match){
                case FRUITS:
                    return FruitEntry.CONTENT_LIST_TYPE;
                case FRUIT_ID:
                    return FruitEntry.CONTENT_ITEM_TYPE;
                default:
                    throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
            }
        } catch (IllegalStateException exception_IS){
            Log.e(LOG_TAG, "IllegalStateException was encountered " + exception_IS);
            return null;
        }
    }

    /**
     * Inserts a new item / row into the db with the given content values.
     * @return Uri of the newly added db item / row
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // Figure out if the URI matcher can match the URI to a specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FRUITS:
                return insertFruit(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Inserts a new Fruit item / row into the db with the given content values.
     * @return Uri of the newly added db item / row
     */
    public Uri insertFruit(Uri uri, ContentValues values) {
        // check whether data to be entered into db complies with the db restrictions
        if (!checkInputData(values)){
            // in input data is incompatible - no data input into db is performed
            // the user remains in the same activity
            // in this case the resulting Uri will be null
            return null;
        } else {
            // in input data check was passed - data input into db is performed:
            // Get writable database
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Insert the data into the db
            long id = database.insert(FruitEntry.TABLE_NAME, null, values);

            // display a toast with a feedback
            //
            // if the ID is <0  - the adding to db has resulted in an error
            // in this case the resulting Uri will be null
            if (id < 0 ){
                Toast.makeText(getContext(), getContext().getString(R.string.toast_insert_to_db_error), Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Failed to insert row for " + uri);
                return null;
            } else {
                Toast.makeText(getContext(), getContext().getString(R.string.toast_insert_to_db_success), Toast.LENGTH_SHORT).show();

                // we notify ContentResolver that there was a change in the data
                // this will then trigger an update to our cursor
                getContext().getContentResolver().notifyChange(uri, null);

                // Once we know the ID of the new row in the table,
                // return the new URI with the ID appended to the end of it
                return ContentUris.withAppendedId(uri, id);
            }
        }
    }

    /**
     * Inserts multiple items / rows into the db with the given content values.
     * @param uri Uri with the address of the db table where the insertion will be performed
     * @param values An array of sets of column_name/value pairs to add to the database.
     * @return int that is equal to the number of rows that were inserted
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values){
        // Figure out if the URI matcher can match the URI to a specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FRUITS:
                return bulkInsertToFruits(uri, values);
            default:
                throw new IllegalArgumentException("Bulk insertion is not supported for " + uri);
        }
    }

    /**
     * Inserts multiple new Fruits / rows into the db with the given content values.
     * @param uri Uri with the address of the db table where the insertion will be performed
     * @param values An array of sets of column_name/value pairs to add to the database.
     * @return int that is equal to the number of rows that were inserted
     *
     * the code being used has been taken from solutions proposed by Warlock
     * (https://stackoverflow.com/questions/12730908/how-to-use-bulkinsert-function-in-android)
     */
    public int bulkInsertToFruits(Uri uri, ContentValues[] values) {
        // we do not perform an additional check whether the data to be inserted is compatible
        // with the db restrictions / rules, as the method will be called only to insert a set of dummy
        // values which have been tailored to the db requirements and will not change

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // variable that stores the number of rows that were inserted
        int numInserted = 0;

        // Begin a db transaction
        database.beginTransaction();
        try {
            for (ContentValues contentValues : values) {
                long newID = database.insertOrThrow(FruitEntry.TABLE_NAME, null, contentValues);
                if (newID <= 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }
            }
            // changes will be rolled back if any transaction is ended without being marked as clean (by calling setTransactionSuccessful)
            database.setTransactionSuccessful();
            // if the insert was successful, we notify ContentResolver that there was a change in the data
            // this will then trigger an update to our curso
            getContext().getContentResolver().notifyChange(uri, null);
            numInserted = values.length;
        } catch (IllegalArgumentException exception_IA_04){
            // if an error was encountered - display a Toast for the user and record a Log
            Toast.makeText(getContext(), getContext().getString(R.string.toast_insert_to_db_error_bulk_insert), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "IllegalArgumentException was encountered " + exception_IA_04);
        }
        finally {
            // ending statement for the transaction
            database.endTransaction();
        }
        // return the number of rows that were inserted
        return numInserted;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     *@return integer variable with the number of rows that were deleted from the db
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // number of deleted rows variable
        int numberOfDeletedRows;

        final int match = sUriMatcher.match(uri);

        try {
            switch (match) {
                case FRUITS:
                    // Delete all rows that match the selection and selection args
                    // get the number of rows affected
                    numberOfDeletedRows = database.delete(FruitEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                case FRUIT_ID:
                    // Delete a single row given by the ID in the URI
                    // get the number of rows affected
                    selection = FruitEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    numberOfDeletedRows = database.delete(FruitEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                default:
                    throw new IllegalArgumentException("Deletion is not supported for " + uri);
            }
        } catch (IllegalArgumentException exception_IA_01) {
            // we display Toast that the information could not be deleted from the db
            Toast.makeText(getContext(), getContext().getString(R.string.toast_delete_from_db_error), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "IllegalArgumentException was encountered " + exception_IA_01);

            // the number of rows affected is zero
            numberOfDeletedRows = 0;
        }

        // if the number of rows affected is positive, we notify ContentResolver that there was a change in the data
        // this will then trigger an update to our cursor
        if (numberOfDeletedRows != 0 ){
            getContext().getContentResolver().notifyChange(uri, null);

            // also we display Toast that the information was deleted from the db
            Toast.makeText(getContext(), getContext().getString(R.string.toast_delete_from_db_success), Toast.LENGTH_SHORT).show();
        }

        return numberOfDeletedRows;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     * @return integer variable with the number of rows that were updated in the db
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        // number of updated rows variable
        int numberOfUpdatedRows;

        try{
            switch (match) {
                case FRUITS:
                    numberOfUpdatedRows = updateFruitItem(uri, contentValues, selection, selectionArgs);
                    break;
                case FRUIT_ID:
                    // For the FRUIT_ID code, extract out the ID from the URI,
                    // so we know which row to update. Selection will be "_id=?" and selection
                    // arguments will be a String array containing the actual ID.
                    selection = FruitEntry._ID + "=?";
                    selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                    numberOfUpdatedRows = updateFruitItem(uri, contentValues, selection, selectionArgs);
                    break;
                default:
                    throw new IllegalArgumentException("Update is not supported for " + uri);
            }
        } catch (IllegalArgumentException exception_IA_03){
            Log.e(LOG_TAG, "IllegalArgumentException was encountered " + exception_IA_03);

            // the number of rows affected is zero
            numberOfUpdatedRows = 0;

        }
        return numberOfUpdatedRows;
    }

    /**
     * Update fruits in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more fruits).
     * Return the number of rows that were successfully updated.
     */
    private int updateFruitItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // check #1) whether data to be entered into db complies with the db restrictions,
        // #2) ContentValues are not empty (a case where user pressed button to save changes without any actual changes made)
        if (!checkInputData(values) || values.size() == 0){
            // in input data is incompatible (or there is no input) - no data input into db is performed
            // the user remains in the same activity
            // in this case the resulting Uri will be null
            return 0;
        } else {
            // If the results of Data validation are ok - we proceed with db update:
            // get the database in the write mode
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            // Update the selected fruits in the database table with the given ContentValues
            // get the number of rows affected
            int numberOfRows = database.update(FruitEntry.TABLE_NAME, values, selection, selectionArgs);

            // if the number of rows affected is a positive number, we notify ContentResolver that there was a change in the data
            // this will then trigger an update to our cursor
            if (numberOfRows > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            // Return the number of rows that were affected
            return numberOfRows;
        }
    }


    ////////
    // Other helper methods:
    ////////

    /**
     * Check input data, whether it complies with the db restrictions, before the data is entered into the db;
     * Provide feedback / toast message to the user, if there are errors in input data.
     * @param values an object of ContentValues to be entered
     * @return Boolean that is true, if the input data is correct
     */
    private Boolean checkInputData(ContentValues values){
        // ArrayList that will be used for construction of the error Toast message
        ArrayList<String> inputFieldsWithErrors = new ArrayList<String>();

        //// performing checks:
        // Check that item name is not null, or not an empty String
        if (values.containsKey(FruitEntry.COLUMN_ITEM_NAME)){
            String itemName = values.getAsString(FruitEntry.COLUMN_ITEM_NAME);
            if (itemName == null || itemName.matches("")) {
                Log.e(LOG_TAG, "Inventory item requires a name");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_item_name));
            }
        }

        // Check that quantity is not null and is not negative
        // currently quantity quantity cannot be null, as in EditInventoryActivity empty String input
        // is set to be equal to "0". However, this check is left here in case there are changes in other parts of the code.
        //--------------------------------------------------------------------> ar quantity input vis dar yra String, ar nepasikeite Edit activity?
        if(values.containsKey(FruitEntry.COLUMN_QUANTITY)){
            Integer quantity = values.getAsInteger(FruitEntry.COLUMN_QUANTITY);
            if(quantity == null || quantity < 0){
                Log.e(LOG_TAG, "Inventory quantity has to be defined & cannot be less than zero");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_quantity));
            }
        }

        // Check that price is not null and is greater than zero
        if(values.containsKey(FruitEntry.COLUMN_PRICE)){
            Float price = values.getAsFloat(FruitEntry.COLUMN_PRICE);
            if(price == null || price <= 0){
                Log.e(LOG_TAG, "Inventory price has to be defined & has to be positive");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_price));
            }
        }

        // check that item image is not null, or not an empty String
        if(values.containsKey(FruitEntry.COLUMN_IMAGE)){
            String image = values.getAsString(FruitEntry.COLUMN_IMAGE);
            if (image == null || image.matches("")) {
                Log.e(LOG_TAG, "Item requires an image");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_image));
            }
        }

        // description can be null; no additional check is necessary

        // check that item name is not null, or not an empty String
        if(values.containsKey(FruitEntry.COLUMN_SUPPLIER_NAME)){
            String supplierName = values.getAsString(FruitEntry.COLUMN_SUPPLIER_NAME);
            if (supplierName == null || supplierName.matches("")) {
                Log.e(LOG_TAG, "Supplier field requires a name");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_supplier_name));
            }
        }

        //check that at least one of the two: supplier's email of phone number is not null or not empty
        if(values.containsKey(FruitEntry.COLUMN_SUPPLIER_EMAIL) || values.containsKey(FruitEntry.COLUMN_SUPPLIER_PHONE)){
            String supplierEMail = values.getAsString(FruitEntry.COLUMN_SUPPLIER_EMAIL);
            String supplierPhone = values.getAsString(FruitEntry.COLUMN_SUPPLIER_PHONE);
            if ((supplierEMail == null || supplierEMail.matches("")) && (supplierPhone == null || supplierPhone.matches(""))){
                Log.e(LOG_TAG, "At least one of the two: supplier's email of phone number has to be provided");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_supplier_contacts));
            }

            //check that supplier's email (if provided) matches an e-mail pattern
            if(!supplierEMail.matches("") && !Patterns.EMAIL_ADDRESS.matcher(supplierEMail).matches()){
                Log.e(LOG_TAG, "Supplier's email does not match an e-mail pattern");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_supplier_e_mail));
            }
            //check that supplier's phone (if provided) matches an e-mail pattern
            if(!supplierPhone.matches("") && !Patterns.PHONE.matcher(supplierPhone).matches()){
                Log.e(LOG_TAG, "Supplier's phone does not match a phone number pattern");
                inputFieldsWithErrors.add(getContext().getString(R.string.toast_input_field_supplier_phone));
            }
        }

        //// prepare return result and feedback to the user:
        // if size of ArrayList is empty - all checks were passed
        if (inputFieldsWithErrors.isEmpty()){
            return true;
        } else {
            // prepare feedback message
            String toastString = getContext().getString(R.string.toast_data_input_check_error);
            for(int i = 0; i < inputFieldsWithErrors.size(); i++){
            toastString = toastString.concat(inputFieldsWithErrors.get(i) + ", ");
            }
            if (toastString.endsWith(", ")){
                toastString = toastString.substring(0, toastString.length() -  2);
            }
            //display the message
            Toast.makeText(getContext(), toastString, Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
