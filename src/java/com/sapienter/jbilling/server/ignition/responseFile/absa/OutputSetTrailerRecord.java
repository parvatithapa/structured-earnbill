package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class OutputSetTrailerRecord {

    private String numberOfDebitRecords;
    private String numberOfCreditRecords;
    private String homingAccountHashTotal;
    private String debitAmountHashTotal;
    private String creditAmountHashTotal;

    public OutputSetTrailerRecord(String numberOfDebitRecords, String numberOfCreditRecords,
                                  String homingAccountHashTotal, String debitAmountHashTotal, String creditAmountHashTotal) {
        this.numberOfDebitRecords = numberOfDebitRecords;
        this.numberOfCreditRecords = numberOfCreditRecords;
        this.homingAccountHashTotal = homingAccountHashTotal;
        this.debitAmountHashTotal = debitAmountHashTotal;
        this.creditAmountHashTotal = creditAmountHashTotal;
    }

    public String getNumberOfDebitRecords() {
        return numberOfDebitRecords;
    }

    public void setNumberOfDebitRecords(String numberOfDebitRecords) {
        this.numberOfDebitRecords = numberOfDebitRecords;
    }

    public String getNumberOfCreditRecords() {
        return numberOfCreditRecords;
    }

    public void setNumberOfCreditRecords(String numberOfCreditRecords) {
        this.numberOfCreditRecords = numberOfCreditRecords;
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

    public void setCreditAmountHashTotal(String creditAmountHashtotal) {
        this.creditAmountHashTotal = creditAmountHashtotal;
    }
}
