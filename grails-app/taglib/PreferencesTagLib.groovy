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


import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.util.PreferenceBL
import org.apache.commons.lang.StringUtils
import org.springframework.dao.EmptyResultDataAccessException
import org.hibernate.ObjectNotFoundException
/**
 * PreferencesTagLib 
 *
 * @author Brian Cowdery
 * @since 12/01/11
 */
class PreferencesTagLib {

    /**
     * Prints the preference value
     *
     * @param preferenceId ID of the preference to check
     */
    def preference = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preference') as Integer

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
            if (!StringUtils.isEmpty(preferenceValue))
                out << preferenceValue

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the preference exists and is not null.
     *
     * @param preferenceId ID of the preference to check
     */
    def hasPreference = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'hasPreference') as Integer

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
            if (!StringUtils.isEmpty(preferenceValue))
                out << body()

        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the preference value or preference type default equals the given value.
     *
     * @param preferenceId ID of the preference to check
     * @param value to compare
     */
    def preferenceEquals = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preferenceEquals') as Integer
        def value = assertAttribute('value', attrs, 'preferenceEquals') as String

        try {
			def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)
			if (StringUtils.isEmpty(preferenceValue)) {
				return;
			}
			
            if (value.equals(preferenceValue) ||
               (preferenceId.equals(Constants.PREFERENCE_USE_JQGRID) && preferenceValue.isInteger() && value.isInteger() && preferenceValue.toInteger()>0 && value.toInteger()>0)){
                out << body()
            }
        } catch (EmptyResultDataAccessException e) {
            /* ignore */
            log.debug("empty result data access exception")

        } catch (ObjectNotFoundException e) {
            /* ignore */
            log.debug("object not found exception")
        }
    }

    /**
     * Prints the tag body if the preference value is equal, or if the preference is not set and has no
     * default value for the preference type. Useful for "default if not set" style preferences.
     *
     * @param preferenceId ID of the preference to check
     * @param value to compare
     */
    def preferenceIsNullOrEquals = { attrs, body ->

        def preferenceId = assertAttribute('preferenceId', attrs, 'preferenceIsNullOrEquals') as Integer
        def value = assertAttribute('value', attrs, 'preferenceIsNullOrEquals') as String

        try {
            def preferenceValue = PreferenceBL.getPreferenceValue(session['company_id'], preferenceId)

            if (StringUtils.isEmpty(preferenceValue) || preferenceValue.equals(value) ||
                    (preferenceId.equals(Constants.PREFERENCE_USE_JQGRID) && preferenceValue.isInteger() && value.isInteger() && preferenceValue.toInteger()>0 && value.toInteger()>0)){
                out << body()
            }
        } catch (EmptyResultDataAccessException e) {
            /* ignore */
        } catch (ObjectNotFoundException e) {
            /* ignore */
        }
    }

    /**
     * Prints the tag body if the jbilling.properties setting is equal to the given value.
     *
     * @param property property key of the value from jbilling.properties
     * @param value value to compare
     */
    def settingEquals = { attrs, body ->
        def propertyKey = assertAttribute('property', attrs, 'settingEquals') as String
        def value = assertAttribute('value', attrs, 'settingEquals') as String

        def prop = Util.getSysProp(propertyKey)
        if (StringUtils.isEmpty(prop)) {
            return
        }

        if (value.equals(prop))
            out << body()
    }

    /**
     * Prints the tag body if the jbilling.properties setting, when read as a boolean,
     * evaluates to true.
     *
     * @param property property key of the value from jbilling.properties
     * @param value value to compare
     */
    def settingEnabled = { attrs, body ->
        def propertyKey = assertAttribute('property', attrs, 'settingEnabled') as String

        if (Util.getSysPropBooleanTrue(propertyKey))
            out << body()
    }

    protected assertAttribute(String name, attrs, String tag) {
        if (!attrs.containsKey(name)) {
            throwTagError "Tag [$tag] is missing required attribute [$name]"
        }
        attrs.remove name
    }
}
