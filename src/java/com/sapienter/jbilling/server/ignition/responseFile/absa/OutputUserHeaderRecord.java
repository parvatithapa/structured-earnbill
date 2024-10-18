package com.sapienter.jbilling.server.ignition.responseFile.absa;

/**
 * Created by Taimoor Choudhary on 7/24/17.
 */
public class OutputUserHeaderRecord {

    private String bankServUserCode;
    private String bankServGenerationNumber;
    private String bankServService;

    public OutputUserHeaderRecord(String bankServUserCode, String bankServGenerationNumber, String bankServService) {
        this.bankServUserCode = bankServUserCode;
        this.bankServGenerationNumber = bankServGenerationNumber;
        this.bankServService = bankServService;
    }

    public String getBankServUserCode() {
        return bankServUserCode;
    }

    public void setBankServUserCode(String bankServUserCode) {
        this.bankServUserCode = bankServUserCode;
    }

    public String getBankServGenerationNumber() {
        return bankServGenerationNumber;
    }

    public void setBankServGenerationNumber(String bankServGenerationNumber) {
        this.bankServGenerationNumber = bankServGenerationNumber;
    }

    public String getBankServService() {
        return bankServService;
    }

    public void setBankServService(String bankServService) {
        this.bankServService = bankServService;
    }

    @Override
    public String toString() {
        return "OutputUserHeaderRecord{" +
                "bankServUserCode='" + bankServUserCode + '\'' +
                ", bankServGenerationNumber='" + bankServGenerationNumber + '\'' +
                ", bankServService='" + bankServService + '\'' +
                '}';
    }
}
