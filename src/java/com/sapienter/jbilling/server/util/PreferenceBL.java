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

package com.sapienter.jbilling.server.util;

import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ACCOUNT_LOCKOUT_TIME;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ADJUSTMENT_ORDER_CREATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ALLOW_DUPLICATE_META_FIELDS_IN_COPY_COMPANY;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ALLOW_INVOICES_WITHOUT_ORDERS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ALLOW_NEGATIVE_PAYMENTS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ASSET_RESERVATION_DURATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ATTACH_INVOICE_TO_NOTIFICATIONS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_AUTO_RECHARGE_THRESHOLD;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_BACKGROUND_CSV_EXPORT;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_CUSTOMER_CONTACT_EDIT;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S1;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S2;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S3;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DELAY_NEGATIVE_PAYMENTS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DIAMETER_QUOTA_THRESHOLD;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DIAMETER_SESSION_GRACE_PERIOD_SECONDS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_FAILED_LOGINS_LOCKOUT;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_FIRST_REMINDER;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_FORCE_UNIQUE_EMAILS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_GRACE_PERIOD;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_INVOICE_DECIMALS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_INVOICE_DELETE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_INVOICE_NUMBER;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ITG_INVOICE_NOTIFICATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_LINK_AGEING_TO_SUBSCRIPTION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_MEDIATION_JDBC_READER_LAST_ID;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_NEXT_REMINDER;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ORDER_IN_INVOICE_LINE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ORDER_LINE_TIER;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_ORDER_OWN_INVOICE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_PAPER_SELF_DELIVERY;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_PASSWORD_EXPIRATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_PDF_ATTACHMENT;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_SHOW_NOTE_IN_INVOICE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_SUBMIT_TO_PAYMENT_GATEWAY;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_UNIQUE_PRODUCT_CODE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_BLACKLIST;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_DF_FM;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_INVOICE_REMINDERS;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_JQGRID;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_ORDER_ANTICIPATION;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_OVERDUE_PENALTY;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_USE_PROVISIONING;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_REMOVE_USER_FROM_AGEING_ON_PAYING_OVERDUE_INVOICE;
import static com.sapienter.jbilling.common.CommonConstants.PREFERENCE_DISPLAY_PAYMENT_URL_LINK_NOTIFICATION;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.validation.ValidationReport;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import com.sapienter.jbilling.server.util.db.PreferenceDAS;
import com.sapienter.jbilling.server.util.db.PreferenceDTO;
import com.sapienter.jbilling.server.util.db.PreferenceTypeDAS;
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO;

public class PreferenceBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PreferenceDAS preferenceDas = null;
    private PreferenceTypeDAS typeDas = null;
    private PreferenceDTO preference = null;
    private PreferenceTypeDTO type = null;
    private JbillingTableDAS jbDAS = null;

    // cache management
    private static CacheProviderFacade cache;
    private static CachingModel cacheModelPreference;
    private static FlushingModel flushModelPreference;

    // All Preferences which values have be a positive integer number
    private static final List<Integer> positiveIntegerPreferences = Arrays.asList(
            PREFERENCE_FAILED_LOGINS_LOCKOUT,
            PREFERENCE_ACCOUNT_LOCKOUT_TIME
            );

    // All Preferences which values have to be a positive integer number or zero
    private static final List<Integer> positiveIntegerOrZeroPreferences = Arrays.asList(
            PREFERENCE_PASSWORD_EXPIRATION,
            PREFERENCE_USE_BLACKLIST,
            PREFERENCE_MEDIATION_JDBC_READER_LAST_ID,
            PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING,
            PREFERENCE_ATTACH_INVOICE_TO_NOTIFICATIONS,
            PREFERENCE_DIAMETER_QUOTA_THRESHOLD,
            PREFERENCE_DIAMETER_SESSION_GRACE_PERIOD_SECONDS,
            PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS,
            PREFERENCE_FORGOT_PASSWORD_EXPIRATION
            );

    // All Preferences which values have to be blank or a positive integer number
    private static final List<Integer> integerOrBlankPreferences = Arrays.asList(
            PREFERENCE_GRACE_PERIOD,
            PREFERENCE_SHOW_NOTE_IN_INVOICE,
            PREFERENCE_DAYS_ORDER_NOTIFICATION_S1,
            PREFERENCE_DAYS_ORDER_NOTIFICATION_S2,
            PREFERENCE_DAYS_ORDER_NOTIFICATION_S3,
            PREFERENCE_INVOICE_NUMBER,
            PREFERENCE_FIRST_REMINDER,
            PREFERENCE_NEXT_REMINDER,
            PREFERENCE_AUTO_RECHARGE_THRESHOLD,
            PREFERENCE_INVOICE_DECIMALS
            );

    // All Preferences which values have to be 1 or 0
    private static final List<Integer> ZeroOrOnePreferences = Arrays.asList(
            PREFERENCE_PAPER_SELF_DELIVERY,
            PREFERENCE_SHOW_NOTE_IN_INVOICE,
            PREFERENCE_INVOICE_DELETE,
            PREFERENCE_USE_INVOICE_REMINDERS,
            PREFERENCE_USE_DF_FM,
            PREFERENCE_USE_OVERDUE_PENALTY,
            PREFERENCE_USE_ORDER_ANTICIPATION,
            PREFERENCE_PDF_ATTACHMENT,
            PREFERENCE_ORDER_OWN_INVOICE,
            PREFERENCE_ORDER_IN_INVOICE_LINE,
            PREFERENCE_CUSTOMER_CONTACT_EDIT,
            PREFERENCE_LINK_AGEING_TO_SUBSCRIPTION,
            PREFERENCE_ALLOW_NEGATIVE_PAYMENTS,
            PREFERENCE_DELAY_NEGATIVE_PAYMENTS,
            PREFERENCE_ALLOW_INVOICES_WITHOUT_ORDERS,
            PREFERENCE_USE_PROVISIONING,
            PREFERENCE_FORCE_UNIQUE_EMAILS,
            PREFERENCE_UNIQUE_PRODUCT_CODE,
            PREFERENCE_USE_JQGRID,
            PREFERENCE_ITG_INVOICE_NOTIFICATION,
            PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT,
            PREFERENCE_ASSET_RESERVATION_DURATION,
            PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY,
            PREFERENCE_ADJUSTMENT_ORDER_CREATION,
            PREFERENCE_BACKGROUND_CSV_EXPORT,
            PREFERENCE_ALLOW_DUPLICATE_META_FIELDS_IN_COPY_COMPANY,
            PREFERENCE_SUBMIT_TO_PAYMENT_GATEWAY,
            PREFERENCE_ORDER_LINE_TIER,
            PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS,
            PREFERENCE_REMOVE_USER_FROM_AGEING_ON_PAYING_OVERDUE_INVOICE,
            PREFERENCE_DISPLAY_PAYMENT_URL_LINK_NOTIFICATION );

    static {
        cache = Context.getBean(Context.Name.CACHE);
        cacheModelPreference = Context.getBean(Context.Name.PREFERENCE_CACHE_MODEL);
        flushModelPreference = Context.getBean(Context.Name.PREFERENCE_FLUSH_MODEL);
    }

    public PreferenceBL() {
        init();
    }

    public PreferenceBL(Integer entityId, Integer preferenceTypeId) {
        init();
        set(entityId, preferenceTypeId);
    }

    public static final PreferenceWS getWS(PreferenceTypeDTO preferenceType) {

        PreferenceWS ws = new PreferenceWS();
        ws.setPreferenceType(PreferenceBL.getPreferenceTypeWS(preferenceType));
        return ws;
    }

    public static final PreferenceWS getWS(PreferenceDTO dto) {

        PreferenceWS ws = new PreferenceWS();
        ws.setId(dto.getId());
        ws.setPreferenceType(dto.getPreferenceType() != null ? PreferenceBL
                .getPreferenceTypeWS(dto.getPreferenceType()) : null);
        ws.setTableId(dto.getJbillingTable() != null ? dto.getJbillingTable()
                .getId() : null);
        ws.setForeignId(dto.getForeignId());
        ws.setValue(dto.getValue());
        return ws;
    }


    public static final PreferenceTypeWS getPreferenceTypeWS(PreferenceTypeDTO dto) {

        PreferenceTypeWS ws = new PreferenceTypeWS();
        ws.setId(dto.getId());
        ws.setDescription(dto.getDescription());
        ws.setDefaultValue(dto.getDefaultValue());
        ws.setValidationRule(null != dto.getValidationRule() ? MetaFieldBL
                .getValidationRuleWS(dto.getValidationRule()) : null);
        return ws;
    }


    private void init() {
        preferenceDas = new PreferenceDAS();
        typeDas = new PreferenceTypeDAS();
        jbDAS = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
    }

    /**
     * This function returns a preference by matching entity and preference type id.
     * Use this function in a static context to fetch the required preference value from cache.
     * No need to use new PreferenceBL().set(...) method as this is to be used only if you need PreferenceDTO.
     * As such set(..) method is used only from WS API.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return preference value, or default value from preference type if preference value is null/blank
     */
    public static synchronized String getPreferenceValue(Integer entityId, Integer preferenceTypeId) throws EmptyResultDataAccessException {

        logger.debug("Looking for preference {}, for entity {} and table {}", preferenceTypeId, entityId, Constants.TABLE_ENTITY);

        String preferenceValue = getPreferences(entityId).get(preferenceTypeId);

        if (preferenceValue == null) {
            logger.debug("throwing EmptyResultDataAccessException preferenceValue : |{}|", preferenceValue);
            throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
        }

        logger.debug("preferenceValue : |{}|", preferenceValue);
        return preferenceValue;
    }

    /**
     * This function is to be used when preference value is Integer and needs to be fetched as an Integer.
     * If the preference value or default value are both not set, this method returns null.
     * Instead of prefBL.set and prefBL.getInt, simply call PreferenceBL.getPreferenceValueAsInteger in a static context.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return Integer or null
     */
    public static synchronized Integer getPreferenceValueAsInteger(Integer entityId, Integer preferenceTypeId) {
        String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? Integer.valueOf(value) : null;
    }

    /**
     * This method is same as getPreferenceValueAsInteger, except that it returns zero in case
     * the preference value or default value, both are not set. This is to be used when value
     * is not to be checked against null, but needs to be compared against zero or any other number.
     * Instead of prefBL.set and prefBL.getInt, simply call PreferenceBL.getPreferenceValueAsIntegerOrZero in a static context.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return Integer
     */
    public static synchronized Integer getPreferenceValueAsIntegerOrZero(Integer entityId, Integer preferenceTypeId) {
        try {
            Integer value = getPreferenceValueAsInteger(entityId, preferenceTypeId);
            return value != null ? value : 0;
        } catch(EmptyResultDataAccessException ex) {
            logger.warn("Swallowing Exception !", ex);
            return 0;
        }
    }

    /**
     * Use this method if the preference value is a floating point value and needs to be returned as one.
     * Instead of prefBL.set and prefBL.getFloat, simply call PreferenceBL.getPreferenceValueAsFloat in a static context.
     * This method returns null if the value and default value both are not set.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return Float or null
     */
    public static synchronized Float getPreferenceValueAsFloat(Integer entityId, Integer preferenceTypeId) {
        String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? Float.valueOf(value) : null;
    }

    /**
     * Use this method if the preference value is a BigDecimal value and needs to be returned as one.
     * Instead of prefBL.set and prefBL.getDecimal, simply call PreferenceBL.getPreferenceValueAsDecimal in a static context.
     * This method returns null if the value and default value both are not set.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return BigDecimal or null
     */
    public static synchronized BigDecimal getPreferenceValueAsDecimal(Integer entityId, Integer preferenceTypeId) {
        String value = getPreferenceValue(entityId, preferenceTypeId);
        return !StringUtils.isEmpty(value) ? new BigDecimal(value) : null;
    }

    /**
     * This method is similar to getPreferenceValueAsDecimal, except that this one returns zero if the value,
     * and default value, both are not set. This is to be used when checking against null is to be avoided,
     * and functionality requires to compare the value against zero or any other number.
     *
     * @param entityId
     * @param preferenceTypeId
     * @return BigDecimal
     */
    public static synchronized BigDecimal getPreferenceValueAsDecimalOrZero(Integer entityId, Integer preferenceTypeId) {
        BigDecimal value = getPreferenceValueAsDecimal(entityId, preferenceTypeId);
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * This method uses the getPreferenceValueAsIntegerOrZero and returns
     * Boolean.TRUE when the preference is 1
     * Boolean.FALSE otherwise
     *
     * @param entityId
     * @param preferenceTypeId
     * @return Boolean
     */
    public static synchronized Boolean getPreferenceValueAsBoolean(Integer entityId, Integer preferenceTypeId) {
        Integer value = getPreferenceValueAsIntegerOrZero(entityId, preferenceTypeId);
        return value == 1 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * This function is only used from WS API and as such should not be used from the application,
     * as it hits the db for fetching preference value or its default value.
     * The getPreferenceValue function should be used in static context to fetch preference value from cache.
     *
     * @param entityId
     * @param preferenceTypeId
     * @throws EmptyResultDataAccessException
     */
    public void set(Integer entityId, Integer preferenceTypeId) throws EmptyResultDataAccessException {

        logger.debug("Looking for preference {}, for entity {} and table {}", preferenceTypeId, entityId, Constants.TABLE_ENTITY);

        try {
            preference = preferenceDas.findByType_Row(preferenceTypeId, entityId, Constants.TABLE_ENTITY);
            type = typeDas.findNow(preferenceTypeId);

            // throw exception if there is no preference, or if the type does not have a
            // default value that can be returned.
            if (preference == null) {
                if (type == null || type.getDefaultValue() == null) {
                    throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
                }
            }
        } catch (ObjectNotFoundException e) {
            throw new EmptyResultDataAccessException("Could not find preference " + preferenceTypeId, 1);
        }
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, Integer value) {
        createUpdateForEntity(entityId, preferenceTypeId, (value != null ? value.toString() : ""));
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, BigDecimal value) {
        createUpdateForEntity(entityId, preferenceTypeId, (value != null ? value.toString() : ""));
    }

    public void createUpdateForEntity(Integer entityId, Integer preferenceTypeId, String value) {

        preference = preferenceDas.findByType_Row(preferenceTypeId, entityId, Constants.TABLE_ENTITY);
        type = typeDas.find(preferenceTypeId);

        if (null != type.getValidationRule()) {
            validate(value);
        }

        if (preference != null) {
            // update preference
            preference.setValue(value);
            preferenceDas.save(preference);
        } else {
            // create a new preference
            preference = new PreferenceDTO();
            preference.setValue(value);
            preference.setForeignId(entityId);
            preference.setJbillingTable(jbDAS.findByName(Constants.TABLE_ENTITY));
            preference.setPreferenceType(type);
            preference = preferenceDas.save(preference);
        }

        invalidatePreferencesCache();
    }

    /**
     * This function returns preferences from cache if available in the cache.
     * If not available in the cache, then selects from db and puts in the cache as a map.
     * The map key is a combination of preference type id, entity id and entity table name.
     *
     * @param entityId
     * @return
     */
    private static synchronized Map<Integer, String> getPreferences(Integer entityId) {
        String cacheKey = getPreferenceCacheKey(entityId);
        Map<Integer, String> cachedPreferences = (Map<Integer, String>) cache.getFromCache(cacheKey, cacheModelPreference);

        if (cachedPreferences != null && !cachedPreferences.isEmpty()) {
            logger.debug("Preferences Cache hit for {}", cacheKey);
            return cachedPreferences;
        }

        // not found in cache, fetch from db
        List<Object[]> preferences = new PreferenceDAS().getPreferencesByEntity(entityId);

        cachedPreferences = new HashMap<>();

        for (Object[] preferenceTypeAndValue : preferences) {
            if (preferenceTypeAndValue != null && preferenceTypeAndValue[0] != null) {
                Integer preferenceTypeId = new Integer(preferenceTypeAndValue[0].toString());
                String preferenceValue = "";
                if (preferenceTypeAndValue[1] != null && !preferenceTypeAndValue[1].toString().isEmpty()) {
                    preferenceValue = preferenceTypeAndValue[1].toString();
                } else if (preferenceTypeAndValue[2] != null) {
                    preferenceValue = preferenceTypeAndValue[2].toString();
                }
                // populate the map
                cachedPreferences.put(preferenceTypeId, preferenceValue);
            }
        }

        // put the populated map in cache
        cache.putInCache(cacheKey, cacheModelPreference, cachedPreferences);
        return cachedPreferences;
    }

    /**
     * Returns the preference value if set. If the preference is null or has no
     * set value (is blank), the preference type default value will be returned.
     *
     * @return preference value as a string
     */
    public String getString() {
        if (preference != null && StringUtils.isNotBlank(preference.getValue())) {
            return preference.getValue();
        } else {
            return type != null ? type.getDefaultValue() : null;
        }
    }

    public Integer getInt() {
        String value = getString();
        return value != null ? Integer.valueOf(value) : null;
    }

    public Float getFloat() {
        String value = getString();
        return value != null ? Float.valueOf(value) : null;
    }

    public BigDecimal getDecimal() {
        String value = getString();
        return value != null ? new BigDecimal(value) : null;
    }

    /**
     * Returns the preference value as a string.
     *
     * @return string value of preference
     * @see #getString()
     */
    public String getValueAsString() {
        return getString();
    }

    /**
     * Returns the default value for the given preference type.
     *
     * @param preferenceTypeId preference type id
     * @return default preference value
     */
    public String getDefaultValue(Integer preferenceTypeId) {
        type = typeDas.find(preferenceTypeId);
        return type != null ? type.getDefaultValue() : null;
    }

    /**
     * Returns true if the preference value is null, false if value is set.
     * <p>
     * This method ignores the preference type default value, unlike {@link #getString()}
     * and {@link #getValueAsString()} which will return the type default value if the
     * preference itself is unset.
     *
     * @return true if preference value is null, false if value is set.
     */
    public boolean isNull() {
        return preference == null || preference.getValue() == null;
    }

    public PreferenceDTO getEntity() {
        return preference;
    }

    private static synchronized String getPreferenceCacheKey(Integer entityId) {
        return "preferenceCache entity:" + entityId;
    }

    public void invalidatePreferencesCache() {
        logger.debug("Invalidating preferences cache");
        cache.flushCache(flushModelPreference);
    }

    private void validate(String value) {
        ValidationReport validationReport = this.type.getValidationRule().getRuleType().getValidationRuleModel().
                doValidation(null, value, type.getValidationRule(), Constants.LANGUAGE_ENGLISH_ID);
        if (validationReport != null && !validationReport.getErrors().isEmpty()) {
            throw new SessionInternalError("Field value failed validation.",
                    validationReport.getErrors().toArray(new String[validationReport.getErrors().size()]));
        }
    }

    public static synchronized boolean isTierCreationAllowed(Integer entityId){
        return getPreferenceValueAsBoolean(entityId, Constants.PREFERENCE_ORDER_LINE_TIER);
    }

    public void validatePreferenceValue(PreferenceWS preference) {
        Integer preferenceType = preference.getPreferenceType().getId();
        String preferenceValue = preference.getValue();
        if (positiveIntegerPreferences.contains(preferenceType)) {
            validatePositiveIntegerPreferences(preferenceValue);
        } else if (positiveIntegerOrZeroPreferences.contains(preferenceType)) {
            validatePositiveIntegerOrZeroPreferences(preferenceValue);
        } else if (integerOrBlankPreferences.contains(preferenceType)) {
            if (!StringUtils.isEmpty(preferenceValue)) {
                validatePositiveIntegerOrZeroPreferences(preferenceValue);
            }
        } else if (ZeroOrOnePreferences.contains(preferenceType)) {
            validateZeroOrOnePreferenceValue(preferenceValue);
        }
    }

    private Integer validatePositiveIntegerOrZeroPreferences(String value) {
        try {
            Integer preferenceValue = Integer.valueOf(value);
            if (preferenceValue < 0) {
                throw new SessionInternalError("Preference value can not be a negative number",
                        new String[]{"bean.PreferenceWS.preference.value.error.negative.number," + value});
            }
            return preferenceValue;
        } catch (NumberFormatException nfe) {
            throw new SessionInternalError("Preference value has to be a valid positive integer number",
                    new String[]{"bean.PreferenceWS.preference.value.error.not.number," + value});
        }
    }

    private void validateZeroOrOnePreferenceValue(String value) {
        Integer preferenceValue = validatePositiveIntegerOrZeroPreferences(value);
        if (preferenceValue < 0 || preferenceValue > 1) {
            throw new SessionInternalError("Preference value has to be 1 or 0, other values are not allowed",
                    new String[]{"bean.PreferenceWS.preference.value.error," + value});
        }
    }

    private void validatePositiveIntegerPreferences(String value) {
        Integer preferenceValue = validatePositiveIntegerOrZeroPreferences(value);
        if (preferenceValue < 1) {
            throw new SessionInternalError("Preference value has to be greater than zero, other values are not allowed",
                    new String[]{"bean.PreferenceWS.preference.value.error.positive.number," + value});
        }
    }

    /**
     * Creates {@link PreferenceDTO} for all entities with given value,
     * if preference not present for entity.
     * @param preferenceTypeId
     * @param value
     */
    public static void createIfNotPresentForAllEntities(Integer preferenceTypeId, String value) {
        IMethodTransactionalWrapper txAction = Context.getBean("methodTransactionalWrapper");
        txAction.execute(()-> {
            PreferenceDAS preferenceDAS = new PreferenceDAS();
            for(Integer entityId : new CompanyDAS().getEntitiyIds()) {
                PreferenceDTO preferenceDTO = preferenceDAS.findByType_Row(preferenceTypeId, entityId, Constants.TABLE_ENTITY);
                if(null!= preferenceDTO) {
                    if(StringUtils.isEmpty(preferenceDTO.getValue())) {
                        preferenceDTO.setValue(value);
                    }
                    continue;
                }
                // create a new preference
                preferenceDTO = new PreferenceDTO();
                preferenceDTO.setValue(value);
                preferenceDTO.setForeignId(entityId);
                JbillingTableDAS jbillingTableDAS = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
                preferenceDTO.setJbillingTable(jbillingTableDAS.findByName(Constants.TABLE_ENTITY));
                preferenceDTO.setPreferenceType(new PreferenceTypeDAS().find(preferenceTypeId));
                preferenceDTO = preferenceDAS.save(preferenceDTO);
                logger.debug("preference {} created with value {}", preferenceTypeId, value);
            }
        });
    }

}

