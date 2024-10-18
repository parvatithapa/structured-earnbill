package com.sapienter.jbilling.server.usageRatingScheme.scheme;

import com.sapienter.jbilling.server.quantity.usage.domain.IUsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;

import java.util.Date;
import java.util.Optional;
import java.util.function.BiFunction;


public interface IUsageConfiguration {

    boolean requiresUsage();

    Optional<IUsageRecord> getUsage(IUsageRatingSchemeModel model, IUsageQueryRecord query);

    boolean hasResetCycle();

    Date getCycleStartDate(IUsageRatingSchemeModel model, Date eventDate);

    Date getCycleEndDate(IUsageRatingSchemeModel model, Date startDate);
}
