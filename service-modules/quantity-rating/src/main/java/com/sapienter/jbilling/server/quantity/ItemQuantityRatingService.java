package com.sapienter.jbilling.server.quantity;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.item.PricingField;

public interface ItemQuantityRatingService {

    BigDecimal rate(BigDecimal quantity, QuantityRatingContext ratingContext) throws IllegalStateException;
}
