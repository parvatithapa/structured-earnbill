package com.sapienter.jbilling.server.report;

import java.util.List;
import java.util.Map;

/**
 * @author Leandro Bagur
 * @since 01/08/17.
 */
public interface IReportBuilder {

    /**
     * Each Report Builder enum implements its way to get data
     * @param entityId entity id
     * @param childs childs
     * @param parameters parameters
     * @return List
     */
    List<Map<String, ?>> getData(Integer entityId, List<Integer> childs, Map<String, Object> parameters);
}
