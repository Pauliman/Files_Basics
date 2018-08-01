package com.werk21.ergu.files_basics;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This Activity reads a user specified image from the DCIM/Camera folder, transforms it to a Bitmap and saves
 * it to the DCIM/Test folder.
 */
public class MainActivity extends AppCompatActivity {

    private View root;
    private Button btn;
    private EditText et;
    private static final int PERMISSION_REQUEST_CODE = 111;
    private static final String PROMT_TEXT = "Please enter a filename";
    private Bitmap bit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    } // end of onCreate()

    private void init(){
        root = (View) this.findViewById(R.id.root);
        btn = (Button) this.findViewById(R.id.button);
        et = (EditText) this.findViewById(R.id.edit_text);
    } // end of init()

    /**
     * This is a simple check whether the external storage is available for reading/writing.
     * @return true if it is available, false if not.
     */
    private boolean isExternalStorageWritable(){
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    } // end of isExternalStorageWritable()

    /**
     * Receives a file name without pre- and suffix adds the 'thumb' infix
     * and returns a file pointer to the new DCIM/Test/ folder.
     * @param file_name_image File name without pre- and suffixes.
     * @return A file pointer with the received file name but a path to DCIM/Test.
     */
    private File createFilePointer(String file_name_image){
        // Always points to DCIM/Test
        String file_name = file_name_image + "thumb" + ".jpg";
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Test");
        File pointer = new File(path, file_name);
        return pointer;
     }

    /**
     * 1.Checks if external storage is available for Writing/reading
     * 2. Tests if the necessary permissions have been granted
     * 3. Attempts to verify the existence of DCIM/Test folder.
     * 4. Retrieves the user input
     * 6. Checks the user input for plausibility
     * 7. Turns the specified picture into a Bitmap, reduces it to a thumbnail and writes it
     * back to the DCIM/Test folder
     * @param v
     */
     public void createThumbnail(View v){
        if(isExternalStorageWritable()){
            if(isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if(isTestFolderPresent()) {
                    String user_input = getUserInput();
                    if (user_input != null) {
                        File original = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
                        bit = BitmapFactory.decodeFile(original + File.separator + user_input + ".jpg");
                        Bitmap thumb = ThumbnailUtils.extractThumbnail(bit, 48, 48);
                        File new_location = createFilePointer(user_input);
                        if (writeImageToDisk(thumb, new_location)) {
                            Toast.makeText(this, "SUCCESS", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "FAILURE", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //do nothing
                    }
                } else{
                    Toast.makeText(this, "COULD NOT CREATE FOLDER", Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(this, "NO PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "EXTERNAL STORAGE NOT WRITABLE", Toast.LENGTH_SHORT).show();
        }
     } // end of createThumbnail

    /**
     * Sets up the Android standard permission request procedure.
     * This method only returns false, when permissions have been revoked and the new request to grant them
     * to grant them has been dismissed.
     * @param permission The type of permission as final string argument.
     * @return true if permission is granted, false if not.
     */
     private boolean isPermissionGranted(final String permission){
        if(ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED){
            return true;
        } else{
            if(shouldShowRequestPermissionRationale(permission)){ // If permissions have been revoked in the past
                new AlertDialog.Builder(this)
                        .setTitle("GRANT PERMISSION")
                        .setMessage("WITHOUT PERMISSIONS NOTHING HAPPENS")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            } else{
                ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
            }
        }
        return false;
     } // end of isPermissionGranted()


    /**
     * Standard permission result method. If the program returns to this method it checks the
     * conditions and restarts the createThumbnail()-method.
     * @param requestCode Identifier for the premission request.
     * @param permissions Array of potential permissions asked for.
     * @param grantResults Array of grant results corresponding to the permissions array.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE){
            if(permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                createThumbnail(btn);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    } // end of onRequestPremissionResult()

    /**
     * Retrieves the user input from the EditTextView and checks
     * whether it is empty or the standard promt text.
     * @return Either a string or null.
     */
    private String getUserInput(){
        String result = null;
        result = et.getText().toString();
        if(!result.equals("") && !result.equals(PROMT_TEXT))
            return result;
        else
            et.setText(PROMT_TEXT);
        return null;
    } // end of getUserInput


    /**
     * Takes a Bitmap and a file pointer and writes the image to disk.
     * @param image A bitmap image and the data source
     * @param path A file object pointing at the target location for the image.
     * @return true if successful false if an exception occurred.
     */
    private boolean writeImageToDisk(Bitmap image, File path){
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(path);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return true;
        } catch (Exception e){
            Log.d("DEBUG","writeImageToDisk() says: " + e);
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.d("DEBUG","writeImageToDisk() says: " + e);
                }
            }
        }
        return false;
    } // end of writeImageToDisk()

    /**
     * Verifies whether the folder DCIM/Test exists and if not, it creates it.
     * Returnes false only if creating this folder failed.
     * @return true if exists or file has been successfully created false if otherwise.
     */
    private boolean isTestFolderPresent(){
        File test_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "Test");
        if(test_folder.exists()) {
            return true;
        }else {
            return test_folder.mkdirs();
        }
    } // end of isTestFolderPresent()
} // end of Activity
