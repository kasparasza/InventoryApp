package com.example.kasparasza.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kasparasza.inventoryapp.database.InventoryContract.FruitEntry;

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

    // a helper class to cache looking for view each time (and setting it as a tag for the row View)
    // implementation of the View Holder pattern in the adapter will allow:
    // to avoid numerous findViewById() calls during the ListView scrolling, as
    // UI objects will only be instantiated once and will be reused afterwards.
    // View Holder pattern will improve the performance of the ListView scrolling
    private static class ViewHolder{
        TextView itemName;
        TextView itemQuantity;
        TextView itemPrice;
        LinearLayout sellItemButton;
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
        // inflate a new view / list item view
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);

        // instantiate ViewHolder object and attach the particular UI views that this object will hold
        ViewHolder myViewHolder = new ViewHolder();
        myViewHolder.itemName = (TextView) view.findViewById(R.id.item_name);
        myViewHolder.itemQuantity = (TextView) view.findViewById(R.id.item_quantity);
        myViewHolder.itemPrice = (TextView) view.findViewById(R.id.item_price);
        myViewHolder.sellItemButton = (LinearLayout) view.findViewById(R.id.sell_item_button);

        // we associate / store our ViewHolder object within the newly created list item view
        // a method "void setTag (int key, Object tag)" is used instead of "void setTag (Object tag)"
        // in order to make it more obvious & descriptive which object is being associated with a particular view
        // NOTE: the key is optional in the case of ViewHolder pattern, but it is recommended in other cases
        view.setTag(R.id.list_item_view, myViewHolder);

        // return the prepared view
        return view;
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

        // instantiate a ViewHolder object
        // in this case our ViewHolder object is a view that already holds the child views of the UI,
        // that will be populated.
        // We use a unique tag to fetch it
        ViewHolder holder = (ViewHolder) view.getTag(R.id.list_item_view);

        // bind the data to the relevant views
        holder.itemName.setText(itemNameString);
        holder.itemQuantity.setText(itemQuantityInteger.toString().concat(context.getString(R.string.all_inventory_quantity_measurement_kg)));
        holder.itemPrice.setText(context.getString(R.string.all_inventory_price_text, itemPriceFloat));

        // get position of the view in the Parent viewGroup - the position is equal to the ID of the _ID column
        final int position = cursor.getInt(cursor.getColumnIndexOrThrow(FruitEntry._ID));

        // set OnClickListener for the sellItem Button
        // (Sale Button reduces the quantity available by one).
        // bind the data to the relevant views
        holder.sellItemButton.setOnClickListener(new View.OnClickListener() {
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
