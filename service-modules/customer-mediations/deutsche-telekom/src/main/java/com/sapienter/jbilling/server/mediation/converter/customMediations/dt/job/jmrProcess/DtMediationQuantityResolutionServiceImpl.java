package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.jmrProcess;

import com.sapienter.jbilling.server.mediation.quantityRating.usage.ErrorRecordCache;
import com.sapienter.jbilling.server.quantity.ItemQuantityRatingService;
import com.sapienter.jbilling.server.quantity.MediationQuantityResolutionService;
import com.sapienter.jbilling.server.quantity.QuantityRatingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;


@Service
public class DtMediationQuantityResolutionServiceImpl implements MediationQuantityResolutionService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ItemQuantityRatingService itemQuantityRatingService;
    private ErrorRecordCache errorRecordCache;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BigDecimal resolve(BigDecimal quantity, QuantityRatingContext context) {

        BigDecimal recycledRecordQuantity = getResolvedValueFromErrorRecord(context.getRecordKey());
        if (recycledRecordQuantity != null) {
            logger.info("This is a recycled record that failed with PROCESSED-WITH-ERROR error code.");
            logger.info("Returning previously resolved quantity: {}", recycledRecordQuantity);
            return recycledRecordQuantity;
        }

        return compute(quantity, context);
    }

    private BigDecimal compute(BigDecimal quantity, QuantityRatingContext context) {
        BigDecimal resolvedQty = BigDecimal.ZERO;
        try {
            resolvedQty = itemQuantityRatingService.rate(quantity, context);

        } catch (IllegalStateException e) {
            context.addError("ERR-QUANTITY-RES");
            logger.error("Quantity rating failed {}", context.getPricingFields());
            logger.error("ERR-QUANTITY-RES", e);

        } catch (Exception e) {
            context.addError("ERR-QUANTITY-RES");
            logger.info("Quantity not found {}", context.getPricingFields());
            logger.error("ERR-QUANTITY-RES", e);

        } finally {
            return resolvedQty;
        }
    }

    private BigDecimal getResolvedValueFromErrorRecord(String recordKey) {
        return errorRecordCache.getResolvedQuantity(recordKey);
    }

    public void setItemQuantityRatingService(ItemQuantityRatingService itemQuantityRatingService) {
        this.itemQuantityRatingService = itemQuantityRatingService;
    }

    public void setErrorRecordCache(ErrorRecordCache errorRecordCache) {
        this.errorRecordCache = errorRecordCache;
    }
}
