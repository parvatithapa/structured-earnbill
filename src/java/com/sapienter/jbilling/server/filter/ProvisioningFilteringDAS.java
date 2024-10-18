package com.sapienter.jbilling.server.filter;

import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Created by marcolin on 17/08/16.
 */
public class ProvisioningFilteringDAS extends AbstractFilterDAS<ProvisioningCommandDTO> {

    ProvisioningFilteringDAS() {}

    @Override
    protected Class getClassToFilter() {
        return ProvisioningCommandDTO.class;
    }

    @Override
    protected Criteria addAliasesTo(Criteria criteria) {
        criteria = criteria.createAlias("entity", "company");
        return super.addAliasesTo(criteria);
    }

    @Override
    protected MetaField getMetaFieldByTypeId(Integer typeId) {
        return null;
    }

    @Override
    protected Criterion eqRestriction(Filter filter) {
        if (isFilterForField(filter, "command_type") || isFilterForField(filter, "dtype")) {
            return Restrictions.sqlRestriction("dtype=lower('" + filter.getValue() + "')");
        }

        if (isFilterForField(filter, "type_identifier")) {
            return Restrictions.sqlRestriction(
                    String.format("((EXISTS (SELECT *" +
                                  "            FROM order_line_provisioning_command_map" +
                                  "           WHERE provisioning_command_id = {alias}.id" +
                                  "             AND order_change_id = %1$s))" +
                                  " OR " +
                                  "(EXISTS (SELECT *" +
                                  "           FROM order_provisioning_command_map" +
                                  "          WHERE provisioning_command_id = {alias}.id" +
                                  "            AND order_id = %1$s))" +
                                  " OR " +
                                  "(EXISTS (SELECT *" +
                                  "           FROM payment_provisioning_command_map" +
                                  "          WHERE provisioning_command_id = {alias}.id" +
                                  "            AND payment_id = %1$s))" +
                                  " OR " +
                                  "(EXISTS (SELECT *" +
                                  "           FROM asset_provisioning_command_map " +
                                  "          WHERE provisioning_command_id = {alias}.id" +
                                  "            AND asset_id = %1$s)))",
                    filter.getValue()));
        }

        return super.eqRestriction(filter);
    }
}
