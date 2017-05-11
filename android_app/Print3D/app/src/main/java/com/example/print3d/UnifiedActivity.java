package com.example.print3d;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;

import static com.example.print3d.PrintingService.startActionPrintIpp;

public class UnifiedActivity extends AppCompatActivity {

    //logging
    private static final String TAG = "Unified activity";


    private Set<String> fileset;
    private String printerAddress = null;
    private int printerPort = -1;
    private String printerUri = null;

    private JmDNS jmdns = null;
    private ListView listview = null;
    private List<ServiceInfo> printers = new ArrayList<>();
    private UnifiedActivity.PrinterListAdapter adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified);

        listview = (ListView) findViewById(R.id.listView2);

        adapter = new UnifiedActivity.PrinterListAdapter(this, android.R.layout.simple_list_item_1, printers);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final ServiceInfo  item = (ServiceInfo) adapterView.getItemAtPosition(position);

                String ip = parseIpAddress(item);
                int port  = parsePort(item);
                String uri = parseUri(item);

                selectPrinter(ip,port,uri);
            }
        });

        new Thread( new Runnable() {
            @Override
            public void run(){
                try {
                    // Create a JmDNS instance
                    jmdns = JmDNS.create(InetAddress.getLocalHost());

                    // Add a service listener
                    jmdns.addServiceListener("_ipps-3d._tcp.local.", new UnifiedActivity.SampleListener());

                    // Wait a bit
                    Thread.sleep(30000);
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

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


    //------------------PRIVATE-------------

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

    private String getFileName(String filePath) {
        File file  = new File(filePath);
        return file.getName();
    }

    private class PrinterListAdapter extends ArrayAdapter<ServiceInfo>
    {
        List<ServiceInfo> obj;

        public PrinterListAdapter(Context context, int resource, List<ServiceInfo> objects) {
            super(context, resource, objects);

            obj = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            LayoutInflater inflater =  (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            TextView textView =  (TextView) inflater.inflate(android.R.layout.simple_list_item_1,parent,false);

            textView.setText(obj.get(position).getName());

            textView.setTextColor(Color.BLACK);

            return textView;
        }
    }

    private  class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            Log.d(TAG,"Service added: " + event.getInfo());

            jmdns.requestServiceInfo(event.getType(),
                    event.getName(), 1000);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            Log.d(TAG, "Service removed: " + event.getInfo());

            removeFromList(event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            Log.d(TAG,"Service resolved: " + event.getInfo());

            addToList(event.getInfo());
        }
    }

    private void addToList(ServiceInfo info) {
        Log.d(TAG, "updating UI with new printer: " + info.getName());
        printers.add(info);
        adapter.notifyDataSetChanged();

        //adapter.refresh();
    }

    private void removeFromList(ServiceInfo info){
        Log.d(TAG, "Deleting unregistered printer from UI: " + info.getName());
        String name = info.getName();
        for(int i = 0; i < printers.size(); i++) {
            if(printers.get(i).getName().equals(name)) {
                printers.remove(i);
                break;
            }
        }

        adapter.notifyDataSetChanged();
    }

    //-----Parsers----
    private String parseIpAddress(ServiceInfo serviceInfo) {
        String[] ipAdresses = serviceInfo.getHostAddresses();
        if ( ipAdresses != null && ipAdresses.length > 0)  return ipAdresses[0];
        return null;
    }
    private int parsePort(ServiceInfo serviceInfo) {
        int port = -1;
        port = serviceInfo.getPort();
        return port;
    }
    private String parseUri(ServiceInfo serviceInfo) {
        String uri = null;

        String prefix = "ipp://";
        String folderStructure = "/ipp/print3d/";
        String server = serviceInfo.getServer();
        String port = ":"+Integer.toString(serviceInfo.getPort());
        String name = serviceInfo.getName();

        uri = prefix+server+port+folderStructure+name;
        return uri;
    }

    //----setter----
    private void selectPrinter(String ip, int port, String uri){
        if (ip == null || port == -1 || uri == null) {
            showToast("Selected printer could not be chosen, please choose another one");
            Button btn = (Button) findViewById(R.id.button4);
            btn.setEnabled(false);
            return;
        }

        printerAddress = ip;
        printerPort = port;
        printerUri = uri;
        Button btn = (Button) findViewById(R.id.button4);
        btn.setEnabled(true);
        return;
    }


    //-----on-click----
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
}
