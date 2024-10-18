package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.ignition.responseFile.absa.NAEDOPaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentReply;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by taimoor on 7/11/17.
 */
public class ABSAFileManager {

    private static final FormatLogger LOG = new FormatLogger(ABSAFileManager.class);

    private char dataSetStatus = 'T';

    private Integer entityId = null;
    private String baseLocation = null;
    private String baseFolderName = IgnitionConstants.ABSA_BASE_FOLDER;
    private String fileName = null;
    private String naedoFileName = null;
    private String clientCode = null;
    private String userCode = null;
    private String clientName = null;
    private String companyBranchCode = null;
    private String companyAccountNumber = null;
    private String userAbbreviatedName = null;
    private String contraUserReference = null;
    private String typeOfService = null;

    private String lastTransmissionDate = "";
    private String actionDate = "";
    private Integer sequenceNumber = 0;
    private String userReference = "";
    private String contractReference = "";

    public String getLastTransmissionDate() {
        return lastTransmissionDate;
    }

    public String getActionDate() {
        return actionDate;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber + Integer.valueOf(this.serviceProfile.getTransactionNo());
    }

    public String getUserReference() {
        return userReference;
    }

    public String getContractReference() {
        return contractReference;
    }

    private ServiceProfile serviceProfile;

    public ABSAFileManager(Integer entityId, ServiceProfile serviceProfile) {

        this.baseLocation = Util.getSysProp("base_dir") + baseFolderName;
        this.entityId = entityId;

        if(serviceProfile != null) {
            this.serviceProfile = serviceProfile;
            this.dataSetStatus = serviceProfile.isLive() ? 'L' : 'T';
            this.clientCode = serviceProfile.getCode();
            this.userCode = String.valueOf(serviceProfile.getACBUserCode());
            this.clientName = serviceProfile.getUsername();
            this.companyBranchCode = serviceProfile.getBankAccountBranch();
            this.companyAccountNumber = serviceProfile.getBankAccountNumber();
            this.userAbbreviatedName = String.format("%1.10s", StringUtils.rightPad(serviceProfile.getShortName(), 10, ' '));
            this.typeOfService = serviceProfile.getTypesOfDebitServices();
            this.fileName = generateFileName(serviceProfile.getTypesOfDebitServices(), serviceProfile.getCutOffTime());
            this.naedoFileName = generateNAEDOFileName(serviceProfile.getCutOffTime());
            this.contraUserReference = userAbbreviatedName + String.format("%1.20s", StringUtils.rightPad("CONTRA" + this.clientName, 20, ' '));
        }
    }

    // region EFT File Management

    public void addNewTransaction(String transmissionNumber, String userGenerationNumber, Date firstActionDate, Date lastActionDate,
                                  IgnitionConstants.TransactionType transactionType, String customerBranchCode,
                                  String customerAccountNumber, IgnitionConstants.AccountType accountType, BigDecimal amount, Date actionDate,
                                  String entryClass, String userReference, String customerName, Date transmissionDate) throws Exception {

        LOG.debug("Adding a new Transaction in ABSA Input File");

        String filePath = baseLocation + "/" + entityId + "/" + this.clientCode;

        this.userReference = userAbbreviatedName + userReference;

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        // Save Transmission Date
        this.lastTransmissionDate = dateFormat.format(DateConvertUtils.asLocalDateTime(transmissionDate));
        // Save Action Date
        this.actionDate = dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate));

        if (!inputFileExists(filePath, false, transmissionDate)) {

            LOG.debug("Creating a new ABSA INPUT file: " + fileName);

            // Create new empty Input file
            createNewInputFile(filePath, fileName);

            LOG.debug("Adding headers and trailers to the newly created INPUT file");

            // Update/Reset Transmission Number [First Sequence Number]
            selectSequenceNumberOffset(filePath);

            // Add Transmission and User headers/trailers
            addHeadersAndTrailers(transmissionNumber, userGenerationNumber, firstActionDate, lastActionDate, transmissionDate);
        }


        Path path = Paths.get(filePath + "/" + fileName);

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        if(CollectionUtils.isEmpty(lines)){

            LOG.debug("Adding headers and trailers to an existing empty INPUT file");

            // Add Transmission and User headers/trailers
            addHeadersAndTrailers(transmissionNumber, userGenerationNumber, firstActionDate, lastActionDate, transmissionDate);
        }

        lines = IgnitionUtility.appendCarriageFeed(lines);

        LOG.debug("Updating User Header Record with new lastActionDate %s", lastActionDate);

        // Update User Header and Trailer Record if necessary
        updateUserHeaderAndTrailerRecord(lines, lastActionDate);

        // Append new transaction to the file.
        addTransactionAndContraRecord(transactionType, customerBranchCode, customerAccountNumber,
                accountType, amount, actionDate, entryClass, customerName, lines);

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void addHeadersAndTrailers(String transmissionNumber, String userGenerationNumber, Date firstActionDate, Date lastActionDate, Date transmissionDate){

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        Integer firstSequenceNumber = Integer.valueOf(serviceProfile.getTransactionNo());

        // Save Transmission Date
        this.lastTransmissionDate = dateFormat.format(DateConvertUtils.asLocalDateTime(transmissionDate));

        String transmissionHeader = EFTFileCreator.getTransmissionHeaderRecord(dataSetStatus, transmissionDate,
                clientCode, clientName, String.valueOf(Integer.valueOf(transmissionNumber) + 1));

        String userHeader = EFTFileCreator.getUserHeaderRecord(dataSetStatus, userCode, TimezoneHelper.companyCurrentDate(entityId),
                firstActionDate, lastActionDate, String.valueOf(firstSequenceNumber), String.valueOf(Integer.valueOf(userGenerationNumber) + 1), typeOfService);

        // Initially the number of credit/debit/contra records and total Credit/Debit values will be ZERO
        String userTrailer = EFTFileCreator.getUserTrailerRecord(dataSetStatus, userCode, String.valueOf(firstSequenceNumber), String.valueOf(firstSequenceNumber), firstActionDate, lastActionDate,
                "0", "0", "0", "0", "0", "0");

        // Initially there are only four records i.e. Transmission/User header AND Transmission/User trailer
        String transmissionTrailer = EFTFileCreator.getTransmissionTrailerRecord(dataSetStatus, "4");

        // Write Data
        write(baseLocation + "/" + entityId + "/"+ clientCode + "/"  + fileName, transmissionHeader, userHeader, userTrailer, transmissionTrailer);
    }

    private void addTransactionAndContraRecord(IgnitionConstants.TransactionType transactionType, String customerBranchCode,
                                               String customerAccountNumber, IgnitionConstants.AccountType accountType, BigDecimal amount, Date actionDate,
                                               String entryClass, String customerName, List<String> lines) throws IOException {


        String accountNumber = "0";
        String nonStandardAccountNumber = "0";

        if(customerAccountNumber.length() < 12){
            accountNumber = customerAccountNumber;
        }else{
            nonStandardAccountNumber = customerAccountNumber;
        }

        int position = getTransactionRecordLocation(lines, actionDate, transactionType);

        String transactionRecord = EFTFileCreator.getStandardTransactionRecord(dataSetStatus, transactionType, companyBranchCode,
                companyAccountNumber, userCode, String.valueOf(this.sequenceNumber + Integer.valueOf(serviceProfile.getTransactionNo())),
                customerBranchCode, accountNumber, accountType, String.valueOf(amount).replace(".", ""), actionDate, entryClass, userReference,
                customerName, nonStandardAccountNumber);

        lines.add(position, transactionRecord);

        LOG.debug("Adding incoming transaction details to file");

        String accountNumbersSum = sumAccountNumbers(accountNumber, nonStandardAccountNumber);

        addUpdateContraRecord(lines, transactionType, amount, actionDate, contraUserReference, (position + 1), accountNumbersSum);
    }

    private void addUpdateContraRecord(List<String> lines, IgnitionConstants.TransactionType transactionType, BigDecimal amount,
                                       Date actionDate, String contraUserReference, int contraRecordLocation, String accountNumbersSum) throws IOException {

        boolean newRecord = true;

        String oldContraRecord = lines.get(contraRecordLocation);
        String oldMonetaryValue = "0";

        // Get existing monetary value in contra record
        if(oldContraRecord.substring(4, 6).equals(transactionType.equals(IgnitionConstants.TransactionType.Credit_Transaction)? "12" : "52")) {

            newRecord = false;
            // Insert Decimal Point for last to digits
            oldMonetaryValue = oldContraRecord.substring(51, 60) + "." + oldContraRecord.substring(60, 62);

        }else {
            accountNumbersSum = sumAccountNumbers(accountNumbersSum, companyAccountNumber);
        }

        // Update Monetary Value
        BigDecimal totalAmount = new BigDecimal(oldMonetaryValue).add(amount).setScale(2, BigDecimal.ROUND_HALF_UP);

        String contraRecord = EFTFileCreator.getContraRecord(dataSetStatus, transactionType, companyBranchCode,
                companyAccountNumber, userCode, String.valueOf(this.sequenceNumber + Integer.valueOf(serviceProfile.getTransactionNo()) +1),
                actionDate, contraUserReference, String.valueOf(totalAmount).replace(".", ""));

        // Remove Existing Contra Record
        if(!newRecord) {
            lines.remove(contraRecordLocation);
        }

        LOG.debug("Updating CONTRA record in the INPUT file.");

        // Add Updated Contra Record
        lines.add(contraRecordLocation, contraRecord);

        updateUserTrailerRecord(lines, amount, transactionType, newRecord, accountNumbersSum);
    }

    private void updateUserTrailerRecord(List<String> lines, BigDecimal newAmount, IgnitionConstants.TransactionType transactionType,
                                         boolean newContra, String accountNumbersSum){

        String existingUserRecord = lines.get(lines.size() - 2);

        Date firstActionDate = null;
        Date lastActionDate = null;

        String debitRecords = "0";
        String creditRecords = "0";
        String contraRecords = "0";
        String debitValue = "0";
        String creditValue = "0";

        String hashTotal = "0";

        String firstSequenceNumber = existingUserRecord.substring(10, 16);

        // Get existing values in User Trailer Record
        if(existingUserRecord.substring(4, 6).equals("92")) {

            DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

            firstActionDate = DateConvertUtils.asUtilDate(LocalDate.parse((existingUserRecord.substring(22, 28)), df));
            lastActionDate = DateConvertUtils.asUtilDate(LocalDate.parse((existingUserRecord.substring(28, 34)), df));

            debitRecords = existingUserRecord.substring(34, 40);
            creditRecords = existingUserRecord.substring(40, 46);
            contraRecords = existingUserRecord.substring(46, 52);
            debitValue = existingUserRecord.substring(52, 62) + "." + existingUserRecord.substring(62, 64);
            creditValue = existingUserRecord.substring(64, 74) + "." + existingUserRecord.substring(74, 76);

            hashTotal = existingUserRecord.substring(76, 88);
        }

        if(transactionType.equals(IgnitionConstants.TransactionType.Debit_Transaction)){
            debitRecords = String.format("%.0f", (Double.parseDouble(debitRecords) + 1));
            debitValue = String.valueOf((new BigDecimal(debitValue).add(newAmount).setScale(2, BigDecimal.ROUND_HALF_UP)));

            // Add Debit Contra Record Sum [which will be equal to sum of debit values] into Credit Value total
            creditValue = String.valueOf(new BigDecimal(creditValue).add(newAmount).setScale(2, BigDecimal.ROUND_HALF_UP));
        }else{
            creditRecords = String.format("%.0f", (Double.parseDouble(creditRecords) + 1));
            creditValue = String.valueOf((new BigDecimal(creditValue).add(newAmount).setScale(2, BigDecimal.ROUND_HALF_UP)));

            // Add Credit Contra Record Sum [which will be equal to sum of credit values] into Debit Value total
            debitValue = String.valueOf(new BigDecimal(debitValue).add(newAmount).setScale(2, BigDecimal.ROUND_HALF_UP));
        }

        if (newContra){
            contraRecords = String.format("%.0f", (Double.parseDouble(contraRecords) + 1));

            // Debit Contra Records are added to Credit Transactions Count and Vise Versa
            if(transactionType.equals(IgnitionConstants.TransactionType.Debit_Transaction)){
                creditRecords = String.format("%.0f", (Double.parseDouble(creditRecords) + 1));
            }else{
                debitRecords = String.format("%.0f", (Double.parseDouble(debitRecords) + 1));
            }
        }

        // Add new account numbers sum value to existing hash total
        hashTotal = sumAccountNumbers(hashTotal, accountNumbersSum);

        // Truncate hash total to least significant 11 digits
        String hashTotalTruncated = truncateHashTotal(hashTotal);

        // Create updated user trailer record
        String userTrailer = EFTFileCreator.getUserTrailerRecord(dataSetStatus, userCode, firstSequenceNumber,
                String.valueOf(this.sequenceNumber + Integer.valueOf(serviceProfile.getTransactionNo()) +1), firstActionDate, lastActionDate,
                debitRecords, creditRecords, contraRecords, debitValue.replace(".", ""), creditValue.replace(".", ""), hashTotalTruncated);

        // Remove Existing User Trailer Record
        lines.remove(lines.size() - 2);

        LOG.debug("Updating User Trailer Record");

        // Add Updated User Trailer Record
        lines.add(lines.size() - 1, userTrailer);

        // Update Transmission Trailer Record
        updateTransmissionTrailerRecord(lines);
    }

    // endregion

    // region NAEDO File Management

    public void addNewNAEDOTransaction(String transmissionNumber, Date firstActionDate, Date lastActionDate,
                                       String userGenerationNumber, IgnitionConstants.NAEDO transactionType,
                                       String customerBranchCode, String customerAccountNumber, IgnitionConstants.AccountType accountType, BigDecimal amount,
                                       Date actionDate, String entryClass, String userReference, String customerName, String contractReference, Date transmissionDate) throws Exception{

        LOG.debug("Adding a new Transaction in ABSA NAEDO.Input File");

        String filePath = baseLocation + "/" + entityId + "/" + this.clientCode;

        this.userReference = userAbbreviatedName;
        this.contractReference = contractReference;

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        // Save Transmission Date
        this.lastTransmissionDate = dateFormat.format(DateConvertUtils.asLocalDateTime(transmissionDate));
        // Save Action Date
        this.actionDate = dateFormat.format(DateConvertUtils.asLocalDateTime(actionDate));

        if (!inputFileExists(filePath, true, transmissionDate)) {

            LOG.debug("Creating a new ABSA NAEDO.INPUT file: " + this.naedoFileName);

            // Create new empty Input file
            createNewInputFile(filePath, this.naedoFileName);

            LOG.debug("Adding headers and trailers to the newly created NAEDO.INPUT file");

            // Update/Reset Transmission Number [First Sequence Number]
            selectSequenceNumberOffset(filePath);

            // Add Transmission and User headers/trailers
            addNAEDOHeadersAndTrailers(transmissionNumber, firstActionDate, lastActionDate, userGenerationNumber, typeOfService, transmissionDate);
        }

        Path path = Paths.get(filePath + "/" + naedoFileName);

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        if(CollectionUtils.isEmpty(lines)){

            LOG.debug("Adding headers and trailers to an existing empty NAEDO.INPUT file");

            // Add Transmission and User headers/trailers
            addNAEDOHeadersAndTrailers(transmissionNumber, firstActionDate, lastActionDate, userGenerationNumber, typeOfService, transmissionDate);
        }

        lines = IgnitionUtility.appendCarriageFeed(lines);
        
        LOG.debug("Updating User Header Record with new lastActionDate %s", lastActionDate);

        // Update User Header and Trailer Record if necessary
        updateUserHeaderAndTrailerRecord(lines, lastActionDate);

        // Add new NAEDO Transaction to the file
        addNAEDOTransactionRecord(transactionType, customerBranchCode, accountType, amount, actionDate, entryClass,
                customerName, customerAccountNumber, contractReference, lines);

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void addNAEDOHeadersAndTrailers(String transmissionNumber, Date firstActionDate, Date lastActionDate, String userGenerationNumber, String typeOfService, Date transmissionDate){

        Integer firstSequenceNumber = Integer.valueOf(serviceProfile.getTransactionNo());

        // Transmission Header for NAEDO is in the same format as EFT
        String transmissionHeader = EFTFileCreator.getTransmissionHeaderRecord(dataSetStatus, transmissionDate, clientCode, clientName, transmissionNumber);

        String userHeader = NAEDOFileCreator.getUserHeaderRecord(dataSetStatus, userCode, TimezoneHelper.companyCurrentDate(entityId), lastActionDate,
                firstActionDate, lastActionDate, String.valueOf(firstSequenceNumber), userGenerationNumber, typeOfService);

        // Initially the number of credit/debit records values will be ZERO
        String userTrailer = NAEDOFileCreator.getUserTrailerRecord(dataSetStatus, userCode, String.valueOf(firstSequenceNumber),
                String.valueOf(firstSequenceNumber), firstActionDate, lastActionDate, "0", "0", "0");

        // Initially there are only four records i.e. Transmission/User header AND Transmission/User trailer
        String transmissionTrailer = EFTFileCreator.getTransmissionTrailerRecord(dataSetStatus, "4");

        // Write Data
        write(baseLocation + "/" + entityId + "/" + clientCode + "/" + naedoFileName, transmissionHeader, userHeader, userTrailer, transmissionTrailer);
    }

    private void addNAEDOTransactionRecord(IgnitionConstants.NAEDO transactionType, String customerBranchCode,
                                           IgnitionConstants.AccountType accountType, BigDecimal amount, Date actionDate, String entryClass,
                                           String customerName, String customerAccountNumber, String contractReference, List<String> lines) throws IOException{

        String accountNumber = "0";
        String nonStandardAccountNumber = "0";

        if(customerAccountNumber.length() < 12){
            accountNumber = customerAccountNumber;
        }else{
            nonStandardAccountNumber = customerAccountNumber;
        }

        int position = getNAEDOTransactionRecordLocation(lines);

        String transactionRecord = NAEDOFileCreator.getStandardTransactionRecord(dataSetStatus, transactionType, companyBranchCode,
                companyAccountNumber, userCode, String.valueOf(this.sequenceNumber + Integer.valueOf(serviceProfile.getTransactionNo())), customerBranchCode, accountNumber, accountType,
                String.valueOf(amount).replace(".", ""), actionDate, entryClass, userReference, contractReference, customerName);


        LOG.debug("Adding incoming NAEDO details to file");

        lines.add(position, transactionRecord);

        // Update NAEDO User Trailer Record
        updateNAEDOUserTrailerRecord(lines, amount, (customerAccountNumber.length() < 12)? accountNumber : nonStandardAccountNumber);
    }

    private void updateNAEDOUserTrailerRecord(List<String> lines, BigDecimal newAmount, String accountNumbersSum){

        String existingUserRecord = lines.get(lines.size() - 2);

        Date firstActionDate = null;
        Date lastActionDate = null;

        String debitRecords = "0";
        String debitValue = "0";

        String hashTotal = "0";

        String firstSequenceNumber = existingUserRecord.substring(10, 16);

        // Get existing values in User Trailer Record
        if(existingUserRecord.substring(4, 6).equals("92")) {

            DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

            firstActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(existingUserRecord.substring(22, 28), df));
            lastActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(existingUserRecord.substring(28, 34), df));

            debitRecords = existingUserRecord.substring(34, 40);
            debitValue = existingUserRecord.substring(52, 62) + "." + existingUserRecord.substring(62, 64);

            hashTotal = existingUserRecord.substring(76, 88);
        }

        debitRecords = String.format("%.0f", (Double.parseDouble(debitRecords) + 1));
        debitValue = String.valueOf((new BigDecimal(debitValue).add(newAmount))).replace(".", "");

        // Add new account numbers sum value to existing hash total
        hashTotal = sumAccountNumbers(hashTotal, accountNumbersSum);

        // Truncate hash total to least significant 11 digits
        String hashTotalTruncated = truncateHashTotal(hashTotal);

        // Create updated user trailer record
        String userTrailer = NAEDOFileCreator.getUserTrailerRecord(dataSetStatus, userCode, firstSequenceNumber,
                String.valueOf(this.sequenceNumber + Integer.valueOf(serviceProfile.getTransactionNo())), firstActionDate, lastActionDate,
                debitRecords, debitValue, hashTotalTruncated);

        // Remove Existing User Trailer Record
        lines.remove(lines.size() - 2);

        LOG.debug("Updating NAEDO User Trailer Record");

        // Add Updated User Trailer Record
        lines.add(lines.size() - 1, userTrailer);

        // Update Transmission Trailer Record
        updateTransmissionTrailerRecord(lines);
    }

    // endregion

    // region Response File Management

    public List<PaymentReply> processReplyFile() throws IOException, ParseException {

        List<PaymentReply> paymentReplies = new ArrayList<>();

        File dir = new File(baseLocation + "/" + entityId + "/" + IgnitionConstants.ABSA_RESPONSE_FOLDER);

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("REPLY.");
            }
        });

        if(foundFiles != null) {
            Path path = null;
            String fileName = null;
            for (File file : foundFiles) {
                List<String> lines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.UTF_8);

                paymentReplies.add(ReplyFileParser.parseReplyFile(lines));
                path = Paths.get(dir + File.separator + file.getName());

                if(null != path) {
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                    String date = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

                    Files.move(path, path.resolveSibling("Done-" + date + "-" + file.getName()));
                }
            }
        }

        return paymentReplies;
    }

    public Map<String,List> processOutputFile() throws IOException, ParseException {

        List<PaymentOutput> paymentOutputs = new ArrayList<>();
        List<NAEDOPaymentOutput> naedoPaymentOutputs = new ArrayList<>();
        Map<String, List> outputs = new HashMap<>();

        File dir = new File(baseLocation + "/" + entityId + "/" + IgnitionConstants.ABSA_RESPONSE_FOLDER);

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("OUTPUT.");
            }
        });

        if(foundFiles != null) {
            for (File file : foundFiles) {
                Path path = null;
                List<String> lines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.UTF_8);

                if (lines.get(1).substring(0,3).equals("010")) {

                    paymentOutputs.add(OutputFileParser.parseOutputFile(lines));

                }else if(lines.get(1).substring(0,3).equals("050")) {

                    naedoPaymentOutputs.add(NAEDOOutputFilerParser.parseOutputFile(lines));
                }

                path = Paths.get(dir + File.separator + file.getName());

                if(null != path) {
                    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_PROCESSED_FILE_NAME_DATE_FORMAT);
                    String date = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

                    Files.move(path, path.resolveSibling("Done-" + date + "-" + file.getName()));
                }
            }
        }

        outputs.put("EFT",paymentOutputs);
        outputs.put("NAEDO",naedoPaymentOutputs);

        return outputs;
    }

    public List<NAEDOPaymentOutput> processNAEDOOutputFile() throws IOException, ParseException {

        List<NAEDOPaymentOutput> naedoPaymentOutputs = new ArrayList<>();

        File dir = new File(baseLocation + "/" + entityId + "/" + IgnitionConstants.ABSA_RESPONSE_FOLDER);

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("NAEDO.OUTPUT.");
            }
        });

        if(foundFiles != null) {
            for (File file : foundFiles) {
                List<String> lines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.UTF_8);

                naedoPaymentOutputs.add(NAEDOOutputFilerParser.parseOutputFile(lines));
            }
        }

        return naedoPaymentOutputs;
    }

    // endregion

    private void updateUserHeaderAndTrailerRecord(List<String> lines, Date lastActionDate) throws ParseException {

        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
        String currentUserHeader = lines.get(1);
        String currentUserTrailer = lines.get(lines.size() - 2);
        String currentLastActionDate = currentUserHeader.substring(28, 34);

        Date currentActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(currentLastActionDate, df));

        if(lastActionDate.compareTo(currentActionDate) > 0){
            currentLastActionDate = df.format(DateConvertUtils.asLocalDateTime(lastActionDate));

            // Update Last Action Date in USER HEADER record
            String userHeader = currentUserHeader.substring(0, 28) + currentLastActionDate + currentUserHeader.substring(34);

            // Update Last Action Date in USER TRAILER record
            String userTrailer = currentUserTrailer.substring(0, 28) + currentLastActionDate + currentUserTrailer.substring(34);

            // Insert Update USER HEADER record
            lines.remove(1);
            lines.add(1, userHeader);

            // Insert Update USER TRAILER record
            lines.remove(lines.size() - 2);
            lines.add(lines.size() - 1, userTrailer);
        }
    }

    private void updateTransmissionTrailerRecord(List<String> lines){

        // Update Transmission Trailer as the new records are being added
        String transmissionTrailer = EFTFileCreator.getTransmissionTrailerRecord(dataSetStatus, String.valueOf(lines.size()));

        lines.remove(lines.size() - 1);
        lines.add(transmissionTrailer);
    }

    /**
     * Creates a new file at the given location with provided filename.
     * @param basePath
     * @param fileName
     * @return
     */
    public void createNewInputFile(String basePath, String fileName) throws Exception{

        Path directory = Paths.get(basePath);
        Path file = Paths.get(basePath + "/" + fileName);

        try {
            // Create Directory if doesn't exist
            if(!Files.exists(directory)){
                Files.createDirectories(directory);
            }

            // Create new file
            Files.createFile(file);

        } catch (IOException exception) {
            LOG.debug("Unable to create Input file at %s, exception is: %s", file, exception);
            throw  exception;
        }
    }

    /**
     * Checks if the input file already exists at the given location.
     * @param basePath
     * @return
     */
    public boolean inputFileExists(String basePath, Boolean isNaedo, Date transmissionDate){

        boolean fileExists = false;

        File dir = new File(basePath + "/");

        String startName = getFileNameStartingValue(isNaedo, transmissionDate);

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(startName);
            }
        });

        if(foundFiles != null && foundFiles.length > 0) {

            if (foundFiles.length > 1) {

                LOG.debug("Multiple INPUT files found generated at same day: " + foundFiles.length + ". Starting with: " + startName);
            }

            fileExists = true;

            if (isNaedo) {
                this.naedoFileName = foundFiles[0].getName();
            } else {
                this.fileName = foundFiles[0].getName();
            }

            LOG.debug("Input file selected: %s", this.fileName);
        }

        LOG.debug("Input file at %s, %s", basePath, fileExists ? "Exists." : "Don't Exist.");

        return fileExists;
    }

    /**
     * Generates input file name based on the ABSA Format.
     * @return
     * @param typesOfDebitServices
     * @param cutOffTime
     */
    public String generateFileName(String typesOfDebitServices, String cutOffTime){

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_OUTPUT_FILE_NAME_DATE_FORMAT);

        Date cutOffDate = null;
        try {
            cutOffDate = IgnitionUtility.getActionDate(cutOffTime, TimezoneHelper.companyCurrentDate(this.entityId));
        } catch (ParseException e) {
            LOG.debug("Exception occurred while trying to get date based on cut-off time, current date will be used.");
            LOG.debug(e);
        }

        String date = dateFormat.format(DateConvertUtils.asLocalDateTime(cutOffDate == null ? TimezoneHelper.companyCurrentDate(this.entityId) : cutOffDate));

        String fileName =  typesOfDebitServices + date + (dataSetStatus == 'T'? "T" : "");

        return fileName;
    }

    /**
     * Generates NAEDO input file name based on the ABSA Format.
     * @return
     */
    public String generateNAEDOFileName(String cutOffTime){

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        Date cutOffDate = null;
        try {
            cutOffDate = IgnitionUtility.getActionDate(cutOffTime, TimezoneHelper.companyCurrentDate(this.entityId));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String date = dateFormat.format(DateConvertUtils.asLocalDateTime(cutOffDate == null ? TimezoneHelper.companyCurrentDate(this.entityId) : cutOffDate));

        String fileName = "NAEDO" + date + (dataSetStatus == 'T'? "T" : "");

        return fileName;
    }

    public static char getTransactionAccountTypeValue(IgnitionConstants.AccountType accountType){

        char accountTypeValue = '1';

        switch (accountType){

            case Account_Type_Current:
                accountTypeValue = '1';
                break;

            case Account_Type_Savings:
                accountTypeValue =  '2';
                break;

            case Account_Type_Transmission:
                accountTypeValue =  '3';
                break;

            case Account_Type_Bond:
                accountTypeValue =  '4';
                break;

            case Account_Type_Subscription_Share:
                accountTypeValue =  '6';
                break;

            default: break;
        }

        return accountTypeValue;
    }

    private int getTransactionRecordLocation(List<String> lines, Date actionDate, IgnitionConstants.TransactionType transactionType){

        int location = -1;

        // If starting a new file with no contra record
        if(lines.size() == 4) {
            location = lines.size() - 2;
            this.sequenceNumber = 0;
        }
        else {

            // Right after the user header if no criteria bellow are met
            location = 2;

            SortedMap<Date, Integer> existingContraRecords = new TreeMap<>();

            // Get Existing Contra Records
            for(int i = 0; i < lines.size(); i++){
                String line = lines.get(i);
                if(line.substring(4, 6).equals(transactionType.equals(IgnitionConstants.TransactionType.Credit_Transaction)? "12" : "52")){

                    DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
                    Date firstActionDate = null;
                    firstActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(line.substring(62, 68), df));

                    existingContraRecords.put(firstActionDate, i);
                }
            }

            boolean useExistingBlock = false;
            for (Map.Entry<Date, Integer> line : existingContraRecords.entrySet()) {

                if(actionDate.compareTo(line.getKey()) == 0){
                    location = line.getValue();
                    useExistingBlock = true;
                    break;
                }
                else if(actionDate.compareTo(line.getKey()) == -1){
                    continue;
                }else {
                    location = line.getValue() + 1;
                }
            }

            this.sequenceNumber = (lines.size() - (useExistingBlock ? 1 : 0) - 4);
        }

        return location;
    }

    private int getNAEDOTransactionRecordLocation(List<String> lines){
        int location = -1;

        // If starting a new file
        if(lines.size() == 4) {
            location = 2;
            this.sequenceNumber = 0;
        }
        else {

            // Right before the user trailer
            location = lines.size() - 2;
            this.sequenceNumber = (lines.size() - 4);
        }

        return location;
    }

    private String sumAccountNumbers(String homingAccountNumber, String nonStandardHomingAccount){
        Double accountNumbersSum = 0d;

        if(!StringUtils.isEmpty(homingAccountNumber)) {
            accountNumbersSum += Double.parseDouble(homingAccountNumber);
        }

        if(!StringUtils.isEmpty(nonStandardHomingAccount)) {

            if (nonStandardHomingAccount.length() > 11){
                int startIndex = nonStandardHomingAccount.length() - 11;

                nonStandardHomingAccount = nonStandardHomingAccount.substring(startIndex);
            }

            accountNumbersSum += Double.parseDouble(nonStandardHomingAccount);
        }

        return String.format("%.0f", accountNumbersSum);
    }

    private String truncateHashTotal(String hashTotal){

        String hashTotalTruncated = hashTotal;

        if (hashTotal.length() > 11){
            int startIndex = hashTotal.length() - 11;

            hashTotalTruncated = hashTotal.substring(startIndex);
        }

        return hashTotalTruncated;
    }

    private void write(String filePath, String... lines) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            for (String line : lines) {
                fileWriter.append(line).append("\n");
            }

            LOG.debug("file wrote " + filePath);

        } catch (IOException e) {
            LOG.error("Exception thrown during write!", e);
        }
    }

    private String getFileNameStartingValue(Boolean isNaedo, Date transmissionDate){
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        String date = dateFormat.format(DateConvertUtils.asLocalDateTime(transmissionDate));

        if(isNaedo) {
            return "NAEDO" + date;
        }
        return this.typeOfService + date;
    }

    private void selectSequenceNumberOffset(String basePath) throws ParseException {

        Date currentDate = TimezoneHelper.companyCurrentDate(entityId);

        if(IgnitionUtility.isCutOffTimeReached(serviceProfile.getCutOffTime(), currentDate)){
            // Reset Transaction Number in Service Profile
            // Append LD in user code to match with Service Profile entry
            String userCode = "LD" + serviceProfile.getCode();
            IgnitionUtility.updateColumnInServiceProfile(userCode, entityId, ServiceProfile.Names.TRANSACTION_NO, "1");

            serviceProfile.setTransactionNo("1");

            return;
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
        String stringCurrentDate = dateFormat.format(DateConvertUtils.asLocalDateTime(TimezoneHelper.companyCurrentDate(entityId)));

        boolean fileExists = false;

        File dir = new File(basePath + "/");

        String startName = "Sent-" + stringCurrentDate;

        File[] foundFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(startName);
            }
        });

        if(foundFiles != null && foundFiles.length > 0) {

            if (foundFiles.length > 1) {

                LOG.debug("Multiple INPUT files found Processed at same day: " + foundFiles.length + ". Starting with: " + startName);
            }

            fileExists = true;
        }

        if(!fileExists){
            // Reset Transaction Number in Service Profile
            // Append LD in user code to match with Service Profile entry
            String userCode = "LD" + serviceProfile.getCode();
            IgnitionUtility.updateColumnInServiceProfile(userCode, entityId, ServiceProfile.Names.TRANSACTION_NO, "1");

            serviceProfile.setTransactionNo("1");
        }
    }
}
