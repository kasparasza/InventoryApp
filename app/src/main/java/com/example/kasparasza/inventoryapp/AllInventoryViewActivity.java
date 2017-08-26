package com.example.kasparasza.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
* Activity that shows ListView of the items stored in the db
* */
public class AllInventoryViewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Binding / declaration of the UI Views
    @BindView(R.id.list_view)ListView listView;
    @BindView(R.id.item_name)TextView itemName;
    @BindView(R.id.item_image)ImageView itemImage;
    @BindView(R.id.item_description) TextView itemDescription;
    @BindView(R.id.empty_view) View emptyView;
    @BindView(R.id.container_for_adapter_view) View containerForAdapterView;

    // String constants used:
    private static final String LOG_TAG = AllInventoryViewActivity.class.getSimpleName();
    public static final int LOADER_ID = 0;
    private static final String ITEM_POSITION = "ITEM_POSITION";
    private static final String LIST_VIEW_ITEM_INDEX = "LIST_VIEW_ITEM_INDEX";
    private static final String LIST_VIEW_TOP = "LIST_VIEW_TOP";
    public static final String DIALOG_DELETE_ALL_ITEMS = "DIALOG_DELETE_ALL_ITEMS";
    public static final String DIALOG_INSERT_ITEMS = "DIALOG_INSERT_ITEMS";

    // Global objects & variables:
    private InventoryCursorAdapter cursorAdapter;
    private Cursor cursor; // cursor object that separately from InventoryCursorAdapter we use to populate UI views other than ListView
    private Bitmap itemImageBitmap;
    private Uri itemImageUri;
    private Integer selectedItem = 0;
    private String itemNameString;
    private String itemDescriptionString;
    private Boolean dialogDeleteAllItemsIsOpen = false;
    private Boolean dialogInsertDummyItemsIsOpen = false;
    private AlertDialog alertDialogDeleteAllItems;
    private AlertDialog alertDialogInsertDummyItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_inventory_view);

        // Binding / initialisation - committing the annotations being used to the UI
        ButterKnife.bind(this);

        // initialisation of Cursor adapter and binding it to the ListView;
        // at first the second argument - Cursor - is set to null, it will be replaced by
        // onLoadFinished() method with a Cursor obtained from a CursorLoader
        cursorAdapter = new InventoryCursorAdapter(this, null);

        // set an empty view on the ListView, which will be displayed if the list has 0 items.
        listView.setEmptyView(emptyView);

        // attach the adapter to the ListView - to display the contents of the Cursor
        listView.setAdapter(cursorAdapter);

        // initialise a Loader that will query data from db
        getLoaderManager().initLoader(LOADER_ID, null, this);

        // Setting OnClick Listener for items in the ListView
        // OnClick shows selected detailed information (e.g. description, image, etc) about the ListView item
        // which was clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // the user has clicked/ selected one of the ListView items, therefore we can update
                // the global variable which records the position of this clicked item
                selectedItem = position;
                // call the method that updates UI Views
                showInformationAboutOneItem(position);
            }
        });

        // Setting OnLongClick Listener for items in the ListView
        // OnLongClick is used to distinguish actions of the UI from those performed after normal OClick
        // OnLongClick opens InventoryDetails Activity
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                // get Uri of the inventory item to be opened
                Uri itemClickedUri = ContentUris.withAppendedId(FruitEntry.CONTENT_URI, id);
                // create an Intent and pass the Uri to it
                Intent openItemDetailsActivity = new Intent(AllInventoryViewActivity.this, ItemDetailsActivity.class);
                openItemDetailsActivity.setData(itemClickedUri);
                // start the Activity
                startActivity(openItemDetailsActivity);
                return false;
            }
        });
    }

    ////////
    // Methods that implement elements of the UI:
    ////////

    // Set up of OnClickListener to open EditInventoryActivity
    @OnClick(R.id.button_add_inventory_item) public void OnClickButtonAddInventoryItem (View view){
        Intent openEditInventoryActivity = new Intent(this, EditInventoryActivity.class);
        startActivity(openEditInventoryActivity);
    }

    /**
     * Method which populates UI with selected detailed information (e.g. description, image, etc) about a particular the ListView item
     * @param position position of the particular ListView item in the List
     */
    private void showInformationAboutOneItem (int position){
        // as the method can be called from other multiple methods in the activity, we check
        // that the Cursor object is not null, or it is not empty (e.g. case of a db table with no records)
        // in both cases any further execution of the method is not reasonable, as there is no information to display
        if (cursor != null && cursor.getCount() != 0){
            // check, whether the requested position is within bounds of the cursor size
            // if it is not - we set the position to be the default one
            if (position >= cursor.getCount()) {
                position = 0;
            }
            // the Cursor which contains the data has been already passed to an adapter that populates the parent ListView
            // move the cursor to the correct row
            cursor.moveToPosition(position);
            // obtain data from the Cursor
            itemImageUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_IMAGE)));
            itemNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_ITEM_NAME));
            itemDescriptionString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_DESCRIPTION));

            // using the Uri of an image, generate a Bitmap
            itemImageBitmap = AppUtilities.getBitmapFromUri(getApplicationContext(), itemImageUri,
                    (int) getResources().getDimension(R.dimen.image_width_02),
                    (int) getResources().getDimension(R.dimen.image_height_02));

            // bind the data to the relevant views & set them to be visible
            itemName.setVisibility(View.VISIBLE);
            itemDescription.setVisibility(View.VISIBLE);
            itemImage.setVisibility(View.VISIBLE);
            itemName.setText(itemNameString);
            itemDescription.setText(itemDescriptionString);
            itemImage.setImageBitmap(itemImageBitmap);
        } else {
            // list view is empty - UI views that display detailed info about an item are set to be invisible
            itemName.setVisibility(View.GONE);
            itemDescription.setVisibility(View.GONE);
            // an empty view image is displayed
            itemImage.setImageResource(R.drawable.empty_shopping_cart);
            itemImage.setScaleType(ImageView.ScaleType.CENTER);
        }
    }

    ////////
    // Methods that implement abstract methods of LoaderCallbacks interface:
    ////////

    // method is called when a new Loader is being created
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after querying it.
        String[] projection = {
                FruitEntry._ID,
                FruitEntry.COLUMN_ITEM_NAME,
                FruitEntry.COLUMN_QUANTITY,
                FruitEntry.COLUMN_PRICE,
                FruitEntry.COLUMN_DESCRIPTION,
                FruitEntry.COLUMN_IMAGE
        };

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                this,                   // context
                FruitEntry.CONTENT_URI, // Uri - all lines of the data table need to be accessed
                projection,             // projection - columns to be included in the Cursor, defined above
                null,                   // selection
                null,                   // selectionArgs
                null                    // sortOrder
        );
    }

    // method that populates UI views with the data obtained from Cursor after load finishes
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        cursorAdapter.swapCursor(data);
        // we set our custom cursor object to hold the same data as the cursor loaded by the Loader
        cursor = data;

        // populate UI Views using data from the Cursor
        // we populate those Views which are outside the ListView and therefore are not managed by the CursorAdapter
        showInformationAboutOneItem(selectedItem);
    }

    // Called when a previously created loader is reset, making the data unavailable
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        cursorAdapter.swapCursor(null);
        // if the Loader is no longer necessary, it means that our custom cursor is as well
        // therefore, set it to null to avoid any memory leaks
        cursor = null;
    }

    ////
    // Methods that handle Options menu creation and behaviour
    ////

    // standard method that creates options menu
    // it is called at the time the activity is created
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/*.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.option_menu_all_items_activity, menu);
        return true;
    }

    // method that sets behaviour to the option menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option
        switch (item.getItemId()) {
            // Respond to a click on the "Delete all" menu option
            case R.id.action_delete_all:
                // call a dialog which asks to confirm the deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Insert dummy items" menu option
            case R.id.action_insert_dummy_items:
                // call a dialog which asks to confirm the insertion
                showInsertionConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    ////////
    // Methods that save variables for the changes in activity lifecycle:
    ////////

    /**
     * Method that saves variables
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // record the state of the ListView
        // get index and top positions of the ListView
        // index - returns the top visible list item
        int index = listView.getFirstVisiblePosition();
        View view = listView.getChildAt(0);
        // returns relative offset from the top of the list
        int top = (view == null) ? 0 : (view.getTop() - listView.getPaddingTop());

        // saving variables to a bundle
        outState.putInt(ITEM_POSITION, selectedItem);
        outState.putInt(LIST_VIEW_ITEM_INDEX, index);
        outState.putInt(LIST_VIEW_TOP, top);
        outState.putBoolean(DIALOG_DELETE_ALL_ITEMS, dialogDeleteAllItemsIsOpen);
        outState.putBoolean(DIALOG_INSERT_ITEMS, dialogInsertDummyItemsIsOpen);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * Method that recreates variables
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Superclass that is being always called
        super.onRestoreInstanceState(savedInstanceState);

        // get information form the Bundle
        if(savedInstanceState != null) {
            // retrieving data from a bundle
            selectedItem = savedInstanceState.getInt(ITEM_POSITION);
            dialogDeleteAllItemsIsOpen = savedInstanceState.getBoolean(DIALOG_DELETE_ALL_ITEMS);
            dialogInsertDummyItemsIsOpen = savedInstanceState.getBoolean(DIALOG_INSERT_ITEMS);

            // del sito irgi gali buti problemu gal -------------------------------------------may need debugging------------------------------------------------
            int index = savedInstanceState.getInt(LIST_VIEW_ITEM_INDEX, 0); // data about the state of the ListView - index and top positions
            int top = savedInstanceState.getInt(LIST_VIEW_TOP, 0);

            // set / restore the position of the ListView
            listView.setSelectionFromTop(index, top);

            // populate UI Views which are outside the ListView and therefore are not managed by the CursorAdapter
            showInformationAboutOneItem(selectedItem);
        }

        // restore Dialogs if these were open
        if (dialogDeleteAllItemsIsOpen){
            showDeleteConfirmationDialog();
        }

        if (dialogInsertDummyItemsIsOpen){
            showInsertionConfirmationDialog();
        }
    }

    /**
     * Method that performs necessary "last" actions before the Activity is about to be closed
     * e.g.: dismisses any open alert dialogs
     */
    @Override
    protected void onPause() {
        super.onPause();

        // dismiss alert dialogs if there were open
        if (alertDialogDeleteAllItems != null) {
            alertDialogDeleteAllItems.dismiss();
        }
        if (alertDialogInsertDummyItems != null) {
            alertDialogInsertDummyItems.dismiss();
        }
    }

    ////////
    // Methods that perform CRUD actions on the db:
    ////////

    /**
     * Method that deletes all items from the db
     */
    private void deleteAllItemsInDb(){
        // call the method to delete all the db records
        // Uri used is the Uri that accesses all the data table
        getContentResolver().delete(FruitEntry.CONTENT_URI, null, null);
    }

    /**
     * Method that inserts multiple of dummy item records to the db
     *
     * the code being used has been taken from solutions proposed by user1094747 and m0skit0
     * (https://stackoverflow.com/questions/9395021/duplicated-contentvalues-in-contentvalues-array)
     */
    private void insertMultipleDummyItems(){
        // preparation of Content Values Array which will store the values for the multiple entries to be made to the db

        // STEP 1: create a List that holds ContentValue objects as its items
        List<ContentValues> mValueList = new ArrayList<>();

        // adding the 1st item
        ContentValues values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_aubergine_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 2.9);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.aubergines));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_aubergine_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_aubergine_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_aubergine_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_aubergine_supplier_phone));
        mValueList.add(values);

        // adding the 2nd item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_banana_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 0.9);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.bananas));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_banana_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_banana_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_banana_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_banana_supplier_phone));
        mValueList.add(values);

        // adding the 3rd item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_cherry_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 2.11);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.cherries));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_cherry_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_cherry_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_cherry_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_cherry_supplier_phone));
        mValueList.add(values);

        // adding the 4th item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_grapefruit_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 1.59);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.grapefruits));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_grapefruit_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_grapefruit_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_grapefruit_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_grapefruit_supplier_phone));
        mValueList.add(values);

        // adding the 5th item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_pineapple_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 4.59);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.pineapples));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_pineapple_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_pineapple_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_pineapple_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_pineapple_supplier_phone));
        mValueList.add(values);

        // adding the 6th item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_strawberry_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 6);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.strawberries));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_strawberry_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_strawberry_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_strawberry_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_strawberry_supplier_phone));
        mValueList.add(values);

        // adding the 7th item
        values = new ContentValues();
        values.put(FruitEntry.COLUMN_ITEM_NAME, getString(R.string.dummy_tomato_name));
        values.put(FruitEntry.COLUMN_QUANTITY, 10);
        values.put(FruitEntry.COLUMN_PRICE, 3.99);
        values.put(FruitEntry.COLUMN_IMAGE, getUriOfAnAsset(R.drawable.tomatoes));
        values.put(FruitEntry.COLUMN_DESCRIPTION, getString(R.string.dummy_tomato_description));
        values.put(FruitEntry.COLUMN_SUPPLIER_NAME, getString(R.string.dummy_tomato_supplier));
        values.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, getString(R.string.dummy_tomato_supplier_email));
        values.put(FruitEntry.COLUMN_SUPPLIER_PHONE, getString(R.string.dummy_tomato_supplier_phone));
        mValueList.add(values);

        // STEP 2: Convert the List from step 1 into Content Values Array
        // create Content Values Array that has a size equal to the size of the List
        ContentValues[] mValueArray = new ContentValues[mValueList.size()];
        // convert the List
        mValueList.toArray(mValueArray);

        // call the method to insert multiple rows to the db records
        // Uri used is the Uri that accesses all the data table
        getContentResolver().bulkInsert(FruitEntry.CONTENT_URI, mValueArray);
    }

    /**
     * Method that returns String with a Uri of an asset (in this case an image) that is stored in app resources
     * @param resourceId an Id of an asset Uri of which is needed
     * @return String with a Uri of the asset
     *
     * the method has been recommended by Axarydax (https://stackoverflow.com/questions/4896223/how-to-get-an-uri-of-an-image-resource-in-android)
     */
    private String getUriOfAnAsset(int resourceId){
        return Uri.parse("android.resource://com.example.kasparasza.inventoryapp/" + resourceId).toString();
    }

    ////
    // Implementation of Alert Dialog that handles DELETE ALL action
    ////
    private void showDeleteConfirmationDialog() {
        // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
        dialogDeleteAllItemsIsOpen = true;

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_question_delete_all_items);
        builder.setPositiveButton(R.string.dialog_option_proceed_with_deletion, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete all the items.
                deleteAllItemsInDb();
                // reset global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                dialogDeleteAllItemsIsOpen = false;
            }
        });
        builder.setNegativeButton(R.string.dialog_option_do_not_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();

                    // reset global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                    dialogDeleteAllItemsIsOpen = false;
                }
            }
        });

        // Create and show the AlertDialog
        alertDialogDeleteAllItems = builder.create();
        alertDialogDeleteAllItems.show();
    }

    ////
    // Implementation of Alert Dialog that handles BULK INSERT action
    ////
    private void showInsertionConfirmationDialog() {
        // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
        dialogInsertDummyItemsIsOpen = true;

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_question_insert_multiple_dummy_items);
        builder.setPositiveButton(R.string.dialog_option_proceed_with_insertion, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Insert" button, so proceed with the insertion.
                insertMultipleDummyItems();
                // reset global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                dialogInsertDummyItemsIsOpen = false;
            }
        });
        builder.setNegativeButton(R.string.dialog_option_do_not_insert, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();

                    // reset global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                    dialogInsertDummyItemsIsOpen = false;
                }
            }
        });

        // Create and show the AlertDialog
        alertDialogInsertDummyItems = builder.create();
        alertDialogInsertDummyItems.show();
    }
}

