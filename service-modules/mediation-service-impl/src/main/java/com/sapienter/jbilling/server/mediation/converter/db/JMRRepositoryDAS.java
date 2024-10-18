package com.sapienter.jbilling.server.mediation.converter.db;

import com.sapienter.jbilling.server.filter.Filter;

import java.util.List;

/**
 * Created by marcolin on 04/11/15.
 */
public interface JMRRepositoryDAS {

    List<JbillingMediationRecordDao> findMediationRecordsByFilters(Integer page, Integer size, List<Filter> filters);

    List<JbillingMediationErrorRecordDao> findMediationErrorRecordsByFilters(Integer page, Integer size, List<Filter> filters);

    List<JbillingMediationErrorRecordDao> findMediationDuplicateRecordsByFilters(Integer page, Integer size, List<Filter> filters);

    List<String> getCdrTypes(List<Filter> filters);

    Long countMediationRecordsByFilters(List<Filter> filters);

    Long countMediationErrorsByFilters(List<Filter> filters);
}
