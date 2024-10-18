package com.sapienter.jbilling.server.filter;

/**
 * Created by marcolin on 17/08/16.
 */
public class FilterFactory {
    public static OrderFilteringDAS orderFilterDAS() {
        return new OrderFilteringDAS();
    }
    public static ProvisioningFilteringDAS provisioningFilterDAS() {
        return new ProvisioningFilteringDAS();
    }
}
