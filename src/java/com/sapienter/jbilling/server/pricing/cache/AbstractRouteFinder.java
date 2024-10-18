/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2013] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing.cache;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.AbstractFinder;
import com.sapienter.jbilling.server.mediation.cache.ILoader;
import com.sapienter.jbilling.server.pricing.MatchingRecord;
import com.sapienter.jbilling.server.pricing.Route;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;
import com.sapienter.jbilling.server.util.NanoStopWatch;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.util.*;

/**
 * Abstract Route Finder
 *
 * @author Panche Isajeski
 * @since 22-Aug-2013
 *
 * @param <K> Implementation of matching record from the route table
 * @param <V> Implementation of a route that stores the information in a separate route table
 */
public abstract class AbstractRouteFinder<K extends MatchingRecord, V extends Route> extends AbstractFinder {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractRouteFinder.class));

    public AbstractRouteFinder(JdbcTemplate template, ILoader loader) {
        super(template, loader);
    }

    public void init() {
        // noop
    }

    // wrapper to calculate time elapsed
    // TODO: find out how to apply via AOP to non-spring bean
    protected K findMatchingRecord(V route, List<PricingField> fields) {
        NanoStopWatch stopWatch = new NanoStopWatch("AbstractRouteFinder.findMatchingRecord");
        stopWatch.start();
        K result = _findMatchingRecord(route, fields);
        stopWatch.stop();
        LOG.debug(stopWatch.getName() + " took: " + stopWatch.getElapsedMilliseconds() + " ms.");
        return result;
    }

    /**
     * Find a matching record by matching the provided pricing fields
     * against the configured route matching fields using different types of matching
     *
     * @param route
     * @param fields
     * @return matching record or null if no record found;
     * exception if more than 1 matching record found
     */
    protected K _findMatchingRecord(V route, List<PricingField> fields) {

        MatchingFieldResult mfResult = new MatchingFieldResult(route.getTableName());

        List<MatchingFieldDTO> matchingFields         = new ArrayList<MatchingFieldDTO>(route.getMatchingFields());
        List<MatchingFieldDTO> optionalMatchingFields = new ArrayList<MatchingFieldDTO>();

        Collections.sort(matchingFields, new Comparator<MatchingFieldDTO>() {
            public int compare(MatchingFieldDTO f1, MatchingFieldDTO f2) {
                return f1.getOrderSequence().compareTo(f2.getOrderSequence());
            }
        });

        if ( matchingFields.size() == 0 ) {
            LOG.debug("No matching field.s configured for route search.");
            return null;
        }

        // apply each required matching field to result; how the metafield is applied depends on its type
        // collect list of optional matching fields to use later at step 3.
        for (MatchingFieldDTO mf : matchingFields) {
            if (mf.getRequired()) {
                mf.apply(fields, mfResult);
            } else {
                optionalMatchingFields.add(mf);
            }
        }

        // process the required and optional results in the following order:
        // 1. First check the required fields by sql

        List<K> records = filterRecordsBySql(mfResult, route);

        if (records.isEmpty()) {
            return null;
        } else if (records.size() == 1) {
            return records.get(0);
        }

        // 2. then check the required fields by criteria using the route records already found
        filterRecordsByCriteria(records, mfResult, true);

        if (records.isEmpty()) {
            return null;
        } else if (records.size() == 1) {
            return records.get(0);
        }

        // 3. add optional fields one by one to matching and check if only 0 or 1 route records found
        for(MatchingFieldDTO mf : optionalMatchingFields) {
            mf.apply(fields, mfResult);
            records = filterRecordsBySql(mfResult, route);

            if (records.isEmpty()) {
                return null;
            } else if (records.size() == 1) {
                return records.get(0);
            }
        }

        filterRecordsByCriteria(records, mfResult, false);

        if (records.size() > 1) {
            //no unique matching record could be resolved.
            throw new SessionInternalError("No Route matching mandatory fields resolved.", new String[]{
                    "no.unique.route.resolved"
            });
        }

        return records.isEmpty() ? null : records.get(0);
    }

    protected List<K> filterRecordsBySql(MatchingFieldResult fieldsResult, V routeDTO) {

        List<K> records = new ArrayList<K>();

        String sql = fieldsResult.buildSqlExpression();
        LOG.debug("MatchingFieldResult sql: %s", sql);

        if (sql.length() == 0) {
            return records;
        }

        SqlRowSet sqlRowSet = getJdbcTemplate().queryForRowSet(sql);

        Map<String, Integer> bestMatchLenghts = new HashMap<String, Integer>();
        while (sqlRowSet.next()) {
            if (fieldsResult.hasBestMatchingCriteria()) {
                if (sqlRowSet.isFirst()) {
                    bestMatchLenghts = fieldsResult.buildLengthsMap(sqlRowSet);
                } else {
                    if (! fieldsResult.isBestMatchLenghtsEqual(bestMatchLenghts, sqlRowSet)) {
                        break;
                    }
                }
            }
            K routeRecord = buildRecord(sqlRowSet, routeDTO);
            records.add(routeRecord);
        }

        return records;
    }

    protected void filterRecordsByCriteria(List<K> records, MatchingFieldResult result, boolean mandatoryCriteria) {

        if (result.getFilterCriteria().isEmpty()) {
            return;
        }

        for (Iterator<K> i = records.iterator(); i.hasNext();) {
            K record = i.next();
            boolean foundMatch = mandatoryCriteria ? true : false;
            for (Map.Entry<MatchingFieldDTO, FilterCallback> entry : result.getFilterCriteria().entrySet()) {
                MatchingFieldDTO fieldDTO = entry.getKey();
                FilterCallback filter = entry.getValue();
                try {
                    if (mandatoryCriteria) {
                        foundMatch = foundMatch && filter.accept(fieldDTO, record);
                    } else {
                        foundMatch = foundMatch || filter.accept(fieldDTO, record);
                    }
                } catch (Exception e) {
                    LOG.error("Error applying the filter criteria %s on field %s", filter, fieldDTO.getMatchingField());
                }
            }

            if (!foundMatch) {
                i.remove();
            }
        }
    }

    protected Map<String, String> buildAttributeMap(SqlRowSet sqlRowSet, String... excludedAttributes) {

        Map<String, String> attributeMap = new HashMap<String, String>();
        List<String> excludedAttributeList = Arrays.asList(excludedAttributes);

        SqlRowSetMetaData sqlRowSetMetadata = sqlRowSet.getMetaData();
        String[] columnNames = sqlRowSetMetadata.getColumnNames();
        for (String column : columnNames) {
            String columnLowerCase = column.toLowerCase();
            if (!excludedAttributeList.contains(columnLowerCase)) {
                attributeMap.put(columnLowerCase, sqlRowSet.getString(column));
            }
        }

        return attributeMap;
    }

    protected abstract K buildRecord(SqlRowSet sqlRowSet, V route);
}
