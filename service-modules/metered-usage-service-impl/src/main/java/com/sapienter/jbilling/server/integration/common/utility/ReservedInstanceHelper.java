package com.sapienter.jbilling.server.integration.common.utility;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.MeteredUsageServiceImpl;
import com.sapienter.jbilling.server.integration.common.service.HelperDataAccessService;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedPlanInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedUsageInfo;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;

/**
 * Created by abhishek.yadav
 */
public class ReservedInstanceHelper {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    @Setter
    private HelperDataAccessService dataAccessService;

    public boolean isReservedInstancePlan(Integer entityId, Integer planId) {

        MetaField metafieldPaymentOption = dataAccessService.getFieldByName(entityId, EntityType.PLAN, Constants.PLAN_PAYMENT_OPTION_MF);
        MetaField metafieldDuration = dataAccessService.getFieldByName(entityId, EntityType.PLAN, Constants.PLAN_DURATION_MF);
        if (null == metafieldPaymentOption || null == metafieldDuration) {
            return false;
        }
        MetaFieldValueWS paymentOption = dataAccessService.getPlanMetafieldValue(entityId, planId, metafieldPaymentOption.getId());
        if (null == paymentOption) {
            return false;
        }
        MetaFieldValueWS duration = dataAccessService.getPlanMetafieldValue(entityId, planId, metafieldDuration.getId());
        return (null != duration);
    }

    public Optional<MetaFieldValueWS> getMetafieldValueByName(String metafieldName, Integer entityId, Integer id, EntityType entityType) {

        MetaField metafield = dataAccessService.getFieldByName(entityId, entityType, metafieldName);
        if (null == metafield ) {
            return Optional.empty();
        }
        MetaFieldValueWS metafieldValue = dataAccessService.getPlanMetafieldValue(entityId, id, metafield.getId());
        return  Optional.ofNullable(metafieldValue);
    }

    public ReservedPlanInfo getReservedPlanInfo(Integer entityId, int planId, BigDecimal price) {

        return dataAccessService.getReservedPlanInfo(entityId, planId, price);
    }

    public List<ReservedUsageInfo> getReservedUsageInfo(Integer entityId, Integer orderLineId, Integer languageId) {

        List<ReservedUsageInfo> reservedUsageInfos = new ArrayList<>();
        Map<Integer, BigDecimal> customerPoolIdsAndQuantiy = dataAccessService.getCustomerPoolsWithUtilizedQty(entityId, orderLineId, languageId);
        if (customerPoolIdsAndQuantiy.isEmpty()) {
            return reservedUsageInfos;
        }

        Integer planId;

        for (Map.Entry<Integer, BigDecimal> entry : customerPoolIdsAndQuantiy.entrySet()) {
            planId = dataAccessService.getPlanAssociatedToCustomerPool(entityId, entry.getKey(), languageId);
            if (isReservedInstancePlan(entityId, planId)) {
                ReservedUsageInfo reservedUsageInfo = ReservedUsageInfo.builder()
                    .planId(planId)
                    .quantity(entry.getValue()).build();
                reservedUsageInfos.add(reservedUsageInfo);
            }
        }
        return reservedUsageInfos;
    }

    public BigDecimal prorateMonthlyReservedPurchaseQuantity(BigDecimal quantity, Date activeSince) {

        Calendar calendarLastDayOfMonth = Calendar.getInstance();
        int lastDateInt = calendarLastDayOfMonth.getActualMaximum(Calendar.DATE);
        calendarLastDayOfMonth.set(Calendar.DATE, lastDateInt);
        DateUtility.setTimeToEndOfDay(calendarLastDayOfMonth);
        Date lastDayOfMonth = calendarLastDayOfMonth.getTime();

        Calendar calendarActiveDate = Calendar.getInstance();
        calendarActiveDate.setTime(activeSince);
        DateUtility.setTimeToStartOfDay(calendarActiveDate);
        Date activeSinceDate = calendarActiveDate.getTime();

        if (calendarLastDayOfMonth.get(Calendar.YEAR) == calendarActiveDate.get(Calendar.YEAR) &&
            calendarLastDayOfMonth.get(Calendar.MONTH) == calendarActiveDate.get(Calendar.MONTH)) {
            BigDecimal proratedQuantity;

            try {
                proratedQuantity = prorateQuantity(activeSinceDate, lastDayOfMonth, quantity);
            } catch (SessionInternalError se) {
                logger.warn("prorating Reserved Purchase caused error {}", se.getMessage());
                proratedQuantity = BigDecimal.ZERO;
            }
            return proratedQuantity;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal prorateQuantity(Date fromDate, Date toDate, BigDecimal quantity) {

        long difference = toDate.getTime() - fromDate.getTime();
        if (difference < 0 ) {
            throw new SessionInternalError("Invalid Arguments Passed To prorateQuantity Method in " + MeteredUsageServiceImpl.class);
        }
        long daysCharged = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS) + 1;
        Integer noOfDaysInMonth = DateUtility.numberOfDaysInMonth(fromDate);
        return quantity.multiply(BigDecimal.valueOf(daysCharged).divide(new BigDecimal(noOfDaysInMonth), 6, RoundingMode.HALF_DOWN)).setScale(10);
    }

    public BigDecimal getAdjustment(BigDecimal priceReported, Date activeUntil){
        Calendar cal = Calendar.getInstance();
        cal.setTime(activeUntil);
        int monthMaxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int noOfDaysRemaining = monthMaxDays - cal.get(Calendar.DATE);
        BigDecimal adjustment = BigDecimal.valueOf(noOfDaysRemaining).divide(BigDecimal.valueOf(monthMaxDays),6, RoundingMode.HALF_DOWN);
        adjustment = adjustment.multiply(priceReported).setScale(10);
        return adjustment.negate();
    }
}
