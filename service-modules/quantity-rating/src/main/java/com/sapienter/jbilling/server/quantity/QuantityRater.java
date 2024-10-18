package com.sapienter.jbilling.server.quantity;

import com.sapienter.jbilling.server.mediation.cache.CacheProvider;

import java.math.BigDecimal;


public interface QuantityRater extends CacheProvider {

    BigDecimal rate(BigDecimal quantity, QuantityRatingContext context) throws IllegalStateException;
}
