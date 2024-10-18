package com.sapienter.jbilling.server.spa;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by pablo_galera on 10/01/17.
 */
public class SpaProductsOrderedWS implements Serializable {
    private Integer modemId;
    private String serialNumber;
    private Integer planId;
    private Date startDate;
    private String installationTime;
    private String trackingNumber;
    private List<Integer> servicesIds;
    private String banffAccountId;
    private String pppoeUsername;
    private String pppoePassword;
    private String macAddress;
    private String model;
    private String processCenter;
    private String hexencodedMessage;
    private String phoneNumber;
    private String serviceType;
    private String courier;
    private String host;
    private String sipPassword;
    private String configurationPortal;
    private String allowg729;
    private String customerType;
    private String voiceMailPassword;
    private String serviceAssetIdentifier;
    private String modemAssetIdentifier;

    public String getAllowg729() {
        return allowg729;
    }

    public void setAllowg729(String allowg729) {
        this.allowg729 = allowg729;
    }

    public String getConfigurationPortal() {
        return configurationPortal;
    }

    public void setConfigurationPortal(String configurationPortal) {
        this.configurationPortal = configurationPortal;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSipPassword() {
        return sipPassword;
    }

    public void setSipPassword(String sipPassword) {
        this.sipPassword = sipPassword;
    }

    public String getVoiceMailPassword() {
        return voiceMailPassword;
    }

    public void setVoiceMailPassword(String voiceMailPassword) {
        this.voiceMailPassword = voiceMailPassword;
    }

    public String getCourier() {
        return courier;
    }

    public void setCourier(String courier) {
        this.courier = courier;
    }

    public Integer getModemId() {
        return modemId;
    }

    public void setModemId(Integer modemId) {
        this.modemId = modemId;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getInstallationTime() {
        return installationTime;
    }

    public void setInstallationTime(String installationTime) {
        this.installationTime = installationTime;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public List<Integer> getServicesIds() {
        return servicesIds;
    }

    public void setServicesIds(List<Integer> servicesIds) {
        this.servicesIds = servicesIds;
    }

    public String getBanffAccountId() {
        return banffAccountId;
    }

    public void setBanffAccountId(String banffAccountId) {
        this.banffAccountId = banffAccountId;
    }

    public String getPppoeUsername() {
        return pppoeUsername;
    }

    public void setPppoeUsername(String pppoeUsername) {
        this.pppoeUsername = pppoeUsername;
    }

    public void setPppoePassword(String pppoePassword) {
        this.pppoePassword = pppoePassword;
    }

    public String getPppoePassword() {
        return pppoePassword;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProcessCenter() {
        return processCenter;
    }

    public void setProcessCenter(String processCenter) {
        this.processCenter = processCenter;
    }

    public String getHexencodedMessage() {
        return hexencodedMessage;
    }

    public void setHexencodedMessage(String hexencodedMessage) {
        this.hexencodedMessage = hexencodedMessage;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceAssetIdentifier() {
        return serviceAssetIdentifier;
    }

    public void setServiceAssetIdentifier(String serviceAssetIdentifier) {
        this.serviceAssetIdentifier = serviceAssetIdentifier;
    }

    public String getModemAssetIdentifier() {
        return modemAssetIdentifier;
    }

    public void setModemAssetIdentifier(String modemAssetIdentifier) {
        this.modemAssetIdentifier = modemAssetIdentifier;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("modemId: ").append(modemId);
        stringBuilder.append("\nserialNumber: ").append(serialNumber);
        stringBuilder.append("\nplanId: ").append(planId);
        stringBuilder.append("\nstartDate: ").append(startDate);
        stringBuilder.append("\ninstallationTime: ").append(installationTime);
        stringBuilder.append("\ntrackingNumber: ").append(trackingNumber);
        stringBuilder.append("\nservicesIds: ").append(servicesIds);
        stringBuilder.append("\nbanffAccountId: ").append(banffAccountId);
        stringBuilder.append("\npppoeUsername: ").append(pppoeUsername);
        stringBuilder.append("\npppoePassword: ").append(pppoePassword);
        stringBuilder.append("\nmacAddress: ").append(macAddress);
        stringBuilder.append("\nmodel: ").append(model);
        stringBuilder.append("\nprocessCenter: ").append(processCenter);
        stringBuilder.append("\nhexencodedMessage: ").append(hexencodedMessage);
        stringBuilder.append("\nphoneNumber: ").append(phoneNumber);
        stringBuilder.append("\nserviceType: ").append(serviceType);
        stringBuilder.append("\ncourier: ").append(courier);
        stringBuilder.append("\nhost: ").append(host);
        stringBuilder.append("\nsipPassword: ").append(sipPassword);
        stringBuilder.append("\nconfiguration_portal: ").append(configurationPortal);
        stringBuilder.append("\nallowg729: ").append(allowg729);
        stringBuilder.append("\ncustomerType: ").append(customerType);
        stringBuilder.append("\nvoiceMailPassword: ").append(voiceMailPassword);
        stringBuilder.append("\nserviceAssetIdentifier: ").append(serviceAssetIdentifier);
        return stringBuilder.toString();
    }

    public boolean isManualProcess() {
        return !StringUtils.isEmpty(hexencodedMessage) && !StringUtils.isEmpty(processCenter);
    }

    public boolean isPPPOE() {
        return !StringUtils.isEmpty(pppoeUsername) && !StringUtils.isEmpty(pppoePassword);
    }
}
