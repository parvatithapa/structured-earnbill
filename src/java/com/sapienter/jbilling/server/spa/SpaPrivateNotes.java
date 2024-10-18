package com.sapienter.jbilling.server.spa;

import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpaPrivateNotes implements Serializable{

    private static final long serialVersionUID = 1L;
    private String[] serviceName;
    private String[] ratePlan;
    private String[] cTIACA;
    private String[] serviceConnectionDate;
    private String[] serviceStatus;
    private String[] servicePhoneNumber;
    private String[] pPPoEuserName;
    private String[] pPPoEdomain;
    private String[] pPPoEPassword;
    private String[] cPEstatus;
    private String[] cPEmakeModel;
    private String[] cPEMACaddress;
    private String[] cPEserialNumber;
    private String[] cPEacqMethod;
    private String[] cPEPurchaseDate;
    private String[] cYX;
    private String[] dialupUserName;
    private String[] dialupPassword;
    private String[] dialupAccessNumber;
    private String[] serviceFeatures;
    private String[] displayName;
    private String[] overseasFeatureStatus;
    private String[] sipResourceID;
    private String[] sipDomain;
    private String[] sipUserName;
    private String[] sipPassword;
    private String[] serviceAtaPort;
    private String[] iSP;
    private String[] activationCode;
    private String[] fullEmailAddress;
    private String[] emailPassword;
    private String[] totalAllottedSpace;
    private String[] totalUsedSpace;
    private String[] emailStatus;
    private String emailPortalUserID;
    private String emailPortalPassword;
    private String[] equalAccess;
    private String[] serviceNumberType;
    private String[] eCareAccountUserName;
    private String[] webspaceUsername;
    private String[] webspacePassword;

    @JsonProperty("ServiceName")
    public String[] getServiceName() {
        return serviceName;
    }

    @JsonProperty("ServiceName")
    public void setServiceName(String[] serviceName) {
        this.serviceName = serviceName;
    }

    @JsonProperty("RatePlan")
    public String[] getRatePlan() {
        return ratePlan;
    }

    @JsonProperty("RatePlan")
    public void setRatePlan(String[] ratePlan) {
        this.ratePlan = ratePlan;
    }

    @JsonProperty("CTIACA")
    public String[] getcTIACA() {
        return cTIACA;
    }

    @JsonProperty("CTIACA")
    public void setcTIACA(String[] cTIACA) {
        this.cTIACA = cTIACA;
    }

    @JsonProperty("ServiceConnectionDate")
    public String[] getServiceConnectionDate() {
        return serviceConnectionDate;
    }

    @JsonProperty("ServiceConnectionDate")
    public void setServiceConnectionDate(String[] serviceConnectionDate) {
        this.serviceConnectionDate = serviceConnectionDate;
    }

    @JsonProperty("ServiceStatus")
    public String[] getServiceStatus() {
        return serviceStatus;
    }

    @JsonProperty("ServiceStatus")
    public void setServiceStatus(String[] serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @JsonProperty("ServicePhoneNumber")
    public String[] getServicePhoneNumber() {
        return servicePhoneNumber;
    }

    @JsonProperty("ServicePhoneNumber")
    public void setServicePhoneNumber(String[] servicePhoneNumber) {
        this.servicePhoneNumber = servicePhoneNumber;
    }

    @JsonProperty("PPPoEuserName")
    public String[] getpPPoEuserName() {
        return pPPoEuserName;
    }

    @JsonProperty("PPPoEuserName")
    public void setpPPoEuserName(String[] pPPoEuserName) {
        this.pPPoEuserName = pPPoEuserName;
    }

    @JsonProperty("PPPoEdomain")
    public String[] getpPPoEdomain() {
        return pPPoEdomain;
    }

    @JsonProperty("PPPoEdomain")
    public void setpPPoEdomain(String[] pPPoEdomain) {
        this.pPPoEdomain = pPPoEdomain;
    }

    @JsonProperty("PPPoEPassword")
    public String[] getpPPoEPassword() {
        return pPPoEPassword;
    }

    @JsonProperty("PPPoEPassword")
    public void setpPPoEPassword(String[] pPPoEPassword) {
        this.pPPoEPassword = pPPoEPassword;
    }

    @JsonProperty("CPEstatus")
    public String[] getcPEstatus() {
        return cPEstatus;
    }

    @JsonProperty("CPEstatus")
    public void setcPEstatus(String[] cPEstatus) {
        this.cPEstatus = cPEstatus;
    }

    @JsonProperty("CPEmakeModel")
    public String[] getcPEmakeModel() {
        return cPEmakeModel;
    }

    @JsonProperty("CPEmakeModel")
    public void setcPEmakeModel(String[] cPEmakeModel) {
        this.cPEmakeModel = cPEmakeModel;
    }

    @JsonProperty("CPEMACaddress")
    public String[] getcPEMACaddress() {
        return cPEMACaddress;
    }

    @JsonProperty("CPEMACaddress")
    public void setcPEMACaddress(String[] cPEMACaddress) {
        this.cPEMACaddress = cPEMACaddress;
    }

    @JsonProperty("CPEserialNumber")
    public String[] getcPEserialNumber() {
        return cPEserialNumber;
    }

    @JsonProperty("CPEserialNumber")
    public void setcPEserialNumber(String[] cPEserialNumber) {
        this.cPEserialNumber = cPEserialNumber;
    }

    @JsonProperty("CPEacqMethod")
    public String[] getcPEacqMethod() {
        return cPEacqMethod;
    }

    @JsonProperty("CPEacqMethod")
    public void setcPEacqMethod(String[] cPEacqMethod) {
        this.cPEacqMethod = cPEacqMethod;
    }

    @JsonProperty("CPEPurchaseDate")
    public String[] getcPEPurchaseDate() {
        return cPEPurchaseDate;
    }

    @JsonProperty("CPEPurchaseDate")
    public void setcPEPurchaseDate(String[] cPEPurchaseDate) {
        this.cPEPurchaseDate = cPEPurchaseDate;
    }

    @JsonProperty("CYX")
    public String[] getcYX() {
        return cYX;
    }

    @JsonProperty("CYX")
    public void setcYX(String[] cYX) {
        this.cYX = cYX;
    }

    @JsonProperty("DialupUserName")
    public String[] getDialupUserName() {
        return dialupUserName;
    }

    @JsonProperty("DialupUserName")
    public void setDialupUserName(String[] dialupUserName) {
        this.dialupUserName = dialupUserName;
    }

    @JsonProperty("DialupPassword")
    public String[] getDialupPassword() {
        return dialupPassword;
    }

    @JsonProperty("DialupPassword")
    public void setDialupPassword(String[] dialupPassword) {
        this.dialupPassword = dialupPassword;
    }

    @JsonProperty("DialupAccessNumber")
    public String[] getDialupAccessNumber() {
        return dialupAccessNumber;
    }

    @JsonProperty("DialupAccessNumber")
    public void setDialupAccessNumber(String[] dialupAccessNumber) {
        this.dialupAccessNumber = dialupAccessNumber;
    }

    @JsonProperty("ServiceFeatures")
    public String[] getServiceFeatures() {
        return serviceFeatures;
    }

    @JsonProperty("ServiceFeatures")
    public void setServiceFeatures(String[] serviceFeatures) {
        this.serviceFeatures = serviceFeatures;
    }

    @JsonProperty("DisplayName")
    public String[] getDisplayName() {
        return displayName;
    }

    @JsonProperty("DisplayName")
    public void setDisplayName(String[] displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("OverseasFeatureStatus")
    public String[] getOverseasFeatureStatus() {
        return overseasFeatureStatus;
    }

    @JsonProperty("OverseasFeatureStatus")
    public void setOverseasFeatureStatus(String[] overseasFeatureStatus) {
        this.overseasFeatureStatus = overseasFeatureStatus;
    }

    @JsonProperty("SipResourceID")
    public String[] getSipResourceID() {
        return sipResourceID;
    }

    @JsonProperty("SipResourceID")
    public void setSipResourceID(String[] sipResourceID) {
        this.sipResourceID = sipResourceID;
    }

    @JsonProperty("SipDomain")
    public String[] getSipDomain() {
        return sipDomain;
    }

    @JsonProperty("SipDomain")
    public void setSipDomain(String[] sipDomain) {
        this.sipDomain = sipDomain;
    }

    @JsonProperty("SipUserName")
    public String[] getSipUserName() {
        return sipUserName;
    }

    @JsonProperty("SipUserName")
    public void setSipUserName(String[] sipUserName) {
        this.sipUserName = sipUserName;
    }

    @JsonProperty("SipPassword")
    public String[] getSipPassword() {
        return sipPassword;
    }

    @JsonProperty("SipPassword")
    public void setSipPassword(String[] sipPassword) {
        this.sipPassword = sipPassword;
    }

    @JsonProperty("ServiceAtaPort")
    public String[] getServiceAtaPort() {
        return serviceAtaPort;
    }

    @JsonProperty("ServiceAtaPort")
    public void setServiceAtaPort(String[] serviceAtaPort) {
        this.serviceAtaPort = serviceAtaPort;
    }

    @JsonProperty("ISP")
    public String[] getiSP() {
        return iSP;
    }

    @JsonProperty("ISP")
    public void setiSP(String[] iSP) {
        this.iSP = iSP;
    }

    @JsonProperty("ActivationCode")
    public String[] getActivationCode() {
        return activationCode;
    }

    @JsonProperty("ActivationCode")
    public void setActivationCode(String[] activationCode) {
        this.activationCode = activationCode;
    }

    @JsonProperty("FullEmailAddress")
    public String[] getFullEmailAddress() {
        return fullEmailAddress;
    }

    @JsonProperty("FullEmailAddress")
    public void setFullEmailAddress(String[] fullEmailAddress) {
        this.fullEmailAddress = fullEmailAddress;
    }

    @JsonProperty("EmailPassword")
    public String[] getEmailPassword() {
        return emailPassword;
    }

    @JsonProperty("EmailPassword")
    public void setEmailPassword(String[] emailPassword) {
        this.emailPassword = emailPassword;
    }

    @JsonProperty("TotalAllottedSpace")
    public String[] getTotalAllottedSpace() {
        return totalAllottedSpace;
    }

    @JsonProperty("TotalAllottedSpace")
    public void setTotalAllottedSpace(String[] totalAllottedSpace) {
        this.totalAllottedSpace = totalAllottedSpace;
    }

    @JsonProperty("TotalUsedSpace")
    public String[] getTotalUsedSpace() {
        return totalUsedSpace;
    }

    @JsonProperty("TotalUsedSpace")
    public void setTotalUsedSpace(String[] totalUsedSpace) {
        this.totalUsedSpace = totalUsedSpace;
    }

    @JsonProperty("EmailStatus")
    public String[] getEmailStatus() {
        return emailStatus;
    }

    @JsonProperty("EmailStatus")
    public void setEmailStatus(String[] emailStatus) {
        this.emailStatus = emailStatus;
    }

    @JsonProperty("EqualAccess")
    public String[] getEqualAccess() {
        return equalAccess;
    }

    @JsonProperty("EqualAccess")
    public void setEqualAccess(String[] equalAccess) {
        this.equalAccess = equalAccess;
    }

    @JsonProperty("ServiceNumberType")
    public String[] getServiceNumberType() {
        return serviceNumberType;
    }

    @JsonProperty("ServiceNumberType")
    public void setServiceNumberType(String[] serviceNumberType) {
        this.serviceNumberType = serviceNumberType;
    }

    @JsonProperty("CareAccountUserName")
    public String[] geteCareAccountUserName() {
        return eCareAccountUserName;
    }

    @JsonProperty("CareAccountUserName")
    public void seteCareAccountUserName(String[] eCareAccountUserName) {
        this.eCareAccountUserName = eCareAccountUserName;
    }

    @JsonProperty("WebspaceUsername")
    public String[] getWebspaceUsername() {
        return webspaceUsername;
    }

    @JsonProperty("WebspaceUsername")
    public void setWebspaceUsername(String[] webspaceUsername) {
        this.webspaceUsername = webspaceUsername;
    }

    @JsonProperty("WebspacePassword")
    public String[] getWebspacePassword() {
        return webspacePassword;
    }

    @JsonProperty("WebspacePassword")
    public void setWebspacePassword(String[] webspacePassword) {
        this.webspacePassword = webspacePassword;
    }

    @JsonProperty("EmailPortalUserID")
    public String getEmailPortalUserID() {
        return emailPortalUserID;
    }

    @JsonProperty("EmailPortalUserID")
    public void setEmailPortalUserID(String emailPortalUserID) {
        this.emailPortalUserID = emailPortalUserID;
    }

    @JsonProperty("EmailPortalPassword")
    public String getEmailPortalPassword() {
        return emailPortalPassword;
    }

    @JsonProperty("EmailPortalPassword")
    public void setEmailPortalPassword(String emailPortalPassword) {
        this.emailPortalPassword = emailPortalPassword;
    }

    @Override
    public String toString() {
        return "SpaPrivateNotes [serviceName=" + Arrays.toString(serviceName)
                + ", ratePlan=" + Arrays.toString(ratePlan) + ", cTIACA="
                + Arrays.toString(cTIACA) + ", serviceConnectionDate="
                + Arrays.toString(serviceConnectionDate) + ", serviceStatus="
                + Arrays.toString(serviceStatus) + ", servicePhoneNumber="
                + Arrays.toString(servicePhoneNumber) + ", pPPoEuserName="
                + Arrays.toString(pPPoEuserName) + ", pPPoEdomain="
                + Arrays.toString(pPPoEdomain) + ", pPPoEPassword="
                + Arrays.toString(pPPoEPassword) + ", cPEstatus="
                + Arrays.toString(cPEstatus) + ", cPEmakeModel="
                + Arrays.toString(cPEmakeModel) + ", cPEMACaddress="
                + Arrays.toString(cPEMACaddress) + ", cPEserialNumber="
                + Arrays.toString(cPEserialNumber) + ", cPEacqMethod="
                + Arrays.toString(cPEacqMethod) + ", cPEPurchaseDate="
                + Arrays.toString(cPEPurchaseDate) + ", cYX="
                + Arrays.toString(cYX) + ", dialupUserName="
                + Arrays.toString(dialupUserName) + ", dialupPassword="
                + Arrays.toString(dialupPassword) + ", dialupAccessNumber="
                + Arrays.toString(dialupAccessNumber) + ", serviceFeatures="
                + Arrays.toString(serviceFeatures) + ", displayName="
                + Arrays.toString(displayName) + ", overseasFeatureStatus="
                + Arrays.toString(overseasFeatureStatus) + ", sipResourceID="
                + Arrays.toString(sipResourceID) + ", sipDomain="
                + Arrays.toString(sipDomain) + ", sipUserName="
                + Arrays.toString(sipUserName) + ", sipPassword="
                + Arrays.toString(sipPassword) + ", serviceAtaPort="
                + Arrays.toString(serviceAtaPort) + ", iSP="
                + Arrays.toString(iSP) + ", activationCode="
                + Arrays.toString(activationCode) + ", fullEmailAddress="
                + Arrays.toString(fullEmailAddress) + ", emailPassword="
                + Arrays.toString(emailPassword) + ", totalAllottedSpace="
                + Arrays.toString(totalAllottedSpace) + ", totalUsedSpace="
                + Arrays.toString(totalUsedSpace) + ", emailStatus="
                + Arrays.toString(emailStatus) + ", emailPortalUserID="
                + emailPortalUserID + ", emailPortalPassword="
                + emailPortalPassword + ", equalAccess="
                + Arrays.toString(equalAccess) + ", serviceNumberType="
                + Arrays.toString(serviceNumberType) + ", eCareAccountUserName="
                + Arrays.toString(eCareAccountUserName) + ", webspaceUsername="
                + Arrays.toString(webspaceUsername) + ", webspacePassword="
                + Arrays.toString(webspacePassword) + "]";
    }

}
