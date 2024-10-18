package com.sapienter.jbilling.server.ignition.responseFile.absa;

import java.util.List;

/**
 * Created by taimoor on 7/25/17.
 */
public class NAEDOPaymentOutput {

    private List<NAEDOResponseRecord> naedoResponseRecords;

    public NAEDOPaymentOutput(List<NAEDOResponseRecord> naedoResponseRecords) {
        this.naedoResponseRecords = naedoResponseRecords;
    }

    public List<NAEDOResponseRecord> getNaedoResponseRecords() {
        return naedoResponseRecords;
    }

    public void setNaedoResponseRecords(List<NAEDOResponseRecord> naedoResponseRecords) {
        this.naedoResponseRecords = naedoResponseRecords;
    }
}
