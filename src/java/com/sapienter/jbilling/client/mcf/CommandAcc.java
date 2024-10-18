package com.sapienter.jbilling.client.mcf;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class CommandAcc extends MCFCommand{

    private String accountNumber;
    private String accountName;
    private String language;
    private String media;
    private String custType;
    private String reportType;

    public CommandAcc(String name, String date) {
        super("1", name, date);
    }

    @Override
    public String getCommand() {
        StringBuilder command = new StringBuilder();
        command.append(getName()).append(comma);
        command.append(getDate()).append(comma);
        command.append(accountNumber).append(comma);
        command.append("\"" + accountName + "\"").append(comma);
        command.append(language).append(comma);
        command.append(media).append(comma);
        command.append(custType).append(comma);
        command.append(reportType);
        return command.toString();
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public void setCustType(String custType) {
        this.custType = custType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
}
