package com.example.kasparasza.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
* Activity that allows to view detailed information of a selected item stored in the db
* */
public class ItemDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // String constants used:
    private static final String LOG_TAG = EditInventoryActivity.class.getSimpleName();
    public static final int LOADER_ID = 0;
    public static final int QUANTITY_MIN_VALUE = 0;
    public static final int QUANTITY_MAX_VALUE = 999;
    public static final String CHANGE_IN_QUANTITY = "CHANGE_IN_QUANTITY";
    public static final String DIALOG_SAVE_CHANGES_AFTER_UP_NAVIGATION = "DIALOG_SAVE_CHANGES_AFTER_UP_NAVIGATION";
    public static final String DIALOG_SAVE_CHANGES_AFTER_BACK_BUTTON = "DIALOG_SAVE_CHANGES_AFTER_BACK_BUTTON";
    public static final String DIALOG_DELETE_ITEM = "DIALOG_DELETE_ALL_ITEMS";
    public static final int KEY_CODE_BACK_KEY = 0;
    public static final int KEY_CODE_NAVIGATE_UP_KEY = 1;

    // Binding / declaration of the UI Views
    @BindView(R.id.item_name)TextView itemName;
    @BindView(R.id.item_image)ImageView itemImage;
    @BindView(R.id.item_description)TextView itemDescription;
    @BindView(R.id.item_price)TextView itemPrice;
    @BindView(R.id.item_quantity)TextView itemQuantity;
    @BindView(R.id.supplier_name)TextView supplierName;
    @BindView(R.id.supplier_e_mail)TextView supplierEMail;
    @BindView(R.id.supplier_phone)TextView supplierPhone;
    @BindView(R.id.button_edit_inventory_item) Button editInventoryItem;

    // global objects and variables:
    private Uri selectedItemUri;
    private int changeInQuantity;
    private Integer itemQuantityInteger;
    private Boolean dialogSaveChangesIsOpen_backButton = false;
    private Boolean dialogSaveChangesIsOpen_upNavigation = false;
    private Boolean dialogDeleteItemIsOpen = false;
    private String supplierEMailString;
    private String supplierPhoneString;
    private String itemNameString;
    private AlertDialog alertDialogDeleteItem;
    private AlertDialog alertDialogDiscardChanges;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);

        // Binding / initialisation - committing the annotations being used to the UI
        ButterKnife.bind(this);

        // get Uri of the item selected to be viewed
        selectedItemUri = getIntent().getData();

        // call LoaderManager which will initialise Loader to obtain data from the db
        // as uri in this Activity is a global variable, we do not pass it to the Loader / Bundle is set to null
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    ////////
    // Methods that implement elements of the UI:
    ////////

    // OnClickListener implementation for Buttons that change item quantity
    @OnClick({R.id.button_add_one, R.id.button_add_ten, R.id.button_decrease_by_one,
            R.id.button_decrease_by_ten})
    public void OnClickChangeQuantity(View view) {
        // execute task only if the Loader has finished to load
        if (itemQuantityInteger != null) {
            switch (view.getId()) {
                case R.id.button_add_one:
                    // update variable that accounts for accumulated change in quantity
                    changeInQuantity++;
                    break;
                case R.id.button_add_ten:
                    // update variable that accounts for accumulated change in quantity
                    changeInQuantity = changeInQuantity + 10;
                    break;
                case R.id.button_decrease_by_one:
                    // update variable that accounts for accumulated change in quantity
                    changeInQuantity--;
                    break;
                case R.id.button_decrease_by_ten:
                    // update variable that accounts for accumulated change in quantity
                    changeInQuantity = changeInQuantity - 10;
                    break;
            }
            // update quantity shown in UI (no changes to db data are performed at this stage)
            showQuantityInUi(changeInQuantity);
        }
    }

    /**
     * Method that: 1) checks whether the quantity value that the user wants to set is within bounds;
     * 2) updates quantity shown in UI (no changes to db data are performed at this stage)
     * @param quantityAdjustment accumulated change in quantity that the user wants to set
     */
    private void showQuantityInUi(Integer quantityAdjustment){
        // as the method can be called from other multiple methods in the activity, we check
        // that the variable itemQuantityInteger is not null;
        // if it is null, it means that the Cursor that contains the variable did not finnish to load,
        // and any further execution of the method would result in an exception
        if (itemQuantityInteger != null){
            // check whether quantity value (current in db + adjustment) is within bounds
            // if that is not the case - limit the adjustment value to be within the bounds
            if (itemQuantityInteger + quantityAdjustment  < QUANTITY_MIN_VALUE){
                quantityAdjustment = QUANTITY_MIN_VALUE - itemQuantityInteger;
                // no Toast is being displayed as it is logical that quantity can not be lower than 0.
            }
            if (itemQuantityInteger + quantityAdjustment > QUANTITY_MAX_VALUE){
                quantityAdjustment = QUANTITY_MAX_VALUE - itemQuantityInteger;
                // display a toast message to inform that an upper bound for the quantity was reached
                Toast.makeText(this, R.string.toast_quantity_value_bounds, Toast.LENGTH_SHORT).show();
            }

            // set the global variable that stores accumulated adjustment data to be equal to the
            // value validated above
            changeInQuantity = quantityAdjustment;

            // quantity to be displayed
            Integer quantityForDisplay = changeInQuantity + itemQuantityInteger;

            // display the quantity value in UI
            itemQuantity.setText(quantityForDisplay.toString().concat(getString(R.string.item_details_quantity_measurement_kg)));
        }
    }

    // OnClickListener implementation for "order by e-mail" button
    @OnClick (R.id.action_send_mail) public void OnClickSendEMail (View view){
        // with ACTION_SENDTO only e-mail clients can resolve the intent
        Intent sendEMail = new Intent(Intent.ACTION_SENDTO);

        // Construct Uri that will be passed to the intent:
        String emailAddress = "mailto:" + Uri.encode(supplierEMailString);
        String emailSubject = "?subject=" + Uri.encode(getString(R.string.item_details_email_subject_text, itemNameString));
        String emailBody = "&body=" + Uri.encode("... e-mail body text to be entered here ..."); // currently we do not pass any email body text to the Uri

        // pass Uri with the data (recipient address, subject, etc.) to the Intent
        sendEMail.setData(Uri.parse(emailAddress + emailSubject));

        // start the intent
        // at first verify that there is an activity that can resolve the intent (otherwise the intent ins not run)
        if (sendEMail.resolveActivity(getPackageManager()) != null) {
            startActivity(sendEMail);
        }
    }

    // OnClickListener implementation for "order by phone" button
    @OnClick (R.id.action_call) public void OnClickCall (View view){
        Intent callSupplier = new Intent(Intent.ACTION_DIAL);
        callSupplier.setData(Uri.parse("tel:" + supplierPhoneString));

        // start the intent
        // at first verify that there is an activity that can resolve the intent (otherwise the intent ins not run)
        if (callSupplier.resolveActivity(getPackageManager()) != null) {
            startActivity(callSupplier);
        }
    }

    // OnClickListener implementation for button that opens EditInventoryActivity
    @OnClick (R.id.button_edit_inventory_item) public void OnClickOpenEditInventoryActivity (View view){
        Intent openEditInventoryActivity = new Intent(this, EditInventoryActivity.class);
        // pass the Uri data that informs which inventory item is to be opened to the intent
        openEditInventoryActivity.setData(selectedItemUri);

        // start the intent
        startActivity(openEditInventoryActivity);
    }

    ////////
    // Methods that perform CRUD actions on the db:
    ////////

    /**
     * Method that updates Item information (in this case its quantity) in the db
     */
    private void updateItemQuantityInDb(){
        // if the new quantity happens to be equal to the quantity currently in the db - no action is performed;
        // no changes to the db are made in this case;
        if (changeInQuantity != 0){
            // Create a new map of values, where column names are the keys
            // itemQuantityInteger = current quantity in db;
            // changeInQuantity = value by which the quantity should be changed
            ContentValues contentValues = new ContentValues();
            contentValues.put(FruitEntry.COLUMN_QUANTITY, itemQuantityInteger + changeInQuantity);

            // call the method to update the db records
            // Uri used is the same Uri that was passed to this activity
            int numberOfUpdatedItems = getContentResolver().update(selectedItemUri, contentValues, null, null);

            // If the update is successful
            if (numberOfUpdatedItems >0 ) {
                // display a toast message
                Toast.makeText(this, R.string.toast_data_update_success,Toast.LENGTH_SHORT).show();

                // reset the global variable that stores accumulated adjustment data
                // (set it to be equal to zero)
                changeInQuantity = 0;
            }
        }
    }

    /**
     * Method that deletes the item from the db
     */
    private void deleteItemInDb(){
        // call the method to delete the db record
        // Uri used is the same Uri that was passed to this activity
        getContentResolver().delete(selectedItemUri, null, null);
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
        getMenuInflater().inflate(R.menu.option_menu_item_details_activity, menu);
        return true;
    }

    // method that sets behaviour to the option menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save the quantity data into the db
                updateItemQuantityInDb();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // call a dialog which asks to confirm the deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item quantity was not changed
                // (new quantity happens to be equal to the quantity currently in the db)
                // continue with navigating up to parent activity
                if (changeInQuantity == 0) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }

                // Otherwise if there are unsaved changes to quantity, setup a dialog to warn the user.
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(KEY_CODE_NAVIGATE_UP_KEY);
                // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                dialogSaveChangesIsOpen_upNavigation = true;

                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    ////////
    // Methods that implement abstract methods of LoaderCallbacks interface:
    ////////

    // method is called when a new Loader is being created
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after querying it.
        String[] projection = {
                FruitEntry._ID,
                FruitEntry.COLUMN_ITEM_NAME,
                FruitEntry.COLUMN_QUANTITY,
                FruitEntry.COLUMN_PRICE,
                FruitEntry.COLUMN_DESCRIPTION,
                FruitEntry.COLUMN_IMAGE,
                FruitEntry.COLUMN_SUPPLIER_NAME,
                FruitEntry.COLUMN_SUPPLIER_EMAIL,
                FruitEntry.COLUMN_SUPPLIER_PHONE
        };

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                this,                   // context
                selectedItemUri,        // Uri - uri of the item selected to be viewed (passed via an Intent to this Activity)
                projection,             // projection - columns to be included in the Cursor, defined above
                null,                   // selection
                null,                   // selectionArgs
                null                    // sortOrder
        );
    }

    // method that populates UI views with the data obtained from Cursor after load finishes
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // move the Cursor to the first position
        // if we would be implementing cursor Adapter or a while statement, this would be handled by their functionality
        cursor.moveToFirst();

        // perform further actions only if the cursor is not empty
        if (cursor.moveToFirst()){
            // read data from the cursor
            itemNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_ITEM_NAME));
            itemQuantityInteger = cursor.getInt(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_QUANTITY));
            Float itemPriceFloat = cursor.getFloat(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_PRICE));
            String itemDescriptionString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_DESCRIPTION));
            String itemImageUriString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_IMAGE));
            String supplierNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_NAME));
            supplierEMailString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_EMAIL));
            supplierPhoneString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_PHONE));

            // using the Uri of an image, generate a Bitmap
            Uri itemImageUri = Uri.parse(itemImageUriString);
            Bitmap itemImageBitmap = AppUtilities.getBitmapFromUri(getApplicationContext(), itemImageUri,
                    (int) getResources().getDimension(R.dimen.image_width_02),
                    (int) getResources().getDimension(R.dimen.image_height_02));

            // populate UI views with the data
            itemName.setText(itemNameString);
            showQuantityInUi(0); // a separate method is called to handle quantity view
            itemPrice.setText(getString(R.string.item_details_price_text, itemPriceFloat));
            itemDescription.setText(itemDescriptionString);
            itemImage.setImageBitmap(itemImageBitmap);
            supplierName.setText(supplierNameString);
            supplierEMail.setText(supplierEMailString);
            // depending on the SDK version of the System, set the supplierPhone String
            // to be displayed in PhoneNumber format
            if (Build.VERSION.SDK_INT < 21) {
                supplierPhone.setText(PhoneNumberUtils.formatNumber(supplierPhoneString));
            } else {
                supplierPhone.setText(PhoneNumberUtils.formatNumber(supplierPhoneString, "LT"));
            }
        }
    }

    // Method that resets the Loader
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // reset the Loader
        loader.reset();

        // no need to explicitly close the Cursor, as this will be done by the
        // LoaderManager
    }

    ////////
    // Methods that saves variables for the changes in activity lifecycle:
    ////////

    /**
     * Method that saves variables
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // saving variables to a bundle
        outState.putInt(CHANGE_IN_QUANTITY, changeInQuantity);
        outState.putBoolean(DIALOG_SAVE_CHANGES_AFTER_BACK_BUTTON, dialogSaveChangesIsOpen_backButton);
        outState.putBoolean(DIALOG_SAVE_CHANGES_AFTER_UP_NAVIGATION, dialogSaveChangesIsOpen_upNavigation);
        outState.putBoolean(DIALOG_DELETE_ITEM, dialogDeleteItemIsOpen);

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
            changeInQuantity = savedInstanceState.getInt(CHANGE_IN_QUANTITY);
            dialogSaveChangesIsOpen_backButton = savedInstanceState.getBoolean(DIALOG_SAVE_CHANGES_AFTER_BACK_BUTTON);
            dialogSaveChangesIsOpen_upNavigation = savedInstanceState.getBoolean(DIALOG_SAVE_CHANGES_AFTER_UP_NAVIGATION);
            dialogDeleteItemIsOpen = savedInstanceState.getBoolean(DIALOG_DELETE_ITEM);
        }

        // update quantity displayed in the UI (other views remain unchanged during orientation change)
        showQuantityInUi(changeInQuantity);

        // restore Dialogs if these were open
        if (dialogSaveChangesIsOpen_backButton){
            showUnsavedChangesDialog(KEY_CODE_BACK_KEY);
        }

        if (dialogSaveChangesIsOpen_upNavigation){
            showUnsavedChangesDialog(KEY_CODE_NAVIGATE_UP_KEY);
        }

        if (dialogDeleteItemIsOpen){
            showDeleteConfirmationDialog();
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
        if (alertDialogDeleteItem != null) {
            alertDialogDeleteItem.dismiss();
        }
        if (alertDialogDiscardChanges != null){
            alertDialogDiscardChanges.dismiss();
        }
    }

    ////
    // Implementation of Alert Dialog that handles EXIT WITHOUT SAVE form ItemDetailsActivity
    ////

    // handling of BACK button press
    @Override
    public void onBackPressed() {
        // If the item quantity was not changed
        // (new quantity happens to be equal to the quantity currently in the db)
        // continue with handling back button press
        if (changeInQuantity == 0) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.

        // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
        dialogSaveChangesIsOpen_backButton = true;

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(KEY_CODE_BACK_KEY);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the activity.
     * @param keyCode constant that denotes which key (BACK or NAVIGATE_UP) was pressed by the user
     */
    private void showUnsavedChangesDialog(final int keyCode) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_question_unsaved_changes);
        builder.setPositiveButton(R.string.dialog_option_discard_changes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                if(keyCode == KEY_CODE_BACK_KEY){
                    // User clicked "Discard" button after BACK button press,
                    // close the current activity.
                    finish();
                } else {
                    // User clicked "Discard" button after UP_NAVIGATION button press,
                    // navigate to parent activity.
                    NavUtils.navigateUpFromSameTask(ItemDetailsActivity.this);
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_option_save_changes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Save" button, so:
                // dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();

                    // reset global variables that track whether the dialog is open (for the case if onSaveInstanceState is called)
                    dialogSaveChangesIsOpen_upNavigation = false;
                    dialogSaveChangesIsOpen_backButton = false;
                }
                // save changes to the db
                updateItemQuantityInDb();

                // if the BACK button was originally pressed - close the current activity
                if(keyCode == KEY_CODE_BACK_KEY){
                    finish();
                } else {
                    // it was NAVIGATE_UP button press - navigate to parent activity
                    NavUtils.navigateUpFromSameTask(ItemDetailsActivity.this);
                }
            }
        });

        // Create and show the AlertDialog
        alertDialogDiscardChanges = builder.create();
        alertDialogDiscardChanges.show();
    }


    ////
    // Implementation of Alert Dialog that handles DELETE action
    ////
    private void showDeleteConfirmationDialog() {
        // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
        dialogDeleteItemIsOpen = true;

        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_question_delete_item);
        builder.setPositiveButton(R.string.dialog_option_proceed_with_deletion, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItemInDb();
                // close this activity
                finish();
            }
        });
        builder.setNegativeButton(R.string.dialog_option_do_not_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();

                    // reset global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                    dialogDeleteItemIsOpen = false;
                }
            }
        });

        // Create and show the AlertDialog
        alertDialogDeleteItem = builder.create();
        alertDialogDeleteItem.show();
    }
}
