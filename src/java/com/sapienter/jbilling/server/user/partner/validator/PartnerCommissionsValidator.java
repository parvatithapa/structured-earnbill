package com.sapienter.jbilling.server.user.partner.validator;


import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.user.partner.*;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class PartnerCommissionsValidator {
    public final static String COMMISSION_EXCEPTION_EMPTY_PRODUCT_ID = "partner.error.commissionException.emptyProductId";
    public final static String COMMISSION_EXCEPTION_INVALID_PRODUCT_ID = "partner.error.commissionException.invalidProductId";
    public final static String COMMISSION_EXCEPTION_PRODUCT_NOT_FOUND = "partner.error.commissionException.productNotFound";
    public final static String COMMISSION_EXCEPTION_EMPTY_START_DATE = "partner.error.commissionException.emptyStartDate";
    public final static String COMMISSION_EXCEPTION_INVALID_DATE_RANGE = "partner.error.commissionException.invalidDateRange";
    public final static String COMMISSION_EXCEPTION_EMPTY_PERCENTAGE = "partner.error.commissionException.emptyPercentage";
    public final static String COMMISSION_EXCEPTION_INVALID_PERCENTAGE = "partner.error.commissionException.invalidPercentage";
    public final static String COMMISSION_EXCEPTION_DATES_OVERLAP = "partner.error.commissionException.datesOverlap";
    public final static String COMMISSION_EXCEPTION_INVALID_START_DATE_VALUE = "partner.error.commissionException.invalidStartDateValue";
    public final static String COMMISSION_EXCEPTION_INVALID_END_DATE_VALUE = "partner.error.commissionException.invalidEndDateValue";

    public final static String REFERRAL_COMMISSION_EMPTY_PARTNER_ID = "partner.error.referrerCommission.emptyPartnerId";
    public final static String REFERRAL_COMMISSION_INVALID_PARTNER_ID = "partner.error.referrerCommission.invalidPartnerId";
    public final static String REFERRAL_COMMISSION_PARTNER_NOT_FOUND = "partner.error.commissionException.partnerNotFound";
    public final static String REFERRAL_COMMISSION_SAME_AS_REFERRER = "partner.error.commissionException.sameAsReferrer";
    public final static String REFERRAL_COMMISSION_EMPTY_START_DATE = "partner.error.referrerCommission.emptyStartDate";
    public final static String REFERRAL_COMMISSION_INVALID_DATE_RANGE = "partner.error.referrerCommission.invalidDateRange";
    public final static String REFERRAL_COMMISSION_EMPTY_PERCENTAGE = "partner.error.referrerCommission.emptyPercentage";
    public final static String REFERRAL_COMMISSION_INVALID_PERCENTAGE = "partner.error.referrerCommission.invalidPercentage";
    public final static String REFERRAL_COMMISSION_DATES_OVERLAP = "partner.error.referrerCommission.datesOverlap";
    public final static String REFERRAL_COMMISSION_INVALID_START_DATE_VALUE = "partner.error.referrerCommission.invalidStartDateValue";
    public final static String REFERRAL_COMMISSION_INVALID_END_DATE_VALUE = "partner.error.referrerCommission.invalidEndDateValue";

    public final static String TYPE_INVALID = "partner.error.commission.type.invalid";
    public final static String TYPE_CUSTOMER_ONE_VALUE = "partner.error.commission.customer.one.value";
    public final static String TYPE_CONSUMPTION_AT_LEAST_ONE_VALUE = "partner.error.commission.consumption.at.least.one.value";
    public final static String COMMISSION_VALUE_DAY_UNIQUE = "partner.error.commission.value.days.unique";

    public String validate(PartnerWS partner) {
        String error = validateCommissionExceptions(partner.getCommissionExceptions());
        if (error != null) return error;
        error = validateReferrerCommissions(partner.getReferrerCommissions());
        if( error != null) return error;
        error = validateCommissionValues(partner);
        return error;
    }

    private String validateCommissionValues(PartnerWS partner) {
        PartnerCommissionType type;
        if(!StringUtils.isBlank(partner.getCommissionType())) {
            try {
                type = PartnerCommissionType.valueOf(partner.getCommissionType());
            } catch (Exception e) {
                return TYPE_INVALID;
            }

            if(type.equals(PartnerCommissionType.CUSTOMER)) {
                if(partner.getCommissionValues() == null || partner.getCommissionValues().length != 1) {
                    return TYPE_CUSTOMER_ONE_VALUE;
                }
            } else if(type.equals(PartnerCommissionType.CONSUMPTION)) {
                if(partner.getCommissionValues() == null || partner.getCommissionValues().length == 0) {
                    return TYPE_CONSUMPTION_AT_LEAST_ONE_VALUE;
                }
            }
        }

        Set<Integer> days = new HashSet<>();
        if(partner.getCommissionValues() != null) {
            for(PartnerCommissionValueWS value : partner.getCommissionValues()) {
                if(days.contains(value.getDays())) {
                    return COMMISSION_VALUE_DAY_UNIQUE;
                }
                days.add(value.getDays());
            }
        }
        return null;
    }

    private String validateCommissionExceptions(PartnerCommissionExceptionWS[] commissionExceptions) {
        if (commissionExceptions != null) {
            for (PartnerCommissionExceptionWS commissionException : commissionExceptions) {
                // Validate that the product id is not empty, it's a number and the item exists.
                ItemDAS itemDAS = new ItemDAS();

                if (commissionException.getItemId() == null) {
                    return COMMISSION_EXCEPTION_EMPTY_PRODUCT_ID;
                } else if (commissionException.getItemId() == 0 || commissionException.getItemId() < 0) {
                    return COMMISSION_EXCEPTION_INVALID_PRODUCT_ID;
                } else {
                    if (itemDAS.findNow(commissionException.getItemId()) == null) {
                        return COMMISSION_EXCEPTION_PRODUCT_NOT_FOUND;
                    }
                }

                // Validate that the start date is not empty and the range is valid.
                if (commissionException.getStartDate() == null) {
                    return COMMISSION_EXCEPTION_EMPTY_START_DATE;
                }

                if (!isValidYear(commissionException.getStartDate())) {
                    return COMMISSION_EXCEPTION_INVALID_START_DATE_VALUE;
                }

                if (commissionException.getEndDate() != null) {
                    if (isValidYear(commissionException.getEndDate())) {
                        if (commissionException.getStartDate().after(commissionException.getEndDate())) {
                            return COMMISSION_EXCEPTION_INVALID_DATE_RANGE;
                        }
                    } else {
                        return COMMISSION_EXCEPTION_INVALID_END_DATE_VALUE;
                    }
                }

                // Validate that the percentage is not empty and is a valid number from 0 to 100.
                if (commissionException.getPercentage() == null) {
                    return COMMISSION_EXCEPTION_EMPTY_PERCENTAGE;
                }
                if (commissionException.getPercentageAsDecimal() == null) {
                    return COMMISSION_EXCEPTION_INVALID_PERCENTAGE;
                } else if (commissionException.getPercentageAsDecimal().compareTo(BigDecimal.ZERO) < 0
                        || commissionException.getPercentageAsDecimal().compareTo(new BigDecimal(100)) > 0) {
                    return COMMISSION_EXCEPTION_INVALID_PERCENTAGE;
                }
            }

            boolean isNullLastCommissionExceptionEndDate = false;

            for (PartnerCommissionExceptionWS current : commissionExceptions) {
                for (PartnerCommissionExceptionWS other : commissionExceptions) {
                    if (!current.equals(other) && current.getItemId().equals(other.getItemId())) {
                        // validate that the date ranges do not overlap.
                        if (current.getEndDate() != null) {
                            if (other.getEndDate() != null) {
                                if (!((current.getStartDate().before(other.getEndDate()) && current.getEndDate().before(other.getStartDate())) ||
                                        (current.getStartDate().after(other.getEndDate()) && current.getEndDate().after(other.getStartDate())))) {
                                    return COMMISSION_EXCEPTION_DATES_OVERLAP;
                                }
                            } else if (current.getEndDate().after(other.getStartDate()) || current.getEndDate().equals(other.getStartDate())) {
                                return COMMISSION_EXCEPTION_DATES_OVERLAP;
                            }
                        } else {
                            if (isNullLastCommissionExceptionEndDate) {
                                return COMMISSION_EXCEPTION_DATES_OVERLAP;
                            }
                            isNullLastCommissionExceptionEndDate = true;
                        }
                    }
                }
            }

        }
        return null;
    }

    private String validateReferrerCommissions(PartnerReferralCommissionWS[] referrerCommissions) {
        if (referrerCommissions != null) {
            for (PartnerReferralCommissionWS referrerCommission : referrerCommissions) {
                // Validate that the partner id is not empty, it's a number and the partner exists.
                PartnerDAS partnerDAS = new PartnerDAS();

                if (referrerCommission.getReferralId() == null) {
                    return REFERRAL_COMMISSION_EMPTY_PARTNER_ID;
                } else if (referrerCommission.getReferralId() == 0 || referrerCommission.getReferralId() < 0) {
                    return REFERRAL_COMMISSION_INVALID_PARTNER_ID;
                } else if (partnerDAS.findNow(referrerCommission.getReferralId()) == null) {
                    return REFERRAL_COMMISSION_PARTNER_NOT_FOUND;
                } else if (referrerCommission.getReferralId().equals(referrerCommission.getReferrerId())) {
                    return REFERRAL_COMMISSION_SAME_AS_REFERRER;
                }

                // Validate that the start date is not empty and the range is valid.
                if (referrerCommission.getStartDate() == null) {
                    return REFERRAL_COMMISSION_EMPTY_START_DATE;
                }

                if (!isValidYear(referrerCommission.getStartDate())) {
                    return REFERRAL_COMMISSION_INVALID_START_DATE_VALUE;
                }

                if (referrerCommission.getEndDate() != null) {
                    if (isValidYear(referrerCommission.getEndDate())) {
                        if (referrerCommission.getStartDate().after(referrerCommission.getEndDate())) {
                            return REFERRAL_COMMISSION_INVALID_DATE_RANGE;
                        }
                    } else {
                        return REFERRAL_COMMISSION_INVALID_END_DATE_VALUE;
                    }
                }

                // Validate that the Range of Dates is not already defined in another Commission Exception

                // Validate that the percentage is not empty and is a valid number from 0 to 100.
                if (referrerCommission.getPercentageAsDecimal() == null) {
                    return REFERRAL_COMMISSION_EMPTY_PERCENTAGE;
                } else if (referrerCommission.getPercentageAsDecimal().compareTo(BigDecimal.ZERO) < 0
                        || referrerCommission.getPercentageAsDecimal().compareTo(new BigDecimal(100)) > 0) {
                    return REFERRAL_COMMISSION_INVALID_PERCENTAGE;
                }
            }

            boolean isNullLastReferralCommissionEndDate = false;

            for (PartnerReferralCommissionWS current : referrerCommissions) {
                for (PartnerReferralCommissionWS other : referrerCommissions) {
                    if (!current.equals(other) && current.getReferralId().equals(other.getReferralId())) {
                        // validate that the date ranges do not overlap.
                        if (current.getEndDate() != null) {
                            if (other.getEndDate() != null) {
                                if (!((current.getStartDate().before(other.getEndDate()) && current.getEndDate().before(other.getStartDate())) ||
                                        (current.getStartDate().after(other.getEndDate()) && current.getEndDate().after(other.getStartDate())))) {
                                    return REFERRAL_COMMISSION_DATES_OVERLAP;
                                }
                            } else if (current.getEndDate().after(other.getStartDate()) || current.getEndDate().equals(other.getStartDate())) {
                                return REFERRAL_COMMISSION_DATES_OVERLAP;
                            }
                        } else {
                            if (isNullLastReferralCommissionEndDate) {
                                return REFERRAL_COMMISSION_DATES_OVERLAP;
                            }
                            isNullLastReferralCommissionEndDate = true;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidYear(Date date) {
        DateTime datetime = new DateTime(date);
        int year = datetime.getYear();
        if (year > 0 && year <= 9999)
            return true;
        else {
            return false;
        }
    }
}
