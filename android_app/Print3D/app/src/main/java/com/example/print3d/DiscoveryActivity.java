package com.example.print3d;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;


public class DiscoveryActivity extends AppCompatActivity {

    private static final String TAG = "DiscoveryActivity";

    private static final int ADD_MSG = 111;
    private static final int RM_MSG = 222;

    private JmDNS jmdns = null;
    private ListView listview = null;
    private List<ServiceInfo> printers = new ArrayList<>();
    private PrinterListAdapter adapter;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        listview = (ListView) findViewById(R.id.listView1);

        adapter = new PrinterListAdapter(this, android.R.layout.simple_list_item_1, printers);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final ServiceInfo  item = (ServiceInfo) adapterView.getItemAtPosition(position);

                String ip = parseIpAddress(item);
                int port  = parsePort(item);
                String uri = parseUri(item);

                returnToCaller(ip,port,uri);
            }
        });

        new Thread( new Runnable() {
                @Override
                public void run(){
                    try {
                        // Create a JmDNS instance
                        jmdns = JmDNS.create(InetAddress.getLocalHost());

                        // Add a service listener
                        jmdns.addServiceListener("_ipps-3d._tcp.local.", new SampleListener());

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

    }

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
    private void returnToCaller(String ip, int port, String uri){
        if (ip == null || port == -1 || uri == null) {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED, returnIntent);
            finish();
        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra(PrintingService.PRINTER_ADDR_RES,ip);
        returnIntent.putExtra(PrintingService.PRINTER_PORT_RES,port);
        returnIntent.putExtra(PrintingService.PRINTER_URI_RES,uri);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    public void refreshList(View view){

        adapter.notifyDataSetChanged();
        listview.invalidateViews();
        listview.refreshDrawableState();
    }
}
