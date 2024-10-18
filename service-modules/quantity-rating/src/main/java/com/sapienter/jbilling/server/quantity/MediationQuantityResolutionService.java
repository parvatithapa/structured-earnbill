package com.sapienter.jbilling.server.quantity;

import java.math.BigDecimal;

public interface MediationQuantityResolutionService {

    BigDecimal resolve(BigDecimal quantity, QuantityRatingContext context);
}
