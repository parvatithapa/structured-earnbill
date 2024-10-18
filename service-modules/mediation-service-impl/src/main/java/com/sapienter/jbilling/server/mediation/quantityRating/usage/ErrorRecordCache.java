package com.sapienter.jbilling.server.mediation.quantityRating.usage;

import java.math.BigDecimal;

public interface ErrorRecordCache {

    BigDecimal getResolvedQuantity(String key);

    boolean mightContain(String key);
}
