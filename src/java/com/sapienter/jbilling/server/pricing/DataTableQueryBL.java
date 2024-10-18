package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDAS;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDTO;
import com.sapienter.jbilling.server.pricing.db.DataTableQueryEntryDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.user.UserBL;

import java.util.List;

/**
 * @author Gerhard Maree
 * @since 31-01-2014
 */
public class DataTableQueryBL {
    private DataTableQueryDAS das;
    private DataTableQueryDTO dto;

    public DataTableQueryBL() {
        init();
    }

    public DataTableQueryBL(DataTableQueryDTO dto) {
        this.dto = dto;
        init();
    }

    private void init() {
        das = new DataTableQueryDAS();
    }

    public void set(int id) {
        dto = das.find(id);
    }

    public DataTableQueryDTO getEntity() {
        return dto;
    }


    /**
     * Convert a DataTableQueryWS into DataTableQueryDTO, including all entries
     *
     * @param ws
     * @return
     */
    public static DataTableQueryDTO convertToDto(DataTableQueryWS ws) {
        DataTableQueryDTO dto = new DataTableQueryDTO();
        dto.setId(ws.getId());
        dto.setName(ws.getName());
        dto.setRoute(new RouteDAS().find(ws.getRouteId()));
        dto.setVersionNum(ws.getVersionNum());
        dto.setRootEntry( convertToDto(ws.getRootEntry()));
        dto.setUser(new UserBL(ws.getUserId()).getDto());
        dto.setGlobal(ws.getGlobal());
        return dto;
    }

    /**
     * Convert a DataTableQueryEntryWS into a DataTableQueryEntryDTO, including nested queries
     *
     * @param entryWS
     * @return
     */
    private static DataTableQueryEntryDTO convertToDto(DataTableQueryEntryWS entryWS) {
        DataTableQueryEntryDTO dto = new DataTableQueryEntryDTO();
        dto.setId(entryWS.getId());
        dto.setRoute(new RouteDAS().find(entryWS.getRouteId()));
        dto.setVersionNum(entryWS.getVersionNum());
        dto.setColumns(entryWS.getColumns());
        if(entryWS.getNextQuery() != null) {
            dto.setNextQuery(convertToDto(entryWS.getNextQuery()));
        }
        return dto;
    }

    public static DataTableQueryWS convertToWS(DataTableQueryDTO dto) {
        return convertToWS(dto, true);
    }

    /**
     * Convert a DataTableQueryDTO into DataTableQueryWS.
     *
     * @param dto
     * @param includeEntries - if true the root entry and sub-entries will be included
     * @return
     */
    public static DataTableQueryWS convertToWS(DataTableQueryDTO dto, boolean includeEntries) {
        DataTableQueryWS ws = new DataTableQueryWS();
        ws.setId(dto.getId());
        ws.setName(dto.getName());
        ws.setRouteId(dto.getRoute().getId());
        ws.setVersionNum(dto.getVersionNum());
        ws.setUserId(dto.getUser().getUserId());
        ws.setRootEntry( convertToWs(dto.getRootEntry()));
        return ws;
    }

    /**
     * Convert a DataTableQueryEntryDTO into DataTableQueryEntryWS, including nested entries.
     *
     * @param dto
     * @return
     */
    private static DataTableQueryEntryWS convertToWs(DataTableQueryEntryDTO dto) {
        DataTableQueryEntryWS ws = new DataTableQueryEntryWS();
        ws.setId(dto.getId());
        ws.setRouteId(dto.getRoute().getId());
        ws.setVersionNum(dto.getVersionNum());
        ws.setColumns(dto.getColumns());
        if(dto.getNextQuery() != null) {
            ws.setNextQuery(convertToWs(dto.getNextQuery()));
        }
        return ws;
    }

    public DataTableQueryDTO create(DataTableQueryDTO dto) throws SessionInternalError {
        Integer count = das.countDataTableQueriesForTableAndName(dto.getRoute().getId(), dto.getUser().getUserId(), dto.getName());
        if(count > 0) {
            throw new SessionInternalError("Duplicate name for query for the table", new String[] {"bean.DataTableQueryWS.name.duplicate"});
        }
        dto = das.save(dto);
        return dto;
    }

    public void delete(int id) {
        das.delete(das.find(id));
    }

    /**
     * Delete all queries linked to the table
     *
     * @param routeId
     * @throws SessionInternalError
     */
    public void deleteQueriesLinkedToTable(int routeId) throws SessionInternalError {
        //find all entries linked to the table
        List<DataTableQueryEntryDTO> entries = das.findEntriesLinkedToTable(routeId);
        for(DataTableQueryEntryDTO entry: entries) {
            while(entry.getQuery() == null) {
                entry = entry.getPrevQuery();
            }
            DataTableQueryDTO query = entry.getQuery();
            das.delete(query);
        }

        //find all queries with the root table linked to the table
        List<DataTableQueryDTO> queries = das.findAllDataTableQueriesForTable(routeId);
        for(DataTableQueryDTO query: queries) {
            das.delete(query);
        }
    }

    /**
     * Find all queries that the user may execute on the table.
     *
     * @param routeId ID of the data table that is the root target of the query
     * @param userId Only return queries this user may execute
     * @return
     * @throws SessionInternalError
     */
    public List<DataTableQueryDTO> findDataTableQueriesForTable(int routeId, int userId) throws SessionInternalError {
        return das.findDataTableQueriesForTable(routeId, userId);
    }

    /**
     * Convert a list of DataTableQueryDTO into a DataTableQueryWS[]
     *
     * @param list
     * @param includeEntries if true the entries for each query will be converted as well
     * @return
     */
    public DataTableQueryWS[] convertToWsArray(List<DataTableQueryDTO> list, boolean includeEntries) {
        DataTableQueryWS[] ws = new DataTableQueryWS[list.size()];
        for(int i=0; i<list.size(); i++) {
            ws[i] = convertToWS(list.get(i), includeEntries);
        }
        return ws;
    }
}
