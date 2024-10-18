/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2013] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing.cache;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pricing.MatchingRecord;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;

import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;

/**
 *  Type of Matching Field
 *  <p/>
 *  Each matching field can be applied to a matching field result
 *
 * @author Panche Isajeski
 * @since  18-Aug-2013
 */
public enum MatchingFieldType {

    BEST_MATCH () {
        @Override
        public void apply (final MatchingFieldDTO matchingFieldDTO, final PricingField inputParameter, final MatchingFieldResult matchingFieldResult) {
            if (inputParameter != null) {
                Integer longestValue;
                Integer smallestValue;

                if (matchingFieldDTO.getRoute() != null && matchingFieldDTO.getRoute().getId() != null) {
                    RouteBL routeBL = new RouteBL(matchingFieldDTO.getRoute().getId());
                    longestValue = routeBL.getLongestValueFor(matchingFieldDTO.getMatchingField());
                    smallestValue = routeBL.getSmallestValueFor(matchingFieldDTO.getMatchingField());
                } else {
                    RouteBasedRateCardBL rateCardBL = new RouteBasedRateCardBL(matchingFieldDTO.getRouteRateCard().getId());
                    longestValue = rateCardBL.getLongestValueFor(matchingFieldDTO.getMatchingField());
                    smallestValue = rateCardBL.getSmallestValueFor(matchingFieldDTO.getMatchingField());
                }

                matchingFieldResult.addBestMatchCriteria(matchingFieldDTO.getMatchingField(),
                                                         inputParameter.getStrValue(),
                                                         smallestValue,
                                                         longestValue);
            }
        }
    },

    EXACT () {
        @Override
        public void apply (final MatchingFieldDTO matchingFieldDTO, final PricingField inputParameter, final MatchingFieldResult matchingFieldResult) {
            if (inputParameter != null) {
                matchingFieldResult.addExactCriteria(matchingFieldDTO.getMatchingField(), inputParameter.getStrValue());
            }
        }
    },

    TIME () {

        private DateTimeFormatter timeDateFormat = DateTimeFormat.forPattern("HH:mm");

        @Override
        public void apply(final MatchingFieldDTO matchingFieldDTO, final PricingField inputParameter, final MatchingFieldResult matchingFieldResult) {
            matchingFieldResult.addFilterCriteria(matchingFieldDTO, new FilterCallback() {
                @Override
                public boolean accept(MatchingFieldDTO matchingFieldDTO, MatchingRecord record) throws Exception {
                    final Date eventDate= null!= inputParameter ? inputParameter.getDateValue() : null;

                    String activeTimeRange = record.getAttributes().get(matchingFieldDTO.getMatchingField());
                    Date startTime = null;
                    Date endTime = null;

                    String[] dateRange = activeTimeRange.split("-");
                    if (dateRange.length > 0) {
                        startTime = timeDateFormat.parseDateTime(dateRange[0]).toDate();

                        if (dateRange.length > 1) {
                            endTime = timeDateFormat.parseDateTime(dateRange[1]).toDate();
                        } else {
                            endTime = DateConvertUtils.asUtilDate(LocalDate.now());
                        }
                    }

                    if (startTime == null || endTime == null) {
                        throw new SessionInternalError("Cannot find start or end time of day");
                    }

                    return eventDate != null && (!startTime.after(eventDate) && eventDate.before(endTime));
                }
            });
        }
    },
    DAY_OF_WEEK () {

        private DateTimeFormatter dayOfWeekFormat = DateTimeFormat.forPattern("EE");

        @Override
        public void apply(final MatchingFieldDTO matchingFieldDTO, final PricingField inputParameter, final MatchingFieldResult matchingFieldResult) {
            matchingFieldResult.addFilterCriteria(matchingFieldDTO, new FilterCallback() {
                @Override
                public boolean accept(MatchingFieldDTO matchingFieldDTO, MatchingRecord record) throws Exception {

                    final Date eventDate= null!= inputParameter ? inputParameter.getDateValue() : null;
                    String eventDayOfWeek = dayOfWeekFormat.print(eventDate.getTime());

                    String dayOfWeekRange = record.getAttributes().get(matchingFieldDTO.getMatchingField());
                    String[] dayOfWeeks = dayOfWeekRange.split("-");
                    return (Arrays.asList(dayOfWeeks)).contains(eventDayOfWeek);
                }
            });
        }
    },
    ACTIVE_DATE () {

        private DateTimeFormatter dateFormat = DateTimeFormat.forPattern("MM/dd/yyyy");

        @Override
        public void apply(final MatchingFieldDTO matchingFieldDTO, final PricingField inputParameter, final MatchingFieldResult matchingFieldResult) {

            Object inputObject= inputParameter.getValue();
            Date eventDateObject = null;
            if (inputObject instanceof Date) {
                eventDateObject = (Date) inputObject;
            } else {
                try {
                    eventDateObject = dateFormat.parseDateTime(inputObject.toString()).toDate();
                } catch (IllegalArgumentException e) {
                    throw new SessionInternalError("The date value in the \""+matchingFieldDTO.getDescription()+"\" field is invalid.");
                }
            }

            final Date eventDate = eventDateObject;
            if (eventDate != null) {

                matchingFieldResult.addFilterCriteria(matchingFieldDTO, new FilterCallback() {
                    @Override
                    public boolean accept(MatchingFieldDTO matchingFieldDTO, MatchingRecord record) throws Exception {
                        String activeDateRange = record.getAttributes().get(matchingFieldDTO.getMatchingField());
                        Date startDate = null;
                        Date endDate = null;

                        String[] dateRange = activeDateRange.split("-");
                        if (dateRange.length > 0) {
                            startDate = dateFormat.parseDateTime(dateRange[0].trim()).toDate();

                            if (dateRange.length > 1) {
                                endDate = dateFormat.parseDateTime(dateRange[1].trim()).toDate();
                            } else {
                                endDate = new Date((long) Double.POSITIVE_INFINITY);
                            }

                        }

                        if (startDate == null || endDate == null) {
                            throw new SessionInternalError("Cannot find start or end date");
                        }

                        return eventDate != null && (!startDate.after(eventDate) && eventDate.before(endDate));
                    }
                });
            }
        }
    };

    public abstract void apply(MatchingFieldDTO matchingFieldDTO, PricingField inputParameter, MatchingFieldResult matchingFieldResult);
}
