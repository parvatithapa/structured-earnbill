package com.sapienter.jbilling.server.metafields;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by leandro on 25/01/17.
 */
public interface MetaFieldValidator {

    String VALIDATION_ERROR_PATTERN_COUNTRY_CODE = "metafield.validation.error.country.code, %s";
    String VALIDATION_ERROR_PATTERN_EMAIL = "metafield.validation.error.email, %s";
    String VALIDATION_PATTERN_COUNTRY_CODE = "^$|[A-Z]{2}";
    String VALIDATION_PATTERN_EMAIL = "^([a-zA-Z0-9#\\!$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]|\\\\ ))+(\\.([a-zA-Z0-9!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]))+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.([A-Za-z]{2,})$";
    String VALIDATION_ERROR_SIZE_2 = "metafield.validation.error.size,%s,0,2";
    String VALIDATION_ERROR_SIZE_15 = "metafield.validation.error.size,%s,0,15";
    String VALIDATION_ERROR_SIZE_20 = "metafield.validation.error.size,%s,0,20";
    String VALIDATION_ERROR_SIZE_30 = "metafield.validation.error.size,%s,0,30";
    String VALIDATION_ERROR_SIZE_50 = "metafield.validation.error.size,%s,0,50";
    String VALIDATION_ERROR_SIZE_100 = "metafield.validation.error.size,%s,0,100";
    String VALIDATION_ERROR_SIZE_200 = "metafield.validation.error.size,%s,0,200";
    String VALIDATION_ERROR_SIZE_320 = "metafield.validation.error.size,%s,6,320";
    List<String> COUNTRY_CODES = Arrays.asList(Locale.getISOCountries());

    String validate(String value);
}
