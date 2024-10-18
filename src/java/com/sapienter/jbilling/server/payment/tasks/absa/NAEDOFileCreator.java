package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Created by taimoor on 7/11/17.
 */
public class NAEDOFileCreator {

    private NAEDOFileCreator() {
    }

    public static String getUserHeaderRecord(char dataSetStatus, String userCode, Date creationDate, Date purgeDate, Date firstActionDate,
                                      Date lastActionDate, String firstSequenceNumber, String userGenerationNumber,
                                      String typeOfService){

        StringBuilder userHeaderRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        userHeaderRecord.append("050");

        // Data Set Status to choose between Live and Test data
        userHeaderRecord.append(dataSetStatus);

        // Bank Server Record Identifier
        userHeaderRecord.append("04");

        // User Code
        userHeaderRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(creationDate)));
        // Purge Date => Last Action Date
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(purgeDate)));
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(firstActionDate)));
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(lastActionDate)));
        userHeaderRecord.append(String.format("%1.6s", StringUtils.leftPad(firstSequenceNumber,6,'0')));
        userHeaderRecord.append(String.format("%1.4s", StringUtils.leftPad(userGenerationNumber,4,'0')));
        userHeaderRecord.append(String.format("%1.10s", StringUtils.rightPad(typeOfService, 10, ' ')));

        // Accepted Report
        userHeaderRecord.append(' ');
        // Account Type Correct
        userHeaderRecord.append(' ');
        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        userHeaderRecord.append(StringUtils.repeat(" ", 142));

        // Insert Carriage Return
        userHeaderRecord.append("\r");

        return userHeaderRecord.toString();
    }

    public static String getStandardTransactionRecord(char dataSetStatus, IgnitionConstants.NAEDO transactionType, String originatingBranchCode,
                                               String originatingAccountNumber, String userCode, String sequenceNumber, String customerBranchCode,
                                               String customerAccountNumber, IgnitionConstants.AccountType accountType, String amount, Date actionDate,
                                               String entryClass, String userReference, String contractReference, String customerAccountName){

        StringBuilder standardTransactionRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        standardTransactionRecord.append("050");

        // Data Set Status to choose between Live and Test data
        standardTransactionRecord.append(dataSetStatus);

        // BankServ Record Identifier
        standardTransactionRecord.append(transactionType == IgnitionConstants.NAEDO.Naedo_Request ? "55" : "90");

        // Originating Branch(will be Company Branch code)
        standardTransactionRecord.append(String.format("%1.6s", StringUtils.leftPad(originatingBranchCode, 6, '0')));

        // Originating Account Number (Will be Company Account Number)
        standardTransactionRecord.append(String.format("%1.11s", StringUtils.leftPad(originatingAccountNumber, 11, '0')));

        // User Code
        standardTransactionRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        // Sequence Number
        standardTransactionRecord.append(transactionType == IgnitionConstants.NAEDO.Naedo_Request ?
                (StringUtils.leftPad(sequenceNumber, 6, '0')) : StringUtils.repeat("0", 6));

        // Homing Branch (will be Customer Branch code)
        standardTransactionRecord.append(String.format("%1.6s", StringUtils.leftPad(customerBranchCode, 6, '0')));

        // Homing Account Number (Will be Customer Account Number)
        standardTransactionRecord.append(String.format("%1.11s", StringUtils.leftPad(customerAccountNumber, 11, '0')));

        // Account Type
        standardTransactionRecord.append(ABSAFileManager.getTransactionAccountTypeValue(accountType));

        // Installment Amount
        standardTransactionRecord.append(StringUtils.leftPad(amount, 11, '0'));

        // Action Date (Must be within First and Last Action date from the User Record Header)
        // NAEDO Recall: Action Date of Original transaction to be recalled.
        standardTransactionRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(actionDate)));

        // As per fields defined in the NAEDO segment in the file specifications
        standardTransactionRecord.append(entryClass);

        // TAX Code
        standardTransactionRecord.append("0");

        // Filler
        standardTransactionRecord.append(StringUtils.repeat(" ", 3));

        // User Reference - First 2 digits to be registered with BankServ
        standardTransactionRecord.append(String.format("%1.10s", StringUtils.rightPad(userReference, 10, ' ')));

        // Contract or Policy reference
        standardTransactionRecord.append(String.format("%1.14s", StringUtils.rightPad(contractReference, 14, ' ')));

        // Cycle Date = Action Date
        standardTransactionRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(actionDate)));

        // Homing Account Name (Will be Customer Account Name)
        standardTransactionRecord.append(String.format("%1.30s", StringUtils.rightPad(customerAccountName, 30, ' ')));

        // Non-Standard Homing Account Number (Will always be ZERO)
        standardTransactionRecord.append(StringUtils.repeat(" ", 20));

        // Filler
        standardTransactionRecord.append(StringUtils.repeat(" ", 16));

        // Homing Institution
        standardTransactionRecord.append("21");

        // Filler
        standardTransactionRecord.append(StringUtils.repeat(" ", 6));

        // Effective Action Date - Will be inserted by Bank
        standardTransactionRecord.append(StringUtils.repeat(" ", 6));

        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        standardTransactionRecord.append(StringUtils.repeat(" ", 14));

        // Insert Carriage Return
        standardTransactionRecord.append("\r");

        return standardTransactionRecord.toString();
    }

    public static String getUserTrailerRecord(char dataSetStatus, String userCode, String firstSequenceNumber, String lastSequenceNumber, Date firstActionDate, Date lastActionDate, String noOfdebitRecords, String totalDebitValue, String hashTotalOfHomingAccountNos){

        StringBuilder userTrailerRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        userTrailerRecord.append("050");

        // Data Set Status to choose between Live and Test data
        userTrailerRecord.append(dataSetStatus);

        // BankServ Record Identifier - Identifies the record as Trailer
        userTrailerRecord.append("92");

        // User Code
        userTrailerRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        // First Sequence Number - Same as the one on the User Header Record
        // For NAEDO Recalls - will always be ZERO
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(firstSequenceNumber,6,'0')));

        // Last Sequence Number - Same as the Transaction Record preceding this record
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(lastSequenceNumber,6,'0')));

        // First Action Date - Same as the one on the User Header Record
        userTrailerRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(firstActionDate)));

        // Last Action Date - Same as the one on the User Header Record
        userTrailerRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(lastActionDate)));

        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(noOfdebitRecords,6,'0')));

        // No of Credit records - ZERO for NAEDO
        userTrailerRecord.append(StringUtils.repeat("0", 6));

        // No of Contra records - ZERO for NAEDO
        userTrailerRecord.append(StringUtils.repeat("0", 6));

        userTrailerRecord.append(StringUtils.leftPad(totalDebitValue, 12, '0'));

        // Total Credit Value - Will be ZERO for NAEDOs
        userTrailerRecord.append(StringUtils.repeat("0", 12));

        userTrailerRecord.append(String.format("%1.12s", StringUtils.leftPad(hashTotalOfHomingAccountNos, 12, '0')));

        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        userTrailerRecord.append(StringUtils.repeat(" ", 110));

        // Insert Carriage Return
        userTrailerRecord.append("\r");

        return userTrailerRecord.toString();
    }
}
