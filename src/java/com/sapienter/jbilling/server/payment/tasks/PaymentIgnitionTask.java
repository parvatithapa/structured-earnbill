package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.event.NewOrderAndChangeEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentSuccessfulEvent;
import com.sapienter.jbilling.server.payment.ignition.IgnitionPaymentManager;
import com.sapienter.jbilling.server.payment.event.CustomPaymentEvent;
import com.sapienter.jbilling.server.payment.tasks.absa.NAEDOWorkflowManager;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeComparator;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by taimoor on 8/5/17.
 */
public class PaymentIgnitionTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(PaymentIgnitionTask.class);

    /* Plugin parameters */

    public static final ParameterDescription PARAMETER_ENABLE_PAYMENT =
            new ParameterDescription("Enable Payment", false, ParameterDescription.Type.BOOLEAN, false);

    public static final ParameterDescription PARAMETER_ENABLE_NAEDO =
            new ParameterDescription("Enable NAEDO", false, ParameterDescription.Type.BOOLEAN, false);

    public static final ParameterDescription PARAMETER_UPPER_LEVEL_TRACKING_DAYS =
            new ParameterDescription("Upper Level Tracking Days", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_MIDDLE_LEVEL_TRACKING_DAYS =
            new ParameterDescription("Middle Level Tracking Days", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_LOWER_LEVEL_TRACKING_DAYS =
            new ParameterDescription("Lower Level Tracking Days", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_MIDDLE_LEVEL_START_TRACK =
            new ParameterDescription("Middle Level Start Track", false, ParameterDescription.Type.INT, false);

    public static final ParameterDescription PARAMETER_LOWER_LEVEL_START_TRACK =
            new ParameterDescription("Lower Level Start Track", false, ParameterDescription.Type.INT, false);

    public static final ParameterDescription PARAMETER_UPPER_LEVEL_UPDATE_DEBIT_DATE =
            new ParameterDescription("Upper Level Update Debit Date", false, ParameterDescription.Type.BOOLEAN, false);

    public static final ParameterDescription PARAMETER_MIDDLE_LEVEL_UPDATE_DEBIT_DATE =
            new ParameterDescription("Middle Level Update Debit Date", false, ParameterDescription.Type.BOOLEAN, false);

    public static final ParameterDescription PARAMETER_LOWER_LEVEL_UPDATE_DEBIT_DATE =
            new ParameterDescription("Lower Level Update Debit Date", false, ParameterDescription.Type.BOOLEAN, false);

    //initializer for pluggable parameters
    {
        descriptions.add(PARAMETER_ENABLE_PAYMENT);
        descriptions.add(PARAMETER_ENABLE_NAEDO);
        descriptions.add(PARAMETER_UPPER_LEVEL_TRACKING_DAYS);
        descriptions.add(PARAMETER_MIDDLE_LEVEL_TRACKING_DAYS);
        descriptions.add(PARAMETER_LOWER_LEVEL_TRACKING_DAYS);
        descriptions.add(PARAMETER_MIDDLE_LEVEL_START_TRACK);
        descriptions.add(PARAMETER_LOWER_LEVEL_START_TRACK);
        descriptions.add(PARAMETER_UPPER_LEVEL_UPDATE_DEBIT_DATE);
        descriptions.add(PARAMETER_MIDDLE_LEVEL_UPDATE_DEBIT_DATE);
        descriptions.add(PARAMETER_LOWER_LEVEL_UPDATE_DEBIT_DATE);
    }

    private  boolean isPaymentEnabled() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_ENABLE_PAYMENT.getName()))){
            return true;
        }
        return Boolean.parseBoolean(parameters.get(PARAMETER_ENABLE_PAYMENT.getName()));
    }

    private  boolean isNAEDOEnabled() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_ENABLE_NAEDO.getName()))){
            return false;
        }
        return Boolean.parseBoolean(parameters.get(PARAMETER_ENABLE_NAEDO.getName()));
    }

    private  String getUpperLevelTrackingDays() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_UPPER_LEVEL_TRACKING_DAYS.getName()))){
            return StringUtils.EMPTY;
        }

        return parameters.get(PARAMETER_UPPER_LEVEL_TRACKING_DAYS.getName());
    }

    private  String getMiddleLevelTrackingDays() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_MIDDLE_LEVEL_TRACKING_DAYS.getName()))){
            return StringUtils.EMPTY;
        }

        return parameters.get(PARAMETER_MIDDLE_LEVEL_TRACKING_DAYS.getName());
    }

    private  String getLowerLevelTrackingDays() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_LOWER_LEVEL_TRACKING_DAYS.getName()))){
            return StringUtils.EMPTY;
        }

        return parameters.get(PARAMETER_LOWER_LEVEL_TRACKING_DAYS.getName());
    }

    private  Integer getMiddleLevelStartTracking() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_MIDDLE_LEVEL_START_TRACK.getName()))){
            return 0;
        }

        return Integer.valueOf(parameters.get(PARAMETER_MIDDLE_LEVEL_START_TRACK.getName()));
    }

    private  Integer getLowerLevelStartTracking() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_LOWER_LEVEL_START_TRACK.getName()))){
            return 0;
        }

        return Integer.valueOf(parameters.get(PARAMETER_LOWER_LEVEL_START_TRACK.getName()));
    }

    private  boolean updateUpperLevelDebitDate() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_UPPER_LEVEL_UPDATE_DEBIT_DATE.getName()))){
            return false;
        }

        return Boolean.parseBoolean(parameters.get(PARAMETER_UPPER_LEVEL_UPDATE_DEBIT_DATE.getName()));
    }

    private  boolean updateMiddleLevelDebitDate() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_MIDDLE_LEVEL_UPDATE_DEBIT_DATE.getName()))){
            return false;
        }

        return Boolean.parseBoolean(parameters.get(PARAMETER_MIDDLE_LEVEL_UPDATE_DEBIT_DATE.getName()));
    }

    private  boolean updateLowerLevelDebitDate() {
        if(StringUtils.isEmpty(parameters.get(PARAMETER_LOWER_LEVEL_UPDATE_DEBIT_DATE.getName()))){
            return false;
        }

        return Boolean.parseBoolean(parameters.get(PARAMETER_LOWER_LEVEL_UPDATE_DEBIT_DATE.getName()));
    }

    // Subscribed Events
    private static final Class<Event> events[] = new Class[] {
            CustomPaymentEvent.class,
            NewOrderAndChangeEvent.class,
            IgnitionPaymentSuccessfulEvent.class,
            IgnitionPaymentFailedEvent.class,
            IgnitionPaymentEvent.class
    };

    private List<Date> holidays;
    private Map<Date, List<Date>> debitDateHolidays;

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        try {
            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            this.holidays = getHolidays();
            this.debitDateHolidays = loadDebitDateUpdateHolidays();

            if(isPaymentEnabled() && event instanceof IgnitionPaymentEvent) {
                IgnitionPaymentEvent ignitionPaymentEvent = (IgnitionPaymentEvent) event;

                logger.debug("Processing Ignition scheduled payment for ID: " + ignitionPaymentEvent.getPaymentWS().getId());

                IgnitionPaymentManager.processScheduledPayment(this.getEntityId(), getUpperLevelTrackingDays(), getMiddleLevelTrackingDays(),
                        getLowerLevelTrackingDays(), isNAEDOEnabled(), this.holidays,ignitionPaymentEvent.getPaymentWS(),
                        ignitionPaymentEvent.getOrderId(), ignitionPaymentEvent.getAllServiceProfiles());

            }else if(isPaymentEnabled() && event instanceof CustomPaymentEvent) {
                CustomPaymentEvent customPaymentEvent = (CustomPaymentEvent) event;

                logger.debug("Processing Ignition manual payment for ID: " + customPaymentEvent.getPaymentWS().getId());

                IgnitionPaymentManager.processManualPayment(this.getEntityId(), getUpperLevelTrackingDays(), getMiddleLevelTrackingDays(),
                        getLowerLevelTrackingDays(), isNAEDOEnabled(), this.holidays, customPaymentEvent.getPaymentWS(),
                        customPaymentEvent.getOrderId(), webServicesSessionBean);

            }else if(event instanceof IgnitionPaymentSuccessfulEvent) {
                IgnitionPaymentSuccessfulEvent paymentSuccessfulEvent = (IgnitionPaymentSuccessfulEvent) event;

                UserBL userBL = new UserBL(paymentSuccessfulEvent.getPayment().getUserId());
                UserWS userWS = userBL.getUserWS();

                logger.debug("Processing Ignition Payment Successful Event for Payment Id : " + paymentSuccessfulEvent.getPayment().getId());

                NAEDOWorkflowManager naedoWorkflowManager = new NAEDOWorkflowManager(userWS, paymentSuccessfulEvent.getPayment(),
                        updateUpperLevelDebitDate(), updateMiddleLevelDebitDate(), updateLowerLevelDebitDate(),
                        getMiddleLevelStartTracking(), getLowerLevelStartTracking(), this.holidays, this.debitDateHolidays);

                naedoWorkflowManager.applyNAEDOWorkflow(true, isNAEDOEnabled());

            }else if(event instanceof IgnitionPaymentFailedEvent){

                IgnitionPaymentFailedEvent ignitionPaymentFailedEvent = (IgnitionPaymentFailedEvent) event;

                UserBL userBL = new UserBL(ignitionPaymentFailedEvent.getPayment().getUserId());
                UserWS userWS = userBL.getUserWS();

                logger.debug("Processing Ignition Payment Failed Event for Payment Id : " + ignitionPaymentFailedEvent.getPayment().getId());

                NAEDOWorkflowManager naedoWorkflowManager = new NAEDOWorkflowManager(userWS, ignitionPaymentFailedEvent.getPayment(),
                        updateUpperLevelDebitDate(), updateMiddleLevelDebitDate(), updateLowerLevelDebitDate(),
                        getMiddleLevelStartTracking(), getLowerLevelStartTracking(), this.holidays, this.debitDateHolidays);

                naedoWorkflowManager.applyNAEDOWorkflow(false, isNAEDOEnabled());

            }else if(event instanceof NewOrderAndChangeEvent) {
                updateCustomerActionDate(webServicesSessionBean, (NewOrderAndChangeEvent) event);

            }

        }catch (Exception exception){
            throw new PluggableTaskException(exception);
        }
    }

    private List<Date> getHolidays() throws ParseException {

        logger.debug("Getting holiday parameters from the Task");

        Map<String, String> parameters = this.getParameters();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

        List<Date> holidays = new ArrayList<>();

        for(Map.Entry<String, String> parameterDescription : parameters.entrySet()){

            if(parameterDescription.getKey().contains(IgnitionConstants.PARAMETER_HOLIDAY)){
                holidays.add(DateConvertUtils.asUtilDate(LocalDate.parse(String.valueOf(parameterDescription.getValue()), dateFormat)));//dateFormat. parse(String.valueOf(parameterDescription.getValue())));
            }
        }

        Collections.sort(holidays, Collections.reverseOrder());

        logger.debug(String.format("%s holidays found", holidays.size()));

        return holidays;
    }

    private void updateCustomerActionDate(IWebServicesSessionBean webServicesSessionBean, NewOrderAndChangeEvent event) throws Exception {
        try {
            OrderBL orderBL = new OrderBL(event.getOrder());
            Collection<OrderChangeDTO> orderChanges = event.getOrderChanges();
            OrderWS order = orderBL.getWS(webServicesSessionBean.getCallerLanguageId());
            Integer userId = order.getUserId();
            UserWS userWs = webServicesSessionBean.getUserWS(userId);

            MetaFieldValueWS[] userMetafields = userWs.getMetaFields();
            boolean userActionDateFound = false;
            boolean inNaedo = false;
            String naedoType = null;
            Date customerActionDate = null;

            for (MetaFieldValueWS metaFieldValueWS : userMetafields) {
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_ACTION_DATE)) {
                    userActionDateFound = true;
                    customerActionDate = metaFieldValueWS.getDateValue();
                }
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_IN_NAEDO)) {
                    inNaedo = metaFieldValueWS.getBooleanValue();
                }
                if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_NAEDO_TYPE)) {
                    naedoType = (String) metaFieldValueWS.getValue();
                }
            }

            if (!userActionDateFound) {

                ServiceProfile serviceProfile = IgnitionUtility.getServiceProfileFromItemForBrand(event.getEntityId(), userWs, order, orderChanges);

                Calendar currentCal = Calendar.getInstance();
                currentCal.setTime(TimezoneHelper.companyCurrentDate(this.getEntityId()));

                currentCal.set(Calendar.HOUR_OF_DAY, 0);
                currentCal.set(Calendar.MINUTE, 0);
                currentCal.set(Calendar.SECOND, 0);
                currentCal.set(Calendar.MILLISECOND, 0);

                MetaFieldValueWS[] paymentInstrumentMetafields = userWs.getPaymentInstruments().get(0).getMetaFields();
                Date nextPaymentDate = null;
                Calendar originalNextPaymentDate = new GregorianCalendar();

                Calendar nextPaymentDateCal = new GregorianCalendar();
                Integer debitDay = 0;

                // Get Debit Day Value
                for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_DEBIT_DAY)) {

                        debitDay = metaFieldValueWS.getIntegerValue();
                        break;
                    }
                }

                MetaFieldValueWS nextPaymentDateMetafield = null;
                MetaFieldValueWS originalNextPaymentDateMetafield = null;
                for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER)) {
                        nextPaymentDate = metaFieldValueWS.getDateValue();
                        //setting original payment date coming from the UI for future references
                        originalNextPaymentDate.setTime(nextPaymentDate);
                        nextPaymentDateMetafield = metaFieldValueWS;

                        nextPaymentDateCal = Calendar.getInstance();
                        nextPaymentDateCal.setTime(nextPaymentDate);

                        nextPaymentDateCal = IgnitionUtility.getUserActionDateForWorkingDays(nextPaymentDateCal, this.holidays);
                        nextPaymentDateCal = IgnitionUtility.getNextPaymentDateForCustomerForDebitDateHolidays(nextPaymentDateCal, debitDateHolidays);

                        if (DateTimeComparator.getDateOnlyInstance().compare(nextPaymentDateCal.getTime(), currentCal.getTime()) <= 0) {

                            nextPaymentDateCal = IgnitionUtility.getNextPaymentDateForCustomer(nextPaymentDateCal, debitDay, this.holidays, this.debitDateHolidays);
                            originalNextPaymentDate = IgnitionUtility.getOriginalNextPaymentDateForCustomer(Calendar.getInstance(), debitDay);
                        }

                        nextPaymentDate = nextPaymentDateCal.getTime();
                        metaFieldValueWS.setValue(nextPaymentDate);
                    }
                }

                for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.METAFIELD_ORIGINAL_NEXT_PAYMENT_DATE_INDENTIFIER)) {
                        metaFieldValueWS.setValue(originalNextPaymentDate.getTime());
                        originalNextPaymentDateMetafield = metaFieldValueWS;
                    }
                }

                Date nextActionDate = null;

                if (inNaedo) {
                    if (naedoType != null && IgnitionConstants.NAEDOWorkflowType.UPPER.equalsName(naedoType)) {
                        Calendar actionDateCalender = Calendar.getInstance();
                        actionDateCalender.setTime(new Date());

                        actionDateCalender.set(Calendar.DAY_OF_MONTH, 25);
                        actionDateCalender = IgnitionUtility.getUserActionDateForWorkingDays(actionDateCalender, this.holidays);

                        if(DateTimeComparator.getDateOnlyInstance().compare(actionDateCalender.getTime(), currentCal.getTime()) <= 0){
                            actionDateCalender.add(Calendar.MONTH,1);
                            actionDateCalender.set(Calendar.DAY_OF_MONTH, 25);
                            actionDateCalender = IgnitionUtility.getUserActionDateForWorkingDays(actionDateCalender, this.holidays);
                        }

                        nextActionDate = actionDateCalender.getTime();

                    } else {
                        nextActionDate = nextPaymentDate;
                    }
                } else {

                    nextActionDate = IgnitionUtility.getUserActionDateForCurrentService(nextPaymentDateCal.getTime(),
                            (serviceProfile != null ? serviceProfile.getTypesOfDebitServices() : StringUtils.EMPTY), this.holidays);

                    if (DateTimeComparator.getDateOnlyInstance().compare(nextActionDate, TimezoneHelper.companyCurrentDate(this.getEntityId())) <= 0) {

                        currentCal.add(Calendar.DATE, 1);
                        nextActionDate = IgnitionUtility.getActionDateForWorkingDays(currentCal, this.holidays).getTime();

                        if(serviceProfile.getTypesOfDebitServices().equals(IgnitionConstants.ServiceType.TWO_DAY.toString())) {

                            int dayDifference = IgnitionUtility.getWorkingDayDifference(currentCal, nextPaymentDateCal, this.holidays);

                            if (dayDifference < 5) {

                                for(int iterator = 0; iterator < (5 - dayDifference); iterator++) {

                                    nextPaymentDateCal.add(Calendar.DATE, 1);

                                    nextPaymentDateCal = IgnitionUtility.getActionDateForWorkingDays(nextPaymentDateCal, holidays);
                                }

                                currentCal.setTime(nextActionDate);

                                // NOTE: Get Next Payment Date as per Debit Date Holidays information provided
                                nextPaymentDateCal = IgnitionUtility.getNextPaymentDateForCustomerForDebitDateHolidays(nextPaymentDateCal, debitDateHolidays);

                                dayDifference = IgnitionUtility.getWorkingDayDifference(currentCal, nextPaymentDateCal, this.holidays);

                                // If the working day difference is less than 5 or the next payment date goes on the past then move it 1 month ahead
                                if ((dayDifference < 5) || DateTimeComparator.getDateOnlyInstance().compare(nextPaymentDateCal.getTime(),
                                        TimezoneHelper.companyCurrentDate(this.getEntityId())) <= 0) {

                                    nextPaymentDateCal = IgnitionUtility.getNextPaymentDateForCustomer(nextPaymentDateCal, debitDay, this.holidays, this.debitDateHolidays);
                                    originalNextPaymentDate = IgnitionUtility.getOriginalNextPaymentDateForCustomer(Calendar.getInstance(), debitDay);
                                }

                                nextPaymentDateMetafield.setValue(nextPaymentDateCal.getTime());
                                originalNextPaymentDateMetafield.setValue(originalNextPaymentDate.getTime());
                            }
                        }
                    }
                }
                logger.debug(String.format("Calculated Next Action Date is: %tD", nextActionDate));

                boolean nextActionDateMetaFieldFound = false;

                for (MetaFieldValueWS metaFieldValueWS : userMetafields) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_ACTION_DATE)) {
                        metaFieldValueWS.setValue(nextActionDate);
                        nextActionDateMetaFieldFound = true;
                        break;
                    }
                }

                if (!nextActionDateMetaFieldFound) {
                    List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>(Arrays.asList(userMetafields));

                    MetaField metaField = new MetaFieldDAS().getFieldByName(this.getEntityId(), new EntityType[]{EntityType.CUSTOMER}, IgnitionConstants.USER_ACTION_DATE);
                    if (metaField != null) {
                        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                        metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_ACTION_DATE);
                        metaFieldValueWS.setDateValue(nextActionDate);
                        metaFieldValueList.add(metaFieldValueWS);

                        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
                        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);
                        userWs.setMetaFields(updatedMetaFieldValueWSArray);
                    }
                }
                webServicesSessionBean.updateUser(userWs);
            } else {
                logger.debug("Action date " + customerActionDate + " already exist for user " + userId);
            }
        }catch (Exception exception){
            logger.error("Exception occured while trying to update Customer Action Date on Order Creation");
            logger.error(exception);
            throw exception;
        }
    }

    private Map<Date, List<Date>> loadDebitDateUpdateHolidays() throws ParseException {

        logger.debug("Getting Debit Date holiday parameters from the Task");

        Map<Date, List<Date>> result = new HashMap<>();

        for(Map.Entry<String, String> parameterDescription : parameters.entrySet()){

            if(parameterDescription.getKey().toLowerCase().contains(IgnitionConstants.PARAMETER_DEBIT_DATE_HOLIDAY)){

                IgnitionUtility.calculateDebitDateHolidays(String.valueOf(parameterDescription.getValue()), result);
            }
        }

        logger.debug(String.format("%s debit date holidays found", result.size()));

        return result;
    }
}
