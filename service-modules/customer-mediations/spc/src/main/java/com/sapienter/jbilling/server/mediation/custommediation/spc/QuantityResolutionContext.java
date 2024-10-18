package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.util.Constants;

public class QuantityResolutionContext {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer MINUTE_IN_SECONDS = 60;

    private BigDecimal initialIncrement;
    private BigDecimal subsequentIncrement;
    private Integer initialRoundingMode;
    private Integer subsequentRoundingMode;

    public QuantityResolutionContext(BigDecimal initialIncrement, BigDecimal subsequentIncrement,
            Integer initialRoundingMode, Integer subsequentRoundingMode) {
        this.initialIncrement = initialIncrement;
        this.subsequentIncrement = subsequentIncrement;
        this.initialRoundingMode = initialRoundingMode;
        this.subsequentRoundingMode = subsequentRoundingMode;
    }

    public BigDecimal getInitialIncrement() {
        return initialIncrement;
    }

    public BigDecimal getSubsequentIncrement() {
        return subsequentIncrement;
    }

    public Integer getInitialRoundingMode() {
        return initialRoundingMode;
    }

    public Integer getSubsequentRoudingMode() {
        return subsequentRoundingMode;
    }

    public BigDecimal resolveQuantity(BigDecimal callDuration) {
        BigDecimal quantity;
        if (callDuration.compareTo(initialIncrement) <= 0) {
            quantity = callDuration.divide(initialIncrement, Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND)
                    .setScale(0, initialRoundingMode)
                    .multiply(initialIncrement);
        } else {
            quantity = initialIncrement.add((callDuration.subtract(initialIncrement))
                    .divide(subsequentIncrement, Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND)
                    .setScale(0, subsequentRoundingMode)
                    .multiply(subsequentIncrement));
        }
        int scale = callDuration.compareTo(initialIncrement) <= 0 ? Constants.BIGDECIMAL_QUANTITY_SCALE : 2;
        quantity = quantity.divide(BigDecimal.valueOf(MINUTE_IN_SECONDS), scale, Constants.BIGDECIMAL_ROUND);
        logger.debug("Resolved quantity is {} after applying rule {}", quantity, this);
        return quantity;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("QuantityResolutionContext [initialIncrement=");
        builder.append(initialIncrement);
        builder.append(", subsequentIncrement=");
        builder.append(subsequentIncrement);
        builder.append(", initialRoundingMode=");
        builder.append(initialRoundingMode);
        builder.append(", subsequentRoundingMode=");
        builder.append(subsequentRoundingMode);
        builder.append("]");
        return builder.toString();
    }

}
