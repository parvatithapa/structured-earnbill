package com.sapienter.jbilling.server.metafields;

import java.util.Arrays;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public enum MetaFieldType implements MetaFieldValidator {
    ORGANIZATION{
        @Override
        public String validate(String value){
            return validLength(0, 200, VALIDATION_ERROR_SIZE_200, value);
        }
    },
    ADDRESS1{
        @Override
        public String validate(String value) {
            return validLength(0, 100, VALIDATION_ERROR_SIZE_100, value);
        }
    },
    ADDRESS2 {
        @Override
        public String validate(String value) {
            return validLength(0, 100, VALIDATION_ERROR_SIZE_100, value);
        }
    },
    CITY {
        @Override
        public String validate(String value) {
            return validLength(0, 50, VALIDATION_ERROR_SIZE_50, value);
        }
    },
    STATE_PROVINCE {
        @Override
        public String validate(String value) {
            return validLength(0, 30, VALIDATION_ERROR_SIZE_30, value);
        }
    },
    POSTAL_CODE {
        @Override
        public String validate(String value) {
            return validLength(0, 15, VALIDATION_ERROR_SIZE_15, value);
        }
    },
    COUNTRY_CODE {
        @Override
        public String validate(String value) {
            String result = validLength(0, 2, VALIDATION_ERROR_SIZE_2, value);

            if(result == null){
                result = validPattern(VALIDATION_PATTERN_COUNTRY_CODE, VALIDATION_ERROR_PATTERN_COUNTRY_CODE, value);

                if(isNotBlank(value) && !COUNTRY_CODES.contains(value)){
                    return VALIDATION_ERROR_PATTERN_COUNTRY_CODE;
                }
            }

            return result;
        }
    },
    BANK_BRANCH_CODE{
        @Override
        public String validate(String value) {
            return null;
        }
    },
    FIRST_NAME {
        @Override
        public String validate(String value) {
            return validLength(0, 100, VALIDATION_ERROR_SIZE_100, value);
        }
    },
    LAST_NAME {
        @Override
        public String validate(String value) {
            return validLength(0, 30, VALIDATION_ERROR_SIZE_30, value);
        }
    },
    INITIAL {
        @Override
        public String validate(String value) {
            return validLength(0, 30, VALIDATION_ERROR_SIZE_30, value);
        }
    },
    TITLE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    PHONE_COUNTRY_CODE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    PHONE_AREA_CODE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    PHONE_NUMBER {
        @Override
        public String validate(String value) {
            return validLength(0, 20, VALIDATION_ERROR_SIZE_20, value);
        }
    },
    FAX_COUNTRY_CODE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    FAX_AREA_CODE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    FAX_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    EMAIL {
        @Override
        public String validate(String value) {
            String result = validLength(6, 320, VALIDATION_ERROR_SIZE_320, value);

            if(result == null){
                result = validPattern(VALIDATION_PATTERN_EMAIL, VALIDATION_ERROR_PATTERN_EMAIL, value);
            }

            return result;
        }
    },
    BANK_NAME {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BANK_ACCOUNT_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BANK_ROUTING_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BANK_ACCOUNT_TYPE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BANK_ACCOUNT_NUMBER_ENCRYPTED {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    DATE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    CHEQUE_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    PAYMENT_ID {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    PAYMENT_CARD_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    GATEWAY_KEY {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    CC_TYPE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    TRANSACTION_ID {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    AUTO_PAYMENT_LIMIT {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BILLING_EMAIL {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    AUTO_PAYMENT_AUTHORIZATION {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    POST_BOX {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BPAY_REF {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BPAY_BILLIER_CODE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    COUNTRY_NAME {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    STREET_TYPE {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    STREET_NAME {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    STREET_NUMBER {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    SUB_PREMISES {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    CENTREPAY_REF {
        @Override
        public String validate(String value) {
            return null;
        }
    },
    BUSINESS_NAME {
        @Override
        public String validate(String value) {
            return null;
        }
    }
    ;


    public static MetaFieldType fromString(String name) {
        return Arrays.stream(MetaFieldType.values())
                .filter(mft -> mft.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static String validLength(int minLength, int maxLength, String validation, String value){
        if(value != null && (value.length() < minLength || value.length() > maxLength)){
            return validation;
        }

        return null;
    }

    private static String validPattern(String pattern, String validationError, String value){
        if(isNotBlank(value) && !Pattern.compile(pattern).matcher(value).matches()){
            return validationError;
        }

        return null;
    }
}
