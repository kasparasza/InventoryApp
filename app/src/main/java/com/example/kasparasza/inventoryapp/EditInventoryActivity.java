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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
* Activity that allows to add a new item into db, or to edit db information of an existing item
* */
public class EditInventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ID = 0;
    public static final int QUANTITY_MIN_VALUE = 0;
    public static final int QUANTITY_MAX_VALUE = 999;
    public static final String DIALOG_SAVE_CHANGES = "DIALOG_SAVE_CHANGES";
    public static final String DIALOG_DELETE_ITEM = "DIALOG_DELETE_ALL_ITEMS";
    public static final String BOOLEAN_ITEM_INFO_WAS_CHANGED = "BOOLEAN_ITEM_INFO_WAS_CHANGED";
    // String constants used:
    private static final String LOG_TAG = EditInventoryActivity.class.getSimpleName();
    private static final int PICK_IMAGE_CALL_IDENTIFIER = 0;
    private static final String IMAGE_URI = "IMAGE_URI";
    // Binding / declaration of the UI Views
    @BindView(R.id.item_name)EditText itemName;
    @BindView(R.id.item_quantity) EditText itemQuantity;
    @BindView(R.id.item_price)EditText itemPrice;
    @BindView(R.id.item_image)ImageView itemImage;
    @BindView(R.id.item_image_empty_view_text)TextView emptyImageText;
    @BindView(R.id.item_description)EditText itemDescription;
    @BindView(R.id.supplier_name)EditText supplierName;
    @BindView(R.id.supplier_e_mail)EditText supplierEMail;
    @BindView(R.id.supplier_phone)EditText supplierPhone;
    // Global variables used:
    private Uri imageUri;
    private Uri itemBeingEditedUri;
    private String imageUriString;
    private Bitmap itemImageBitmap;
    private Boolean itemInfoWasChanged = false; // boolean value that is True when any changes to the UI views are made
    private Boolean dialogSaveChangesIsOpen = false;
    private Boolean dialogDeleteItemIsOpen = false;
    private AlertDialog alertDialogDeleteItem;
    private AlertDialog alertDialogDiscardChanges;

    // we create a Listener that will track if there are any changes to the UI views
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            itemInfoWasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_inventory);

        // Binding / initialisation - committing the annotations being used to the UI
        ButterKnife.bind(this);

        // get information from an intent
        itemBeingEditedUri = getIntent().getData();

        // we attach the Listener to the UI Views
        itemName.setOnTouchListener(mTouchListener);
        itemQuantity.setOnTouchListener(mTouchListener);
        itemPrice.setOnTouchListener(mTouchListener);
        itemImage.setOnTouchListener(mTouchListener);
        itemDescription.setOnTouchListener(mTouchListener);
        supplierName.setOnTouchListener(mTouchListener);
        supplierEMail.setOnTouchListener(mTouchListener);
        supplierPhone.setOnTouchListener(mTouchListener);

        // identify the mode {EDIT_ITEM or ADD_AN_ITEM} in which activity was started
        // if Uri of the variable is not null - activity mode is EDIT_ITEM
        if (itemBeingEditedUri != null) {
            // set the title for the activity
            setTitle(R.string.edit_inventory_label_edit_item);

            // initiate LoaderManager that will manage information query from the db
            // in order to display the information about the selected Inventory item in UI
            getLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            // if Uri of the variable is null - activity mode is ADD_AN_ITEM
            // set the title for the activity
            setTitle(R.string.edit_inventory_label_add_item);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // If activity mode is ADD_AN_ITEM - this menu option item is irrelevant.
            invalidateOptionsMenu();
        }
    }

    ////////
    // Methods that implement elements of the UI:
    ////////


    // Set up of OnClickListener to start an activity for an image selection
    @OnClick(R.id.item_image)
    public void OnClickSelectImage(View view) {
        // set up an intent that will start activity for result
        // the intent will be implicit; no component is set;
        // system will choose the component based on other attributes of the intent
        Intent selectImageIntent;

        // depending on the SDK version of the System, set an action for the intent
        // in both cases the action is to open FilePicker
        if (Build.VERSION.SDK_INT < 19) {
            // action to be performed - pick data
            selectImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            selectImageIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            selectImageIntent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // MIME type of the data - image file of any format
        selectImageIntent.setType("image/*");
        // verify that there is an activity that can resolve the intent (otherwise the intent ins not run)
        if (selectImageIntent.resolveActivity(getPackageManager()) != null) {
            // call the intent:
            // createChooser call sets the intent to display a selection screen where the user can choose which app will be started
            // without the createChooser, the user would select a default app that would be used each time
            // the last parameter / constant that identifies the call; it is meant to disambiguate between multiple calls to startActivityForResult
            startActivityForResult(Intent.createChooser(selectImageIntent, getString(R.string.edit_inventory_image_intent_chooser_text)), PICK_IMAGE_CALL_IDENTIFIER);
            // the result of the activity is received/ handled by onActivityResult() method
        }
    }

    // Set up of OnClickListener to save edits made to the inventory item
    @OnClick(R.id.button_save_input)
    public void OnClickButtonSaveInput(View view) {
        // if 1) the activity was started in EDIT ITEM mode and 2) there were no changes made to item
        // information - just close the activity
        if (itemBeingEditedUri != null && !itemInfoWasChanged) {
            finish();
        }
        // otherwise - call method that saves the input data
        if (saveInventoryItem()) {
            // if data input has finished successfully,
            // close the activity and return to the previous one
            finish();
        }
    }

    /**
     * Method that handles / receives results from other activities
     * in this case the method receives a Uri of an image file selected by the user
     *
     * @param requestCode - constant that identifies the particular Intent that has requested to receive the result
     * @param resultCode  - code returned by an Activity, it identifies whether the action was completed successfully
     * @param data        - returned Intent object that contains resulting data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // switch statement is not used, because we expect a result from only one activity
        // instead we just check for the identity of the caller Intent
        if (requestCode == PICK_IMAGE_CALL_IDENTIFIER) {
            // if code is "RESULT_OK" - the requested action was completed successfully
            if (resultCode == RESULT_OK) {
                // An image was picked, if the Intent received back is not empty,
                if (data != null) {
                    // we obtain Uri of the selected image
                    imageUri = data.getData();
                    // Uri is converted to string (it will be stored as String in the db)
                    imageUriString = imageUri.toString();

                    // set an image into the ImageView
                    setImageView();
                }
            }
        }
    }

    /**
     * Method that sets an image into the ImageView
     */
    private void setImageView() {
        // using the Uri of an image, generate a Bitmap
        itemImageBitmap = AppUtilities.getBitmapFromUri(this, imageUri,
                (int) this.getResources().getDimension(R.dimen.image_width_01),
                (int) this.getResources().getDimension(R.dimen.image_height_01));

        // set the Bitmap into the ImageView
        itemImage.setImageBitmap(itemImageBitmap);
    }


    ////////
    // Methods that implement interaction with db:
    ////////

    /**
     * Method that saves inventory item data to db
     *
     * @return boolean that is true if the data was successfully added to db,
     * the boolean is false if: i) there was an error when adding an item into db;
     * ii) the input data contains errors / is insufficient / inputs are out of bounds;
     * iii) there were no changes made to the item data (Activity opened in EDIT MODE case)
     */
    private Boolean saveInventoryItem() {
        // Read input data
        String itemNameInput = itemName.getText().toString().trim();
        int itemQuantityInput = Integer.parseInt(handleEmptyString(itemQuantity.getText().toString().trim()));
        // check if quantity variable is within bounds
        if(!checkIfQuantityIsWithinBounds(itemQuantityInput)){
            // if the input is out of bounds - the method is terminated
            // and Toast message is shown
            Toast.makeText(this, getString(R.string.toast_quantity_value_bounds_with_value_provided, QUANTITY_MAX_VALUE), Toast.LENGTH_SHORT).show();
            return false;
        }
        // Read input data - continued
        float itemPriceInput = Float.valueOf(handleEmptyString(itemPrice.getText().toString().trim()));
        String itemDescriptionInput = itemDescription.getText().toString().trim();
        String supplierNameInput = supplierName.getText().toString().trim();
        String supplierEMailInput = supplierEMail.getText().toString().trim();
        String supplierPhoneInput = supplierPhone.getText().toString().trim();

        // prepare ContentValues object with the input data
        ContentValues contentValues = new ContentValues();
        contentValues.put(FruitEntry.COLUMN_ITEM_NAME, itemNameInput);
        contentValues.put(FruitEntry.COLUMN_QUANTITY, itemQuantityInput);
        contentValues.put(FruitEntry.COLUMN_PRICE, itemPriceInput);
        contentValues.put(FruitEntry.COLUMN_IMAGE, imageUriString);
        contentValues.put(FruitEntry.COLUMN_DESCRIPTION, itemDescriptionInput);
        contentValues.put(FruitEntry.COLUMN_SUPPLIER_NAME, supplierNameInput);
        contentValues.put(FruitEntry.COLUMN_SUPPLIER_EMAIL, supplierEMailInput);
        contentValues.put(FruitEntry.COLUMN_SUPPLIER_PHONE, supplierPhoneInput);

        // if the activity was started in ADD ITEM mode - method performs these actions
        if (itemBeingEditedUri == null) {
            // call ContentResolver to write the item into db
            Uri newItemUri = getContentResolver().insert(FruitEntry.CONTENT_URI, contentValues);

            // check whether the write to db was successful
            // if yes, the resulting Uri is not null - return true
            return (newItemUri != null);
        } else {
            // the activity was started in EDIT ITEM mode - method performs these actions
            // variable that stores the number of updated rows in the db
            int numberOfUpdatedRows = 0;

            // if there were any changes to the item data - save these changes to db
            // call ContentResolver to write the updated item info into db
            if (itemInfoWasChanged) {
                numberOfUpdatedRows = getContentResolver().update(itemBeingEditedUri, contentValues, null, null);
            }
            // check whether the write to db was successful
            // if yes, the resulting int is > 0 - return true

            // if yes - display a Toast message true
            Toast.makeText(this, getString(R.string.toast_data_update_success), Toast.LENGTH_SHORT).show();

            return (numberOfUpdatedRows > 0);
        }
    }

    /**
     * Method that converts empty string into "0"
     */
    private String handleEmptyString(String inputString) {
        if (inputString.isEmpty()) {
            inputString = "0";
        }
        return inputString;
    }

    /**
     * Method that checks whether the quantity value that the user wants to set is within bounds;
     * @param quantity quantity value that the user has entered
     */
    private boolean checkIfQuantityIsWithinBounds(int quantity){
        if (QUANTITY_MIN_VALUE <= quantity && quantity <= QUANTITY_MAX_VALUE){
            return true;
        } else {
            return false;
        }
    }


    /**
     * Method that deletes the item from the db
     */
    private void deleteItemInDb(){
        // call the method to delete the db record
        // Uri used is the same Uri that was passed to this activity
        getContentResolver().delete(itemBeingEditedUri, null, null);
    }

    ////////
    // Methods that save variables for the changes in activity lifecycle:
    ////////

    /**
     * Method that saves variables
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // saving variables to a bundle
        outState.putString(IMAGE_URI, imageUriString);
        outState.putBoolean(BOOLEAN_ITEM_INFO_WAS_CHANGED, itemInfoWasChanged);
        outState.putBoolean(DIALOG_SAVE_CHANGES, dialogSaveChangesIsOpen);
        outState.putBoolean(DIALOG_DELETE_ITEM, dialogDeleteItemIsOpen);
    }

    /**
     * Method that recreates variables
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get information form the Bundle
        if (savedInstanceState != null) {
            // retrieving data from a bundle
            imageUriString = savedInstanceState.getString(IMAGE_URI);
            itemInfoWasChanged = savedInstanceState.getBoolean(BOOLEAN_ITEM_INFO_WAS_CHANGED);
            dialogSaveChangesIsOpen = savedInstanceState.getBoolean(DIALOG_SAVE_CHANGES);
            dialogDeleteItemIsOpen = savedInstanceState.getBoolean(DIALOG_DELETE_ITEM);

            // set an image into the ImageView
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString);
                setImageView();
            }
        }

        // restore Dialogs if these were open
        if (dialogSaveChangesIsOpen) {
            showUnsavedChangesDialog();
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
        if (alertDialogDiscardChanges != null) {
            alertDialogDiscardChanges.dismiss();
        }
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
        getMenuInflater().inflate(R.menu.option_menu_edit_items_activity, menu);
        return true;
    }

    // this method is called in case invalidateOptionsMenu() method
    // was called previously
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item (activity is in ADD_ITEM mode), hide the "Delete" menu item.
        if (itemBeingEditedUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }


    // method that sets behaviour to the option menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option
        switch (item.getItemId()) {
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // call a dialog which asks to confirm the deletion
                showDeleteConfirmationDialog();
                return true;
                // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If details of the were not changed
                // continue with navigating up to parent activity
                if (!itemInfoWasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog();
                // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
                dialogSaveChangesIsOpen = true;

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ////////
    // Methods that implement abstract methods of LoaderCallbacks interface:
    ////////

    // method is called when a new Loader is being created
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
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
                itemBeingEditedUri,     // Uri - uri of the item selected to be edited (passed via an Intent to this Activity)
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
        if (cursor.moveToFirst()) {
            // read data from the cursor
            String itemNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_ITEM_NAME));
            Integer itemQuantityInteger = cursor.getInt(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_QUANTITY));
            Float itemPriceFloat = cursor.getFloat(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_PRICE));
            String itemDescriptionString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_DESCRIPTION));
            imageUriString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_IMAGE));
            String supplierNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_NAME));
            String supplierEMailString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_EMAIL));
            String supplierPhoneString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_SUPPLIER_PHONE));

            // using the Uri of an image, generate a Bitmap
            imageUri = Uri.parse(imageUriString);
            // display the Bitmap in UI
            setImageView();

            // populate other UI views with the data
            itemName.setText(itemNameString);
            itemQuantity.setText(itemQuantityInteger.toString());
            itemPrice.setText(itemPriceFloat.toString());
            itemDescription.setText(itemDescriptionString);
            supplierName.setText(supplierNameString);
            supplierEMail.setText(supplierEMailString);
            supplierPhone.setText(supplierPhoneString);
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


    ////
    // Implementation of Alert Dialog that handles EXIT WITHOUT SAVE form ItemDetailsActivity
    ////

    // handling of BACK button press
    @Override
    public void onBackPressed() {
        // If there were no changes to the details of an item
        // continue with handling back button press
        if (!itemInfoWasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.

        // set global variable that tracks whether the dialog is open (for the case if onSaveInstanceState is called)
        dialogSaveChangesIsOpen = true;

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog();
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the activity.
     */
    private void showUnsavedChangesDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_question_unsaved_changes);
        builder.setPositiveButton(R.string.dialog_option_discard_changes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "Discard" button after BACK button press,
                // close the current activity.
                finish();
            }
        });
        builder.setNegativeButton(R.string.dialog_option_save_changes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Save" button, so:
                // dismiss the dialog
                if (dialog != null) {
                    dialog.dismiss();

                    // reset global variables that track whether the dialog is open (for the case if onSaveInstanceState is called)
                    dialogSaveChangesIsOpen = false;
                }
                // save changes to the db
                if (saveInventoryItem()) {
                    // if data input has finished successfully,
                    // close the activity and return to the previous one
                    finish();
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
                // & start AllInventoryViewActivity (because after the deletion, there should be no
                // information to show in the parent activity - ItemDetailsActivity
                Intent startAllInventoryViewActivity = new Intent(EditInventoryActivity.this, AllInventoryViewActivity.class);
                // for the same reason we clear the back stack of Activities, so that there is no navigation back
                // to the Activity that shows details of the item that was already deleted
                startAllInventoryViewActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startAllInventoryViewActivity);
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
