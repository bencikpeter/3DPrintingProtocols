package com.example.print3d;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class PrintingService extends IntentService {

    //Results form printer picker
    public static final String PRINTER_ADDR_RES = "com.example.print3d.result.PRINTER_ADDR_RES";
    public static final String PRINTER_PORT_RES = "com.example.print3d.result.PRINTER_PORT_RES";
    public static final String PRINTER_URI_RES = "com.example.print3d.result.PRINTER_URI_RES";

    //hard-coded printerAddress & printerPort
    private String printerAddress = null;
    private int printerPort = -1;
    private String printerUri = null;

    private static final String IPP_PRINT = "com.example.print3d.action.IPP_PRINT";

    //Intent extras
    private static final String FILE_PATH = "com.example.print3d.extra.FILE_PATH";
    private static final String JOB_NAME = "com.example.print3d.extra.JOB_NAME";
    private static final String CONNECTION_TYPE = "com.example.print3d.extra.CONNECTION_TYPE";
    private static final String PRINTER_ADDRES = "com.example.print3d.extra.PRINTER_ADDRES";
    private static final String PRINTER_PORT = "com.example.print3d.extra.PRINTER_PORT";
    private static final String PRINTER_URI = "com.example.print3d.extra.PRINTER_URI";

    public static final int HTTP = 0;
    public static final int HTTPS = 1;

    private static final String LOG_TAG = "PrintingService";

    private byte[] printJobData;
    private String jobName;
    private String filePath;
    private int connectionType;

    public PrintingService() {
        super("PrintingService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionPrintIpp(Context context, String filePath, String jobName, String connectionType, String printerIp, int printerPort, String printerUri) {
        Intent intent = new Intent(context, PrintingService.class);
        intent.setAction(IPP_PRINT);
        intent.putExtra(FILE_PATH, filePath);
        intent.putExtra(JOB_NAME, jobName);
        intent.putExtra(CONNECTION_TYPE, connectionType);
        intent.putExtra(PRINTER_ADDRES, printerIp);
        intent.putExtra(PRINTER_PORT, printerPort);
        intent.putExtra(PRINTER_URI, printerUri);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (IPP_PRINT.equals(action)) {
                final String param1 = intent.getStringExtra(FILE_PATH);
                final String param2 = intent.getStringExtra(JOB_NAME);
                final String param3 = intent.getStringExtra(CONNECTION_TYPE);
                //TODO: read new extras;
                final String param4 = intent.getStringExtra(PRINTER_ADDRES);
                final int param5 = intent.getIntExtra(PRINTER_PORT,-1);
                final String param6 = intent.getStringExtra(PRINTER_URI);

                handleActionIppPrint(param1, param2, param3, param4, param5, param6);
            }
        }
    }

    /**
     * Handle action IppPrint in the provided background thread with the provided
     * parameters.
     */
    private void handleActionIppPrint(String filePath, String jobName, String connectionTypeString, String printerIp, int printerPort, String printerUri) {
        this.jobName = jobName;
        this.filePath = filePath;
        this.printerAddress = printerIp;
        this.printerPort = printerPort;
        this.printerUri = printerUri;
        if (connectionTypeString.equals("https")) {
            this.connectionType = HTTPS;
        } else {
            this.connectionType = HTTP;
        }

        Log.i(LOG_TAG, "doTheTask");
        try {
            printJobData = loadFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Can't load this file. What sorcery is this?");
        }

        if (printJobData == null) {
            Log.e(LOG_TAG, "printJobData == null");
            return;
        }
        if (connectionType == HTTPS)
            setTrustedCertificates();

        HttpURLConnection connection = prepareConnection();
        if (connection == null) {
            Log.e(LOG_TAG, "Couldn't create connection");
        } else {
            setConnectionData(connection);
            int response = sendJob(connection);
            Log.i(LOG_TAG, "Response code: " + response);
        }
        return;
    }


    /**
     * @return Loads file data
     * @throws IOException
     */
    private byte[] loadFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream buf = null;
        FileInputStream fileInputStream = null;
        try {
            File file = new File(filePath);
            fileInputStream = new FileInputStream(file);

            buf = new BufferedInputStream(fileInputStream);

            byte[] tmpBuf = new byte[8192];

            int n = buf.read(tmpBuf);

            while (n > 0) {
                baos.write(tmpBuf, 0, n);

                n = buf.read(tmpBuf);

            }
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (buf != null) {
                buf.close();
            }
        }

        return baos.toByteArray();
    }

    /**
     *  Setting all trusting trust manager. Used for testing purposes with testing server.
     */
    private void setTrustedCertificates() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            return;
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    /** Prepares connection according to the fact if https or http is selected
     * @return
     */
    private HttpURLConnection prepareConnection() {
        URL url;

        HttpURLConnection connection;

        String connectionType = "http";
        if (this.connectionType == HTTPS)
            connectionType = "https";

        try {
            url = new URL(connectionType, printerAddress, printerPort, "/");
            Log.i(LOG_TAG, url.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "bad url");
            return null;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "can't connect");
            return null;
        }

        connection.setReadTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoInput(true);
        connection.setDoOutput(true);



        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "protocol fail");
            return null;
        }

        connection.setRequestProperty("Content-Type", "application/ipp");
        connection.setRequestProperty("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");

        return connection;
    }

    /**
     * Creates print task and sets data to connection stream.
     *
     * @param connection open connection
     */
    private void setConnectionData(HttpURLConnection connection) {
        OutputStream os;
        String response = "";

        try {
            os = connection.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Can't open output stream");
            return;
        }

        try {
            BufferedOutputStream bos = new BufferedOutputStream(os);

            byte[] ippRequest = new IppRequest(jobName, printJobData).getBytes();

            bos.write(ippRequest);

            bos.flush();
            bos.close();


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "unsupported encoding");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "io exception");
            return;
        }

        Log.i(LOG_TAG, response);
    }

    /** sends job to the opened connection
     * @param connection opened connection
     * @return response code
     */
    private int sendJob(HttpURLConnection connection) {
        int responseCode = -1;
        String response = "";
        try {
            responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                StringBuilder buf = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    while ((line = br.readLine()) != null) {
                        buf.append(line);
                    }
                }
                response = buf.toString();
            } else {
                response = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "io exception");
        }
        Log.i(LOG_TAG, response);
        return responseCode;
    }
}
