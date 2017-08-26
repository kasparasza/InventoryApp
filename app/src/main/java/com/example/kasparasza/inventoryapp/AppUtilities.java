package com.example.kasparasza.inventoryapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class that contains helper methods of the App
 */

public class AppUtilities {

    // String constants used:
    private static final String LOG_TAG = AppUtilities.class.getSimpleName();


    /**
     * Create a private constructor because no one should ever create a {@link AppUtilities} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name AppUtilities (and an object instance of AppUtilities is not needed).
     */
    private AppUtilities() {
    }

    /**
     * Method that generates a Bitmap from a given image Uri
     * @param context Context
     * @param uri of an image from which a Bitmap will be generated
     * @param targetW width of the Bitmap image
     * @param targetH height of the Bitmap image
     *
     * The key parts of the code of this method are based on the code by crlsndrsjmnz
     * following link: https://github.com/crlsndrsjmnz/MyShareImageExample/
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri, int targetW, int targetH) {

        // check whether input Uri is valid
        if (uri == null || uri.toString().isEmpty())
            return null;

        // create InputStream object that will convert Uri resource into bytes
        InputStream input = null;

        // try-catch blocks are used, because if there is no data associated with the URI,
        // the method openInputStream() will throw FileNotFoundException
        try {
            // Open a stream on to the content associated with a content URI
            input = context.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap - Read Bitmap Dimensions and Type
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true; // If set to true, the decoder will return null (no bitmap), but the out... fields will still be set, allowing the caller to query the bitmap without having to allocate the memory for its pixels
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            // height and width of the image obtained from the Uri
            int imageW = bmOptions.outWidth;
            int imageH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(imageW / targetW, imageH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false; // no we set this to false, so that the decoder returns a Bitmap
            bmOptions.inSampleSize = scaleFactor; // If set to a value > 1, requests the decoder to subsample the original image, returning a smaller image to save memory.

            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            bmOptions.inBitmap = bitmap; // an option for more efficient memory management; decode methods that take the Options object will attempt to reuse this bitmap when loading content.
            input.close();
            return bitmap;

        } catch (FileNotFoundException exception_FileNotFound) {
            Log.e(LOG_TAG, "Failed to load image.", exception_FileNotFound);
            return null;
        } catch (Exception exception) {
            Log.e(LOG_TAG, "Failed to load image.", exception);
            return null;
        }  finally {
            // we close the InputStream (in case the try block fails)
            try {
                input.close();
            } catch (IOException exception_IO) {
                Log.e(LOG_TAG, "Failed to load image.", exception_IO);
            }
        }
    }
}

