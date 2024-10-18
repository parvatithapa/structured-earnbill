package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.responseFile.absa.NAEDOPaymentOutput;
import com.sapienter.jbilling.server.ignition.responseFile.absa.NAEDOResponseRecord;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by taimoor on 7/25/17.
 */
public class NAEDOOutputFilerParser {

    public static NAEDOPaymentOutput parseOutputFile(List<String> lines) throws ParseException {

        List<NAEDOResponseRecord> responseRecords = new ArrayList<>();

        for(String line : lines){

            if(line.substring(0,3).equals("052")){
                responseRecords.add(parseResponseRecord(line));
            }
        }

        return new NAEDOPaymentOutput(responseRecords);
    }

    static NAEDOResponseRecord parseResponseRecord(String line) throws ParseException {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        NAEDOResponseRecord.Identifier recordIdentifier = getRecordIdentifier(line.substring(4, 6));
        String bankServUserCode = line.substring(23, 27);
        String responseCode = line.substring(71, 73);
        String userReference = line.substring(74, 84);
        String contractReference = line.substring(84, 98);
        String homingAccountName = line.substring(104, 134);
        String homingBranchNumber = line.substring(134, 140);
        String homingAccountNumber = line.substring(140, 151);

        BigDecimal installmentAmount = new BigDecimal(line.substring(51, 62));

        Date originalActionDate = DateConvertUtils.asUtilDate(LocalDate.parse(line.substring(62, 68), dateFormat));
        Date originalEffectiveDate = DateConvertUtils.asUtilDate(LocalDate.parse(line.substring(178, 184), dateFormat));


        return new NAEDOResponseRecord(recordIdentifier, bankServUserCode, responseCode, userReference, contractReference, homingAccountName, homingBranchNumber, homingAccountNumber,
                installmentAmount, originalActionDate, originalEffectiveDate);
    }

    static NAEDOResponseRecord.Identifier getRecordIdentifier(String identifier){
        switch (identifier){
            case "65":
                return NAEDOResponseRecord.Identifier.REQUEST_RESPONSE_RECORD;
            case "90":
                return NAEDOResponseRecord.Identifier.RECALL_RESPONSE_RECORD;
            case "35":
                return NAEDOResponseRecord.Identifier.DISPUTE_RECORD;
            case "67":
                return NAEDOResponseRecord.Identifier.HOMEBACK_TRANSACTION_RECORD;
            default:
                return null;
        }
    }
}
