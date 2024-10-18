package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.log4j.Logger;

/**
 * Created by vivek on 15/1/15.
 */
public class ConfigurationCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ConfigurationCopyTask.class));
    private static final Class dependencies[] = new Class[]{
            AgeingConfigCopyTask.class,
            AccountInformationTypeCopyTask.class,
            AgeingEntityStepCopyTask.class,
            BillingProcessConfigurationCopyTask.class,
            CurrencyExchangeCopyTask.class,
            DataTableCopyTask.class,
            EnumerationCopyTask.class,
            InvoiceTemplateCopyTask.class,
            MetaFieldsCopyTask.class,
            NotificationMessageCopyTask.class,
            OrderChangeStatusCopyTask.class,
            OrderChangeTypeCopyTask.class,
            OrderPeriodCopyTask.class,
            OrderStatusCopyTask.class,
            PaymentMethodTypeCopyTask.class,
            PaymentMethodTemplateCopyTask.class,
            PluginCopyTask.class,
            PreferenceCopyTask.class,
            RateCardCopyTask.class,
            RatingUnitCopyTask.class,
            RouteRateCardCopyTask.class,
            UsagePoolCopyTask.class
    };
    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        return false;
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("All configuration related content has been copied here from dependency.");
    }
}
