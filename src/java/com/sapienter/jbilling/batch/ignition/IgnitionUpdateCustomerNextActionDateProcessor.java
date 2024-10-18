package com.sapienter.jbilling.batch.ignition;

import com.sapienter.jbilling.batch.support.PartitionService;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by wajeeha on 2/21/18.
 */

public class IgnitionUpdateCustomerNextActionDateProcessor
        implements ItemProcessor<Integer, Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;
    @Resource
    private MetaFieldDAS metaFieldDAS;
    @Resource
    private OrderDAS orderDAS;
    @Resource
    private PartitionService partitionService;

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    private List<Date> holidays;
    private Map<Date, List<Date>> debitDateHolidays;
    private Map<String, Map<String, ServiceProfile>> allServiceProfiles;

    private boolean userInNaedo (UserWS userWS) {

        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (IgnitionConstants.USER_IN_NAEDO.equals(metaFieldValueWS.getFieldName())) {
                return (boolean) metaFieldValueWS.getValue();
            }
        }
        return false;
    }

    @Override
    public Integer process (Integer userId) throws Exception {
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {

            UserWS userWS = new UserBL(userId).getUserWS();
            Integer orderId = findActiveOrdersForPayment(userId);

            if (userInNaedo(userWS)) {
                partitionService.markUserAsSuccessful(jobId, userId, 1);
                return userId;
            }

            logger.debug("OrderId: {}, User: {}", orderId, userWS.getId());

            // Get Brand and Service Provider name
            Pair<String, String> brandAndServiceProvider = IgnitionUtility
                    .getBrandAndServiceProviderFromOrder(this.entityId, userWS, orderId);
            // Get Service Profile for given Brand & Service Provider
            ServiceProfile serviceProfile = IgnitionUtility.getServiceProfileForBrand(brandAndServiceProvider,
                    allServiceProfiles);

            MetaFieldValueWS[] paymentInstrumentMetafields = userWS.getPaymentInstruments().get(0).getMetaFields();
            MetaFieldValueWS[] userMetafields = userWS.getMetaFields();

            Integer debitDay = 0;

            // Get Debit Day Value
            for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                if (IgnitionConstants.PAYMENT_DEBIT_DAY.equals(metaFieldValueWS.getFieldName())) {

                    debitDay = metaFieldValueWS.getIntegerValue();
                    break;
                }
            }

            Calendar currentCal = Calendar.getInstance();
            currentCal.setTime(TimezoneHelper.companyCurrentDate(entityId));

            Date originalNextPaymentDate = null;

            // Get Original Next Payment Date of the Customer
            for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                if (IgnitionConstants.METAFIELD_ORIGINAL_NEXT_PAYMENT_DATE_INDENTIFIER.equals(metaFieldValueWS.getFieldName())) {
                    originalNextPaymentDate = metaFieldValueWS.getDateValue();
                    currentCal.setTime(originalNextPaymentDate);

                    currentCal.set(Calendar.HOUR_OF_DAY, 0);
                    currentCal.set(Calendar.MINUTE, 0);
                    currentCal.set(Calendar.SECOND, 0);
                    currentCal.set(Calendar.MILLISECOND, 0);

                    break;
                }
            }

            for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                if (IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER.equals(metaFieldValueWS.getFieldName())) {

                    // Get Next Payment Date for the Customer with respect to Debit Day and "Debit Date Update" holidays
                    currentCal = IgnitionUtility.getNextPaymentDateForCustomer(currentCal, debitDay, this.holidays,
                            this.debitDateHolidays);

                    metaFieldValueWS.setValue(currentCal.getTime());
                    break;
                }
            }

            Date nextActionDate = IgnitionUtility.getUserActionDateForCurrentService(currentCal.getTime(),
                    (serviceProfile != null ? serviceProfile.getTypesOfDebitServices() : StringUtils.EMPTY),
                    this.holidays);

            logger.debug("Calculated Next Action Date is: {}", nextActionDate);

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

                MetaField metaField = metaFieldDAS.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER },
                        IgnitionConstants.USER_ACTION_DATE);
                if (metaField != null) {
                    MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                    metaFieldValueWS.setFieldName(IgnitionConstants.PAYMENT_ACTION_DATE);
                    metaFieldValueWS.setDateValue(nextActionDate);
                    metaFieldValueList.add(metaFieldValueWS);

                    MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
                    metaFieldValueList.toArray(updatedMetaFieldValueWSArray);
                    userWS.setMetaFields(updatedMetaFieldValueWSArray);
                }
            }

            // Update Original Next Payment Date of the Customer
            for (MetaFieldValueWS metaFieldValueWS : paymentInstrumentMetafields) {
                if (IgnitionConstants.METAFIELD_ORIGINAL_NEXT_PAYMENT_DATE_INDENTIFIER.equals(metaFieldValueWS.getFieldName())) {
                    originalNextPaymentDate = metaFieldValueWS.getDateValue();
                    currentCal.setTime(originalNextPaymentDate);

                    Calendar updateOriginalNextPaymentDate = IgnitionUtility.getOriginalNextPaymentDateForCustomer(currentCal, debitDay);
                    metaFieldValueWS.setValue(updateOriginalNextPaymentDate.getTime());

                    break;
                }
            }

            webServicesSessionBean.updateUser(userWS);

        } catch (Exception exception) {
            logger.error("Exception occurred while trying to update customer {} next action date.", userId, exception);
        }
        partitionService.markUserAsSuccessful(jobId, userId, 1);
        return userId;
    }

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);

    public void setHolidays (String holidays) {

        if (StringUtils.isBlank(holidays)) {
            this.holidays = null;
            return;
        }

        List<Date> result = new ArrayList<>();
        String[] holidayList = holidays.split(",");

        for (String holiday : holidayList) {
            result.add(DateConvertUtils.asUtilDate(LocalDate.parse(holiday, dateFormat)));
        }

        Collections.sort(result, Collections.reverseOrder());

        logger.debug("{} holidays found", result.size());
        this.holidays = result;
    }

    public void setDebitDateHolidays (String debitDateHolidays) {

        if (StringUtils.isBlank(debitDateHolidays)) {
            this.debitDateHolidays = null;
            return;
        }

        Map<Date, List<Date>> result = new HashMap<>();
        String[] holidayList = debitDateHolidays.split(",");

        for (String holiday : holidayList) {
            IgnitionUtility.calculateDebitDateHolidays(holiday, result);
        }

        logger.debug("{} debit date holidays found", result.size());
        this.debitDateHolidays = result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet () throws Exception {
        allServiceProfiles = (Map<String, Map<String, ServiceProfile>>) Context.getApplicationContext()
                .getBean("allServiceProfilesGroupedByBrand", entityId);
    }
    private Integer findActiveOrdersForPayment (Integer userId) {

        List<OrderDTO> orders = orderDAS.findByUserSubscriptions(userId);

        if (orders.isEmpty()) {
            return null;
        }
        if(orders.size()>1) {
            logger.debug("More than one Active order found for the User {}",userId);
        }
        return orders.get(0).getId();
    }
}
