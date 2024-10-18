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

package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.process.event.ApplySuspendedPeriods;
import com.sapienter.jbilling.server.process.event.ReactivatedStatusEvent;
import com.sapienter.jbilling.server.process.event.SuspendedStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.CustomerStatusChangeHistoryDAS;
import com.sapienter.jbilling.server.user.db.CustomerStatusChangeHistoryDTO;
import com.sapienter.jbilling.server.user.db.CustomerStatusType;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This simple task will manage all the events triggered for Suspension/Activation feature
 * 
 * @author Leandro Zoi
 * @since 01/15/18
 */
public class ActivePeriodChargingTask extends PluggableTask implements IInternalEventsTask {

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            SuspendedStatusEvent.class,
            ReactivatedStatusEvent.class,
            ApplySuspendedPeriods.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        CustomerStatusChangeHistoryDAS customerStatusChangeHistoryDAS = new CustomerStatusChangeHistoryDAS();
        if (event instanceof SuspendedStatusEvent) {
            SuspendedStatusEvent suspendedStatusEvent = (SuspendedStatusEvent) event;
            UserDTO user = suspendedStatusEvent.getUser();
            UserStatusDTO status = suspendedStatusEvent.getStatus();

            if (status.isSuspended() && !customerStatusChangeHistoryDAS.hasSuspendenHistoryOpen(user.getId())) {
                customerStatusChangeHistoryDAS.createCustomerHistory(suspendedStatusEvent.getUser(),
                                                                     suspendedStatusEvent.getUserLoggedName(),
                                                                     suspendedStatusEvent.getStatusDescription(),
                                                                     CustomerStatusType.SUSPENDED,
                                                                     suspendedStatusEvent.getDate());
            }

        } else if (event instanceof ReactivatedStatusEvent) {
            ReactivatedStatusEvent reactivatedStatusEvent = (ReactivatedStatusEvent) event;
            UserStatusDTO status = reactivatedStatusEvent.getStatus();
            UserDTO user = reactivatedStatusEvent.getUser();
            AgeingEntityStepDTO step = reactivatedStatusEvent.getUser().getUserStatus().getAgeingEntityStep();

            if (status.getId() == UserDTOEx.STATUS_ACTIVE && (step == null || step.getSuspend() == 1) &&
                    customerStatusChangeHistoryDAS.hasSuspendenHistoryOpen(user.getId())) {
                customerStatusChangeHistoryDAS.createCustomerHistory(user,
                                                                     reactivatedStatusEvent.getUserLoggedName(),
                                                                     reactivatedStatusEvent.getStatusDescription(),
                                                                     CustomerStatusType.ACTIVE,
                                                                     reactivatedStatusEvent.getDate());
            }
        } else if (event instanceof ApplySuspendedPeriods) {
            ApplySuspendedPeriods reactivatedStatusEvent = (ApplySuspendedPeriods) event;
            Integer userId = reactivatedStatusEvent.getUserId();
            List<SuspendedCycle> cycles = reactivatedStatusEvent.getCycles();
            UserDTO user = new UserBL(userId).getDto();
            Date startDate = reactivatedStatusEvent.getStartDate();
            Date endDate = reactivatedStatusEvent.getEndDate();

            if (user.getUserStatus().getAgeingEntityStep() == null || user.getUserStatus().getAgeingEntityStep().getSuspend() == 0) {
                List<CustomerStatusChangeHistoryDTO> suspendedRecords =  customerStatusChangeHistoryDAS.getHistoryByUserAndType(userId, CustomerStatusType.SUSPENDED, startDate, endDate);
                List<CustomerStatusChangeHistoryDTO> reactivatedRecords =  customerStatusChangeHistoryDAS.getHistoryByUserAndType(userId, CustomerStatusType.ACTIVE, startDate, endDate);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                IntStream.range(0, Math.min(suspendedRecords.size(), reactivatedRecords.size()))
                         .filter(i -> !(formatter.format(suspendedRecords.get(i).getModifiedAt())
                                                           .equals(formatter.format(reactivatedRecords.get(i).getModifiedAt()))))
                         .mapToObj(i -> new SuspendedCycle(suspendedRecords.get(i).getModifiedAt(),
                                                           reactivatedRecords.get(i).getModifiedAt()))
                         .collect(Collectors.toCollection(() -> cycles));
            } else {
                CustomerStatusChangeHistoryDTO lastHistory =  customerStatusChangeHistoryDAS.getLastSuspendedPeriod(userId);
                if (lastHistory != null) {
                    cycles.add(new SuspendedCycle(lastHistory.getModifiedAt(), null));
                }
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public class SuspendedCycle {
        private Date startDate;
        private Date endDate;

        public SuspendedCycle(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }

        public List<PeriodOfTime> splitPeriods(PeriodOfTime period, SuspendedCycle suspendedCycle, Boolean isPreviousCyclePresent) {
            List<PeriodOfTime> periods = new ArrayList<>();

            if (this.getEndDate() == null) {
                if (period.getStart() != null && this.getStartDate().after(period.getStart())) {
                    periods.add(new PeriodOfTime(period.getStart(), minDate(this.getStartDate(), period.getEnd()), period.getDaysInCycle()));
                } else if (period.getStart() == null) {
                    periods.add(PeriodOfTime.OneTimeOrderPeriodOfTime);
                }
            } else if (period.getStart() == null && period.getEnd() == null) {
                periods.add(PeriodOfTime.OneTimeOrderPeriodOfTime);
            } else if (this.getStartDate().after(period.getStart()) && this.getEndDate().before(period.getEnd())) {
                Date periodStartDate = period.getStart();
                Date periodEndDate = period.getEnd();
                if (null != suspendedCycle) {
                    periodEndDate = suspendedCycle.getStartDate().before(period.getEnd()) ? suspendedCycle.getStartDate() :
                        period.getEnd();
                }
                if(isPreviousCyclePresent) {
                    periodStartDate = this.getStartDate();
                }
                periods.add(new PeriodOfTime(periodStartDate, this.getStartDate(), period.getDaysInCycle()));
                periods.add(new PeriodOfTime(this.getEndDate(), periodEndDate, period.getDaysInCycle()));
            } else if (this.getStartDate().before(period.getStart()) && this.getEndDate().before(period.getEnd()) &&
                            this.getEndDate().after(period.getStart())) {
                periods.add(new PeriodOfTime(this.getEndDate(), period.getEnd(), period.getDaysInCycle()));
            } else if (!this.getEndDate().before(period.getEnd()) && this.getStartDate().after(period.getStart())) {
                periods.add(new PeriodOfTime(period.getStart(), minDate(this.getStartDate(), period.getEnd()), period.getDaysInCycle()));
            } else if (this.getStartDate().before(period.getStart()) && this.getEndDate().before(period.getStart())) {
                periods.add(new PeriodOfTime(period.getStart(), period.getEnd(), period.getDaysInCycle()));
            }

            return periods;
        }

        private Date minDate(Date date1, Date date2) {
            return date1.before(date2) ? date1 : date2;
        }
    }
}
