package com.example.print3d;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.example.print3d.PrintingService.startActionPrintIpp;

public class MainActivity extends AppCompatActivity {

    //logging
    private static final String TAG = "MainActivity";

    //starting activity for request
    private static final int PICK_PRINTER_REQUEST = 99;

    private Set<String> fileset;
    private String printerAddress = null;
    private int printerPort = -1;
    private String printerUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()))
        {
            Log.d(TAG,"one file received");
            Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                fileset = new HashSet();
                fileset.add(getPathfromUri(uri));
            }
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
        {
            Log.d(TAG,"multiple files received");
            ArrayList<Uri> uris= intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                fileset = new HashSet();
                for(Uri uri : uris)
                {
                    fileset.add(getPathfromUri(uri));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == PICK_PRINTER_REQUEST){
            if(resultCode == RESULT_OK) {
                printerAddress = data.getStringExtra(PrintingService.PRINTER_ADDR_RES);
                printerPort = data.getIntExtra(PrintingService.PRINTER_PORT_RES,-1);
                printerUri = data.getStringExtra(PrintingService.PRINTER_URI_RES);
            }
        }
    }

    private String getPathfromUri(Uri uri) {
        if(uri.toString().startsWith("file://"))
            return uri.getPath();
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path= cursor.getString(column_index);
        //cursor.close();
        return path;
    }

    private void showToast(String message){
        Context context = getApplicationContext();
        CharSequence text = message;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void onClickPrint(View view){
        Log.d(TAG,"print button clicked");

        if (fileset == null || fileset.isEmpty()) {
            Log.w(TAG,"no files shared");
            showToast("No files shared correctly");
            return;
        }

        if (fileset.size() == 1){
            Log.d(TAG,"one file to be printed");
            String path = fileset.iterator().next();
            String name = getFileName(path);
            //startActionPostModel(this.getBaseContext(),path);
            startActionPrintIpp(this.getBaseContext(),path,name,"http",printerAddress,printerPort,printerUri);
        } else {
            //TODO: multiple files
            Log.d(TAG, fileset.size() + " files to be printed");
        }
    }

    public void onClickSelectPrinter(View view) {
        Log.d(TAG, "Select printer button clicked");
        Log.d(TAG, "Starting selection activity");

        Intent pickPrinterIntent = new Intent(getApplicationContext(),DiscoveryActivity.class);
        startActivityForResult(pickPrinterIntent, PICK_PRINTER_REQUEST);
    }

    private String getFileName(String filePath) {
        File file  = new File(filePath);
        return file.getName();
    }
}
