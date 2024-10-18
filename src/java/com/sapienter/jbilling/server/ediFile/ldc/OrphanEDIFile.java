package com.sapienter.jbilling.server.ediFile.ldc;

/**
 * Created by aman on 29/8/16.
 */

import java.io.Serializable;

/**
 * This is view object to display orphan edi files on GUI.
 */
public class OrphanEDIFile implements Serializable {
    String fileName;
    String date;    //File creation date

    public OrphanEDIFile(String fileName, String date) {
        this.fileName = fileName;
        this.date = date;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
