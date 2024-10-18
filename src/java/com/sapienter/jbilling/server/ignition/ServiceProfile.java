package com.sapienter.jbilling.server.ignition;

/**
 * Created by Wajeeha Ahmed on 7/19/17.
 */
public class ServiceProfile {

    public class Names{

        public static final String SERVICE_PROVIDER = "serviceprovider";
        public static final String BRAND_NAME = "brandname";
        public static final String BANK_DETAILS = "bankdetails";
        public static final String BANK = "bank";
        public static final String BANK_ACCOUNT_NUMBER = "bankaccountnumber";
        public static final String BANK_ACCOUNT_NAME = "bankaccountname";
        public static final String BANK_ACCOUNT_TYPE = "bankaccounttype";
        public static final String BANK_ACCOUNT_BRANCH = "bankaccountbranch";
        public static final String SERVICE_PROFILE = "serviceprofile";
        public static final String SHORT_NAME = "shortname";
        public static final String USER_NAME = "username";
        public static final String CODE = "code";
        public static final String ACB_USER_CODE = "acbusercode";
        public static final String SERVICES = "services";
        public static final String TYPES_OF_DEBIT_SERVICES = "typesofdebitservices";
        public static final String TO_FI_FOLDER_LOCATION = "toflfolderlocation";
        public static final String FROM_FI_FOLDER_LOCATION = "fromflfolderlocation";
        public static final String FILE_SEQUENCE_NO = "filesequencenumber";
        public static final String GENERATION_NO = "generationnumber";
        public static final String TRANSACTION_NO = "transactionnumber";
        public static final String CUTOFF_TIME = "cutofftime";
        public static final String ENTITY_NAME = "entityname";
        public static final String ISLIVE = "islive";
    }

    private String serviceProvider = "";
    private String brandName = "";
    private String bankDetails = "";
    private String bank = "";
    private String bankAccountNumber = "";
    private String bankAccountName = "";
    private String bankAccountType = "";
    private String bankAccountBranch = "";
    private String name = "";
    private String shortName = "";
    private String username = "";
    private String code = "";
    private String ACBUserCode = "";
    private String services = "";
    private String typesOfDebitServices = "";
    private String toFIFolderLocation = "";
    private String fromFIFolderLocation = "";
    private Integer fileSequenceNo = null;
    private String generationNo = "";
    private String transactionNo = "";
    private String cutOffTime = "";
    private String entityName = "";
    private ServiceProfile naedoServiceProfile = null;
    private boolean isLive = false;

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBankDetails() {
        return bankDetails;
    }

    public void setBankDetails(String bankDetails) {
        this.bankDetails = bankDetails;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankAccountType() {
        return bankAccountType;
    }

    public void setBankAccountType(String bankAccountType) {
        this.bankAccountType = bankAccountType;
    }

    public String getBankAccountBranch() {
        return bankAccountBranch;
    }

    public void setBankAccountBranch(String bankAccountBranch) {
        this.bankAccountBranch = bankAccountBranch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getACBUserCode() {
        return ACBUserCode;
    }

    public void setACBUserCode(String ACBUserCode) {
        this.ACBUserCode = ACBUserCode;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getTypesOfDebitServices() {
        return typesOfDebitServices;
    }

    public void setTypesOfDebitServices(String typesOfDebitServices) {
        this.typesOfDebitServices = typesOfDebitServices;
    }

    public String getToFIFolderLocation() {
        return toFIFolderLocation;
    }

    public void setToFIFolderLocation(String toFIFolderLocation) {
        this.toFIFolderLocation = toFIFolderLocation;
    }

    public String getFromFIFolderLocation() {
        return fromFIFolderLocation;
    }

    public void setFromFIFolderLocation(String fromFIFolderLocation) {
        this.fromFIFolderLocation = fromFIFolderLocation;
    }

    public Integer getFileSequenceNo() {
        return fileSequenceNo;
    }

    public void setFileSequenceNo(Integer fileSequenceNo) {
        this.fileSequenceNo = fileSequenceNo;
    }

    public String getGenerationNo() {
        return generationNo;
    }

    public void setGenerationNo(String generationNo) {
        this.generationNo = generationNo;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getCutOffTime() {
        return cutOffTime;
    }

    public void setCutOffTime(String cutOffTime) {
        this.cutOffTime = cutOffTime;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public ServiceProfile getNaedoServiceProfile() {
        return naedoServiceProfile;
    }

    public void setNaedoServiceProfile(ServiceProfile naedoServiceProfile) {
        this.naedoServiceProfile = naedoServiceProfile;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
