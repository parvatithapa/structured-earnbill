package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class OutputUserTrailerRecord {

    private String totalDebitRecords;
    private String totalCreditRecords;
    private String homingAccountHashTotal;
    private String debitAmountHashTotal;
    private String creditAmountHashTotal;

    public OutputUserTrailerRecord(String totalDebitRecords, String totalCreditRecords,
                                   String homingAccountHashTotal, String debitAmountHashTotal, String creditAmountHashTotal) {
        this.totalDebitRecords = totalDebitRecords;
        this.totalCreditRecords = totalCreditRecords;
        this.homingAccountHashTotal = homingAccountHashTotal;
        this.debitAmountHashTotal = debitAmountHashTotal;
        this.creditAmountHashTotal = creditAmountHashTotal;
    }

    public String getTotalDebitRecords() {
        return totalDebitRecords;
    }

    public void setTotalDebitRecords(String totalDebitRecords) {
        this.totalDebitRecords = totalDebitRecords;
    }

    public String getTotalCreditRecords() {
        return totalCreditRecords;
    }

    public void setTotalCreditRecords(String totalCreditRecords) {
        this.totalCreditRecords = totalCreditRecords;
    }

    public String getHomingAccountHashTotal() {
        return homingAccountHashTotal;
    }

    public void setHomingAccountHashTotal(String homingAccountHashTotal) {
        this.homingAccountHashTotal = homingAccountHashTotal;
    }

    public String getDebitAmountHashTotal() {
        return debitAmountHashTotal;
    }

    public void setDebitAmountHashTotal(String debitAmountHashTotal) {
        this.debitAmountHashTotal = debitAmountHashTotal;
    }

    public String getCreditAmountHashTotal() {
        return creditAmountHashTotal;
    }

    public void setCreditAmountHashTotal(String creditAmountHashTotal) {
        this.creditAmountHashTotal = creditAmountHashTotal;
    }
}
