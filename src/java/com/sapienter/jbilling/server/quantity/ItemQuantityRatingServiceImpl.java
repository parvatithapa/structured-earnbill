package com.sapienter.jbilling.server.quantity;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
public class ItemQuantityRatingServiceImpl implements ItemQuantityRatingService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter @Setter
    private QuantityRater ratingUnitRater;

    @Getter @Setter
    private QuantityRater ratingSchemeRater;


    @Override
    public BigDecimal rate(BigDecimal quantity, QuantityRatingContext context)
            throws IllegalStateException {

        BigDecimal inputQuantity = quantity;

        if (ratingUnitRater != null) {
            quantity = ratingUnitRater.rate(quantity, context);
        }

        logger.info("rated quantity after applying rating unit= {}", quantity);

        if (ratingSchemeRater != null) {
            quantity = ratingSchemeRater.rate(quantity, context);
        }
        logger.info("Input quantity= {} rated quantity= {}", inputQuantity, quantity);

        return quantity;
    }
}
