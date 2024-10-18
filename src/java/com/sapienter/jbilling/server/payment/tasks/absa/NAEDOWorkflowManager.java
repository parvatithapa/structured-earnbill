package com.sapienter.jbilling.server.payment.tasks.absa;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by Taimoor Choudhary on 9/26/17.
 */
public class NAEDOWorkflowManager {

    private static final FormatLogger logger = new FormatLogger(NAEDOWorkflowManager.class);

    private UserWS userWS;
    private PaymentDTOEx paymentDTOEx;
    private boolean updateUpperLevelDebitDate;
    private boolean updateMiddleLevelDebitDate;
    private boolean updateLowerLevelDebitDate;
    private Integer middleLevelStartTrack;
    private Integer lowerLevelStartTrack;
    private List<Date> holidays;
    private Map<Date, List<Date>> debitDateHolidays;

    public NAEDOWorkflowManager(UserWS userWS, PaymentDTOEx paymentDTOEx,
                                boolean updateUpperLevelDebitDate, boolean updateMiddleLevelDebitDate, boolean updateLowerLevelDebitDate,
                                Integer middleLevelStartTrack, Integer lowerLevelStartTrack, List<Date> holidays, Map<Date, List<Date>> debitDateHolidays) {
        this.userWS = userWS;
        this.paymentDTOEx = paymentDTOEx;
        this.updateUpperLevelDebitDate = updateUpperLevelDebitDate;
        this.updateMiddleLevelDebitDate = updateMiddleLevelDebitDate;
        this.updateLowerLevelDebitDate = updateLowerLevelDebitDate;
        this.middleLevelStartTrack = middleLevelStartTrack;
        this.lowerLevelStartTrack = lowerLevelStartTrack;
        this.holidays = holidays;
        this.debitDateHolidays = debitDateHolidays;
    }

    /**
     *
     * @param isPaid
     * @return
     */
    public void applyNAEDOWorkflow(boolean isPaid, boolean isNAEDOWorkflowEnabled){

        if(userWS == null & paymentDTOEx == null){
            logger.debug("UserWS and PaymentDTOEx should not be null");
            return;
        }

        Boolean inNAEDO = false;
        Integer lowerLevelNAEDOCount = 0;
        IgnitionConstants.NAEDOWorkflowType NAEDOType = IgnitionConstants.NAEDOWorkflowType.NONE;
        IgnitionConstants.LastNAEDOResult lastNAEDOResult = IgnitionConstants.LastNAEDOResult.NONE;

        // Get required Meta-Fields values from UserWS
        for(MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_IN_NAEDO)){
                inNAEDO = metaFieldValueWS.getBooleanValue();

            }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_NAEDO_TYPE)){
                NAEDOType = IgnitionConstants.NAEDOWorkflowType.getNAEDOWorkflowType(metaFieldValueWS.getStringValue());

            }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_LAST_NAEDO_RESULT)){
                lastNAEDOResult = IgnitionConstants.LastNAEDOResult.getNAEDOResultType(metaFieldValueWS.getStringValue());

            }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_LOWER_LEVEL_NAEDO_COUNT)){
                lowerLevelNAEDOCount = metaFieldValueWS.getIntegerValue();
            }
        }

        logger.debug("User details found for NAEDO flow: In NAEDO: %s, NAEDO Type: %s, Last NAEDO: %s, Lower level count: %s",
                inNAEDO, NAEDOType.toString(), lastNAEDOResult.toString(), lowerLevelNAEDOCount);

        String paymentTrackingDays = "No Tracking";
        String paymentType = "EFT";
        String paymentDate = StringUtils.EMPTY;
        IgnitionConstants.NAEDOWorkflowType paymentNAEDOType = IgnitionConstants.NAEDOWorkflowType.NONE;

        // Get required Meta-Fields values from PaymentDTOEx
        for(MetaFieldValue metaFieldValue : paymentDTOEx.getMetaFields()){
            if(metaFieldValue.getField().getName().equals(IgnitionConstants.PAYMENT_TYPE)){
                paymentType = String.valueOf(metaFieldValue.getValue());

            }else if(metaFieldValue.getField().getName().equals(IgnitionConstants.PAYMENT_NAEDO_TYPE)){
                paymentNAEDOType = IgnitionConstants.NAEDOWorkflowType.getNAEDOWorkflowType(String.valueOf(metaFieldValue.getValue()));

            }else if(metaFieldValue.getField().getName().equals(IgnitionConstants.PAYMENT_TRACKING_DAYS)){
                paymentTrackingDays = (String) metaFieldValue.getValue();

            }else if(metaFieldValue.getField().getName().equals(IgnitionConstants.PAYMENT_DATE)){
                paymentDate = (String) metaFieldValue.getValue();

            }
        }

        logger.debug("Payment details found for NAEDO flow - Payment Type: %s, NAEDO Type: %s, Tracking Days: %s, Payment Date: %s",
                paymentType, paymentNAEDOType.toString(), paymentTrackingDays, paymentDate);

        // Workflow is disabled
        if(!isNAEDOWorkflowEnabled){

            logger.debug("Moving Customer to EFT irrespective of the workflow values as the NAEDO workflow is disabled");

            updateUser(false, null, IgnitionConstants.NAEDOWorkflowType.NONE, null);

            return;
        }

        // EFT AND Successful Payment
        if(IgnitionConstants.IgnitionPaymentType.EFT.equalsName(paymentType) && isPaid){

            logger.debug("Updating User for EFT flow and Successful Payment");

            updateUser(false, null, IgnitionConstants.NAEDOWorkflowType.NONE, null);
            return;
        }

        // EFT AND Failed Payment
        if(IgnitionConstants.IgnitionPaymentType.EFT.equalsName(paymentType) && !isPaid){

            logger.debug("Updating User for EFT flow and Failed Payment");

            Integer lastNAEDOPaymentId = isUserNAEDOEDBefore();
            if(lastNAEDOPaymentId < 1){

                logger.debug("User is not in NAEDO before");
                updateActionAndScheduleDateValues(false, StringUtils.EMPTY, this.middleLevelStartTrack, false);
                updateUser(true, null, IgnitionConstants.NAEDOWorkflowType.MIDDLE, null);

            }else{

                logger.debug("User has been in NAEDO before");
                processLastNAEDOPayment(lastNAEDOPaymentId, lowerLevelNAEDOCount, paymentDate);
            }

            return;
        }

        // NAEDO Payment
        if(IgnitionConstants.IgnitionPaymentType.NAEDO.equalsName(paymentType)){
            processNAEDOPayment(paymentNAEDOType, paymentType, IgnitionConstants.LastNAEDOResult.getNAEDOResultType(isPaid?
                    IgnitionConstants.LastNAEDOResult.PAID.toString(): IgnitionConstants.LastNAEDOResult.UNPAID.toString()), lowerLevelNAEDOCount, paymentDate);
        }
    }

    private void processNAEDOPayment(IgnitionConstants.NAEDOWorkflowType lastNAEDOType,
                                     String paymentType, IgnitionConstants.LastNAEDOResult lastNAEDOResult, Integer lowerNAEDOCount, String paymentDate){

        // Successful
        if(lastNAEDOResult.equalsName(IgnitionConstants.LastNAEDOResult.PAID.toString())){

            switch (lastNAEDOType){
                case UPPER:

                    updateActionAndScheduleDateValues(this.updateUpperLevelDebitDate, paymentDate, this.middleLevelStartTrack, false);
                    updateUser(true, null, IgnitionConstants.NAEDOWorkflowType.MIDDLE, IgnitionConstants.LastNAEDOResult.PAID);
                    break;
                case MIDDLE:

                    updateActionAndScheduleDateValues(this.updateMiddleLevelDebitDate, paymentDate, this.lowerLevelStartTrack, false);
                    updateUser(true, (lowerNAEDOCount + 1), IgnitionConstants.NAEDOWorkflowType.LOWER, IgnitionConstants.LastNAEDOResult.PAID);
                    break;
                case LOWER:

                    // Check if it has been in Lower NAEDO before
                    if(IgnitionConstants.IgnitionPaymentType.NAEDO.equalsName(paymentType)){

                        if(lowerNAEDOCount <= 1){
                            updateActionAndScheduleDateValues(this.updateLowerLevelDebitDate, paymentDate, 0, false);
                            updateUser(false, null, IgnitionConstants.NAEDOWorkflowType.NONE, IgnitionConstants.LastNAEDOResult.PAID);
                            break;
                        }
                    }

                    updateActionAndScheduleDateValues(this.updateLowerLevelDebitDate, paymentDate, this.lowerLevelStartTrack, false);
                    updateUser(true, (lowerNAEDOCount + 1), IgnitionConstants.NAEDOWorkflowType.LOWER, IgnitionConstants.LastNAEDOResult.PAID);
                    break;
            }

        } else {

            switch (lastNAEDOType) {
                case UPPER:

                    updateActionAndScheduleDateValues(false, paymentDate, 0, true);
                    updateUser(true, null, IgnitionConstants.NAEDOWorkflowType.UPPER, IgnitionConstants.LastNAEDOResult.UNPAID);
                    break;
                case MIDDLE:

                    updateActionAndScheduleDateValues(false, paymentDate, 0, true);
                    updateUser(true, null, IgnitionConstants.NAEDOWorkflowType.UPPER, IgnitionConstants.LastNAEDOResult.UNPAID);
                    break;
                case LOWER:

                    updateActionAndScheduleDateValues(false, paymentDate, this.middleLevelStartTrack, false);
                    updateUser(true, null, IgnitionConstants.NAEDOWorkflowType.MIDDLE, IgnitionConstants.LastNAEDOResult.UNPAID);
                    break;
            }
        }
    }


    private void processLastNAEDOPayment(Integer lastNAEDOPaymentId, Integer lowerLevelNAEDOCount, String paymentDate) {
        try {

            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

            PaymentBL paymentBL = new PaymentBL(lastNAEDOPaymentId);
            PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

            IgnitionConstants.NAEDOWorkflowType paymentNAEDOType = IgnitionConstants.NAEDOWorkflowType.NONE;

            // Get required Meta-Fields values from PaymentDTOEx
            for(MetaFieldValue metaFieldValue : paymentDTOEx.getMetaFields()){
                if(metaFieldValue.getField().getName().equals(IgnitionConstants.PAYMENT_NAEDO_TYPE)){

                    paymentNAEDOType = IgnitionConstants.NAEDOWorkflowType.getNAEDOWorkflowType(String.valueOf(metaFieldValue.getValue()));
                }
            }

            IgnitionConstants.LastNAEDOResult lastNAEDOResult =
                    paymentDTOEx.getResultId().equals(Constants.PAYMENT_RESULT_FAILED) ?
                            IgnitionConstants.LastNAEDOResult.UNPAID : IgnitionConstants.LastNAEDOResult.PAID;

            processNAEDOPayment(paymentNAEDOType, "EFT", lastNAEDOResult, lowerLevelNAEDOCount, paymentDate);

        } catch (Exception exception) {
            logger.error("Exception occurred trying to process NAEDO workflow on EFT Failed Payment with last NAEDO Payment: %s for User: %s", lastNAEDOPaymentId, userWS.getId());
            logger.error(exception);
        }
    }

    private void updateUser(boolean inNAEDO, Integer lowerLevelNAEDOCount,
                            IgnitionConstants.NAEDOWorkflowType NAEDOType, IgnitionConstants.LastNAEDOResult lastNAEDOResult){

        try{
            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

            boolean inNaedoMetaFieldFound =  false;
            boolean naedoTypeMetaFieldFound =  false;
            boolean lastNaedoResultMetaFieldFound =  false;
            boolean lowerLevelNaedoCountMetaFieldFound =  false;

            // Set required Meta-Fields values from UserWS
            for(MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()){
                if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_IN_NAEDO)){
                    metaFieldValueWS.setBooleanValue(inNAEDO);
                    inNaedoMetaFieldFound = true;

                }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_NAEDO_TYPE)){
                    if(NAEDOType != null) {
                        metaFieldValueWS.setStringValue(NAEDOType.toString());
                        naedoTypeMetaFieldFound = true;
                    }

                }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_LAST_NAEDO_RESULT)){
                    if(lastNAEDOResult != null) {
                        metaFieldValueWS.setStringValue(lastNAEDOResult.toString());
                        lastNaedoResultMetaFieldFound =  true;
                    }

                }else if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_LOWER_LEVEL_NAEDO_COUNT)){
                    if(lowerLevelNAEDOCount != null) {
                        metaFieldValueWS.setIntegerValue(lowerLevelNAEDOCount);
                        lowerLevelNaedoCountMetaFieldFound =  true;
                    }
                }
            }

            if (!inNaedoMetaFieldFound || !naedoTypeMetaFieldFound || !lastNaedoResultMetaFieldFound || !lowerLevelNaedoCountMetaFieldFound) {

                List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>(Arrays.asList(userWS.getMetaFields()));

                if(!inNaedoMetaFieldFound) {
                    MetaField metaField = new MetaFieldDAS().getFieldByName(this.userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER},
                            IgnitionConstants.USER_IN_NAEDO);
                    if (metaField != null) {
                        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                        metaFieldValueWS.setFieldName(IgnitionConstants.USER_IN_NAEDO);
                        metaFieldValueWS.setBooleanValue(inNAEDO);
                        metaFieldValueList.add(metaFieldValueWS);
                    }
                }

                if(NAEDOType != null && !naedoTypeMetaFieldFound) {
                    MetaField metaField = new MetaFieldDAS().getFieldByName(this.userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER},
                            IgnitionConstants.USER_NAEDO_TYPE);
                    if (metaField != null) {
                        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                        metaFieldValueWS.setFieldName(IgnitionConstants.USER_NAEDO_TYPE);
                        metaFieldValueWS.setStringValue(NAEDOType.toString());
                        metaFieldValueList.add(metaFieldValueWS);
                    }
                }

                if(lastNAEDOResult != null && !lastNaedoResultMetaFieldFound ) {
                    MetaField metaField = new MetaFieldDAS().getFieldByName(this.userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER},
                            IgnitionConstants.USER_LAST_NAEDO_RESULT);
                    if (metaField != null) {
                        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                        metaFieldValueWS.setFieldName(IgnitionConstants.USER_LAST_NAEDO_RESULT);
                        metaFieldValueWS.setStringValue(lastNAEDOResult.toString());
                        metaFieldValueList.add(metaFieldValueWS);
                    }
                }

                if(lowerLevelNAEDOCount != null && !lowerLevelNaedoCountMetaFieldFound) {
                    MetaField metaField = new MetaFieldDAS().getFieldByName(this.userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER},
                            IgnitionConstants.USER_LOWER_LEVEL_NAEDO_COUNT);
                    if (metaField != null) {
                        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                        metaFieldValueWS.setFieldName(IgnitionConstants.USER_LOWER_LEVEL_NAEDO_COUNT);
                        metaFieldValueWS.setIntegerValue(lowerLevelNAEDOCount);
                        metaFieldValueList.add(metaFieldValueWS);
                    }
                }

                MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
                metaFieldValueList.toArray(updatedMetaFieldValueWSArray);
                userWS.setMetaFields(updatedMetaFieldValueWSArray);
            }


            webServicesSessionBean.updateUser(userWS);
        }catch (Exception exception) {
            logger.error("Exception occurred while trying to update customer: " + userWS.getId());
            logger.error(exception);
        }
    }

    private void updateActionAndScheduleDateValues(boolean updateScheduleDate, String stringPaymentDate, Integer startTracking, Boolean isUpperTracking){
        try{

            logger.debug("Updating Action date with information - Update Debit Date: %s, Payment Date: %s, Start Tracking: %s",
                    updateScheduleDate, stringPaymentDate, startTracking);

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);
            Calendar currentCal = Calendar.getInstance();
            Date scheduledDate = null;
            Integer debitDate = 0;

            // Get Schedule Date and Debit Day Meta-Field value from UserWS Payment Instrument
            for (MetaFieldValueWS metaFieldValueWS : userWS.getPaymentInstruments().get(0).getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER)) {
                    if(!updateScheduleDate) {
                        scheduledDate = metaFieldValueWS.getDateValue();
                    }
                    else {
                        if(!StringUtils.isEmpty(stringPaymentDate)) {
                            scheduledDate = DateConvertUtils.asUtilDate(LocalDate.parse(stringPaymentDate, dateFormat));
                        }else {
                            logger.debug("Incoming payment date is empty, therefore using existing Payment Instrument Scheduled Date.");
                        }
                    }
                }else if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_DEBIT_DAY)) {

                    debitDate = metaFieldValueWS.getIntegerValue();
                }
            }

            if (scheduledDate == null){
                throw new NullPointerException("Next Payment Date meta-field was not found.");
            }

            currentCal.setTime(scheduledDate);

            // If have to update the scheduled date we will disregard debit date for calculation
            if(updateScheduleDate){
                debitDate = 0;
            }

            // Calculate Next Payment Date
            currentCal = IgnitionUtility.getNextPaymentDateForCustomer(currentCal, debitDate, this.holidays, this.debitDateHolidays);

            // Set Schedule Date Meta-Field value from UserWS Payment Instrument
            for (MetaFieldValueWS metaFieldValueWS : userWS.getPaymentInstruments().get(0).getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER)) {
                    metaFieldValueWS.setValue(currentCal.getTime());
                    break;
                }
            }

            // Set Action Date Meta-Field value from UserWS
            for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_ACTION_DATE)) {

                    if(isUpperTracking){
                        currentCal.set(Calendar.DAY_OF_MONTH, 25);
                    }else if(startTracking > 0 && startTracking <= 30 ){

                        // We will be subtracting working days, Weekends and Holidays will not be counted.
                        currentCal = IgnitionUtility.subtractWorkingDays(currentCal, this.holidays, startTracking);
                    }

                    Date actionDate = IgnitionUtility.getUserActionDateForCurrentService(currentCal.getTime(), StringUtils.EMPTY, this.holidays);

                    metaFieldValueWS.setDateValue(actionDate);
                    break;
                }
            }

        }catch (Exception exception) {
            logger.error("Exception occurred while trying to update customer: " + userWS.getId() + "'s Action and Schedule Date values");
            logger.error(exception);
        }
    }

    private Integer isUserNAEDOEDBefore(){
        try {
            Map<String, String> metaFields = new HashMap<>();
            metaFields.put(IgnitionConstants.PAYMENT_TYPE, "NAEDO");

            // Find if there is a payment which was of type NAEDO
            Integer paymentId = new PaymentDAS().findFirstPaymentByMetaFields(metaFields, userWS.getId());

            if(paymentId != null && paymentId > 0){
                return paymentId;
            }

            return -1;

        }catch (Exception exception){
            logger.error("Exception occured while trying to find if the user has been NAEDOED before.");
            logger.error(exception);
            return -1;
        }
    }
}
