package com.sapienter.jbilling.server.sapphire.signupprocess;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public enum SapphirePaymentMethodType {

    VISA_DEBIT("Visa Debit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    MASTERCARD_CREDIT("Mastercard Credit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    VISA_CREDIT("Visa Credit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    MASTERCARD_CORPORATE_CREDIT("Mastercard Corporate Credit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    VISA_CORPORATE_CREDIT("Visa Corporate Credit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    MASTERCARD_DEBIT("Mastercard Debit") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    AMEX("AMEX") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    MAESTRO("Maestro") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return findPaymentMethodId(this, languageId);
        }
    },
    OTHER("OTHER") {
        @Override
        Integer getPaymentMethod(Integer languageId) {
            return CommonConstants.PAYMENT_METHOD_GATEWAY_KEY;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FIND_PAYMENT_METHOD_SQL = "SELECT id FROM payment_method WHERE id = (SELECT foreign_id FROM international_description WHERE language_id = ? AND content = ?)";

    private String methodName;

    private SapphirePaymentMethodType(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    abstract Integer getPaymentMethod(Integer languageId);

    public static Integer findPaymentMethodIdByTypeAndLanguageId(String type, Integer languageId) {
        if(StringUtils.isEmpty(type)) {
            return OTHER.getPaymentMethod(languageId);
        }
        for(SapphirePaymentMethodType paymentMethodType : values()) {
            if(paymentMethodType.name().equals(type)) {
                return paymentMethodType.getPaymentMethod(languageId);
            }
        }
        return OTHER.getPaymentMethod(languageId);
    }

    /**
     * Returns payment method id based on given {@link SapphirePaymentMethodType} and Language Id.
     * @param type
     * @param languageId
     * @return
     */
    private static Integer findPaymentMethodId(SapphirePaymentMethodType type, Integer languageId) {
        Assert.notNull(languageId, "Please provide language id");
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        try {
            SqlRowSet paymentMethodRow = jdbcTemplate.queryForRowSet(FIND_PAYMENT_METHOD_SQL, languageId, type.getMethodName());
            if(paymentMethodRow.next()) {
                Integer paymentMethodId = paymentMethodRow.getInt("id");
                logger.debug("Found Payment method id {} for payment method type {} with language {}", paymentMethodId, type, languageId);
                return paymentMethodId;
            }
            String errorMessage = String.format("Payment Method [%s] not found for language %d", type.name(), languageId);
            logger.error(errorMessage);
            throw new SessionInternalError(errorMessage);
        } catch(DataAccessException ex) {
            logger.error("Error in findPaymentMethodId", ex);
            throw new SessionInternalError("Error in findPaymentMethodId!", ex);
        }
    }
}
