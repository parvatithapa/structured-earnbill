package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CALL_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CARRIER_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.COUNTRY_CODE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.DEST_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.HOLIDAY_DATA_TABLE_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.HOLIDAY_DATE_FORMAT;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.INTERNATIONAL;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.LANDLINE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.LOCATION;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.MOBILE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.NATIONAL;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.NGN;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OFF_PEAK;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OFF_PEAK_DATE_FORMAT;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ORIGINAL_QUANTITY;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OTHER_CARRIER_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.PEAK_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.REQUESTED_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.REST_OF_WORLD;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.SATELLITE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.SATELLITE_COUNTRY_CODE_DATA_TABLE_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.UNDERSCORE;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.common.SessionInternalError;
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
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public JMRPricingResolutionStep(SapphireMediationHelperService service, DataSource dataSource, JdbcTemplate jdbcTemplate) {
        Assert.notNull(service, "provide non null service instance");
        this.service = service;
        this.countryCodeNameMap = createCountryCodeNameMap();
        Assert.isTrue(MapUtils.isNotEmpty(countryCodeNameMap), "Country code Name map creation failed!");
        logger.debug("country code name map {}", this.countryCodeNameMap);
        Assert.notNull(dataSource, "provide non null dataSource instance");
        this.dataSource = dataSource;
        Assert.notNull(jdbcTemplate, "provide non null jdbcTemplate instance");
        this.jdbcTemplate = jdbcTemplate;
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

            Integer entityId = context.getEntityId();
            Map<String, String> entityLevelMetaFieldValueNameMap = service.getMetaFieldsForEntity(entityId);
            // Get country info
            Integer country = phoneNumber.getCountryCode();
            String satelliteTableName  = entityLevelMetaFieldValueNameMap.get(SATELLITE_COUNTRY_CODE_DATA_TABLE_NAME);

            if(StringUtils.isEmpty(satelliteTableName)) {
                result.addError("SATELLITE-TABLE-ENTITY-LEVEL-METAFIELD-NOT-FOUND");
                return false;
            }

            if(!isTablePresent(satelliteTableName)) {
                result.addError("SATELLITE-TABLE-NOT-FOUND");
                return false;
            }

            Map<Integer, String> satelliteCountryCodeLocationMap = getSatelliteCountryCodeLocationMap(satelliteTableName);
            if(MapUtils.isEmpty(satelliteCountryCodeLocationMap)) {
                result.addError("No-RECORDS-IN-SATELLITE-TABLE");
                return false;
            }
            String location = StringUtils.EMPTY;
            if(!satelliteCountryCodeLocationMap.containsKey(country)) {
                String regionCode = phoneNumberUtil.getRegionCodeForNumber(phoneNumber);
                if(StringUtils.isNotEmpty(regionCode)) {
                    regionCode = regionCode.toUpperCase();
                }
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
            } else {
                logger.debug("satellite call found for country code {}", country);
                location = satelliteCountryCodeLocationMap.get(country);
                logger.debug("location of satellite call {} is {}", destAddr, location);
            }

            // Adding country info and location info
            PricingField countryCode = new PricingField(COUNTRY_CODE, country);
            fields.add(countryCode);
            PricingField numberLocation = new PricingField(LOCATION, location);
            fields.add(numberLocation);

            Locale locale = service.getLocaleForEntity(entityId);
            StringBuilder callType = new StringBuilder();
            if(!satelliteCountryCodeLocationMap.containsKey(country)) {
                // Adding callType info
                PhoneNumberType type = phoneNumberUtil.getNumberType(phoneNumber);
                String requestedAddr = context.getPricingField(REQUESTED_ADDR).getStrValue();
                if((StringUtils.isNotBlank(requestedAddr) && requestedAddr.startsWith("00"))
                        || PhoneNumberType.UAN.equals(type)
                        || PhoneNumberType.TOLL_FREE.equals(type)) {
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
                    } else if (PhoneNumberType.UAN.equals(type)) {
                        callType.append(NGN);
                    } else if (PhoneNumberType.TOLL_FREE.equals(type)) {
                        callType.append(PhoneNumberType.TOLL_FREE.name());
                    } else {
                        callType.append(LANDLINE);
                    }
                }
            } else {
                callType.append(SATELLITE);
            }


            PricingField callTypeField = new PricingField(CALL_TYPE, callType.toString());
            logger.debug("callTypeField {}", callTypeField);
            fields.add(callTypeField);


            // Adding duration
            PricingField duration = new PricingField(SapphireMediationConstants.DURATION, context.getResult().getQuantity());
            logger.debug("duration {}", duration);
            fields.add(duration);
            String formatedDate = DateFormatUtils.format(result.getEventDate().getTime(), OFF_PEAK_DATE_FORMAT);
            Date connectTime = DateUtils.parseDate(formatedDate, OFF_PEAK_DATE_FORMAT);
            String peakValue  = entityLevelMetaFieldValueNameMap.get(PEAK_FIELD_NAME);
            String holidayTableName  = entityLevelMetaFieldValueNameMap.get(HOLIDAY_DATA_TABLE_NAME);
            boolean offPeakValue = false;
            if(isWeekend(connectTime, locale) || isHoliday(holidayTableName, connectTime)) {
                offPeakValue = true;
            } else {
                offPeakValue = checkTimeInBetweenRange(connectTime, peakValue);
            }
            PricingField offPeak = new PricingField(OFF_PEAK, offPeakValue);
            logger.debug("offPeak value {} for user name {} for connect time {}", offPeak.getValue(),
                    findUserNameById(result.getUserId()), connectTime);
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

    private boolean checkTimeInBetweenRange(Date connectTime, String range) {
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
        logger.debug("connect time is {}", connectTime);
        return !(connectTime.after(startDate.getTime()) && connectTime.before(endDate.getTime()));
    }

    private boolean isWeekend(Date date, Locale locale) {
        Calendar calendar = Calendar.getInstance(locale);
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    private boolean isHoliday(String tableName, Date date) {
        if(StringUtils.isEmpty(tableName)) {
            logger.error("holiday table name meta field not set for entity");
            return false;
        }
        if(!isTablePresent(tableName)) {
            logger.error("holiday table not found");
            return false;
        }
        Map<String, Date> holidayDateMap = getHolidayDateMap(tableName);
        if(MapUtils.isEmpty(holidayDateMap)) {
            logger.error("table {} has no record", tableName);
            return false;
        }
        Date convertedDate = convertDateByFormat(HOLIDAY_DATE_FORMAT, date);
        if(holidayDateMap.values().contains(convertedDate)) {
            return true;
        }
        return false;
    }

    /**
     * return true if table exists on database.
     * @param tableName
     * @return
     */
    private boolean isTablePresent(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }

    private static final String GET_SATELLITE_COUNTRY_CODE_LOCATION_SQL =
            "SELECT country_code, location FROM %s";

    /**
     * fetches satellite country code from given table.
     * @param tableName
     * @return
     */
    private Map<Integer,String> getSatelliteCountryCodeLocationMap(String tableName) {
        return jdbcTemplate.query(String.format(GET_SATELLITE_COUNTRY_CODE_LOCATION_SQL, tableName), (ResultSet resultSet)-> {
            Map<Integer,String> results = new HashMap<>();
            while (resultSet.next()) {
                results.put(Integer.parseInt(resultSet.getString("country_code")), resultSet.getString("location"));
            }
            return results;
        });
    }

    private static final String GET_HOLIDAYS_SQL =
            "SELECT day, date FROM %s";

    /**
     * fetches holiday from given table.
     * @param tableName
     * @return
     */
    private Map<String, Date> getHolidayDateMap(String tableName) {
        return jdbcTemplate.query(String.format(GET_HOLIDAYS_SQL, tableName), (ResultSet resultSet)-> {
            Map<String,Date> results = new HashMap<>();
            while (resultSet.next()) {
                results.put(resultSet.getString("day"), convertStringDateByFormat(HOLIDAY_DATE_FORMAT, resultSet.getString("date")));
            }
            return results;
        });
    }

    /**
     * Converts String date to given format.
     * @param format
     * @param date
     * @return
     */
    private Date convertStringDateByFormat(String format, String date) {
        try {
            return DateUtils.parseDate(date, format);
        } catch (ParseException e) {
            throw new SessionInternalError("error in convertStringDateByFormat", e);
        }
    }

    /**
     * Converts Date to given dateFormat.
     * @param format
     * @param date
     * @return
     */
    private Date convertDateByFormat(String format, Date date) {
        try {
            return DateUtils.parseDate(DateFormatUtils.format(date, format), format);
        } catch (ParseException e) {
            throw new SessionInternalError("error in convertDateByFormat", e);
        }
    }

    private static final String FIND_USER_NAME_BY_ID_SQL =
            "SELECT user_name FROM base_user WHERE id = ?";

    private String findUserNameById(Integer userId) {
        return jdbcTemplate.queryForObject(FIND_USER_NAME_BY_ID_SQL, new Object[] { userId }, String.class);
    }
}
