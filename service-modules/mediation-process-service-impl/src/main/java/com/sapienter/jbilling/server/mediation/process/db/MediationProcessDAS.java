package com.sapienter.jbilling.server.mediation.process.db;

import com.sapienter.jbilling.server.filter.Filter;

import java.util.List;

/**
 * Created by bilal on 10/29/15
 */
public interface MediationProcessDAS {

    List<MediationProcessDAO> findMediationProcessByFilters(int page, int size, String sort, String order, List<Filter> filters);

}
