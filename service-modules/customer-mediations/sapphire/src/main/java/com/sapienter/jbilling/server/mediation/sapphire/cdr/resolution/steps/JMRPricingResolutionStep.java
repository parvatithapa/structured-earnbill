package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CALL_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CARRIER_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.COUNTRY_CODE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.DEST_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.INTERNATIONAL;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.LANDLINE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.LOCATION;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.MOBILE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.NATIONAL;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OFF_PEAK;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OFF_PEAK_DATE_FORMAT;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ORIGINAL_QUANTITY;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OTHER_CARRIER_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.PEAK_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.REQUESTED_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.REST_OF_WORLD;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.UNDERSCORE;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;

public class JMRPricingResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SapphireMediationHelperService service;
    private final Map<String, String> countryCodeNameMap;

    public JMRPricingResolutionStep(SapphireMediationHelperService service) {
        this.service = service;
        this.countryCodeNameMap = createCountryCodeNameMap();
        Assert.isTrue(MapUtils.isNotEmpty(countryCodeNameMap), "Country code Name map creation failed!");
        logger.debug("country code name map {}", this.countryCodeNameMap);
    }

    /**
     * Creates Country Code and Name map.
     * @return
     */
    private Map<String, String> createCountryCodeNameMap() {
        Map<String, String> map = new HashMap<>();
        for (String country : Locale.getISOCountries()) {
            Locale locale = new Locale("en", country);
            map.put(locale.getCountry(), locale.getDisplayCountry());
        }
        return map;
    }


    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            List<PricingField> fields = context.getPricingFields();
            if(result.hasError()) {
                logger.debug("Recycle Process will add additional pricing fields!");
                return false;
            }
            String destAddr = context.getPricingField(DEST_ADDR).getStrValue();

            PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            PhoneNumber phoneNumber = phoneNumberUtil.parse(destAddr, StringUtils.EMPTY);
            if(!phoneNumberUtil.isValidNumber(phoneNumber)) {
                result.addError("DESTINATION-NUMBER-IS-INVALID");
                return false;
            }

            Locale locale = service.getLocaleForEntity(context.getEntityId());

            // Get country info
            Integer country = phoneNumber.getCountryCode();

            String regionCode = phoneNumberUtil.getRegionCodeForNumber(phoneNumber);
            if(StringUtils.isNotEmpty(regionCode)) {
                regionCode = regionCode.toUpperCase();
            }
            String location = StringUtils.EMPTY;
            if(countryCodeNameMap.containsKey(regionCode)) {
                location = countryCodeNameMap.get(regionCode);
                logger.debug("Country Name {} found for country code {} from desitnation number {}", location, regionCode, destAddr);
            } else {
                logger.debug("no country name found for country code {}", regionCode);
            }

            String itemId = result.getItemId();
            Date eventDate = result.getEventDate();
            if(service.getRouteRateCardForItem(itemId, eventDate).isPresent()) {
                // Verify if the country and location is defined in the RateCards,
                // else set the country as `0`
                // and location as ROW - Rest Of World
                boolean isCountryPresent = service.isCountryPresentForItemAndCountry(itemId, eventDate,
                        String.valueOf(country), location);
                if(!isCountryPresent) {
                    logger.debug("Country not found in the Rate Card, searching with '{}'", REST_OF_WORLD);
                    country = 0;
                    location = REST_OF_WORLD;
                }
            } else {
                logger.debug("Item {} has no rate card on it", itemId);
            }
            // Adding country info and location info
            PricingField countryCode = new PricingField(COUNTRY_CODE, country);
            fields.add(countryCode);
            PricingField numberLocation = new PricingField(LOCATION, location);
            fields.add(numberLocation);

            // Adding callType info
            PhoneNumberType type = phoneNumberUtil.getNumberType(phoneNumber);
            String requestedAddr = context.getPricingField(REQUESTED_ADDR).getStrValue();
            StringBuilder callType = new StringBuilder();
            if(StringUtils.isNotBlank(requestedAddr) &&
                    requestedAddr.startsWith("00")) {
                callType.append(INTERNATIONAL);
            } else {
                callType.append(NATIONAL);
            }
            callType.append(UNDERSCORE);

            if(!PhoneNumberType.UNKNOWN.equals(type)) {
                if(PhoneNumberType.MOBILE.equals(type)) {
                    callType.append(MOBILE);
                    // Adding carrier name
                    PhoneNumberToCarrierMapper carrierMapper = PhoneNumberToCarrierMapper.getInstance();
                    String carrierValue = carrierMapper.getNameForNumber(phoneNumber, locale);
                    logger.debug("Carrier resolved by google library : {}", carrierValue);
                    if(StringUtils.isEmpty(carrierValue) ||
                            !service.isCarrierNamePresentForItemAndCountry(result.getItemId(),
                                    result.getEventDate(), carrierValue.toUpperCase(), location)) {
                        carrierValue = OTHER_CARRIER_NAME;
                    }
                    PricingField carrierName = new PricingField(CARRIER_NAME, carrierValue.toUpperCase());
                    logger.debug("carrierName {}", carrierName);
                    fields.add(carrierName);
                } else {
                    callType.append(LANDLINE);
                }
            } else {
                //TODO Add logic for NGN and satellite phone call
            }
            PricingField callTypeField = new PricingField(CALL_TYPE, callType.toString());
            logger.debug("callTypeField {}", callTypeField);
            fields.add(callTypeField);


            // Adding duration
            PricingField duration = new PricingField(SapphireMediationConstants.DURATION, context.getResult().getQuantity());
            logger.debug("duration {}", duration);
            fields.add(duration);

            SimpleDateFormat dateFormater = new SimpleDateFormat(OFF_PEAK_DATE_FORMAT);
            Date connectTime = dateFormater.parse(dateFormater.format(result.getEventDate()));
            String peakValue  = service.getMetaFieldsForEntity(context.getEntityId()).get(PEAK_FIELD_NAME);

            PricingField offPeak = new PricingField(OFF_PEAK, checkTimeInBetweenRange(connectTime, peakValue));
            logger.debug("offPeak {}", offPeak);
            fields.add(offPeak);

            PricingField originalQuantity = new PricingField(ORIGINAL_QUANTITY, result.getOriginalQuantity());
            logger.debug("originalQuantity {}", originalQuantity);
            fields.add(originalQuantity);
            result.setPricingFields(fields.stream().map(PricingField::encode).collect(Collectors.joining(",")));
            return true;
        } catch(Exception ex) {
            logger.error("Error in JMRPricingResolutionStep!", ex);
            result.addError("ERROR-PRICING-FIELDS-RESOLUTION");
            return false;
        }

    }

    private static boolean checkTimeInBetweenRange(Date connectTime, String range) {
        String[] rangeArray = range.split("-");
        String startRange = rangeArray[0];
        String endRange = rangeArray[1];

        String[] startRangeArray = startRange.split(":");
        String[] endRangeArray = endRange.split(":");

        logger.debug("start range {}", Arrays.toString(startRangeArray));
        logger.debug("end range {}", Arrays.toString(endRangeArray));

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(connectTime);
        startDate.set(Calendar.HOUR_OF_DAY, Integer.valueOf(startRangeArray[0]));
        startDate.set(Calendar.MINUTE, Integer.valueOf(startRangeArray[1]));
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        logger.debug("start Time is {}", startDate.getTime());

        Calendar endDate = Calendar.getInstance();
        endDate.setTime(connectTime);
        endDate.set(Calendar.HOUR_OF_DAY, Integer.valueOf(endRangeArray[0]));
        endDate.set(Calendar.MINUTE, Integer.valueOf(endRangeArray[1]));
        endDate.set(Calendar.SECOND, 0);
        endDate.set(Calendar.MILLISECOND, 0);
        logger.debug("end time is {}", endDate.getTime());
        return connectTime.after(startDate.getTime()) && connectTime.before(endDate.getTime());
    }

}
