package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

import java.util.List;

/**
 * @author Gerhard Maree
 * @since 31-01-2014
 */
public class DataTableQueryDAS extends AbstractDAS<DataTableQueryDTO> {


    /**
     * Find all queries that the user may execute on the table.
     *
     * @param routeId ID of the data table that is the root target of the query
     * @param userId Only return queries this user may execute
     * @return
     */
    public List<DataTableQueryDTO> findDataTableQueriesForTable(int routeId, int userId) {
        Query query = getSession().getNamedQuery("DataTableQueryDTO.findDataTableQueriesForTable");
        query.setInteger("userId", userId);
        query.setInteger("routeId", routeId);
        return query.list();
    }

    /**
     * Find all queries that the user may execute on the table.
     *
     * @param routeId ID of the data table that is the root target of the query
     * @param userId Only return queries this user may execute
     * @return
     */
    public Integer countDataTableQueriesForTableAndName(int routeId, int userId, String name) {
        Query query = getSession().getNamedQuery("DataTableQueryDTO.countDataTableQueriesForTableAndName");
        query.setInteger("userId", userId);
        query.setString("name", name);
        query.setInteger("routeId", routeId);
        return ((Number)query.uniqueResult()).intValue();
    }

    /**
     * Find all queries linked to the table.
     *
     * @param routeId ID of the data table that is the root target of the query
     * @return
     */
    public List<DataTableQueryDTO> findAllDataTableQueriesForTable(int routeId) {
        Query query = getSession().getNamedQuery("DataTableQueryDTO.findAllDataTableQueriesForTable");
        query.setInteger("routeId", routeId);
        return query.list();
    }

    /**
     * Return all the entries linked that can execute against the table
     * @param routeId
     * @return
     */
    public List<DataTableQueryEntryDTO> findEntriesLinkedToTable(int routeId) {
        Query query = getSession().getNamedQuery("DataTableQueryEntryDTO.findAllDataTableEntriesForTable");
        query.setInteger("routeId", routeId);
        return query.list();
    }

    /* NGES: This method search ratecode in dataTable for a rate.
    * @params tableName
    * @params rate
    *
    * @return ratecode
    * */
    public String getRateCode(String tableName, String rate){
        SQLQuery query = getSession().createSQLQuery("SELECT ratereadycode from "+tableName+" where CAST(rate AS DECIMAL(18, 7))=(SELECT MAX(CAST(rate AS DECIMAL(18, 7))) FROM "+tableName+" WHERE CAST(rate AS DECIMAL(18, 7)) <= CAST(:rate AS DECIMAL(18, 7)))");
        query.setParameter("rate", rate);
        query.setMaxResults(1);
        return  (String)query.uniqueResult();
    }
}
