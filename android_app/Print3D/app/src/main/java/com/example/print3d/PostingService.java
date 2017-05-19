package com.example.print3d;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.*;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class PostingService extends IntentService {

    //logging
    private static final String TAG = "PostingService";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_POST_MODEL = "com.example.print3d.action.ACTION_POST_MODEL";

    private static final String FILE_PATH = "com.example.print3d.extra.FILE_PATH";

    //ignore certificates
    // always verify the host - dont check for certificate
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public PostingService() {
        super("PostingService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionPostModel(Context context, String modelPath) {
        Log.i(TAG, ACTION_POST_MODEL + " has been requested");
        Intent intent = new Intent(context, PostingService.class);
        intent.setAction(ACTION_POST_MODEL);
        intent.putExtra(FILE_PATH, modelPath);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST_MODEL.equals(action)) {
                Log.i(TAG,ACTION_POST_MODEL + " has been started");
                final String modelPath = intent.getStringExtra(FILE_PATH);
                handleActionPostModel(modelPath);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionPostModel(String modelPath) {
        Log.i(TAG, "Path to file to be send: " + modelPath);



        jLpr jLpr = new jLpr();
        jLpr.setUseOutOfBoundPorts(true);

        try {
            jLpr.printFile(modelPath, //path to file
                            "10.0.13.52", //IP
                            "515", // PORT
                            "3D print android: Test doc"); //Name to be displayed on spooler
        } catch (IOException e) {
            Log.e(TAG,"file not send: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(TAG,"file not send: " + e.getMessage());
            e.printStackTrace();
        }
        Log.d(TAG, "file sent");
    }

    private void handleActionHTTPPostModel(String modelPath) {

        Log.i(TAG, "Path to file to be send: " + modelPath);
        File model = openFilePath(modelPath);
        handleConnection(model);
    }


    private  void handleConnection(File model) {
        HttpURLConnection client = null;
        try {
            Log.d(TAG,"establishing connection");
            URL url = new URL("http://10.0.13.52:5559/ReceiveJob");
            //trustAllHosts();
            //HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
            //https.setHostnameVerifier(DO_NOT_VERIFY);
            //code above was used to bypass the non-valid certificates used in testing and
            // is left here for illustrative purposes only
            client = (HttpURLConnection) url.openConnection();


            client.setRequestMethod("POST");
            client.setDoOutput(true);
            client.setChunkedStreamingMode(0);

            OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
            writeOutputStream(outputPost,model);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }
    private File openFilePath(String filePath) {
        Log.d(TAG,"opening file");
        File file = null;
        if (filePath != null){
            file = new File(filePath);
        }
        return file;
    }

    private void writeOutputStream(OutputStream outputStream, File file) throws IOException {
        if (outputStream != null && file != null) {
            Log.d(TAG,"writing to output stream");
            InputStream inputStream = new FileInputStream(file);

            IOUtils.copy(inputStream,outputStream);
            Log.d(TAG,"posted");
            inputStream.close();
            outputStream.close();
        }
    }
}
