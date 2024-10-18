package com.sapienter.jbilling.client.mcf;

import com.sapienter.jbilling.server.spa.SpaConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by pablo_galera on 15/02/17.
 */
public class MCFServiceBL {

    private String url;
    private MCFClient mcfClient;
    private static final String requestTask = "MCF 8 Import";
    private String transaction;
    private static final String ERROR_RESPONSE = "error";
    private static final String SUCCESS_RESPONSE = "success";

    public MCFServiceBL(String url) {
        this.url = url;
        this.mcfClient = new MCFClient();
        this.transaction = UUID.randomUUID().toString();
    }

    public String sendADDACCandADDBILLCommands(Map<String, String> parameters) {
        AddAcc commandADDACC = generateADDACC(parameters);
        AddBill commandADDBILL = generateADDBILL(parameters);
        return sendCommands(parameters.get(SpaConstants.PARAM_BUSINESS_UNIT), commandADDACC, commandADDBILL);
    }

    private String sendCommands(String businessUnit, MCFCommand... mcfCommands) {
        MCFRequest request = new MCFRequest();
        request.setTask(requestTask);
        request.setBusinessUnit(businessUnit);
        request.setTransaction(transaction);
        request.setJobs(generateRequestJobs(mcfCommands));

        MCFResponse response = mcfClient.request(request,url);

        if (response == null) {
            return ERROR_RESPONSE;            
        } else {
            for (MCFCommand mcfCommand : mcfCommands) {
                if (response.getResults().get(mcfCommand.getPosition()).contains("ERROR")) {
                    return ERROR_RESPONSE;
                }
            }
        }
        return SUCCESS_RESPONSE;
    }

    public String sendMODACCCommand(String businessUnit, String effectiveDate, String accountNumber, String accountName, String language) {
        ModAcc commandMODACC = new ModAcc(effectiveDate);
        commandMODACC.setAccountNumber(accountNumber);
        commandMODACC.setAccountName(accountName);
        commandMODACC.setLanguage(language);
        commandMODACC.setMedia("JBIL");
        commandMODACC.setCustType("R");
        commandMODACC.setReportType("N");
        return sendCommands(businessUnit, commandMODACC);
    }

    private AddAcc generateADDACC(Map<String, String> parameters) {
        AddAcc commandADDACC = new AddAcc(parameters.get(SpaConstants.PARAM_EFFECTIVE_DATE));
        commandADDACC.setAccountNumber(parameters.get(SpaConstants.PARAM_CUSTOMER_NUMBER));
        commandADDACC.setAccountName(parameters.get(SpaConstants.PARAM_CUSTOMER_NAME));
        commandADDACC.setLanguage(parameters.get(SpaConstants.PARAM_LANGUAGE));
        commandADDACC.setMedia("JBIL");
        commandADDACC.setCustType("R");
        commandADDACC.setReportType("N");
        return commandADDACC;
    }

    private AddBill generateADDBILL(Map<String, String> parameters){
        AddBill commandADDBILL = new AddBill(parameters.get(SpaConstants.PARAM_EFFECTIVE_DATE));
        commandADDBILL.setAccountNumber(parameters.get(SpaConstants.PARAM_CUSTOMER_NUMBER));
        commandADDBILL.setBillingId(parameters.get(SpaConstants.PARAM_BILLING_IDENTIFIER));
        commandADDBILL.setTimezoneCode(parameters.get(SpaConstants.PARAM_TIME_ZONE));
        List<String> serviceIds = new ArrayList<>();
        serviceIds.add(parameters.get(SpaConstants.PARAM_SERVICE_NUMBER));
        commandADDBILL.setServiceIds(serviceIds);
        return commandADDBILL;
    }

    private Map<String, String> generateRequestJobs(MCFCommand... mcfCommands) {
        Map<String, String> jobs = new HashMap<>();
        for (MCFCommand mcfCommand : mcfCommands) {
            jobs.put(mcfCommand.getPosition(), mcfCommand.getCommand());
        }
        return jobs;
    }
}
