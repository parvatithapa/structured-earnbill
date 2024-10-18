package com.sapienter.jbilling.server.quantity.data.das;

import java.util.Date;
import java.util.NavigableMap;
import java.util.SortedMap;

import com.sapienter.jbilling.server.ratingUnit.domain.RatingUnit;

public interface RatingUnitDAS {

    NavigableMap<Date, RatingUnit> getRatingUnit(int entityId, int itemId);
}
