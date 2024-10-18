package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CombinedCdrProcessResult implements Serializable {

    public enum Status { VALID, INVALID }

    public static String ERROR_CHECKSUM = "Checksum invalid";
    public static String INVALID_ZIP_PACKAGE = "Invalid Zip Package";
    public static String ZIP_PACKAGE_NOT_2_FILES = "Zip package does not have 2 files";
    public static String ERROR_EXTRACTING_CDR_FILES = "Cdr files could not be extracted";
    public static String ERROR_NO_HEADER = "No header line";
    public static String ERROR_NOT_TEXT_TYPE = "Header type not text";
    public static String ERROR_SPLIT_FAILED = "Unable to split file";
    public static String DECRYPTION_FAILED = "Decryption failed";

    private File file;
    private List<String> errors;
    private Status status = Status.VALID;


    public CombinedCdrProcessResult(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if(errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isValid() {
        return status == Status.VALID;
    }

    public void invalid() {
        status = Status.INVALID;
    }
}
