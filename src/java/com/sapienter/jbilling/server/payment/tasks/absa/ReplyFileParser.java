package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.responseFile.absa.PaymentReply;
import com.sapienter.jbilling.server.ignition.responseFile.absa.TransmissionHeader;
import com.sapienter.jbilling.server.ignition.responseFile.absa.TransmissionRejectedReason;
import com.sapienter.jbilling.server.ignition.responseFile.absa.TransmissionStatus;
import com.sapienter.jbilling.server.ignition.responseFile.absa.UserSetStatus;
import com.sapienter.jbilling.server.ignition.responseFile.absa.RejectedMessage;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Created by taimoor on 7/24/17.
 */
public class ReplyFileParser {

    public static PaymentReply parseReplyFile(List<String> lines) throws ParseException {

        TransmissionHeader transmissionHeader = null;
        TransmissionStatus transmissionStatus = null;
        TransmissionRejectedReason transmissionRejectedReason = null;
        UserSetStatus userSetStatus = null;
        RejectedMessage rejectedMessage = null;

        for (String line : lines) {
            if(line.substring(0,3).equals("000")) {
                transmissionHeader = parseTransmissionHeader(line);
            }
            else if(line.substring(0,3).equals("900") && line.substring(4, 7).equals("000")) {
                transmissionStatus = parseTransmissionStatus(line);
            }
            else if(line.substring(0,3).equals("901") && line.substring(4, 7).equals("000")) {
                transmissionRejectedReason = parseTransmissionRejectedReason(line);
            }
            else if(line.substring(0,3).equals("900") && (line.substring(4, 7).equals("001") || line.substring(4, 7).equals("020"))) {
                userSetStatus = parseUserSetStatus(line);
            }
            else if(line.substring(0,3).equals("901") && (line.substring(4, 7).equals("001") || line.substring(4, 7).equals("020"))) {
                rejectedMessage = parseRejectedMessage(line);
            }
        }

        return new PaymentReply(transmissionHeader, transmissionStatus, transmissionRejectedReason, userSetStatus, rejectedMessage);
    }

    static TransmissionHeader parseTransmissionHeader(String transmissionHeader) throws ParseException {

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

        String recordStatus = transmissionHeader.substring(3, 4);
        String userCode = transmissionHeader.substring(54, 59);
        String transmissionNumber = transmissionHeader.substring(47, 54);
        Date transmissionDate = DateConvertUtils.asUtilDate(LocalDate.parse(transmissionHeader.substring(4, 12), dateFormat));

        return new TransmissionHeader(recordStatus, userCode, transmissionNumber, transmissionDate);
    }

    static TransmissionStatus parseTransmissionStatus(String transmissionStatus){

        String userCode = transmissionStatus.substring(21, 26);
        String transmissionNumber = transmissionStatus.substring(27, 34);
        String transmissionStatusCode = transmissionStatus.substring(35, 43);

        return new TransmissionStatus(userCode, transmissionNumber, transmissionStatusCode);
    }

    static TransmissionRejectedReason parseTransmissionRejectedReason(String transmissionRejectedReason){

        String errorCode = transmissionRejectedReason.substring(8, 13);
        String errorMessage = transmissionRejectedReason.substring(14, 64);

        return new TransmissionRejectedReason(errorCode, errorMessage);
    }

    static UserSetStatus parseUserSetStatus(String userSetStatus){

        String serviceIndicator = userSetStatus.substring(4, 7);
        String bankServUserCode = userSetStatus.substring(21, 25);
        String userGenerationNumber = userSetStatus.substring(26, 33);
        String lastSequenceNumber = userSetStatus.substring(34, 40);
        String userSetStatusCode = userSetStatus.substring(41, 49);

        return new UserSetStatus(serviceIndicator, bankServUserCode, userGenerationNumber,
                lastSequenceNumber, userSetStatusCode);
    }

    static RejectedMessage parseRejectedMessage(String rejectedMessage){

        String serviceIndicator = rejectedMessage.substring(4, 7);
        String bankServUserCode = rejectedMessage.substring(8, 12);
        String userGenerationNumber = rejectedMessage.substring(13, 20);
        String userSequenceNumber = rejectedMessage.substring(21, 27);
        String errorCode = rejectedMessage.substring(28, 33);
        String errorMessage = rejectedMessage.substring(34, 94);

        return new RejectedMessage(serviceIndicator, bankServUserCode, userGenerationNumber,
                userSequenceNumber, errorCode, errorMessage);
    }
}
