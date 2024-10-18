package com.sapienter.jbilling.server.order.dt;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.JMRQuantity;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.quantity.MediationQuantityResolutionService;
import com.sapienter.jbilling.server.quantity.QuantityRatingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Arrays;


public class DtOrderServiceImpl extends OrderServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private MediationQuantityResolutionService dtMediationQuantityResolutionService;

    @Override
    @Transactional(readOnly = true)
    public JMRQuantity resolveQuantity(JbillingMediationRecord jmr, PricingField[] fields) {

        QuantityRatingContext context = new QuantityRatingContext(
                jmr.getjBillingCompanyId(),
                jmr.getUserId(),
                jmr.getItemId(),
                jmr.getResourceId(),
                jmr.getEventDate(),
                jmr.getProcessId().toString(),
                jmr.getRecordKey(),
                Arrays.asList(fields));

        BigDecimal resolvedQty = dtMediationQuantityResolutionService
                .resolve(jmr.getOriginalQuantity(), context);

        JMRQuantity.JMRQuantityBuilder jb = JMRQuantity.builder();

        if (context.hasErrors()) {
            jb.fromErrorCodes(context.getErrors());
        } else {
            jb.quantity(resolvedQty);
        }

        return jb.build();
    }

    @Override
    protected void processLines(OrderDTO order, Integer languageId,
                                Integer entityId, Integer userId, Integer currencyId,
                                String pricingFields, Integer itemId) {

        OrderHelper.synchronizeOrderLines(order);
        OrderBL orderBL = new OrderBL(order);

        for (OrderLineDTO line : order.getLines()) {
            if (line.getItemId().equals(itemId)) {
                logger.debug("Processing line {}", line);
                orderBL.processLine(line, languageId, entityId,
                        userId, currencyId, pricingFields);
            }
        }

        OrderHelper.desynchronizeOrderLines(order);
    }
}
