/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.ProRatePeriodCalculator;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class ProRateOrderPeriodUtil {

    public static Date calculateCycleStarts (OrderDTO order, Date periodStart) {

        LocalDate retValue = null;
        LocalDate startDate = DateConvertUtils.asLocalDate(periodStart);
        List<Integer> results = new OrderProcessDAS().findActiveInvoicesForOrder(order.getId());
        Date nextBillableDayFromOrderChanges = order.calcNextBillableDayFromChanges();
        MainSubscriptionDTO mainSubscription = order.getUser()
                                                    .getCustomer()
                                                    .getMainSubscription();

        ProRatePeriodCalculator calculator = ProRatePeriodCalculator.valueOfPeriodUnit(mainSubscription.getSubscriptionPeriod()
                                                                                                       .getPeriodUnit()
                                                                                                       .getId());

        if (!results.isEmpty() && nextBillableDayFromOrderChanges != null) {
            retValue = DateConvertUtils.asLocalDate(nextBillableDayFromOrderChanges);
        } else {
            for (OrderLineDTO line : order.getLines()) {
                for (OrderChangeDTO change : line.getOrderChanges()) {
                    LocalDate nextBillableDate = DateConvertUtils.asLocalDate(change.getNextBillableDate() == null ? change.getStartDate():
                                                                                                                     change.getNextBillableDate());

                    if ((retValue == null) || nextBillableDate.isBefore(retValue)) {
                        retValue = nextBillableDate;
                    }
                }
            }

            if (retValue == null) {
                retValue = DateConvertUtils.asLocalDate(order.getActiveSince() != null ? order.getActiveSince():
                                                                                         order.getCreateDate());
            }
        }

        retValue = calculator.getDate(retValue, mainSubscription);
        while (retValue.isAfter(startDate)) {
            retValue = calculator.getNextBeforeDate(retValue, mainSubscription);
        }

        return DateConvertUtils.asUtilDate(retValue);
    }
}
