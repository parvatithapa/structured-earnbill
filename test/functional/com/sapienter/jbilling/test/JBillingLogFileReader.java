package com.sapienter.jbilling.test;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Martin on 10/13/2016.
 */
public class JBillingLogFileReader {

    private File jbLogfile = null;
    private long offset = 0;

    public JBillingLogFileReader() throws IOException {
        String path = new File(".").getCanonicalPath();
        String jbLogPath = path + File.separator + "logs" + File.separator + "jbilling.log";
        jbLogfile = new File(jbLogPath);
    }

    public void setWatchPoint() {
        offset = jbLogfile.length();
    }

    public String readLogAsString() {
        return tail(jbLogfile, offset);
    }

    //offset in bytes
    private String tail(File file, long offset) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile(file, "r");
            long fileLengthTotal = fileHandler.length();
            long fileLength = fileLengthTotal - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for (long filePointer = fileLength; filePointer != offset; filePointer--) {
                fileHandler.seek(filePointer);
                int readByte = fileHandler.readByte();

                if (readByte == 0xA) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                    }
                } else if (readByte == 0xD) {
                    if (filePointer < fileLength - 1) {
                        line = line + 1;
                    }
                }
                sb.append((char) readByte);
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileHandler != null)
                try {
                    fileHandler.close();
                } catch (IOException e) {
                }
        }
    }
}
