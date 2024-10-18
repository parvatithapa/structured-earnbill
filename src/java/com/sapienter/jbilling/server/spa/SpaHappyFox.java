package com.sapienter.jbilling.server.spa;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpaHappyFox implements Serializable{

    private static final long serialVersionUID = 1L;
    @JsonProperty("common_fields")
    private SpaCommonFields commonFields;
    @JsonProperty("private_notes")
    private SpaPrivateNotes privateNotes;

    public SpaHappyFox(){
    }

    public SpaHappyFox(SpaCommonFields commonFields,
            SpaPrivateNotes privateNotes) {
        super();
        this.commonFields = commonFields;
        this.privateNotes = privateNotes;
    }

    public SpaCommonFields getCommonFields() {
        return commonFields;
    }

    public void setCommonFields(SpaCommonFields commonFields) {
        this.commonFields = commonFields;
    }

    public SpaPrivateNotes getPrivateNotes() {
        return privateNotes;
    }

    public void setPrivateNotes(SpaPrivateNotes privateNotes) {
        this.privateNotes = privateNotes;
    }

    @Override
    public String toString() {
        return "SpaHappyFox [commonFields=" + commonFields + ", privateNotes="
                + privateNotes + "]";
    }

}
