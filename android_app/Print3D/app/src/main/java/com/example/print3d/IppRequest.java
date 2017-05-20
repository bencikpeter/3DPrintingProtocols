package com.example.print3d;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * Whole IPP Request is created in constructor and can be returned with getBytes method
 *
 * Created by barton on 17.7.2015.
 *
 * Modified by bencik on 10.4.2017 to conform IPP 3D extention
 */
public class IppRequest {

    //hard-wired URI
    public static final String PRINTER_URI = "ipp://peters-macbook-pro.local.:8501/ipp/print3d/eDee";


    // tags
    private static final byte INTEGER_TAG = 0x21;
    private static final byte BOOLEAN_TAG = 0x22;
    private static final byte NAME_WITHOUT_LANGUAGE_TAG = 0x42;
    private static final byte KEYWORD_TAG = 0x44;
    private static final byte URI_TAG = 0x45;
    private static final byte CHARSET_TAG = 0x47;
    private static final byte NATURAL_LANGUAGE_TAG = 0x48;
    private static final byte MULTIPLE_OBJECT_HANDLING_TAG = KEYWORD_TAG; //type2 keyword - the same tag

    // ipp attributes
    private static final byte[] IPPVERSION = new byte[] {0x01, 0x01}; //version 1.1
    private static final byte[] OPERATIONID = new byte[] {0x00, 0x02}; //0x0002 Print-Job
    private static final byte[] REQUESTID = new byte[] {0x00, 0x00, 0x00, 0x01};

    // ipp sections identifiers
    private static final byte OPERATIONATTRIBUTES = 0x01;
    private static final byte JOBATTRIBUTES = 0x02;
    private static final byte ENDATTRIBUTES = 0x03;

    //private final PrintJobInfo printJobInfo;
    private final String jobName;


    private ByteArrayOutputStream outPutBytes;

    /**
     * @param printJobData job data
     * @throws IOException
     */
    IppRequest(String jobName, byte[] printJobData) throws IOException {
        this.jobName = jobName;
        outPutBytes = new ByteArrayOutputStream();

        setIppAttributes();

        outPutBytes.write(OPERATIONATTRIBUTES);
        setOperationAttributes();

        outPutBytes.write(JOBATTRIBUTES);
        setJobAttributes();

        outPutBytes.write(ENDATTRIBUTES);

        setData(printJobData);
        outPutBytes.flush();
    }

    byte[] getBytes() {
        return outPutBytes.toByteArray();
    }

    private void setData(byte[] printJobData) throws IOException {
        outPutBytes.write(printJobData);
    }

    private void setIppAttributes() throws IOException {
        outPutBytes.write(IPPVERSION);
        outPutBytes.write(OPERATIONID);
        outPutBytes.write(REQUESTID);
    }

    private void setOperationAttributes() throws IOException {
        writeAttribute(CHARSET_TAG, "attributes-charset", "us-ascii");
        writeAttribute(NATURAL_LANGUAGE_TAG, "attributes-natural-language", "en-us");
        //TODO: set this guy automatically
        writeAttribute(URI_TAG,"printer-uri", PRINTER_URI );
        writeAttribute(NAME_WITHOUT_LANGUAGE_TAG, "job-name", jobName);
        writeAttribute(BOOLEAN_TAG, "ipp-attribute-fidelity", new byte[] {0x01});  // 1 - TRUE
    }

    private void setJobAttributes() throws IOException { //job template attributes
        writeAttribute(INTEGER_TAG, "copies", new byte[] {0x00, 0x00, 0x00, 0x01});  // 1 copy (number of copies)
        writeAttribute(KEYWORD_TAG, "sides", "one-sided");
    }

    private void writeAttribute(byte tag, String name, String value) throws IOException {
        writeAttribute(tag, name, value.getBytes(StandardCharsets.UTF_8));
    }

    private void writeAttribute(byte tag, String name, byte[] value) throws IOException {
        outPutBytes.write(tag);
        outPutBytes.write(name.length() / 256);
        outPutBytes.write(name.length() % 256);
        outPutBytes.write(name.getBytes(StandardCharsets.UTF_8));
        outPutBytes.write(value.length / 256);
        outPutBytes.write(value.length % 256);
        outPutBytes.write(value);
    }
}
