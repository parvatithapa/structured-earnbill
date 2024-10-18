package com.sapienter.jbilling.server.ediTransaction;

import java.io.Serializable;

/**
 * Created by neeraj on 21/10/15.
 */
public class EDIFileExceptionCodeWS implements Serializable {
    private String code;
    private String description;
    private Integer id;
    private EDIFileStatusWS ediFileStatusWS;

    public EDIFileExceptionCodeWS() {
    }

    public EDIFileExceptionCodeWS(String code, String description, EDIFileStatusWS ediFileStatusWS) {
        this.code = code;
        this.description = description;
        this.ediFileStatusWS = ediFileStatusWS;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public EDIFileStatusWS getEdiFileStatusWS() {
        return ediFileStatusWS;
    }

    public void setEdiFileStatusWS(EDIFileStatusWS ediFileStatusWS) {
        this.ediFileStatusWS = ediFileStatusWS;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
