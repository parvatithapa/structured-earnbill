package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Locates a jBilling user using the value of one of its
 * metafields.
 */
public class UserLocatorByMetaField implements DiameterUserLocator {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UserLocatorByMetaField.class));
    private String fieldName = "Subscription-Id-Data";
    private String metaFieldName = "Subscriber-Id";

    @Override
    public Integer findUserFromParameters(Integer entityId, List<PricingField> pricingFields) {

        MetaFieldDAS finder = new MetaFieldDAS();
        MetaField metaField = finder.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, metaFieldName);
        if (metaField == null) {
            LOG.debug("No meta field was found with the name %s for the entity CUSTOMER", metaFieldName);
            return null;
        }

        for (PricingField pf : pricingFields) {
            if (fieldName.equals(pf.getName())) {
                LOG.debug("The required field %s was found in the list of fields sent in the Diameter call", fieldName);
                List<Integer> hits = finder.findEntitiesByMetaFieldValue(metaField, pf.getStrValue());
                if (hits == null || hits.isEmpty()) {
                    LOG.debug("No meta field was found with the value %s", pf.getStrValue());
                    return null;
                }

                LOG.debug("About to search for a customer with id %s", hits.get(0));
                CustomerDAS customerDAS = new CustomerDAS();
                return customerDAS.findNow(hits.get(0)).getBaseUser().getId();
            }
        }
        return null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMetaFieldName() {
        return metaFieldName;
    }

    public void setMetaFieldName(String metaFieldName) {
        this.metaFieldName = metaFieldName;
    }
}
