package com.rodrigograca.canvas2image;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Canvas2ImagePlugin.java
 * <p>
 * Android implementation of the Canvas2ImagePlugin for iOS. Inspired by
 * Joseph's "Save HTML5 Canvas Image to Gallery" plugin
 * http://jbkflex.wordpress.com/2013/06/19/save-html5-canvas-image-to-gallery-phonegap-android-plugin/
 *
 * @author Vegard Løkken <vegard@headspin.no>
 */
public class Canvas2ImagePlugin extends CordovaPlugin {
    public static final String ACTION = "saveImageDataToLibrary";
    public static final int WRITE_PERM_REQUEST_CODE = 1;
    private final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private CallbackContext callbackContext;
    private String format;
    private Bitmap bmp;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.format = args.optString(1);

        if (action.equals(ACTION)) {

            String base64 = args.optString(0);
            if (base64.equals("")) // isEmpty() requires API level 9
                callbackContext.error("Missing base64 string");

            // Create the bitmap from the base64 string
            // Log.d("Canvas2ImagePlugin", base64);
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (bmp == null) {
                callbackContext.error("The image could not be decoded");
            } else {
                this.bmp = bmp;
                this.callbackContext = callbackContext;
                // Save the image
                askPermissionAndSave();
            }

            return true;
        } else {
            return false;
        }
    }

    private void askPermissionAndSave() {

        if (PermissionHelper.hasPermission(this, WRITE_EXTERNAL_STORAGE)) {
            Log.d("SaveImage", "Permissions already granted, or Android version is lower than 6");
            savePhoto();
        } else {
            Log.d("SaveImage", "Requesting permissions for WRITE_EXTERNAL_STORAGE");
            PermissionHelper.requestPermission(this, WRITE_PERM_REQUEST_CODE, WRITE_EXTERNAL_STORAGE);
        }
    }

    private void savePhoto() {

        File image = null;
        // Bitmap bmp = this.bmp;
        CallbackContext callbackContext = this.callbackContext;

        try {
            File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            // long startTime = System.currentTimeMillis();
            File imageFile = new File(folder, "c2i_" + System.currentTimeMillis() + (this.format.equals("png") ? ".png" : ".jpg"));
            FileOutputStream out = new FileOutputStream(imageFile);

            this.bmp.compress(this.format.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            // Log.d("Timestamp: ", ""+System.currentTimeMillis());
            // long difference = System.currentTimeMillis() - startTime;
            // Log.d("Time: " + Long.toString(difference), "whatever");

            image = imageFile;
        } catch (Exception e) {
            Log.e("Canvas2ImagePlugin", "An exception occurred while saving image: " + e.toString());
        }

        if (image == null) {
            callbackContext.error("Error while saving image");
        } else {
            // Update image gallery
            scanPhoto(image);
            callbackContext.success(image.toString());
        }

    }

    /*
     * Invoke the system's media scanner to add your photo to the Media Provider's
     * database, making it available in the Android Gallery application and to other
     * apps.
     */
    private void scanPhoto(File imageFile) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        cordova.getActivity().sendBroadcast(mediaScanIntent);
    }

    /**
     * Callback from PermissionHelper.requestPermission method
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d("SaveImage", "Permission not granted by the user");
                callbackContext.error("Permissions denied");
                return;
            }
        }

        switch (requestCode) {
            case WRITE_PERM_REQUEST_CODE:
                Log.d("SaveImage", "User granted the permission for WRITE_EXTERNAL_STORAGE");
                savePhoto();
                break;
        }
    }
}
