package com.sapienter.jbilling.server.billing.task;


/**
 * @author Harhsad Pathan
 * @since 06-09-2019
 * Object for creating manifest in invoice batch export.
 */
public class IndexFileObject {

    private String fileName;
    private Integer userId;
    private String crmAccountNumber;
    private Integer invoiceId;
    private String invoiceNumber;
    private StringBuilder address;
    private Integer startImpression;
    private Integer endImpression;

    public IndexFileObject() {
    }

    public IndexFileObject(String fileName, Integer userId, String crmAccountNumber, Integer invoiceId,
            String invoiceNumber, StringBuilder address) {

        this.fileName = fileName;
        this.userId = userId;
        this.crmAccountNumber = crmAccountNumber;
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.address = address;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCrmAccountNumber() {
        return crmAccountNumber;
    }

    public void setCrmAccountNumber(String crmAccountNumber) {
        this.crmAccountNumber = crmAccountNumber;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public StringBuilder getAddress() {
        return address;
    }

    public void setAddress(StringBuilder address) {
        this.address = address;
    }

    public void setStartImpression(Integer startImpression) {
        this.startImpression = startImpression;
    }

    public Integer getStartImpression() {
        return startImpression;
    }

    public void setEndImpression(Integer endImpression) {
        this.endImpression = endImpression;
    }

    public Integer getEndImpression() {
        return endImpression;
    }

    @Override
    public String toString() {
        return this.fileName + "," + this.userId + "," + this.crmAccountNumber +","+ this.invoiceId +","+this.invoiceNumber
                +","+ this.address+","+this.startImpression+","+this.endImpression;
    }
}
