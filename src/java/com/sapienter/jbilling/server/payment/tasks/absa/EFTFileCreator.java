package com.sapienter.jbilling.server.payment.tasks.absa;


import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Created by taimoor on 7/11/17.
 */
public class EFTFileCreator {

    private EFTFileCreator() {
    }

    public static String getTransmissionHeaderRecord(char dataSetStatus, Date transmissionDate, String clientCode, String clientName, String transmissionNumber){

        StringBuilder transmissionHeaderRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        // Record Identifier
        transmissionHeaderRecord.append("000");

        // Data Set Status to choose between Live and Test data
        transmissionHeaderRecord.append(dataSetStatus);

        transmissionHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(transmissionDate)));
        transmissionHeaderRecord.append(String.format("%1.5s", StringUtils.leftPad(clientCode, 5, '0')));
        transmissionHeaderRecord.append(String.format("%1.30s", StringUtils.rightPad(clientName, 30, ' ')));
        transmissionHeaderRecord.append(String.format("%1.7s", StringUtils.leftPad(transmissionNumber, 7, '0')));

        // Transmission Destination
        transmissionHeaderRecord.append(StringUtils.repeat("0", 5));

        // Filler
        transmissionHeaderRecord.append(StringUtils.repeat(" ", 119));

        // For User's own use
        transmissionHeaderRecord.append(StringUtils.repeat(" ", 20));

        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.

        // Insert Carriage Return
        transmissionHeaderRecord.append("\r");

        return transmissionHeaderRecord.toString();
    }

    public static String getTransmissionTrailerRecord(char dataSetStatus, String numberOfTransmissionRecords){

        StringBuilder transmissionTrailerRecord = new StringBuilder();

        // Record Identifier
        transmissionTrailerRecord.append("999");

        // Data Set Status to choose between Live and Test data
        transmissionTrailerRecord.append(dataSetStatus);

        // Total number of transmission records, it includes Transmission Header and Trailer records as well
        transmissionTrailerRecord.append(String.format("%1.9s", StringUtils.leftPad(numberOfTransmissionRecords, 9, '0')));

        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        transmissionTrailerRecord.append(StringUtils.repeat(" ", 185));

        // Insert Carriage Return
        transmissionTrailerRecord.append("\r");

        return transmissionTrailerRecord.toString();
    }

    public static String getUserHeaderRecord(char dataSetStatus, String userCode, Date creationDate, Date firstActionDate,
                                      Date lastActionDate, String firstSequenceNumber, String userGenerationNumber, String typeOfService){

        StringBuilder userHeaderRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        userHeaderRecord.append("001");

        // Data Set Status to choose between Live and Test data
        userHeaderRecord.append(dataSetStatus);

        // Bank Server Record Identifier
        userHeaderRecord.append("04");

        // User Code
        userHeaderRecord.append(StringUtils.rightPad(userCode, 4, ' '));

        //TimezoneHelper.companyCurrentDate();
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(creationDate)));
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(lastActionDate)));
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(firstActionDate)));
        userHeaderRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(lastActionDate)));
        userHeaderRecord.append(String.format("%1.6s", StringUtils.leftPad(firstSequenceNumber,6, '0')));
        userHeaderRecord.append(String.format("%1.4s", StringUtils.leftPad(userGenerationNumber,4, '0')));
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

    public static String getStandardTransactionRecord(char dataSetStatus, IgnitionConstants.TransactionType transactionType, String companyBranchCode,
                                               String companyAccountNumber, String userCode, String userSequenceNumber, String customerBranchCode,
                                               String customerAccountNumber, IgnitionConstants.AccountType accountType, String amount, Date actionDate, String entryClass, String userReference, String customerAccountName, String nonStandardCustomerAccountNumber){

        StringBuilder standardTransactionRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        standardTransactionRecord.append("001");

        // Data Set Status to choose between Live and Test data
        standardTransactionRecord.append(dataSetStatus);

        // BankServ Record Identifier RCID120
        standardTransactionRecord.append(transactionType == IgnitionConstants.TransactionType.Credit_Transaction ? "10" : "50");

        // User Branch (will be Company Branch code)
        standardTransactionRecord.append(String.format("%1.6s", StringUtils.leftPad(companyBranchCode, 6, '0')));

        // User Account Number (Will be Company Account Number)
        standardTransactionRecord.append(String.format("%1.11s", StringUtils.leftPad(companyAccountNumber, 11, '0')));

        // User Code
        standardTransactionRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        // User Sequence Number
        standardTransactionRecord.append(String.format("%1.6s", StringUtils.leftPad(userSequenceNumber, 6, '0')));

        // Homing Branch (will be Customer Branch code)
        standardTransactionRecord.append(String.format("%1.6s", StringUtils.leftPad(customerBranchCode, 6, '0')));

        // Homing Account Number (Will be Customer Account Number)
        standardTransactionRecord.append(String.format("%1.11s", StringUtils.leftPad(customerAccountNumber, 11, '0')));

        // Account Type
        standardTransactionRecord.append(ABSAFileManager.getTransactionAccountTypeValue(accountType));

        // Transaction Amount
        standardTransactionRecord.append(StringUtils.leftPad(amount, 11, '0'));

        // Action Date (Must be within First and Last Action date from the User Record Header)
        standardTransactionRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(actionDate)));

        // As per Annexure 4 in the file specifications
        standardTransactionRecord.append(entryClass);

        // TAX Code
        standardTransactionRecord.append("0");

        // Filler
        standardTransactionRecord.append(StringUtils.repeat(" ", 3));

        // User Reference
        standardTransactionRecord.append(String.format("%1.30s", StringUtils.rightPad(userReference, 30, ' ')));

        // Homing Account Name (Will be Customer Account Name)
        standardTransactionRecord.append(String.format("%1.30s", StringUtils.rightPad(customerAccountName, 30, ' ')));

        // Non-Standard Homing Account Number (Will be Customer Account Number)
        standardTransactionRecord.append(String.format("%1.20s", StringUtils.leftPad(nonStandardCustomerAccountNumber, 20, '0')));

        // Filler
        standardTransactionRecord.append(StringUtils.repeat(" ", 16));

        // Homing Institution
        standardTransactionRecord.append("21");

        // Filler

        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        standardTransactionRecord.append(StringUtils.repeat(" ", 26));

        // Insert Carriage Return
        standardTransactionRecord.append("\r");

        return standardTransactionRecord.toString();
    }

    public static String getContraRecord(char dataSetStatus, IgnitionConstants.TransactionType transactionType, String userBranch, String accountNumber,
                                         String userCode, String userSequenceNumber, Date actionDate, String userReference, String amount){

        StringBuilder contraRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        contraRecord.append("001");

        // Data Set Status to choose between Live and Test data
        contraRecord.append(dataSetStatus);

        // BankServ Record Identifier RCID130 - Type of transaction
        contraRecord.append(transactionType == IgnitionConstants.TransactionType.Credit_Transaction ? "12":"52");

        // User Branch
        contraRecord.append(String.format("%1.6s", StringUtils.leftPad(userBranch, 6, '0')));

        // User Account Number
        contraRecord.append(String.format("%1.11s", StringUtils.leftPad(accountNumber, 11, '0')));

        // User Code
        contraRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        // User Sequence Number
        contraRecord.append(String.format("%1.6s", StringUtils.leftPad(userSequenceNumber, 6, '0')));

        // Homing branch - Same as User Branch for this record
        contraRecord.append(String.format("%1.6s", StringUtils.leftPad(userBranch, 6, '0')));

        // Homing Account Number - Same as User Account Number for this record
        contraRecord.append(String.format("%1.11s", StringUtils.leftPad(accountNumber, 11, '0')));

        // Account Type - Must always be 1
        contraRecord.append("1");

        // Total monetary value of all preceding transactions
        contraRecord.append(String.format("%1.11s", StringUtils.leftPad(amount, 11, '0')));

        // Action Date - Must be the same as the preceding transaction record
        contraRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(actionDate)));

        // Entry Class - Must always be '10'
        contraRecord.append("10");

        // Filler
        contraRecord.append(StringUtils.repeat("0", 4));

        // User Reference
        contraRecord.append(String.format("%1.30s", StringUtils.rightPad(userReference, 30, ' ')));

        // Filler
        contraRecord.append(StringUtils.repeat(" ", 30));

        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        contraRecord.append(StringUtils.repeat(" ", 64));

        // Insert Carriage Return
        contraRecord.append("\r");

        return contraRecord.toString();
    }

    public static String getUserTrailerRecord(char dataSetStatus, String userCode, String firstSequenceNumber, String lastSequenceNumber,
                                       Date firstActionDate, Date lastActionDate, String noOfdebitRecords, String noOfCreditRecords,
                                       String noOfContraRecords, String totalDebitValue, String totalCreditValue, String hashTotalOfHomingAccountNos){

        StringBuilder userTrailerRecord = new StringBuilder();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        // Record Identifier
        userTrailerRecord.append("001");

        // Data Set Status to choose between Live and Test data
        userTrailerRecord.append(dataSetStatus);

        // BankServ Record Identifier RCID150 - Identifies the record as Trailer
        userTrailerRecord.append("92");

        // User Code
        userTrailerRecord.append(String.format("%1.4s", StringUtils.rightPad(userCode, 4, ' ')));

        // First Sequence Number - Same as the one on the User Header Record
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(firstSequenceNumber,6, '0')));

        // Last Sequence Number - Same as the one on the preceding Contra Record
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(lastSequenceNumber,6, '0')));

        // First Action Date - Same as the one on the User Header Record
        userTrailerRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(firstActionDate)));

        // Last Action Date - Same as the one on the User Header Record
        userTrailerRecord.append(dateFormat.format(DateConvertUtils.asLocalDate(lastActionDate)));

        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(noOfdebitRecords,6, '0')));
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(noOfCreditRecords,6, '0')));
        userTrailerRecord.append(String.format("%1.6s", StringUtils.leftPad(noOfContraRecords,6, '0')));

        userTrailerRecord.append(StringUtils.leftPad(totalDebitValue,12, '0'));
        userTrailerRecord.append(StringUtils.leftPad(totalCreditValue,12, '0'));

        userTrailerRecord.append(String.format("%1.12s", StringUtils.leftPad(hashTotalOfHomingAccountNos,12, '0')));

        // Filler
        // NOTE: Removing 2 filler spaces at the end as the ABSA Bank confirms that the documentation is wrong.
        userTrailerRecord.append(StringUtils.repeat(" ", 110));

        // Insert Carriage Return
        userTrailerRecord.append("\r");

        return userTrailerRecord.toString();
    }

}
