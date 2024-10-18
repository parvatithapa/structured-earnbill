package com.sapienter.jbilling.server.quantity.usage.domain;

import java.math.BigDecimal;
import java.util.Date;


public interface IUsageRecord {

    Integer getItemId();

    Integer getUserId();

    String getResourceId();

    BigDecimal getQuantity();

    Date getStartDate();

    Date getEndDate();
}
