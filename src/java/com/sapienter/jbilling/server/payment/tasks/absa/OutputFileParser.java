package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.responseFile.absa.OutputTransactionRecord;
import com.sapienter.jbilling.server.ignition.responseFile.absa.OutputUserHeaderRecord;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.TransmissionHeader;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by taimoor on 7/24/17.
 */
public class OutputFileParser {

    public static PaymentOutput parseOutputFile(List<String> lines) throws ParseException {

        TransmissionHeader transmissionHeader = null;
        OutputUserHeaderRecord userHeaderRecord = null;
        List<OutputTransactionRecord> transactionRecords = new ArrayList<>();

        for (String line : lines) {
            if(line.substring(0,3).equals("000")) {
                transmissionHeader = parseTransmissionHeader(line);
            }
            else if(line.substring(0,3).equals("010")) {
                userHeaderRecord = parseUserHeaderRecord(line);
            }
            else if(line.substring(0,3).equals("013")) {
                transactionRecords.add(parseTransactionRecord(line));
            }
        }

        return new PaymentOutput(transmissionHeader, userHeaderRecord, transactionRecords);
    }

    static TransmissionHeader parseTransmissionHeader(String transmissionHeader) throws ParseException {

        // As the Transmission Header is the same in both cases
        return ReplyFileParser.parseTransmissionHeader(transmissionHeader);
    }

    static OutputUserHeaderRecord parseUserHeaderRecord(String userHeaderRecord){

        String bankServUserCode = userHeaderRecord.substring(4, 8);
        String bankServGenerationNumber = userHeaderRecord.substring(8, 15);
        String bankServService = userHeaderRecord.substring(15, 17);

        return new OutputUserHeaderRecord(bankServUserCode, bankServGenerationNumber, bankServService);
    }

    /**
     * Here "Set" means collection
     * @param setHeaderRecord
     */
    static void parseSetHeaderRecord(String setHeaderRecord){

    }

    static OutputTransactionRecord parseTransactionRecord(String transactionRecord) throws ParseException {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        BigDecimal amount = new BigDecimal(transactionRecord.substring(42, 53));

        Date transmissionDate = DateConvertUtils.asUtilDate(LocalDate.parse(transactionRecord.substring(6, 14), dateFormat));

        String transactionType = transactionRecord.substring(4, 6);
        String sequenceNumber = transactionRecord.substring(14, 20);
        String homingBranchCode = transactionRecord.substring(20, 26);
        String homingAccountNumber = transactionRecord.substring(26, 42);
        String homingAccountName = transactionRecord.substring(113, 143);
        String userReference = transactionRecord.substring(53, 83);
        String rejectionReason = transactionRecord.substring(83, 86);
        String rejectionQualifier = transactionRecord.substring(86, 91);
        String distributionSequenceNumber = transactionRecord.substring(91, 97);

        return new OutputTransactionRecord(amount, transmissionDate, transactionType, sequenceNumber, homingBranchCode,
                homingAccountNumber, homingAccountName, userReference, rejectionReason, rejectionQualifier, distributionSequenceNumber);
    }

    /**
     * Here "Set" means collection
     * @param setTrailerRecord
     */
    static void parseSetTrailerRecord(String setTrailerRecord){

    }

    static void parseUserTrailerRecord(String userTrailerRecord){

    }
}
