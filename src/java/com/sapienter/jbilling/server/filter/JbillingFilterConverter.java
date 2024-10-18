package com.sapienter.jbilling.server.filter;

import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import jbilling.FilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by marcolin on 04/11/15.
 */
public class JbillingFilterConverter {

    private static final List<Filter> EMPTY_LIST = new ArrayList<>();

    public static List<Filter> convert(List<jbilling.Filter> filters) {
        if (filters == null) {
            return EMPTY_LIST;
        }

        return filters.stream()
                      .map(JbillingFilterConverter::convert)
                      .filter(f -> f != null && f.getValue() != null)
                      .collect(Collectors.toList());
    }

    public static Filter convert(jbilling.Filter uiFilter) {
        Filter filter = null;
        if (FilterType.MEDIATIONPROCESS.equals(uiFilter.getType()) && uiFilter.getValue() != null) {
            filter = convertMediationProcess(uiFilter);
        }

        if (FilterType.PROVISIONING_CMD.equals(uiFilter.getType()) && uiFilter.getValue() != null) {
            filter = convertProvisioningProcess(uiFilter);
        }

        if (filter == null) {
            filter = otherFiltersConvert(uiFilter);
        }

        return filter;
    }


    private static Filter otherFiltersConvert(jbilling.Filter uiFilter) {
        if (uiFilter.getValue() == null) {
            return null;
        }

        if (uiFilter.getConstraintType().equals(jbilling.FilterConstraint.IN)) {
            return Filter.list(uiFilter.getField(),
                   FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                   (List) uiFilter.getValue());
        }

        if (uiFilter.getConstraintType().equals(jbilling.FilterConstraint.META_FIELD) && uiFilter.getFieldKeyData() != null) {
            return Filter.metaField(uiFilter.getField(),
                                    FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                                    (String) uiFilter.getValue(),
                                    Integer.parseInt(uiFilter.getFieldKeyData()));
        }

        if (uiFilter.getConstraintType().equals(jbilling.FilterConstraint.OR)) {
            return Filter.conjDisj(uiFilter.getField(),
                                   FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                                   uiFilter.getFilters());
        }

        if (uiFilter.getValue() instanceof Integer) {
            return Filter.integer(uiFilter.getField(),
                                  FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                                  (Integer) uiFilter.getValue());

        } else if (uiFilter.getValue() instanceof String) {
            return Filter.string(uiFilter.getField(),
                                 FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                                 (String) uiFilter.getValue());

        } else if (uiFilter.getValue() instanceof UUID) {
            return Filter.uuid(uiFilter.getField(),
                               FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                               (UUID) uiFilter.getValue());
        } else if (uiFilter.getConstraintType().equals(jbilling.FilterConstraint.DATE_BETWEEN)) {
            return Filter.betweenDates(uiFilter.getField(),
                                       uiFilter.getStartDateValue(),
                                       uiFilter.getEndDateValue());
        } else if (uiFilter.getValue() instanceof Object) {
            return Filter.object(uiFilter.getField(),
                                 FilterConstraint.valueOf(uiFilter.getConstraintType().name()),
                                 uiFilter.getValue());
        }
        return null;
    }
    
    public static Filter convertMediationProcess(jbilling.Filter filter) {
        if (filter.getField().equals("errors") && filter.getValue() != null) {
            if (filter.getIntegerValue() == 1) {
                return Filter.integer(filter.getField(), FilterConstraint.GREATER_THAN, 0);
            } else {
                return Filter.integer(filter.getField(), FilterConstraint.EQ, 0);
            }
        } else if (filter.getField().equals("id") && filter.getValue() != null){
            try{
                return Filter.uuid(filter.getField(),
                                   FilterConstraint.EQ,
                                   UUID.fromString(filter.getValue().toString()));
            } catch (IllegalArgumentException iae){
                return Filter.uuid(filter.getField(),
                                   FilterConstraint.EQ,
                                   new UUID(0L, 0L));
            }
        }else if(filter.getField().equals("startDate") && filter.getValue() != null){
            return Filter.betweenDates(filter.getField(),
                                       filter.getStartDateValue(),
                                       filter.getEndDateValue());
        }

        return Filter.string(filter.getField(),
                             FilterConstraint.valueOf(filter.getConstraintType().name()),
                             (String) filter.getValue());
    }

    public static Filter convertProvisioningProcess(jbilling.Filter filter) {
        if (filter.getField().equals("command_type") && filter.getValue() != null) {
            return Filter.object(filter.getField(),
                                 FilterConstraint.valueOf(filter.getConstraintType().name()),
                                 ProvisioningCommandType.values()[filter.getIntegerValue()]);
        }

        if (filter.getField().equals("commandStatus") && filter.getValue() != null) {
            return Filter.object(filter.getField(),
                                 FilterConstraint.valueOf(filter.getConstraintType().name()),
                                 ProvisioningCommandStatus.values()[filter.getIntegerValue()]);
        }

        if (filter.getField().equals("createDate") && filter.getValue() != null) {
            return Filter.betweenDates(filter.getField(),
                                       filter.getStartDateValue(),
                                       filter.getEndDateValue());
        }

        if ((filter.getField().equals("id") || filter.getField().equals("type_identifier")) && filter.getValue() != null) {
            return Filter.integer(filter.getField(),
                                  FilterConstraint.valueOf(filter.getConstraintType().name()),
                                  (Integer) filter.getValue());
        }

        return Filter.string(filter.getField(),
                             FilterConstraint.valueOf(filter.getConstraintType().name()),
                             (String) filter.getValue());
    }
}
