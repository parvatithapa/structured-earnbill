package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Aggregates a collection of IRouteUpdaters. All update method calls will be forwarded to all updaters.
 *
 * @author Gerhard
 * @since 09/12/13
 */
public class AggregateRouteUpdater implements ITableUpdater {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AggregateRouteUpdater.class));

    private Set<ITableUpdater> updaters;

    /**
     * The id returned by the first updater will be used by the rest of the updaters.
     *
     * @param row
     * @return
     */
    public int create(Map<String, String> row) {
        if(updaters.isEmpty()) {
            LOG.debug("No updaters configured");
            return 0;
        }

        Iterator<ITableUpdater> iterator = updaters.iterator();

        int id = iterator.next().create(row);
        row.put("id", Integer.toString(id));

        while(iterator.hasNext()) {
            iterator.next().create(row);
        }
        return id;
    }

    public void update(Map<String, String> row) {
        for(ITableUpdater updater : updaters) {
            updater.update(row);
        }
    }

    public void delete(int rowId) {
        for(ITableUpdater updater : updaters) {
            updater.delete(rowId);
        }
    }

    /**
     * Return the content of the first updater.
     *
     * @return
     */
    public List<Map<String, String>> list() {
        if(updaters.isEmpty()) {
            LOG.debug("No updaters configured");
            return new ArrayList<Map<String, String>>(0);
        }

        Iterator<ITableUpdater> iterator = updaters.iterator();

        return iterator.next().list();
    }

    public SearchResultString search(SearchCriteria criteria) {
        if(updaters.isEmpty()) {
            LOG.debug("No updaters configured");
            throw new SessionInternalError("No delegates defined for AggregateRouteUpdater");
        }

        Iterator<ITableUpdater> iterator = updaters.iterator();

        return iterator.next().search(criteria);
    }

    public void setUpdaters(Set<ITableUpdater> updaters) {
        this.updaters = updaters;
    }
}
