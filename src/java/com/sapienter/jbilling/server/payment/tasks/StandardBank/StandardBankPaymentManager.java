package com.sapienter.jbilling.server.payment.tasks.StandardBank;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentResponseEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by Wajeeha Ahmed on 7/8/17.
 */
public class StandardBankPaymentManager {
    private static final Logger logger = LoggerFactory.getLogger(StandardBankPaymentManager.class);
    private final List<Date> holidays;
    private String bankFilesDir = "Standard_Bank";
    private ServiceProfile serviceProfile = null;
    private Integer sequenceNo = null;
    private Integer firstSequenceNo = null;
    private String userReferenceNo = null;
    private Date actionDate = null;
    private String userAccountNo = null;
    private Integer userAccountType = 1;
    private String userAccountName = null;
    private String userBranchCode = null;

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    private Integer entityId = null;

    public StandardBankPaymentManager(ServiceProfile serviceProfile, Integer entityId, List<Date> holidays) {
        this.serviceProfile = serviceProfile;
        this.entityId = entityId;
        this.holidays = holidays;
    }

    public String createInstallationHeader(String clientUserCode, Date createdDate, Date purgeDate) {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");

        String clientUserCodeStr = StringUtils.rightPad(clientUserCode, 4, ' ');

        StringBuilder installationHeaderRecord = new StringBuilder();

        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_RECORD_IDENTIFIER);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_VOLMUE_NO);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_TAPE_SERIAL_NO);
        installationHeaderRecord.append(clientUserCodeStr);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_INSTALLATION_CODE);
        installationHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDateTime(createdDate)));
        installationHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDateTime(purgeDate)));
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_INSTALLATION_GENERATION_NO);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_BLOCK_LENGTH);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_RECORD_LENGTH);
        installationHeaderRecord.append(IgnitionConstants.SB_INSTALLATION_HEADER_SERVICE);
        String installationHeader = StringUtils.rightPad(installationHeaderRecord.toString(), 180, " ");
        logger.debug("Installation Header Record: " + installationHeader);

        return installationHeader;
    }

    public String createUserHeader(String clientUserCode, Date createdDate, Date purgeDate,
                                   Date firstActionDate, Date lastActionDate, String firstSequenceNo,
                                   Integer fileSequenceNo, String typeOfService) {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");

        String clientUserCodeStr = StringUtils.rightPad(clientUserCode, 4, ' ');
        firstSequenceNo = StringUtils.leftPad(firstSequenceNo, 6, '0');
        String fileSequenceNoStr = StringUtils.leftPad(fileSequenceNo.toString(), 4, '0');
        typeOfService = StringUtils.rightPad(typeOfService.toString(), 10, ' ');

        StringBuilder userHeaderRecordBuilder = new StringBuilder();
        userHeaderRecordBuilder.append(IgnitionConstants.SB_USER_HEADER_RECORD);
        userHeaderRecordBuilder.append(clientUserCodeStr);
        userHeaderRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(createdDate)));
        userHeaderRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(purgeDate)));
        userHeaderRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(firstActionDate)));
        userHeaderRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(lastActionDate)));
        userHeaderRecordBuilder.append(firstSequenceNo);
        userHeaderRecordBuilder.append(fileSequenceNoStr);
        userHeaderRecordBuilder.append(typeOfService);

        String userHeader = userHeaderRecordBuilder.toString();
        userHeader = StringUtils.rightPad(userHeader, 180, " ");
        logger.debug("User Header Record: " + userHeader);
        return userHeader;
    }

    public String createStandardTransaction(Integer recordIdentifier, String userNominatedBranch,
                                            String userNominatedAccount, String clientUserCode, Integer transactionSequence,
                                            String homingBranch, String homingAccountNo, Integer typeOfAccount, BigDecimal amount,
                                            Date actionDate, Integer classOfEntry, Integer taxCode, String userReferenceNo,
                                            String userHomingAccountName, String nonStandardHomeAccNo) {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");
        userNominatedBranch = StringUtils.leftPad(userNominatedBranch.toString(), 6, '0');
        userNominatedAccount = StringUtils.leftPad(userNominatedAccount.toString(), 11, '0');
        String clientUserCodeStr = StringUtils.rightPad(clientUserCode, 4, ' ');
        String transactionSequenceStr = StringUtils.leftPad(transactionSequence.toString(), 6, '0');
        String homingBranchStr = StringUtils.leftPad(homingBranch.toString(), 6, '0');
        String homingAccountNoStr = StringUtils.leftPad(homingAccountNo, 11, '0');
        String amountStr = amount.toString();
        amountStr = amountStr.replace(".", "");
        amountStr = StringUtils.leftPad(amountStr, 11, '0');
        userHomingAccountName = StringUtils.rightPad(userHomingAccountName.toString(), 30, ' ');
        nonStandardHomeAccNo = StringUtils.leftPad(nonStandardHomeAccNo.toString(), 20, '0');
        String homingInstituteStr = StringUtils.leftPad(IgnitionConstants.SB_STANDARD_TRANSACTION_RECORD_HOMING_INSTITUTE.toString(), 2, '0');

        String filler1 = "";
        filler1 = StringUtils.leftPad(filler1, 3, '0');
        String filler2 = "";
        filler2 = StringUtils.leftPad(filler2, 16, ' ');

        StringBuilder builder = new StringBuilder();
        builder.append(recordIdentifier.toString());
        builder.append(userNominatedBranch);
        builder.append(userNominatedAccount);
        builder.append(clientUserCodeStr);
        builder.append(transactionSequenceStr);
        builder.append(homingBranchStr);
        builder.append(homingAccountNoStr);
        builder.append(typeOfAccount.toString());
        builder.append(amountStr);
        builder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate)));
        builder.append(classOfEntry.toString());
        builder.append(taxCode.toString());
        builder.append(filler1);
        builder.append(userReferenceNo);
        builder.append(userHomingAccountName);
        builder.append(nonStandardHomeAccNo);
        builder.append(filler2);
        builder.append(homingInstituteStr);

        String standardTransaction;
        standardTransaction = StringUtils.rightPad(builder.toString(), 180, " ");

        logger.debug("Standard Transaction: " + standardTransaction);

        return standardTransaction;
    }

    public String createUserTrailer(String clientUserCode, String firstSequenceNo,
                                    Integer fileSequenceNo, Date firstActionDate, Date lastActionDate,
                                    Integer numberOfCreditRecords, Integer numberOfDebitRecords, Integer numberOfContraRecords, BigDecimal totalDebitValue,
                                    BigDecimal totalCreditValue, String hashTotal) {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");

        String clientUserCodeStr = StringUtils.rightPad(clientUserCode, 4, ' ');
        firstSequenceNo = StringUtils.leftPad(firstSequenceNo, 6, '0');
        String fileSequenceNoStr = StringUtils.leftPad(fileSequenceNo.toString(), 6, '0');
        String numberOfCreditRecordsStr = StringUtils.leftPad(numberOfCreditRecords.toString(), 6, '0');
        String numberOfDebitRecordsStr = StringUtils.leftPad(numberOfDebitRecords.toString(), 6, '0');
        String numberOfContraRecordsStr = StringUtils.leftPad(numberOfContraRecords.toString(), 6, '0');
        String totalDebitValueStr = totalDebitValue.toString();
        totalDebitValueStr = totalDebitValueStr.replace(".", "");
        totalDebitValueStr = StringUtils.leftPad(totalDebitValueStr, 12, '0');
        String totalCreditValueStr = totalCreditValue.toString();
        totalCreditValueStr = totalCreditValueStr.replace(".", "");
        totalCreditValueStr = StringUtils.leftPad(totalCreditValueStr, 12, '0');

        StringBuilder userTrailerRecordBuilder = new StringBuilder();
        userTrailerRecordBuilder.append(IgnitionConstants.SB_USER_TRAILER_RECORD);
        userTrailerRecordBuilder.append(clientUserCodeStr);
        userTrailerRecordBuilder.append(firstSequenceNo);
        userTrailerRecordBuilder.append(fileSequenceNoStr);
        userTrailerRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(firstActionDate)));
        userTrailerRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(lastActionDate)));
        userTrailerRecordBuilder.append(numberOfCreditRecordsStr);
        userTrailerRecordBuilder.append(numberOfDebitRecordsStr);
        userTrailerRecordBuilder.append(numberOfContraRecordsStr);
        userTrailerRecordBuilder.append(totalDebitValueStr);
        userTrailerRecordBuilder.append(totalCreditValueStr);
        userTrailerRecordBuilder.append(hashTotal);

        String userTrailer = userTrailerRecordBuilder.toString();
        userTrailer = StringUtils.rightPad(userTrailer, 180, " ");

        logger.debug("User Trailer: " + userTrailer);

        return userTrailer;
    }

    public String createInstallationTrailer(String clientUserCode, Date createdDate,
                                            Date purgeDate, Integer recordCount,
                                            Integer userHeaderTrailerCount) {
        String volumeNumber = IgnitionConstants.SB_INSTALLATION_HEADER_VOLMUE_NO;
        String tapeSerialNo = IgnitionConstants.SB_INSTALLATION_HEADER_TAPE_SERIAL_NO;
        String installationIDCode = IgnitionConstants.SB_INSTALLATION_HEADER_INSTALLATION_CODE;
        String installationGenerationNo = IgnitionConstants.SB_INSTALLATION_HEADER_INSTALLATION_GENERATION_NO;
        String blockLength = IgnitionConstants.SB_INSTALLATION_HEADER_BLOCK_LENGTH;
        String recordLength = IgnitionConstants.SB_INSTALLATION_HEADER_RECORD_LENGTH;
        String service = StringUtils.rightPad(IgnitionConstants.SB_INSTALLATION_HEADER_SERVICE, 10, " ");
        String blockCount = "000000";
        String recordCountStr = StringUtils.leftPad(recordCount.toString(), 6, '0');
        String userHeaderTrailerCountStr = StringUtils.leftPad(userHeaderTrailerCount.toString(), 6, '0');
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");

        StringBuilder installationTrailerRecordBuilder = new StringBuilder();
        installationTrailerRecordBuilder.append(IgnitionConstants.SB_INSTALLATION_TRAILER_RECORD_IDENTIFIER);
        installationTrailerRecordBuilder.append(volumeNumber);
        installationTrailerRecordBuilder.append(tapeSerialNo);
        installationTrailerRecordBuilder.append(clientUserCode);
        installationTrailerRecordBuilder.append(installationIDCode);
        installationTrailerRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(createdDate)));
        installationTrailerRecordBuilder.append(dateFormat.format(DateConvertUtils.asLocalDateTime(purgeDate)));
        installationTrailerRecordBuilder.append(installationGenerationNo);
        installationTrailerRecordBuilder.append(blockLength);
        installationTrailerRecordBuilder.append(recordLength);
        installationTrailerRecordBuilder.append(service);
        installationTrailerRecordBuilder.append(blockCount);
        installationTrailerRecordBuilder.append(recordCountStr);
        installationTrailerRecordBuilder.append(userHeaderTrailerCountStr);

        String installationTrailer = installationTrailerRecordBuilder.toString();
        installationTrailer = StringUtils.rightPad(installationTrailer, 180, " ");

        logger.debug("User Installation Trailer: " + installationTrailer);

        return installationTrailer;
    }

    public Boolean processPayment(PaymentWS payment, Integer[] orderIds, UserWS userWS, String userPolicyNo) {

        try {

            logger.debug("Process Standard Band payment");

            if (orderIds != null) {
                String policyNo = getUserReference(userWS, orderIds);
                userReferenceNo = StringUtils.rightPad(serviceProfile.getShortName(), 10, " ") + StringUtils.rightPad(policyNo, 20, " ");
            } else
                userReferenceNo = StringUtils.rightPad(serviceProfile.getShortName(), 10, " ") + StringUtils.rightPad(userPolicyNo, 20, " ");

            setPaymentInformationDetails(payment);

            String fileType = "";
            if (serviceProfile.getTypesOfDebitServices().toUpperCase().equals(IgnitionConstants.ServiceType.TWO_DAY.getName()))
                fileType = "TWO_DAY";
            else
                fileType = serviceProfile.getTypesOfDebitServices();

            String path = Util.getSysProp("base_dir") + bankFilesDir + File.separator + getEntityId() + File.separator + fileType + File.separator + getFileName();
            Path pathToFile = Paths.get(path);

            logger.debug("Input file path: " + path);
            boolean addHeaderAndTrailer = false;

            if (IsSequenceNoResetRequired(fileType, serviceProfile.getCutOffTime())) {
                IgnitionUtility.updateColumnInServiceProfile(serviceProfile.getCode(), entityId, ServiceProfile.Names.TRANSACTION_NO, String.valueOf(1));
                firstSequenceNo = 1;
            } else {
                firstSequenceNo = new Integer(serviceProfile.getTransactionNo());
            }

            if (Files.notExists(pathToFile)) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
                addHeaderAndTrailer = true;
            } else {
                List<String> lines = Files.readAllLines(pathToFile);
                if (lines.size() == 0) {
                    addHeaderAndTrailer = true;
                }
            }

            String fileContent = createFileContent(addHeaderAndTrailer, pathToFile, payment);
            writeInputFile(path, fileContent);
            updatePaymentMetaFields(payment);

            return true;

        } catch (Exception exception) {
            logger.error("Exception: " + exception);
        }

        return false;
    }

    public String getFileName() {
        String serviceProfileCode = getServiceProfileCode();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(TimezoneHelper.companyCurrentDate(this.getEntityId()));

        if (serviceProfile.isLive()) {
            return ("PR" + "HTHSB.COMIT." + serviceProfileCode + ".BEFT" + serviceProfile.getACBUserCode() + ".INPUT");
        } else {
            return ("TR" + "HTHSB.COMIT." + serviceProfileCode + ".BEFT" + serviceProfile.getACBUserCode() + ".INPUT");
        }
    }

    public String getCompanyName() {
        return serviceProfile.getEntityName();
    }

    public String getServiceProfileCode() {
        return serviceProfile.getCode();
    }

    public String getTransactionRecord(PaymentWS paymentInfo) {
        logger.debug("Create Transaction Record. PaymentWs: " + paymentInfo);

        Integer transactionRecordIdentifier;

        // If payment type is credit
        if (paymentInfo.getIsRefund() != 0) {
            transactionRecordIdentifier = IgnitionConstants.SB_STANDARD_TRANSACTION_RECORD_IDENTIFIER_CREDIT;
        }
        // If payment type is Debit
        else {
            transactionRecordIdentifier = IgnitionConstants.SB_STANDARD_TRANSACTION_RECORD_IDENTIFIER_DEBIT;
        }

        String userNominatedBranch = serviceProfile.getBankAccountBranch();
        Integer classOfEntry = 36;
        Integer taxCode = 0;

        String userNominatedAccount = serviceProfile.getBankAccountNumber();

        String homingAccountNo = "";
        String nonStandardHomingAccNo = "";

        if (userAccountNo.length() <= 11)
            homingAccountNo = userAccountNo;
        else
            nonStandardHomingAccNo = userAccountNo;

        BigDecimal amount = paymentInfo.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP);
        String transactionRecord = createStandardTransaction(transactionRecordIdentifier, userNominatedBranch, userNominatedAccount, serviceProfile.getACBUserCode(), sequenceNo,
                userBranchCode, homingAccountNo, userAccountType, amount, actionDate, classOfEntry, taxCode, userReferenceNo, userAccountName, nonStandardHomingAccNo);

        sequenceNo = sequenceNo + 1;
        logger.debug("Transaction Record: " + transactionRecord);
        return transactionRecord;
    }

    String createContraRecord(PaymentWS paymentWS, Date actionDate) {
        logger.debug("Create Contra Record. PaymentWs: " + paymentWS + ", action date: " + actionDate);

        Integer recordIdentifier;

        if (paymentWS.getIsRefund() != 0) {
            recordIdentifier = IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_CREDIT;
        } else {
            recordIdentifier = IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_DEBIT;
        }

        String userNominatedBranch = serviceProfile.getBankAccountBranch();
        String userNominatedAccountNo = serviceProfile.getBankAccountNumber();
        String clientUserCode = serviceProfile.getACBUserCode();
        String homingBranch = serviceProfile.getBankAccountBranch();
        String homingAccountNo = serviceProfile.getBankAccountNumber();
        String nominatedAccountName = serviceProfile.getBankAccountName();
        String accountType = "1";
        String accountTypeStr = serviceProfile.getBankAccountType();

        if (accountTypeStr.toUpperCase().contains("CURRENT") || accountTypeStr.toUpperCase().contains("CHEQUE")) {
            accountType = "1";
        } else if (accountTypeStr.toUpperCase().contains("SAVINGS")) {
            accountType = "2";
        } else if (accountTypeStr.toUpperCase().contains("TRANSMISSION")) {
            accountType = "3";
        } else if (accountTypeStr.toUpperCase().contains("BOND")) {
            accountType = "4";
        } else if (accountTypeStr.toUpperCase().contains("SUBSCRIPTION SHARE")) {
            accountType = "5";
        }

        Integer classOfEntry = 36;
        String filler1 = "0000";
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");
        userNominatedBranch = StringUtils.leftPad(userNominatedBranch, 6, '0');
        userNominatedAccountNo = StringUtils.leftPad(userNominatedAccountNo.toString(), 11, "0");
        String clientUserCodeStr = StringUtils.rightPad(clientUserCode, 4, " ");
        String transactionSequenceNoStr = StringUtils.leftPad(sequenceNo.toString(), 6, '0');
        String homingBranchStr = StringUtils.leftPad(homingBranch.toString(), 6, "0");
        homingAccountNo = StringUtils.leftPad(homingAccountNo.toString(), 11, "0");
        nominatedAccountName = StringUtils.rightPad(nominatedAccountName, 30, " ");
        String amountStr = paymentWS.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
        amountStr = amountStr.replace(".","");
        amountStr = StringUtils.leftPad(amountStr, 11, '0');
        String contraUserReference = StringUtils.rightPad(userReferenceNo.substring(0, 10) + "CONTRA" + userReferenceNo.substring(0, 10), 30, " ");

        StringBuilder contraRecord = new StringBuilder();
        contraRecord.append(recordIdentifier.toString());
        contraRecord.append(userNominatedBranch);
        contraRecord.append(userNominatedAccountNo);
        contraRecord.append(clientUserCodeStr);
        contraRecord.append(transactionSequenceNoStr);
        contraRecord.append(homingBranchStr);
        contraRecord.append(homingAccountNo);
        contraRecord.append(accountType);
        contraRecord.append(amountStr);
        contraRecord.append(dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate)));
        contraRecord.append(classOfEntry.toString());
        contraRecord.append(filler1);
        contraRecord.append(contraUserReference);
        contraRecord.append(nominatedAccountName);
        String contra = StringUtils.rightPad(contraRecord.toString(), 180, " ");
        sequenceNo = sequenceNo + 1;

        logger.debug("Contra Record: " + contra);

        return contra;
    }

    public String createFileContentWithHeaderAndTrailer(PaymentWS paymentInfo) {

        String content = "";

        logger.debug("Create payment file for PaymentWS: " + paymentInfo);

        try {
            Calendar currentCal = Calendar.getInstance();
            Date createdDate = currentCal.getTime();
            Date purgeDate = currentCal.getTime();
            String clientUserCode = serviceProfile.getACBUserCode();
            String installationHeader = createInstallationHeader(clientUserCode, createdDate, purgeDate);
            String firstSequenceNo = String.valueOf(this.firstSequenceNo);
            Integer fileSequenceNo = serviceProfile.getFileSequenceNo() + 1;
            String typeOfService = serviceProfile.getTypesOfDebitServices();
            List<String> accountNumbers = new ArrayList<>();
            accountNumbers.add(serviceProfile.getBankAccountNumber());


            logger.debug("Created Date: " + createdDate + ", Purge Date: " + purgeDate + ", Client User Code: " + clientUserCode
                    + ", Installation Header: " + installationHeader + ", First Sequence No: " + firstSequenceNo
                    + ", file Sequence No: " + firstSequenceNo + ", Type of service: " + typeOfService + ", Action Date: " + actionDate);

            String userHeader = createUserHeader(clientUserCode, createdDate, purgeDate, actionDate, actionDate, firstSequenceNo, fileSequenceNo, typeOfService);
            String transactionRecord = getTransactionRecord(paymentInfo);
            String contraRecord = createContraRecord(paymentInfo, actionDate);

            Integer numberOfCreditRecords = 1;
            Integer numberOfDebitRecords = 1;
            BigDecimal totalDebitValue = paymentInfo.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalCreditValue = totalDebitValue;
            Integer numberOfContraRecords = 1;

            String hashTotal = null;

            String customerAccountNo = userAccountNo;
            accountNumbers.add(customerAccountNo);

            //Calculate hash for account number
            for (String accountNo : accountNumbers) {
                if (accountNo.length() <= 11) {
                    hashTotal = calculateHashTotal(accountNo, null, hashTotal);
                } else
                    hashTotal = calculateHashTotal(null, accountNo, hashTotal);
            }

            String userTrailer = createUserTrailer(clientUserCode, firstSequenceNo, fileSequenceNo, actionDate, actionDate,
                    numberOfContraRecords, numberOfCreditRecords, numberOfDebitRecords, totalDebitValue, totalCreditValue, hashTotal);

            logger.debug("User Trailer " + userTrailer);

            Integer recordCount = 6;
            Integer userHeaderTrailerCount = 2;
            String installationTrailer = createInstallationTrailer(clientUserCode, createdDate, purgeDate, recordCount, userHeaderTrailerCount);

            logger.debug("User Installation Trailer " + installationTrailer);

            content = installationHeader + "\r\n" + userHeader + "\r\n" + transactionRecord + "\r\n" + contraRecord
                    + "\r\n" + userTrailer + "\r\n" + installationTrailer + "\r\n";
        } catch (Exception exception) {
            logger.error("Exception: " + exception);
        }
        return content;
    }

    public String convertListDataToString(List<String> lines) {
        StringBuilder fileData = new StringBuilder();

        for (String line : lines) {
            fileData.append(line);
            fileData.append("\r");
            fileData.append("\n");
        }
        return fileData.toString();
    }

    public void writeInputFile(String path, String fileContent) {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            File file = new File(path);
            fileWriter = new FileWriter(file.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(fileContent);

        } catch (IOException exception) {
            logger.error("Exception = " + exception);
        } finally {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();

                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException exception) {
                logger.error("Exception = " + exception);
            }
        }
    }

    public String createFileContent(boolean addHeaderAndTrailer, Path path, PaymentWS paymentInfo) {
        String fileContent = null;

        if (addHeaderAndTrailer) {
            sequenceNo = 1;
            fileContent = createFileContentWithHeaderAndTrailer(paymentInfo);
        } else {
            fileContent = getUpdatedFileContent(path, paymentInfo);
        }

        return fileContent;
    }

    public String getUpdatedFileContent(Path path, PaymentWS paymentInfo) {
        logger.debug("Get updated File Content. Path: " + path + ",PaymentWs: " + paymentInfo);

        String fileContent = null;
        try {
            List<String> lines = Files.readAllLines(path);
            Boolean addNewContraRecord = false;
            Integer contraRecordIndex = null;
            String newTransaction = null;
            String lastTransaction = null;
            String lastContraRecord = null;
            String updatedContraRecord = null;
            String updatedUserTrailer = null;
            String transactionType = null;
            String lastUserTrailer = null;
            Integer userTrailerIndex = null;
            Integer newTransactionIndex = null;
            String lastUserInstallationTrailer = null;
            String updatedUserInstallationTrailer = null;
            Integer addedTransactions = null;
            Integer lastHeaderRecordIndex = null;

            if (paymentInfo.getIsRefund() != 0) {
                transactionType = "Credit";
            }
            // If payment type is Debit
            else {
                transactionType = "Debit";
            }

            for (int iterator = lines.size() - 1; iterator > 0; iterator--) {
                String tag = lines.get(iterator).substring(0, 2);

                if (tag.equals(IgnitionConstants.SB_USER_HEADER_RECORD)) {
                    lastHeaderRecordIndex = iterator;
                }

                if ((tag.equals(IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_CREDIT.toString())
                        && transactionType.equals("Credit") == true)
                        || (tag.equals(IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_DEBIT.toString())
                        && transactionType.equals("Debit") == true)) {
                    String contraActionDate = lines.get(iterator).substring(58, 64);
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyMMdd");

                    if (contraActionDate.equals(dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate)).toString())) {
                        addNewContraRecord = false;
                        lastContraRecord = lines.get(iterator);
                        contraRecordIndex = iterator;
                        //break;
                    }
                }

                if (lastContraRecord == null && (tag.equals(IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_CREDIT.toString()) ||
                        tag.equals(IgnitionConstants.SB_CONTRA_RECORD_INDENTIFIER_DEBIT.toString()))) {
                    contraRecordIndex = iterator;
                    addNewContraRecord = true;
                }

                if (tag.equals(IgnitionConstants.SB_USER_TRAILER_RECORD.toString())) {
                    lastUserTrailer = lines.get(iterator);
                    userTrailerIndex = iterator;
                    lastUserInstallationTrailer = lines.get(iterator + 1);
                }
            }

            Boolean incrementContraRecord = false;

            if (addNewContraRecord == true) {
                sequenceNo = lines.size() - 3;
                newTransaction = getTransactionRecord(paymentInfo);
                Date actionDate = this.actionDate;
                updatedContraRecord = createContraRecord(paymentInfo, actionDate);
                contraRecordIndex = userTrailerIndex;
                newTransactionIndex = contraRecordIndex;
                addedTransactions = 2;
                incrementContraRecord = true;
            } else {
                sequenceNo = lines.size() - 4;
                newTransaction = getTransactionRecord(paymentInfo);
                updatedContraRecord = updateContraRecord(lastContraRecord, paymentInfo.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP));
                newTransactionIndex = contraRecordIndex;
                addedTransactions = 1;
            }

            logger.debug("Last Transaction: " + lastTransaction + ", Last Contra Record: " + lastContraRecord + ", Last user Trailer: "
                    + lastUserTrailer + ", Last Installation Trailer: " + lastUserInstallationTrailer + ", Sequence No: " + sequenceNo);


            String accountNo = userAccountNo;
            if (transactionType == "Credit") {
                updatedUserTrailer = updateTrailerRecord(lastUserTrailer, true, paymentInfo.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), incrementContraRecord, accountNo);
            } else if (transactionType == "Debit") {
                updatedUserTrailer = updateTrailerRecord(lastUserTrailer, false, paymentInfo.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), incrementContraRecord, accountNo);
            }

            updatedUserInstallationTrailer = updatedUserInstallationTrailerRecord(lastUserInstallationTrailer, addedTransactions);
            String updatedHeaderRecord = updateHeaderRecord(lines.get(lastHeaderRecordIndex));

            lines.set(lastHeaderRecordIndex, updatedHeaderRecord);
            lines.set(userTrailerIndex, updatedUserTrailer);
            lines.set(userTrailerIndex + 1, updatedUserInstallationTrailer);

            if (addNewContraRecord)
                lines.add(contraRecordIndex, updatedContraRecord);
            else
                lines.set(contraRecordIndex, updatedContraRecord);
            lines.add(newTransactionIndex, newTransaction);

            fileContent = convertListDataToString(lines);
        } catch (Exception exception) {
            logger.error("Exception: " + exception);
        }
        return fileContent;
    }

    public String updateContraRecord(String contra, BigDecimal amount) {

        StringBuilder builder = new StringBuilder(contra);

        BigDecimal lastTotal = new BigDecimal(contra.substring(47,56)+"."+contra.substring(56,58));
        BigDecimal updatedAmount =  amount.add(lastTotal).setScale(2, BigDecimal.ROUND_HALF_UP);
        String amountSt = StringUtils.leftPad(updatedAmount.toString().replace(".",""), 11, "0");
        String seqNo = StringUtils.leftPad(sequenceNo.toString(),6,'0');
        builder.replace(47,58,amountSt);
        builder.replace(23,29,seqNo);

        String updatedContraRecord = builder.toString();

        logger.debug("Updated Contra Record: " + updatedContraRecord);

        sequenceNo = sequenceNo + 1;
        return updatedContraRecord;
    }

    public String updateTrailerRecord(String previousUserTrailer, boolean isCreditRecord, BigDecimal amount, boolean incrementContraRecord, String accountNo) {
        StringBuilder builder = new StringBuilder(previousUserTrailer);

        if(isCreditRecord == true){
            Integer totalCreditCount  = new Integer(previousUserTrailer.substring(36,42));
            totalCreditCount = totalCreditCount +1;
            BigDecimal lastTotal = new BigDecimal(previousUserTrailer.substring(60,68)+"."+previousUserTrailer.substring(68,70));
            BigDecimal totalCreditValue = amount.add(lastTotal).setScale(2, BigDecimal.ROUND_HALF_UP);
            String amountSt = StringUtils.leftPad(totalCreditValue.toString().replace(".",""), 11, "0");

            builder.replace(36, 42, StringUtils.leftPad(totalCreditCount.toString(), 6, "0"));
            //Debit total
            builder.replace(48, 60, StringUtils.leftPad(amountSt, 12, "0"));
            //Credit total
            builder.replace(60, 72, StringUtils.leftPad(amountSt, 12, "0"));

            if (incrementContraRecord) {
                Integer totalDebitCount = new Integer(previousUserTrailer.substring(30, 36));
                totalDebitCount = totalDebitCount + 1;
                builder.replace(30, 36, StringUtils.leftPad(totalDebitCount.toString(), 6, "0"));
            }
        }
        else {
            Integer totalDebitCount  = new Integer(previousUserTrailer.substring(30,36));
            totalDebitCount = totalDebitCount +1;
            BigDecimal lastTotal = new BigDecimal(previousUserTrailer.substring(48,58)+"."+previousUserTrailer.substring(58,60));
            BigDecimal totalDebitValue = amount.add(lastTotal).setScale(2, BigDecimal.ROUND_HALF_UP);
            String amountSt = StringUtils.leftPad(totalDebitValue.toString().replace(".",""), 11, "0");

            builder.replace(30, 36, StringUtils.leftPad(totalDebitCount.toString(), 6, "0"));
            //Debit total
            builder.replace(48, 60, StringUtils.leftPad(amountSt, 12, "0"));
            //Credit total
            builder.replace(60, 72, StringUtils.leftPad(amountSt, 12, "0"));

            if (incrementContraRecord) {
                Integer totalCreditCount = new Integer(previousUserTrailer.substring(36, 42));
                totalCreditCount = totalCreditCount + 1;
                builder.replace(36, 42, StringUtils.leftPad(totalCreditCount.toString(), 6, "0"));
            }
        }

        String preHashTotal = previousUserTrailer.substring(72, 84);
        String hashTotal = null;

        if (accountNo.length() <= 11) {
            hashTotal = calculateHashTotal(accountNo, null, preHashTotal);
        } else
            hashTotal = calculateHashTotal(null, accountNo, preHashTotal);

        builder.replace(72, 84, hashTotal);

        if (incrementContraRecord) {

            //Updating total number of contra records
            Integer contraRecordCount = new Integer(previousUserTrailer.substring(42, 48));
            contraRecordCount = contraRecordCount + 1;
            String contraRecordCountSt = StringUtils.leftPad(contraRecordCount.toString(), 6, "0");

            builder.replace(42, 48, contraRecordCountSt);

            //Calculating hash total for contra
            preHashTotal = previousUserTrailer.substring(72, 84);

            if (serviceProfile.getBankAccountNumber().length() <= 11) {
                hashTotal = calculateHashTotal(serviceProfile.getBankAccountNumber(), null, preHashTotal);
            } else
                hashTotal = calculateHashTotal(null, serviceProfile.getBankAccountNumber(), preHashTotal);

            builder.replace(72, 84, hashTotal);
        }

        //Update last action date
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd");

        Date previousLastActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(previousUserTrailer.substring(24, 30), dateTimeFormatter));

        if (previousLastActionDate.getTime() < actionDate.getTime()) {
            builder.replace(24, 30, dateTimeFormatter.format(DateConvertUtils.asLocalDateTime(actionDate)));
        }

        String updatedTrailerRecord = builder.toString();

        logger.debug("Updated user trailer record: " + updatedTrailerRecord);
        return updatedTrailerRecord;
    }

    public List<String> readFileContent(Path path) {
        List<String> content = null;
        try {
            content = Files.readAllLines(path);
        } catch (Exception exception) {
            logger.error("Exception: " + exception);
        }
        return content;
    }

    public String calculateHashTotal(String homingAccountNo, String nonStandardHomingAccountNo, String lastHashTotal) {
        logger.debug("Calculating Hash total. Homing account no: " + homingAccountNo + ",Non Standard Homing Account No: " + nonStandardHomingAccountNo
                + ", last Hash Total: " + lastHashTotal);

        String hashTotal = "";
        Long hashTotalLong = null;

        if (lastHashTotal == null) {
            hashTotalLong = 0L;
        } else {
            hashTotalLong = Long.parseLong(lastHashTotal.trim());
        }

        if (nonStandardHomingAccountNo != null) {

            hashTotalLong = hashTotalLong + Long.parseLong(nonStandardHomingAccountNo.substring(
                    (nonStandardHomingAccountNo.length() - 11), nonStandardHomingAccountNo.length()));
        } else {
            hashTotalLong = hashTotalLong + Long.parseLong(homingAccountNo);
        }

        if (hashTotalLong.toString().length() < 12) {
            hashTotal = StringUtils.leftPad(hashTotalLong.toString(), 12, '0');
        } else if (hashTotalLong.toString().length() > 12) {
            hashTotal = hashTotalLong.toString().substring(0, 12);
        } else
            hashTotal = hashTotalLong.toString();
        logger.debug("Hash Total: " + hashTotal);
        return hashTotal;
    }

    public String updatedUserInstallationTrailerRecord(String lastUserTrailerRecord, Integer addedTransactionCount) {
        StringBuilder builder = new StringBuilder(lastUserTrailerRecord);
        Integer totalRecord = Integer.parseInt(lastUserTrailerRecord.substring(62, 68));
        totalRecord = totalRecord + addedTransactionCount;
        builder.replace(62, 68, StringUtils.leftPad(totalRecord.toString(), 6, '0'));

        String updatedUserTrailerRecord = builder.toString();
        logger.debug("Updated user Installation Trailer Record: " + updatedUserTrailerRecord);
        return updatedUserTrailerRecord;
    }

    private String getUserReference(UserWS userWS, Integer[] orderIds) {

        logger.debug("Calculating user reference no for User: " + userWS.getId() + " for order id: " + orderIds[0]);

        String userReference = "";

        if (orderIds == null || orderIds.length == 0) {
            logger.debug("No incoming OrderIDs to get User Reference from.");
            return userReference;
        }

        // NOTE: keeping the following code as it was discussed that Policy Number will be in Order
        OrderWS order = IgnitionUtility.getOrder(userWS, orderIds[0]);

        MetaFieldValueWS[] metaFields = order.getMetaFields();

        for (MetaFieldValueWS metaFieldValue : metaFields) {
            if (metaFieldValue.getFieldName().equals(IgnitionConstants.POLICY_NUMBER)) {
                userReference = metaFieldValue.getStringValue();
            }
        }

        logger.debug("User Reference No: " + userReference);
        return userReference;
    }

    private void updatePaymentMetaFields(PaymentWS payment) {
        logger.debug("Updating payment metafields");
        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>();
        MetaField metaField = new MetaFieldDAS().getFieldByName(getEntityId(), new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TRANSACTION_NUMBER);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER);
            metaFieldValueWS.setStringValue(String.valueOf(sequenceNo - 2));
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(getEntityId(), new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_SEQUENCE_NUMBER);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER);
            metaFieldValueWS.setStringValue(String.valueOf((serviceProfile.getFileSequenceNo() + 1)));
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(getEntityId(), new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_SENT_ON);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_SENT_ON);
            metaFieldValueWS.setStringValue(serviceProfile.getName());
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_USER_REFERENCE);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_USER_REFERENCE);
            metaFieldValueWS.setStringValue(userReferenceNo);
            metaFieldValueList.add(metaFieldValueWS);
        }

        metaField = new MetaFieldDAS().getFieldByName(getEntityId(), new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_ACTION_DATE);
        Date actionDate = this.actionDate;
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_ACTION_DATE);
            metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate)));
            metaFieldValueList.add(metaFieldValueWS);
        }

        metaField = new MetaFieldDAS().getFieldByName(getEntityId(), new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_DATE);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_DATE);
            metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate)));
            metaFieldValueList.add(metaFieldValueWS);
        }

        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_CLIENT_CODE);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_CLIENT_CODE);
            metaFieldValueWS.setStringValue(serviceProfile.getACBUserCode());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Type
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TYPE);
        if (metaField != null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TYPE);
            metaFieldValueWS.setStringValue(IgnitionConstants.IgnitionPaymentType.EFT.toString());
            metaFieldValueList.add(metaFieldValueWS);
        }

        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        payment.setMetaFields(updatedMetaFieldValueWSArray);
    }

    public void processStandardBankResponseFile() throws IOException {

        logger.debug("Starting response file processing ");

        String folderPath = Util.getSysProp("base_dir") + bankFilesDir + File.separator + getEntityId() + File.separator + IgnitionConstants.SB_RESPONSE_FILES_FOLDER;
        File dir = new File(folderPath);

        if (dir.isDirectory()) {
            String[] files = dir.list();

            for (String fileName : files) {
                if ((fileName.contains(IgnitionConstants.SB_INTERIM_FILE_IDENTIFIER) || fileName.contains(IgnitionConstants.SB_FINAL_AUDIT_FILE_IDENTIFIER) ||
                        fileName.contains(IgnitionConstants.SB_UNPAID_FILE_IDENTIFIER) || fileName.contains(IgnitionConstants.SB_VET_FILE_IDENTIFIER))
                        && !fileName.contains("Done")) {

                    logger.debug("File found with name " + fileName);

                    String filePath = folderPath + File.separator + fileName;
                    Path path = Paths.get(filePath);
                    List<String> content = readFileContent(path);
                    String fileType = "";

                    if (fileName.contains(IgnitionConstants.SB_FINAL_AUDIT_FILE_IDENTIFIER)) {
                        fileType = IgnitionConstants.FINAL_AUDIT_FILE;
                    } else if (fileName.contains(IgnitionConstants.SB_INTERIM_FILE_IDENTIFIER)) {
                        fileType = IgnitionConstants.INTERIM_FILE;
                    } else if (fileName.contains(IgnitionConstants.SB_UNPAID_FILE_IDENTIFIER)) {
                        fileType = IgnitionConstants.UNPAID_FILE;
                    } else if (fileName.contains(IgnitionConstants.SB_VET_FILE_IDENTIFIER)) {
                        fileType = IgnitionConstants.VET_FILE;
                    }

                    Boolean fileProcessed = false;
                    if (content.get(0).substring(0, 2).equals(IgnitionConstants.SB_AUDIT_FILE_HEADER_RECORD_IDENTIFIER)) {
                        //Process audit /  final audit file
                        fileProcessed = processAuditFile(content, fileType);

                        if (fileProcessed) {
                            DateTimeFormatter dateFormatForOutputFile = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                            String processedDate = dateFormatForOutputFile.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

                            Files.move(path, path.resolveSibling("Done-" + processedDate + "-" + fileName));
                        }

                        if (fileName.contains("INTAUD") && IsIncrementRequiredInFileSequenceNo(content.get(0))) {
                            String serviceProfileCode = content.get(0).substring(2, 7);
                            IgnitionUtility.updateServiceProfile(serviceProfileCode, entityId, ServiceProfile.Names.FILE_SEQUENCE_NO, ServiceProfile.Names.TRANSACTION_NO);
                        }
                    } else if (content.get(0).substring(0, 2).equals(IgnitionConstants.SB_VET_AND_UNPAID_FILE_HEADER_RECORD_IDENTIFIER)) {
                        //process for VET or unpaid file
                        fileProcessed = processVetAndUnpaidFiles(content, fileType);

                        if (fileProcessed) {
                            DateTimeFormatter dateFormatForOutputFile = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                            String processedDate = dateFormatForOutputFile.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

                            Files.move(path, path.resolveSibling("Done-" + processedDate + "-" + fileName));
                        }
                    }
                }
            }
        }
    }

    public Boolean processAuditFile(List<String> lines, String fileType) {
        logger.debug("Started processing Audit/Interim file ");

        try {
            Integer fileSequenceNo = null;
            String headerError = null;
            String headerErrorDetails = null;
            String acbCode = null;
            Map<Integer, String> transactionDetails = new HashMap<>();

            for (String line : lines) {

                if (line.substring(0, 2).equals(IgnitionConstants.SB_AUDIT_FILE_HEADER_RECORD_IDENTIFIER)) {
                    fileSequenceNo = Integer.parseInt(line.substring(52, 56));
                    headerError = line.substring(86, 88).trim();
                    acbCode = line.substring(8, 12);
                    headerErrorDetails = line.substring(88, 118).trim();

                    if (!headerError.isEmpty()) {
                        transactionDetails.put(0, headerError + ";" + headerErrorDetails);
                        IgnitionPaymentResponseEvent event = new IgnitionPaymentResponseEvent(this.entityId, fileType,
                                fileSequenceNo, acbCode, IgnitionConstants.PaymentStatus.SB_TRANSMISSION_FAILURE,
                                transactionDetails, null, IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                        EventManager.process(event);
                        return true;
                    }

                } else if (line.substring(0, 2).equals(IgnitionConstants.SB_AUDIT_FILE_DETAIL_RECORD_IDENTIFIER)) {
                    Integer transactionNo = Integer.parseInt(line.substring(31, 37));

                    String errorCode = line.substring(186, 188);
                    String errorDetails = line.substring(188, 213).trim();

                    transactionDetails.put(transactionNo, errorCode + ";" + errorDetails);
                }
            }

            if (transactionDetails.size() > 0) {
                IgnitionPaymentResponseEvent event = new IgnitionPaymentResponseEvent(this.entityId, fileType,
                        fileSequenceNo, acbCode, IgnitionConstants.PaymentStatus.SB_TRANSACTION_FAILURE, transactionDetails, null, IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                EventManager.process(event);
                return true;
            }
        } catch (Exception exception) {
            logger.error("Exception: " + exception);
            return false;
        }

        return true;
    }

    public Boolean processVetAndUnpaidFiles(List<String> lines, String fileType) {

        logger.debug("Started processing Vet/Unpaid file");

        try {
            Integer fileSequenceNo = null;
            for (Integer iterator = (lines.size() - 1); iterator >= 0; iterator--) {

                String errorCode = "";
                if (lines.get(iterator).substring(0, 2).equals(IgnitionConstants.
                        SB_VET_AND_UNPAID_FILE_CONSOLIDATION_RECORD_IDENTIFIER_CREDIT) ||
                        lines.get(iterator).substring(0, 2).equals(IgnitionConstants.
                                SB_VET_AND_UNPAID_FILE_CONSOLIDATION_RECORD_IDENTIFIER_DEBIT)) {
                    fileSequenceNo = Integer.parseInt(lines.get(iterator).substring(23, 29));
                } else if (lines.get(iterator).substring(0, 2).equals(IgnitionConstants.SB_VET_AND_UNPAID_FILE_TRANSACTION_RECORD_IDENTIFIER_CREDIT) ||
                        lines.get(iterator).substring(0, 2).equals(IgnitionConstants.SB_VET_AND_UNPAID_FILE_TRANSACTION_RECORD_IDENTIFIER_DEBIT)) {
                    Integer transactionNo = Integer.parseInt(lines.get(iterator).substring(23, 29));
                    errorCode = lines.get(iterator).substring(67, 69);

                    Map<String, String> metaFields = new HashMap<>();
                    metaFields.put(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER, transactionNo.toString());
                    metaFields.put(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER, fileSequenceNo.toString());

                    Integer paymentId = getPaymentIdByMetaFields(metaFields);

                    logger.debug("Payment " + paymentId + " found for Transaction Id " + transactionNo + " and Sequence Id " + fileSequenceNo);

                    if (paymentId != null) {
                        IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
                        Boolean paymentUpdated = IgnitionUtility.updatePaymentWithErrorCodeAndMetaField(paymentId, errorCode, "Vet/Unpaid error", webServicesSessionBean, entityId, IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);

                        logger.debug("Payment updated");

                        PaymentBL paymentBL = new PaymentBL(paymentId);
                        // Send Failed Payments Event

                        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

                        IgnitionPaymentFailedEvent event = new IgnitionPaymentFailedEvent(this.entityId, paymentDTOEx, fileType);
                        EventManager.process(event);
                    } else {
                        logger.debug("Payment not found");
                    }
                }

            }
        } catch (Exception exception) {
            logger.debug("Exception: " + exception);
            return false;
        }

        return true;
    }

    private Integer getPaymentIdByMetaFields(Map<String, String> metaFieldMap) {
        PaymentDAS paymentDAS = new PaymentDAS();
        Integer paymentId = paymentDAS.findPaymentByMetaFields(metaFieldMap);
        return paymentId;
    }

    private Date getActionDate() throws ParseException {
        Date firstActionDate = getActionDate(serviceProfile.getCutOffTime(), TimezoneHelper.companyCurrentDate(this.entityId));
        Date actionDate = getActionDateForCurrentService(firstActionDate, serviceProfile.getTypesOfDebitServices());
        return actionDate;
    }

    private Date getActionDate(String cutOffTime, Date currentDate) throws ParseException {

        Date actionDate = currentDate;

        if (IgnitionUtility.isCutOffTimeReached(cutOffTime, currentDate)) {
            Calendar actionDateCal = Calendar.getInstance();
            actionDateCal.setTime(currentDate);
            actionDateCal.add(Calendar.DATE, 1);

            actionDate = actionDateCal.getTime();
        }

        logger.debug("Calculated Action date for incoming Date: %s and Cut-Off-Time: %s is: %s", currentDate, cutOffTime, actionDate);

        return actionDate;
    }

    private Date getActionDateForCurrentService(Date actionDate, String serviceType) {

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(actionDate);

        if (serviceType.toLowerCase().equals(IgnitionConstants.ServiceType.TWO_DAY.toString().toLowerCase())) {
            currentCal.add(Calendar.DATE, 5);
        }

        currentCal = IgnitionUtility.getActionDateForWorkingDays(currentCal, holidays);

        currentCal.set(Calendar.HOUR_OF_DAY, 0);
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        currentCal.set(Calendar.MILLISECOND, 0);

        Date currentCalTime = currentCal.getTime();

        logger.debug("Calculated Action date for incoming Date: %s and ServiceType: %s is: %s", actionDate, serviceType, currentCalTime);

        return currentCalTime;
    }

    public void sendInputFile(String host, int port, String username, String password) {

        File dirForSameDay = new File(Util.getSysProp("base_dir") + "Standard_Bank/" + this.entityId + "/SAMEDAY/");

        File[] foundFiles = dirForSameDay.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.contains("INPUT") && !(name.contains("Sent")));
            }
        });

        if (foundFiles == null) {
            logger.debug("No INPUT file found for Standard Bank Same day service.");
        } else {
            for (File file : foundFiles) {
                logger.debug("Sending file");
                setServiceProfileUsingFileName(file.getName());
                send(file.getAbsolutePath(), host, port, username, password);
            }
        }

        File dirForTwoDay = new File(Util.getSysProp("base_dir") + "Standard_Bank/" + this.entityId + "/TWO_DAY/");

        foundFiles = dirForTwoDay.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.contains("INPUT") && !(name.contains("Sent")));
            }
        });

        for (File file : foundFiles) {
            logger.debug("Sending file");
            setServiceProfileUsingFileName(file.getName());
            send(file.getAbsolutePath(), host, port, username, password);
        }
    }

    public void send(String fileName, String host, int port, String username, String password) {

        String sftpWorkingDirectory = "BankFiles/" + serviceProfile.getToFIFolderLocation();

        logger.debug("sftp dir: " + sftpWorkingDirectory);

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        logger.debug("preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.debug("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            logger.debug("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;

            try {
                channelSftp.cd(sftpWorkingDirectory);
            } catch (SftpException e) {
                logger.debug("Directory not found, creating directory: " + sftpWorkingDirectory);
                channelSftp.mkdir(sftpWorkingDirectory);
                channelSftp.cd(sftpWorkingDirectory);
            }

            File f = new File(fileName);
            channelSftp.put(new FileInputStream(f), f.getName());

            logger.debug("Standard Bank file sent");

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
            String date = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

            File file = new File(fileName);
            Files.move(Paths.get(fileName), Paths.get(fileName).resolveSibling("Sent-" + date + "-" + file.getName()));

        } catch (Exception ex) {
            logger.error("Exception found while sending file." + ex);
        } finally {

            channelSftp.exit();
            logger.debug("sftp Channel exited.");
            channel.disconnect();
            logger.debug("Channel disconnected.");
            session.disconnect();
            logger.debug("Host Session disconnected.");
        }
    }

    public void getResponseFiles(String host, int port, String username, String password) throws Exception {

        int sftpPort = port;

        String path = Util.getSysProp("base_dir") + "Standard_Bank/" + this.entityId + "/" + IgnitionConstants.SB_RESPONSE_FILES_FOLDER + "/";

        if (Files.notExists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path));
        }

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        Map<String, ServiceProfile> serviceProfiles = IgnitionUtility.getAllServiceProfilesForGivenEntity(IgnitionConstants.DATA_TABLE_NAME, this.entityId);

        logger.debug("Preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();

            session = jsch.getSession(username, host, sftpPort);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.debug("Connected to host..");

            channel = session.openChannel("sftp");
            channel.connect();
            logger.debug("Connected to channel..");

            channelSftp = (ChannelSftp) channel;

            for (Map.Entry<String, ServiceProfile> serviceProfileMapEntry : serviceProfiles.entrySet()) {

                if (serviceProfileMapEntry.getValue().getServiceProvider().contains(IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK)) {
                    serviceProfile = serviceProfileMapEntry.getValue();
                    String remoteDir = "BankFiles/" + serviceProfile.getFromFIFolderLocation();

                    try {
                        channelSftp.cd(remoteDir);
                    } catch (SftpException e) {
                        logger.debug("Directory not found: " + remoteDir);
                        continue;
                    }

                    logger.debug("Dir Name : " + remoteDir);

                    SftpATTRS attrs = null;
                    try {
                        attrs = channelSftp.stat("/" + remoteDir + "/" + IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    } catch (Exception e) {
                        logger.error(remoteDir + "/%s. Directory not found", IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    }

                    if (attrs != null) {
                        logger.debug("Archive Directory exists: " + attrs.isDir());
                    } else {
                        logger.debug("Creating dir " + remoteDir + "/" + IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                        channelSftp.mkdir(IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    }

                    // Iterate among all files in the directory
                    // to copy all files having date greater than last fetched file
                    Vector<ChannelSftp.LsEntry> list = channelSftp.ls("*");
                    String hostFileName;
                    logger.debug("Downloading files to path : " + path);

                    OutputStream ous;
                    for (ChannelSftp.LsEntry listEntry : list) {
                        try {
                            if (!listEntry.getAttrs().isDir()) {
                                hostFileName = listEntry.getFilename();
                                logger.debug("Host file Name:" + hostFileName);
                                logger.debug("Path of file:" + path);

                                ous = new FileOutputStream(path + hostFileName);
                                channelSftp.get(hostFileName, ous);

                                logger.info("File %s downloaded successfully.", hostFileName);
                                logger.debug("Moving server file %s to Archive Folder.", hostFileName);

                                DateTimeFormatter dateFormatForOutputFile = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                                String processedDate = dateFormatForOutputFile.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));
                                String archivedFileName = "Archive" + processedDate + "-" + hostFileName;

                                channelSftp.rename(hostFileName, IgnitionConstants.IGNITION_ARCHIVE_FOLDER + "//" + archivedFileName);
                                logger.debug("File %s archived successfully.", hostFileName);
                            }
                        } catch (Exception exp) {
                            logger.error("Invalid File:" + exp.getMessage());
                            throw exp;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("Exception while downloading file : " + ex);
            throw ex;
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();

            if (session != null)
                session.disconnect();
        }
    }

    public String getServiceProfileCodeFromFileName(String fileName) {
        String[] list = fileName.split("\\.");

        return list[2];
    }

    public void setServiceProfileUsingFileName(String fileName) {
        String serviceProfileCode = getServiceProfileCodeFromFileName(fileName);
        Map<String, ServiceProfile> serviceProfiles = IgnitionUtility.getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME, ServiceProfile.Names.CODE, serviceProfileCode, entityId);

        for (Map.Entry<String, ServiceProfile> serviceProfileEntry : serviceProfiles.entrySet()) {
            serviceProfile = serviceProfileEntry.getValue();
        }
    }

    public Boolean IsIncrementRequiredInFileSequenceNo(String header) {
        String headerErrorCode = header.substring(86, 88);
        if (headerErrorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_ACB_HEADER_SEQUENCE_NUMBER_INCORRECT) ||
                headerErrorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_INVALID_ACB_CODE) ||
                headerErrorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_INVALID_RECORD_TYPE) ||
                headerErrorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_NOT_LINKED_TO_BEFT_SYSTEM) ||
                headerErrorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_INVALID_CATS_USER_ID)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean IsSequenceNoResetRequired(String fileType, String cutOffTime) throws ParseException {

        Date currentDate = TimezoneHelper.companyCurrentDate(this.entityId);

        if (IgnitionUtility.isCutOffTimeReached(cutOffTime, currentDate)) {
            return true;
        } else {
            File dir = new File(Util.getSysProp("base_dir") + bankFilesDir + File.separator + getEntityId() + File.separator + fileType);
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
            String processedDate = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

            File[] foundFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.contains("Sent-" + processedDate) && name.contains(getFileName()));
                }
            });

            if (foundFiles != null && foundFiles.length > 0) {
                return false;

            }
        }

        return true;
    }

    private String updateHeaderRecord(String lastHeaderRecord) {

        logger.debug("Last header record: " + lastHeaderRecord);

        StringBuilder recordBuilder = new StringBuilder(lastHeaderRecord);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd");
        Date previousLastActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(lastHeaderRecord.substring(24, 30), dateTimeFormatter));

        if (previousLastActionDate.getTime() < actionDate.getTime()) {
            recordBuilder.replace(24, 30, dateTimeFormatter.format(DateConvertUtils.asLocalDateTime(actionDate)));
        }

        String updatedHeaderRecord = recordBuilder.toString();

        logger.debug("Updated header record: " + updatedHeaderRecord);

        return updatedHeaderRecord;
    }

    private void setPaymentInformationDetails(PaymentWS payment) {
        PaymentInformationWS paymentInstrument = payment.getPaymentInstruments().get(0);

        for (MetaFieldValueWS metaFieldValue : paymentInstrument.getMetaFields()) {
            if (metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NAME)) {
                userAccountName = (String) metaFieldValue.getValue();
            } else if (metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NUMBER)) {

                String value = String.valueOf(metaFieldValue.getValue());

                if (metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)) {
                    value = String.valueOf((char[]) metaFieldValue.getValue());
                }

                userAccountNo = value;
            } else if (metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_TYPE)) {

                String value = (String) metaFieldValue.getValue();

                if (value.toUpperCase().contains("CURRENT") || value.toUpperCase().contains("CHEQUE")) {
                    userAccountType = 1;
                } else if (value.toUpperCase().contains("SAVINGS")) {
                    userAccountType = 2;
                } else if (value.toUpperCase().contains("TRANSMISSION")) {
                    userAccountType = 3;
                } else if (value.toUpperCase().contains("BOND")) {
                    userAccountType = 4;
                } else if (value.toUpperCase().contains("SUBSCRIPTION SHARE")) {
                    userAccountType = 5;
                }
            } else if (metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_BRANCH_CODE)) {
                userBranchCode = (String) metaFieldValue.getValue();
            } else if (metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_NEXT_PAYMENT_DATE)) {
                actionDate = metaFieldValue.getDateValue();
            }
        }
    }
}
