package com.example.kasparasza.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

import butterknife.BindView;

/**
 * custom CursorAdapter that is used to populate the ListView of all inventory items
 */

public class InventoryCursorAdapter extends CursorAdapter {

    //constructor
    public InventoryCursorAdapter(Context context, Cursor cursor){
        // constructor of the enclosing class is used
        // the last parameter int denotes flags; here it is set to zero
        super(context, cursor, 0);
    }

    /**
     * Method returns a new view (list view item) to hold the data pointed to by cursor
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param viewGroup  The parent view to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
    }

    /**
     * Method that binds an existing view to the data pointed to by cursor
     * The newView method is used to inflate a new view and return it, no data binding is performed at that point
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // get information stored in the Cursor
        String itemNameString = cursor.getString(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_ITEM_NAME));
        Float itemPriceFloat = cursor.getFloat(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_PRICE));
        final Integer itemQuantityInteger = cursor.getInt(cursor.getColumnIndexOrThrow(FruitEntry.COLUMN_QUANTITY));

        // get relevant views from the layout that will be populated
        TextView itemName = (TextView) view.findViewById(R.id.item_name);
        TextView itemQuantity = (TextView) view.findViewById(R.id.item_quantity);
        TextView itemPrice = (TextView) view.findViewById(R.id.item_price);
        LinearLayout sellItemButton = (LinearLayout) view.findViewById(R.id.sell_item_button);

        // bind the data to the relevant views
        itemName.setText(itemNameString);
        itemQuantity.setText(itemQuantityInteger.toString().concat(context.getString(R.string.all_inventory_quantity_measurement_kg)));
        itemPrice.setText(context.getString(R.string.all_inventory_price_text, itemPriceFloat));

        // get position of the view in the Parent viewGroup - the position is equal to the ID of the _ID column
        final int position = cursor.getInt(cursor.getColumnIndexOrThrow(FruitEntry._ID));

        // set OnClickListener for the sellItem Button
        // (Sale Button reduces the quantity available by one)
        sellItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if the current quantity is already zero - no action to decrease it further is performed;
                // no changes to the db are made in this case;
                // a Toast message is displayed to the user
                if(itemQuantityInteger == 0) {
                    // display a Toast
                    Toast.makeText(context, R.string.toast_no_sale_is_possible, Toast.LENGTH_SHORT).show();
                } else {
                    // prepare inputs required to the method that updates the db:

                    // create a Uri for the List item that was clicked
                    Uri fruitUri = ContentUris.withAppendedId(FruitEntry.CONTENT_URI, position);

                    // Create a new map of values, where column names are the keys
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(FruitEntry.COLUMN_QUANTITY, itemQuantityInteger - 1);

                    // call the method to update the db records
                    int result = context.getContentResolver().update(fruitUri, contentValues, null, null);
                }
            }
        });
    }
}
