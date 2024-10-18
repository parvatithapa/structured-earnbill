package com.sapienter.jbilling.server.payment.tasks.absa;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.ignition.responseFile.absa.OutputTransactionRecord;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentReply;
import com.sapienter.jbilling.server.ignition.responseFile.absa.NAEDOPaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.NAEDOResponseRecord;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentResponseEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentSuccessfulEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by taimoor on 7/27/17.
 */
public class ABSAPaymentManager {

    private static final FormatLogger LOG = new FormatLogger(ABSAPaymentManager.class);

    private final Integer entityId;

    private ABSAFileManager absaFileManager;
    private List<Date> holidays;
    private ServiceProfile serviceProfile;

    public ABSAPaymentManager(Integer entityId, List<Date> holidays, ServiceProfile serviceProfile){
        this.entityId = entityId;
        this.holidays = holidays;
        this.serviceProfile = serviceProfile;
    }

    public boolean requestPayment(PaymentWS payment, Integer[] orderIds, UserWS userWS, String userReference) throws Exception {

        absaFileManager = new ABSAFileManager(this.entityId, serviceProfile);

        Date transmissionDate = IgnitionUtility.getActionDate(serviceProfile.getCutOffTime(), TimezoneHelper.companyCurrentDate(this.entityId));

        if(userReference == null && orderIds != null) {
            userReference = getUserReference(userWS, orderIds);
        }

        String customerBranchCode = "";
        String customerAccountNumber = "";
        String customerAccountName = "";
        String customerAccountType = "";
        Date nextPaymentDate = null;

        for(MetaFieldValueWS metaFieldValue : payment.getPaymentInstruments().get(0).getMetaFields()){
            if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NAME)
                    && metaFieldValue.getValue() != null){
                customerAccountName = String.valueOf(metaFieldValue.getValue());
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NUMBER)
                    && metaFieldValue.getValue() != null){
                String value = String.valueOf(metaFieldValue.getValue());

                if(metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)){
                    value = String.valueOf((char[])metaFieldValue.getValue());
                }

                customerAccountNumber = value;
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_BRANCH_CODE)
                    && metaFieldValue.getValue() != null){
                String value = String.valueOf(metaFieldValue.getValue());

                if(metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)){
                    value = String.valueOf((char[])metaFieldValue.getValue());
                }

                customerBranchCode = value;
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_TYPE)
                    && metaFieldValue.getValue() != null){
                customerAccountType = String.valueOf(metaFieldValue.getValue());
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_NEXT_PAYMENT_DATE)
                    && metaFieldValue.getValue() != null){

                nextPaymentDate = metaFieldValue.getDateValue();
            }
        }

        // NOTE: First Action date should be equal to the earliest transaction in the file.
        // Which means we have to include service profile calucation as well.
        Date firstActionDate = nextPaymentDate;
        Date actionDate = firstActionDate;
        Date lastActionDate = actionDate;

        LOG.debug("Action Date:%s, User Reference:%s", actionDate, userReference);

        LOG.debug("Sending request to add transaction in INPUT file");

        //NOTE: serviceProfile.getFileSequenceNo() will be used as Transmission Number
        //NOTE: entryClass value not received
        absaFileManager.addNewTransaction(String.valueOf(serviceProfile.getFileSequenceNo()), serviceProfile.getGenerationNo(), firstActionDate, lastActionDate,
                getTransactionType(payment), customerBranchCode, customerAccountNumber, getAccountType(customerAccountType),
                payment.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), actionDate, "36",
                userReference, customerAccountName, transmissionDate);

        updatePaymentMetafields(payment);

        return true;
    }

    public boolean requestNAEDOPayment(PaymentWS payment, Integer[] orderIds, UserWS userWS, String userReference,
                                       IgnitionConstants.NAEDOPaymentTracking naedoPaymentTracking, IgnitionConstants.NAEDOWorkflowType naedoWorkflowType) throws Exception{

        absaFileManager = new ABSAFileManager(this.entityId, serviceProfile);

        Date transmissionDate = IgnitionUtility.getActionDate(serviceProfile.getCutOffTime(), TimezoneHelper.companyCurrentDate(this.entityId));

        if(userReference == null && orderIds != null) {
            userReference = getUserReference(userWS, orderIds);
        }

        String customerBranchCode = "";
        String customerAccountNumber = "";
        String customerAccountName = "";
        String customerAccountType = "";

        for(MetaFieldValueWS metaFieldValue : payment.getPaymentInstruments().get(0).getMetaFields()){
            if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NAME)
                    && metaFieldValue.getValue() != null){
                customerAccountName = String.valueOf(metaFieldValue.getValue());
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_NUMBER)
                    && metaFieldValue.getValue() != null){
                String value = String.valueOf(metaFieldValue.getValue());

                if(metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)){
                    value = String.valueOf((char[])metaFieldValue.getValue());
                }

                customerAccountNumber = value;
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_BRANCH_CODE)
                    && metaFieldValue.getValue() != null){
                String value = String.valueOf(metaFieldValue.getValue());

                if(metaFieldValue.getMetaField().getDataType().equals(DataType.CHAR)){
                    value = String.valueOf((char[])metaFieldValue.getValue());
                }

                customerBranchCode = value;
            }
            else if(metaFieldValue.getFieldName().equals(IgnitionConstants.PAYMENT_ACCOUNT_TYPE)
                    && metaFieldValue.getValue() != null){
                customerAccountType = String.valueOf(metaFieldValue.getValue());
            }
        }

        Date firstActionDate = transmissionDate;
        Date actionDate = firstActionDate;
        Date lastActionDate = actionDate;

        LOG.debug("Action Date:%s, User Reference:%s", actionDate, userReference);

        LOG.debug("Sending request to add NAEDO transaction in INPUT file");

        //NOTE: serviceProfile.getFileSequenceNo() will be used as Transmission Number
        //NOTE: entryClass value EQUALS incoming NAEDO tracking day
        absaFileManager.addNewNAEDOTransaction(String.valueOf(serviceProfile.getFileSequenceNo()), firstActionDate, lastActionDate, serviceProfile.getGenerationNo(),
                getNAEDOTransactionType("request"), customerBranchCode, customerAccountNumber, getAccountType(customerAccountType),
                payment.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), actionDate, naedoPaymentTracking.getValue(),
                userReference, customerAccountName, userReference, transmissionDate);

        updateNAEDOPaymentMetafields(payment, naedoWorkflowType, naedoPaymentTracking);

        return true;
    }

    private IgnitionConstants.TransactionType getTransactionType(PaymentWS paymentWS){

        IgnitionConstants.TransactionType transactionType = paymentWS.getIsRefund() == 0 ? IgnitionConstants.TransactionType.Debit_Transaction : IgnitionConstants.TransactionType.Credit_Transaction;

        LOG.debug("Transaction Type for Payment is %s", transactionType);

        return transactionType;
    }

    private IgnitionConstants.NAEDO getNAEDOTransactionType(String requestType){

        IgnitionConstants.NAEDO request = requestType.toLowerCase().contains("request") ? IgnitionConstants.NAEDO.Naedo_Request : IgnitionConstants.NAEDO.Naedo_Recall;

        LOG.debug("Request Type for NAEDO Payment is %s", request);

        return request;
    }

    private IgnitionConstants.AccountType getAccountType(String accountType){

        IgnitionConstants.AccountType account_type = IgnitionConstants.AccountType.Account_Type_Current;

        if(accountType.toLowerCase().contains("current"))
            account_type = IgnitionConstants.AccountType.Account_Type_Current;
        else if(accountType.toLowerCase().contains("saving"))
            account_type =  IgnitionConstants.AccountType.Account_Type_Savings;
        else if(accountType.toLowerCase().contains("bond"))
            account_type =  IgnitionConstants.AccountType.Account_Type_Bond;
        else if(accountType.toLowerCase().contains("share"))
            account_type =  IgnitionConstants.AccountType.Account_Type_Subscription_Share;
        else if(accountType.toLowerCase().contains("trans"))
            account_type =  IgnitionConstants.AccountType.Account_Type_Transmission;

        LOG.debug("Account type found for incoming type %s is %s", accountType, account_type);
        return account_type;
    }

    private String getUserReference(UserWS userWS, Integer[] orderIds) {

        String userReference = "";

        if (orderIds == null || orderIds.length == 0) {
            LOG.debug("No incoming OrderIDs to get User Reference from.");

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

        return userReference;
    }

    private void updatePaymentMetafields(PaymentWS paymentWS){

        // NOTE: Have to add Meta-Fields because initially no meta-fields are available in the newly created payment
        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>();

        // Save Transmission Date
        MetaField metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TRANSMISSION_DATE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TRANSMISSION_DATE);
            metaFieldValueWS.setStringValue(absaFileManager.getLastTransmissionDate());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Transaction Sequence Number
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_SEQUENCE_NUMBER);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER);
            metaFieldValueWS.setStringValue(StringUtils.leftPad(String.valueOf(absaFileManager.getSequenceNumber()), 6, '0'));
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save User Reference
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_USER_REFERENCE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_USER_REFERENCE);
            metaFieldValueWS.setStringValue(absaFileManager.getUserReference().trim());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Service Provider on which Payment is Sent
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_SENT_ON);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_SENT_ON);
            metaFieldValueWS.setStringValue(serviceProfile.getName());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Action Date
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_ACTION_DATE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_ACTION_DATE);
            metaFieldValueWS.setStringValue(absaFileManager.getActionDate());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Client Code
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_CLIENT_CODE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_CLIENT_CODE);
            metaFieldValueWS.setStringValue(serviceProfile.getCode());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Type
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TYPE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TYPE);
            metaFieldValueWS.setStringValue(IgnitionConstants.IgnitionPaymentType.EFT.toString());
            metaFieldValueList.add(metaFieldValueWS);
        }

        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        paymentWS.setMetaFields(updatedMetaFieldValueWSArray);
    }

    private void updateNAEDOPaymentMetafields(PaymentWS paymentWS, IgnitionConstants.NAEDOWorkflowType naedoWorkflowType, IgnitionConstants.NAEDOPaymentTracking naedoPaymentTracking){

        // NOTE: Have to add Meta-Fields because initially no meta-fields are available in the newly created NAEDO payment
        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>();

        MetaField metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_ACTION_DATE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_ACTION_DATE);
            metaFieldValueWS.setStringValue(absaFileManager.getActionDate());
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_CONTRACT_REFERENCE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_CONTRACT_REFERENCE);
            metaFieldValueWS.setStringValue(absaFileManager.getContractReference().trim());
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_USER_REFERENCE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_USER_REFERENCE);
            metaFieldValueWS.setStringValue(absaFileManager.getUserReference().trim());
            metaFieldValueList.add(metaFieldValueWS);
        }
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_SENT_ON);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_SENT_ON);
            metaFieldValueWS.setStringValue(serviceProfile.getName());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Client Code
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_CLIENT_CODE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_CLIENT_CODE);
            metaFieldValueWS.setStringValue(serviceProfile.getCode());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment Type
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TYPE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TYPE);
            metaFieldValueWS.setStringValue(IgnitionConstants.IgnitionPaymentType.NAEDO.toString());
            metaFieldValueList.add(metaFieldValueWS);
        }

        // Save Payment NAEDO Type
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_NAEDO_TYPE);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_NAEDO_TYPE);
            metaFieldValueWS.setStringValue(naedoWorkflowType.toString());
            metaFieldValueList.add(metaFieldValueWS);
        }

        //Save Payment Tracking Days
        metaField = new MetaFieldDAS().getFieldByName(this.entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_TRACKING_DAYS);
        if(metaField!=null) {
            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_TRACKING_DAYS);
            metaFieldValueWS.setStringValue(naedoPaymentTracking.toString());
            metaFieldValueList.add(metaFieldValueWS);
        }

        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        paymentWS.setMetaFields(updatedMetaFieldValueWSArray);
    }

    public void sendInputFile(String host, int port, String username, String password){

        File dir = new File(Util.getSysProp("base_dir") + "absa_payments/" + this.entityId);

        ArrayList<File> foundFiles = new ArrayList<>();
        listFiles(Util.getSysProp("base_dir") + "absa_payments/" + this.entityId, foundFiles);

        LOG.debug("Number of ABSA INPUT files found: " + foundFiles.size());

        for (File file : foundFiles) {

            // Ignore If Test Files
            if(!file.getName().endsWith("T")) {
                sendFiles(file.getAbsolutePath(), host, port, username, password);
            }else{
                LOG.debug("Ignoring File: " + file.getName());
            }
        }
    }

    public void getResponseFiles(String host, int port, String username, String password) throws Exception {

        int sftpPort = port;

        String path = Util.getSysProp("base_dir") + "absa_payments/" + this.entityId + "/" + IgnitionConstants.ABSA_RESPONSE_FOLDER + "/";

        if (Files.notExists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path));
        }

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        Map<String, ServiceProfile> serviceProfiles = IgnitionUtility.getAllServiceProfilesForGivenEntity(IgnitionConstants.DATA_TABLE_NAME, this.entityId);

        LOG.debug("Preparing to receive Response files from Server.");
        LOG.debug("Preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();

            session = jsch.getSession(username, host, sftpPort);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            LOG.debug("Connected to host..");

            channel = session.openChannel("sftp");
            channel.connect();
            LOG.debug("Connected to channel..");

            channelSftp = (ChannelSftp) channel;

            for (Map.Entry<String, ServiceProfile> serviceProfileMapEntry :serviceProfiles.entrySet()) {

                if(!serviceProfileMapEntry.getValue().getServiceProvider().contains(IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK)) {
                    serviceProfile = serviceProfileMapEntry.getValue();
                    String remoteDirectory = "BankFiles/" + serviceProfile.getFromFIFolderLocation();
                    try {
                        channelSftp.cd(remoteDirectory);
                    } catch (SftpException e) {
                        LOG.debug("Directory not found: " + remoteDirectory);
                        continue;
                    }

                    LOG.debug("Dir Name : " + remoteDirectory);

                    SftpATTRS attrs=null;
                    try {
                        attrs = channelSftp.stat("/" + remoteDirectory + "/" + IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    } catch (Exception e) {
                        LOG.error(remoteDirectory + "/%s. Directory not found", IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    }

                    if (attrs != null) {
                        LOG.debug("Archive Directory exists: " + attrs.isDir());
                    } else {
                        LOG.debug("Creating dir "+ remoteDirectory + "/" + IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                        channelSftp.mkdir(IgnitionConstants.IGNITION_ARCHIVE_FOLDER);
                    }

                    // Iterate among all files in the directory
                    // to copy all files having date greater than last fetched file
                    Vector<ChannelSftp.LsEntry> list = channelSftp.ls("*.*");
                    String hostFileName;
                    LOG.debug("Downloading files to path : " + path);

                    OutputStream ous;
                    for (ChannelSftp.LsEntry listEntry : list) {
                        try {
                            if(!listEntry.getAttrs().isDir()) {
                                hostFileName = listEntry.getFilename();
                                LOG.debug("Host file Name:" + hostFileName);
                                LOG.debug("Path of file:" + path);

                                ous = new FileOutputStream(path + hostFileName);
                                channelSftp.get(hostFileName, ous);

                                LOG.info("File %s downloaded successfully.", hostFileName);
                                LOG.debug("Moving server file %s to Archive Folder.", hostFileName);

                                DateTimeFormatter dateFormatForOutputFile = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                                String processedDate = dateFormatForOutputFile.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));
                                String archivedFileName = "Archive" + processedDate + "-" +hostFileName;

                                channelSftp.rename(hostFileName, IgnitionConstants.IGNITION_ARCHIVE_FOLDER + "//" +  archivedFileName);
                                LOG.debug("File %s archived successfully.", hostFileName);
                            }
                        } catch (Exception exp) {
                            LOG.error("Invalid File:" + exp.getMessage());
                            LOG.error(exp);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.debug("Exception while downloading file : " + ex);
            throw ex;
        } finally {
            if (channelSftp != null)
                channelSftp.exit();
            if (channel != null)
                channel.disconnect();

            if (session != null)
                session.disconnect();

            LOG.debug("Disconnected host after file copy attempt.");
        }
    }

    /**
     * Process Reply files and updates payment status if transmission was rejected.
     * @throws IOException
     * @throws ParseException
     */
    public void processReplyFile() throws IOException, ParseException {

        LOG.debug("Starting REPLY file processing");

        absaFileManager = new ABSAFileManager(this.entityId, serviceProfile);

        List<PaymentReply> paymentReplies = absaFileManager.processReplyFile();
        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        for(PaymentReply paymentReply : paymentReplies){
            /*if(!paymentReply.getTransmissionStatus().getTransmissionStatus().equals("ACCEPTED"))*/{

                Map<String, String> metaFields = new HashMap<>();
                String transmissionDate = df.format(DateConvertUtils.asLocalDateTime(paymentReply.getTransmissionHeader().getTransmissionDate()));

                metaFields.put(IgnitionConstants.PAYMENT_TRANSMISSION_DATE, transmissionDate);
                metaFields.put(IgnitionConstants.PAYMENT_CLIENT_CODE, paymentReply.getTransmissionStatus().getUserCode());

                LOG.debug("REPLY file is %s for %s transmission date", paymentReply.getTransmissionStatus().getTransmissionStatus(), transmissionDate);
                LOG.debug("REPLY file is for: %s",
                        (paymentReply.getUserSetStatus().getServiceIndicator().equals(IgnitionConstants.ABSAResponseFileType.NAEDO.toString()) ? "NAEDO" : "EFT"));

                // Find all payments for the give transmission date
                List<Integer> paymentIds = new PaymentDAS().findAllPaymentsByMetaFields(metaFields);

                IPaymentSessionBean paymentSessionBean = Context.getBean(Context.Name.PAYMENT_SESSION);

                IgnitionPaymentResponseEvent ignitionPaymentResponseEvent = null;

                if(!paymentReply.getTransmissionStatus().getTransmissionStatus().equals("ACCEPTED")){
                    ignitionPaymentResponseEvent = new IgnitionPaymentResponseEvent(entityId,"REPLY",null,paymentReply.getTransmissionStatus().getUserCode(),
                            IgnitionConstants.PaymentStatus.ABSA_REJECTED,null, paymentIds, IgnitionConstants.SERVICE_PROVIDER_ABSA);
                    ignitionPaymentResponseEvent.setTransmissionDate(transmissionDate);
                }
                else{
                    ignitionPaymentResponseEvent = new IgnitionPaymentResponseEvent(entityId,"REPLY",null,paymentReply.getTransmissionStatus().getUserCode(),
                            IgnitionConstants.PaymentStatus.ABSA_ACCEPTED,null, paymentIds, IgnitionConstants.SERVICE_PROVIDER_ABSA);
                    ignitionPaymentResponseEvent.setTransmissionDate(transmissionDate);

                    // Append LD in user code to match with Service Profile entry
                    String userCode = "LD" + paymentReply.getTransmissionStatus().getUserCode();

                    IgnitionUtility.updateServiceProfile(userCode, entityId, ServiceProfile.Names.FILE_SEQUENCE_NO,
                            ServiceProfile.Names.GENERATION_NO);
                    IgnitionUtility.updateColumnInServiceProfile(userCode, entityId, ServiceProfile.Names.TRANSACTION_NO,
                            String.valueOf((Integer.valueOf(paymentReply.getUserSetStatus().getLastSequenceNumber()) + 1)));
                }

                EventManager.process(ignitionPaymentResponseEvent);
            }
        }
    }

    /**
     * Process Output file and update all payments who's rejections are received.
     * @throws IOException
     * @throws ParseException
     */
    public void processOutputFile() throws IOException, ParseException {

        LOG.debug("Starting OUTPUT file processing");

        absaFileManager = new ABSAFileManager(this.entityId, serviceProfile);

        Map<String, List> paymentOutputsMap = absaFileManager.processOutputFile();
        List<PaymentOutput> paymentOutputsList = paymentOutputsMap.get("EFT");

        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        for(PaymentOutput paymentOutput : paymentOutputsList){
            for(OutputTransactionRecord outputTransactionRecord : paymentOutput.getTransactionRecord()){

                Map<String, String> metaFields = new HashMap<>();
                metaFields.put(IgnitionConstants.PAYMENT_TRANSMISSION_DATE, df.format(DateConvertUtils.asLocalDateTime(outputTransactionRecord.getTransmissionDate())));
                metaFields.put(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER, outputTransactionRecord.getSequenceNumber());
                metaFields.put(IgnitionConstants.PAYMENT_USER_REFERENCE, outputTransactionRecord.getUserReference().trim());

                // Set Payment Status for all the payments with this Transmission to 'FALSE'
                Integer paymentId = new PaymentDAS().findPaymentByMetaFields(metaFields);

                if(paymentId != null) {

                    LOG.debug("Requesting status update for payment: %s", paymentId);
                    LOG.debug("Rejection reason is: %s, Rejection Qualifier is: %s", outputTransactionRecord.getRejectionReason(), outputTransactionRecord.getRejectionQualifier());

                    IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

                    PaymentBL paymentBL = new PaymentBL(paymentId);

                    // Reset Failed Payment count in case of Payment Success
                    if(outputTransactionRecord.getRejectionReason().equals("000")) {

                        updateOrderFailedPaymentCountMetaField(paymentBL.getEntity().getPayment().getBaseUser().getUserId());

                    }else {

                        PaymentWS paymentWS = webServicesSessionBean.getPayment(paymentId);

                        if (paymentWS.getAuthorizationId() != null) {
                            LOG.debug("Payment authorization already exist for payment " + paymentId);
                        }else {

                            // Set Payment Authorization Values
                            PaymentAuthorizationDTO paymentAuthorization = IgnitionUtility.buildPaymentAuthorization(outputTransactionRecord.getRejectionReason(),
                                    outputTransactionRecord.getRejectionQualifier(), IgnitionConstants.SERVICE_PROVIDER_ABSA);
                            PaymentAuthorizationBL paymentAuthorizationBL = new PaymentAuthorizationBL();
                            paymentAuthorizationBL.create(paymentAuthorization, paymentId);

                            List<MetaFieldValueWS> metaFieldValueWSList = new ArrayList<>();
                            metaFieldValueWSList.addAll(Arrays.asList(paymentWS.getMetaFields()));

                            MetaField metaField = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_DATE);
                            Calendar calendar = Calendar.getInstance();
                            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

                            if (metaField != null) {
                                Boolean metaFieldExists = false;

                                for (MetaFieldValueWS metaFieldValueWS : paymentWS.getMetaFields()) {
                                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_DATE)) {
                                        metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(calendar.getTime())));
                                        metaFieldExists = true;
                                    }
                                }

                                if (metaFieldExists == false) {
                                    MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                                    metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_DATE);
                                    metaFieldValueWS.setStringValue(dateFormat.format(DateConvertUtils.asLocalDateTime(calendar.getTime())));
                                    metaFieldValueWSList.add(metaFieldValueWS);
                                }
                            }

                            MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueWSList.size()];
                            metaFieldValueWSList.toArray(updatedMetaFieldValueWSArray);

                            paymentWS.setMetaFields(updatedMetaFieldValueWSArray);
                            paymentWS.setResultId(CommonConstants.PAYMENT_RESULT_FAILED);

                            IgnitionUtility.updateInvoiceAndPaymentWithFailedStatus(paymentWS,webServicesSessionBean);

                            paymentBL.getEntity().getPaymentAuthorizations().add(paymentAuthorizationBL.getEntity());
                        }

                        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

                        IgnitionPaymentFailedEvent ignitionPaymentFailedEvent = new IgnitionPaymentFailedEvent(this.entityId,paymentDTOEx,IgnitionConstants.OUTPUT_FILE);
                        EventManager.process(ignitionPaymentFailedEvent);
                    }
                }
            }
        }

        processNAEDOOutput(paymentOutputsMap.get("NAEDO"));
    }

    /**
     * Process NAEDO Output file and update all payments who's success confirmations are received.
     * @throws IOException
     * @throws ParseException
     */
    public void processNAEDOOutput(List<NAEDOPaymentOutput> paymentOutputsList) throws IOException, ParseException {

        LOG.debug("Starting NAEDO OUTPUT file processing");

        absaFileManager = new ABSAFileManager(this.entityId, serviceProfile);

        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        for(NAEDOPaymentOutput paymentOutput : paymentOutputsList){
            for(NAEDOResponseRecord responseRecord : paymentOutput.getNaedoResponseRecords()){

                Map<String, String> metaFields = new HashMap<>();
                metaFields.put(IgnitionConstants.PAYMENT_ACTION_DATE, df.format(DateConvertUtils.asLocalDateTime(responseRecord.getOriginalActionDate())));
                metaFields.put(IgnitionConstants.PAYMENT_USER_REFERENCE, responseRecord.getUserReference().trim());
                metaFields.put(IgnitionConstants.PAYMENT_CONTRACT_REFERENCE, responseRecord.getContractReference().trim());

                // Set Payment Status for all the payments with this Transmission to 'FALSE'
                Integer paymentId = new PaymentDAS().findPaymentByMetaFields(metaFields);

                if(paymentId != null) {
                    LOG.debug("Requesting status update for payment: %s", paymentId);

                    //IPaymentSessionBean paymentSessionBean = Context.getBean(Context.Name.PAYMENT_SESSION);

                    Integer paymentResult = Constants.RESULT_FAIL;

                    IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

                    PaymentWS paymentWS = webServicesSessionBean.getPayment(paymentId);

                    if (IgnitionConstants.NAEDOResponseCode.TRANSACTION_SUCCESSFUL.equalsName(responseRecord.getResponseCode())
                            || IgnitionConstants.NAEDOResponseCode.RECALL_SUCCESSFUL.equalsName(responseRecord.getResponseCode())) {

                        paymentResult = Constants.RESULT_OK;

                        LOG.debug("Payment:%s, is successful", paymentId);
                    }

                    LOG.debug("Updating payment %s, as incoming NAEDO response is %s", paymentId, responseRecord.getResponseCode());

                    List<MetaFieldValueWS> metaFieldValueWSList = new ArrayList<>();
                    metaFieldValueWSList.addAll(Arrays.asList(paymentWS.getMetaFields()));

                    MetaField metaField = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{EntityType.PAYMENT}, IgnitionConstants.PAYMENT_DATE);
                    Calendar calendar = Calendar.getInstance();

                    if (metaField != null) {
                        Boolean metaFieldExists = false;

                        for (MetaFieldValueWS metaFieldValueWS : paymentWS.getMetaFields()) {
                            if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_DATE)) {
                                metaFieldValueWS.setStringValue(df.format(DateConvertUtils.asLocalDateTime(responseRecord.getOriginalEffectiveDate())));
                                metaFieldExists = true;
                            }
                        }

                        if (metaFieldExists == false) {
                            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                            metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_DATE);
                            metaFieldValueWS.setStringValue(df.format(DateConvertUtils.asLocalDateTime(responseRecord.getOriginalEffectiveDate())));
                            metaFieldValueWSList.add(metaFieldValueWS);
                        }
                    }

                    MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueWSList.size()];
                    metaFieldValueWSList.toArray(updatedMetaFieldValueWSArray);

                    paymentWS.setMetaFields(updatedMetaFieldValueWSArray);
                    paymentWS.setResultId(paymentResult);

                    if(paymentResult == Constants.RESULT_FAIL){

                        IgnitionUtility.updateInvoiceAndPaymentWithFailedStatus(paymentWS,webServicesSessionBean);

                        PaymentBL paymentBL = new PaymentBL(paymentId);

                        if (paymentWS.getAuthorizationId() != null) {
                            LOG.debug("Payment authorization already exist for payment " + paymentId);
                        }else {
                            // Set Payment Authorization Values
                            PaymentAuthorizationDTO paymentAuthorization = IgnitionUtility.buildPaymentAuthorization(responseRecord.getResponseCode(), null,
                                    IgnitionConstants.SERVICE_PROVIDER_ABSA_NAEDO);
                            PaymentAuthorizationBL paymentAuthorizationBL = new PaymentAuthorizationBL();
                            paymentAuthorizationBL.create(paymentAuthorization, paymentId);

                            paymentBL.getEntity().getPaymentAuthorizations().add(paymentAuthorizationBL.getEntity());
                        }

                        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

                        IgnitionPaymentFailedEvent ignitionPaymentFailedEvent = new IgnitionPaymentFailedEvent(this.entityId,paymentDTOEx,IgnitionConstants.OUTPUT_FILE);
                        EventManager.process(ignitionPaymentFailedEvent);

                    }else{

                        webServicesSessionBean.updatePayment(paymentWS);

                        PaymentBL paymentBL = new PaymentBL(paymentId);
                        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

                        // Raise Event to notify Webhooks for success
                        IgnitionPaymentSuccessfulEvent event = new IgnitionPaymentSuccessfulEvent(this.entityId, paymentDTOEx);
                        EventManager.process(event);
                    }
                }
            }
        }
    }

    private void updatePaymentStatus(PaymentWS payment, Integer status){
        payment.setResultId(status);
    }

    public void sendFiles(String fileName, String host, int port, String username, String password) {

        LOG.debug("Preparing to send ABSA File %s to server.", fileName);

        String[] filePath = fileName.split("/"+this.entityId.toString()+"/");
        String clientCode = "LD" + filePath[1].split("/")[0];

        Map<String, ServiceProfile> serviceProfiles = IgnitionUtility.getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME,
                ServiceProfile.Names.CODE, clientCode, this.entityId);

        for(Map.Entry<String, ServiceProfile> servPro: serviceProfiles.entrySet()){
            serviceProfile = servPro.getValue();
        }

        if(serviceProfile == null) {
            LOG.debug("No Service Profile found for Client Code: " + clientCode);

            clientCode = clientCode.substring(clientCode.length() - 5);

            LOG.debug("Re-trying to find the Service Profile for: " + clientCode);

            serviceProfiles = IgnitionUtility.getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME,
                    ServiceProfile.Names.CODE, clientCode, this.entityId);

            if (!serviceProfiles.isEmpty()) {
                serviceProfile = serviceProfiles.entrySet().iterator().next().getValue();
            }

            if (serviceProfile == null) {
                LOG.debug("Still No Service Profile found for Client Code: " + clientCode);
                return;
            }
        }

        String sftpWorkingDirectory = "BankFiles/" + serviceProfile.getToFIFolderLocation();

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        LOG.debug("preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            LOG.debug("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            LOG.debug("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;

            try {
                channelSftp.cd(sftpWorkingDirectory);
            }
            catch (SftpException e ) {
                LOG.debug("Directory not found, creating directory: " + sftpWorkingDirectory);
                channelSftp.mkdir(sftpWorkingDirectory);
                channelSftp.cd(sftpWorkingDirectory);
            }

            File f = new File(fileName);
            channelSftp.put(new FileInputStream(f), f.getName());

            LOG.debug("ABSA File %s sent to server.", fileName);

            File file = new File(fileName);

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
            String date = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

            Files.move(Paths.get(fileName),Paths.get(fileName).resolveSibling("Sent-" + date + "-" + file.getName()));

        } catch (Exception ex) {
            LOG.error("Exception found while sending file.");
            LOG.error(ex);
        }
        finally{

            channelSftp.exit();
            channel.disconnect();
            session.disconnect();
            LOG.debug("Host Session disconnected.");
        }
    }

    private void listFiles(String directoryName, ArrayList<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                if((file.getName().toLowerCase().startsWith("sameday") || file.getName().toLowerCase().startsWith("two day")
                        || file.getName().toLowerCase().startsWith("naedo")) && !file.getName().contains("Sent")) {
                    files.add(file);
                }
            } else if (file.isDirectory()) {
                listFiles(file.getAbsolutePath(), files);
            }
        }
    }

    private void updateOrderFailedPaymentCountMetaField(Integer userId){

        List<OrderDTO> orderDTOList = new OrderDAS().findAllUserByUserId(userId);

        if (CollectionUtils.isEmpty(orderDTOList)) {
            LOG.debug("No Order found for the given User: %s", userId);
            return;
        }

        OrderDTO orderDTO = orderDTOList.get(0);

        Boolean failedPaymentCountMetaFieldFound = false;

        for (MetaFieldValue metaFieldValue : orderDTO.getMetaFields()) {
            if (metaFieldValue.getField().getName().equals(IgnitionConstants.FAILED_PAYMENTS_COUNT)) {

                failedPaymentCountMetaFieldFound = true;

                // Reset Meta-Field Value to ZERO
                metaFieldValue.setValue(0);
                break;
            }
        }

        if (failedPaymentCountMetaFieldFound) {
            // Update Order DTO
            new OrderDAS().save(orderDTO);
        }
    }
}
