package com.sapienter.jbilling.server.mediation;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

public class JMRPostProcessorTask extends PluggableTask implements IJMRPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_ROUNDING_MODE =
            new ParameterDescription("rounding mode", true, ParameterDescription.Type.STR, "ROUND_HALF_UP");

    private static final ParameterDescription PARAM_ROUNDING_SCALE =
            new ParameterDescription("rounding scale", true, ParameterDescription.Type.INT, "10");

    private static final ParameterDescription PARAM_MINIMUM_CHARGE =
            new ParameterDescription("minimum charge", false, ParameterDescription.Type.STR, "0.00");

    public static final ParameterDescription PARAM_TAX_TABLE_NAME =
            new ParameterDescription("tax table name", false, ParameterDescription.Type.STR);

    public static final ParameterDescription PARAM_TAX_DATE_FORMAT =
            new ParameterDescription("tax date format", false, ParameterDescription.Type.STR, "dd-MM-yyyy");

    private static final Map<String, Integer> ROUNDING_MODE_MAP;

    static {
        ROUNDING_MODE_MAP = new HashMap<>();
        for(Field field : BigDecimal.class.getDeclaredFields()) {
            if(field.getName().startsWith("ROUND")) {
                try {
                    ROUNDING_MODE_MAP.put(field.getName(), field.getInt(null));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new SessionInternalError("error creating ROUNDING_MODE_MAP", e);
                }
            }
        }
    }

    public JMRPostProcessorTask() {
        descriptions.add(PARAM_ROUNDING_MODE);
        descriptions.add(PARAM_MINIMUM_CHARGE);
        descriptions.add(PARAM_ROUNDING_SCALE);
        descriptions.add(PARAM_TAX_TABLE_NAME);
        descriptions.add(PARAM_TAX_DATE_FORMAT);
    }

    @Override
    public void afterProcessing(JbillingMediationRecord jmr, OrderDTO updatedOrder, MediationEventResult eventResult) {

        OrderLineDTO updatedMediatedLine = updatedOrder.getLineById(eventResult.getOrderLinedId());
        if(null == updatedMediatedLine) {
            logger.debug("mediated line {} not found on order {}", eventResult.getOrderLinedId(), updatedOrder.getId());
            return;
        }

        // applying rounding and scale to mediated line.
        BigDecimal orderLineAmount = updatedMediatedLine.getAmount();
        if((updatedMediatedLine.hasOrderLineUsagePools() &&
                BigDecimal.ZERO.compareTo(orderLineAmount) == 0) ||
                BigDecimal.ZERO.compareTo(eventResult.getAmountForChange()) == 0) {
            return;
        }
        BigDecimal mediatedEventAmount = eventResult.getAmountForChange().setScale(scale(), roundingMode());
        if(mediatedEventAmount.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal minimumAmount = minimumAmount();
            if(minimumAmount.compareTo(BigDecimal.ZERO) != 0) {
                mediatedEventAmount = minimumAmount;
            }
        }
        logger.debug("before appyling rounding and scale, mediated event amount {}, "
                + "after applying new amount {}, for jmr {}", eventResult.getAmountForChange(),
                mediatedEventAmount, jmr.getRecordKey());
        orderLineAmount = orderLineAmount.subtract(eventResult.getAmountForChange());
        eventResult.setAmountForChange(mediatedEventAmount);
        orderLineAmount = orderLineAmount.add(mediatedEventAmount);
        updatedMediatedLine.setAmount(orderLineAmount);
        updatedMediatedLine.setPrice(orderLineAmount.divide(updatedMediatedLine.getQuantity(), MathContext.DECIMAL128));
    }

    public Integer roundingMode() {
        String paramValue = getParameter(PARAM_ROUNDING_MODE.getName(), PARAM_ROUNDING_MODE.getDefaultValue());
        if(ROUNDING_MODE_MAP.containsKey(paramValue)) {
            return ROUNDING_MODE_MAP.get(paramValue);
        }
        logger.error("invalid rounding mode {} passed to plugin parameter, so using default value", paramValue);
        return BigDecimal.ROUND_HALF_UP;
    }

    public Integer scale() {
        String paramValue = getParameter(PARAM_ROUNDING_SCALE.getName(), PARAM_ROUNDING_SCALE.getDefaultValue());
        if(!NumberUtils.isDigits(paramValue)) {
            logger.error("invalid rounding scale {} passed to plugin parameter, so using default value", paramValue);
            return Integer.parseInt(PARAM_ROUNDING_SCALE.getDefaultValue());
        }
        return Integer.parseInt(paramValue);
    }

    private BigDecimal minimumAmount() {
        try {
            return new BigDecimal(getParameter(PARAM_MINIMUM_CHARGE.getName(), PARAM_MINIMUM_CHARGE.getDefaultValue()))
            .setScale(scale(), roundingMode());
        } catch(NumberFormatException numberFormatException) {
            throw new SessionInternalError("invalid minimum charge configured for plugin JMRPostProcessorTask", numberFormatException);
        }
    }

    @Override
    public void afterProcessingUndo(JbillingMediationRecord jmr, OrderLineDTO mediatedLine) {
        logger.debug("undo {} jmr on mediated line {}", jmr, mediatedLine.getId());
    }

}
