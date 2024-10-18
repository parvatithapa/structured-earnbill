package com.sapienter.jbilling.client.mcf;

import java.util.List;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class AddBill extends MCFCommand{

    private String billingId;
    private String accountNumber;
    private String timezoneCode;
    private List<String> serviceIds;

    public AddBill(String date) {
        super("2", "ADDBILL", date);
    }

    @Override
    public String getCommand() {
        StringBuilder command = new StringBuilder();
        command.append(getName()).append(comma);
        command.append(getDate()).append(comma);
        command.append(billingId).append(comma);
        command.append(accountNumber).append(comma);
        command.append(comma); // it would be the <aliasbillid>
        command.append(timezoneCode).append(comma);
        command.append(serviceIds.get(0));

        return command.toString();
    }

    public void setBillingId(String billingId) {
        this.billingId = billingId;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setTimezoneCode(String timezoneCode) {
        this.timezoneCode = timezoneCode;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }
}