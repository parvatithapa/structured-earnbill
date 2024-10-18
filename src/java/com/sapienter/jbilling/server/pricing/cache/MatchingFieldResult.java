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

import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Matching Field Result
 *
 * @author Panche Isajeski
 * @since 22-Aug-2013
 */
public class MatchingFieldResult  {

    private class FieldData {
        public final String fieldName;
        public final String fieldValue;

        public FieldData (String name, String value) {
            this.fieldName = name;
            this.fieldValue = value;
        }
    }
    private class BestMatchFieldData extends FieldData {
        public final Integer smallestValue;
        public final Integer longestValue;

        public BestMatchFieldData (String name, String value, Integer smallest, Integer longest) {
            super(name, value);
            this.smallestValue = smallest;
            this.longestValue = longest;
        }
    }

    private String tableName;

    private List<FieldData> exactMatching = new ArrayList<FieldData>();
    private List<BestMatchFieldData> bestMatching = new ArrayList<BestMatchFieldData>();

    private Map<MatchingFieldDTO, FilterCallback> filterCriteria = new LinkedHashMap<MatchingFieldDTO, FilterCallback>();

    public MatchingFieldResult(String theTableName) {
        this.tableName = theTableName;
    }

    public void addExactCriteria (String fieldName, String fieldValue) {
        exactMatching.add(new FieldData(fieldName, fieldValue));
    }

    public void addBestMatchCriteria (String fieldName, String fieldValue, Integer smallest, Integer longest) {
        bestMatching.add(new BestMatchFieldData(fieldName, fieldValue, smallest, longest));
    }

    public boolean hasBestMatchingCriteria () {
        return bestMatching.size() > 0;
    }

    private boolean hasExactMatchingCriteria () {
        return exactMatching.size() > 0;
    }

    public void addFilterCriteria(MatchingFieldDTO field, FilterCallback filterCallback) {
        getFilterCriteria().put(field, filterCallback);
    }

    public Map<MatchingFieldDTO, FilterCallback> getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(Map<MatchingFieldDTO, FilterCallback> filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    private static String TABLE_SQL_TEMPLATE = "SELECT %s FROM %s rc ";
    private static String MATCHING_SQL_TEMPLATE = TABLE_SQL_TEMPLATE + "WHERE %s %s";

    public String buildSqlExpression() {

        if ((! hasExactMatchingCriteria()) && (! hasBestMatchingCriteria())) {
            return String.format(TABLE_SQL_TEMPLATE, buildFieldList(), tableName);
        }
        return String.format(MATCHING_SQL_TEMPLATE, buildFieldList(), tableName, buildCondition(), buildOrderBy());
    }

    private String buildFieldList () {

        StringBuilder result = new StringBuilder("rc.*");

        if (hasBestMatchingCriteria()) {
            result.append(", ");
            result.append(buildBestMatchLenfields());
        }
        return result.toString();
    }

    private String buildCondition () {

        StringBuilder result = new StringBuilder("");

        if (hasExactMatchingCriteria()) {
            List<String> fields = new ArrayList<String>(exactMatching.size());
            for(FieldData field: exactMatching) {
                fields.add(String.format("%s = '%s'", field.fieldName, field.fieldValue));
            }
            result.append(StringUtils.join(fields, " AND "));
        }
        if (hasBestMatchingCriteria()) {
            if (hasExactMatchingCriteria()) {
                result.append(" AND ");
            }
            List<String> fields = new ArrayList<String>(bestMatching.size());
            for(BestMatchFieldData field: bestMatching) {
                fields.add(String.format("(%s IN (%s))", field.fieldName, 
                        loToHiRange(field.fieldValue, field.smallestValue, field.longestValue)));
            }
            result.append(StringUtils.join(fields, " AND "));
        }
        return result.toString();
    }

    private String loToHiRange (String value, int lo, int hi) {

        StringBuilder result = new StringBuilder();
        int upper = java.lang.Math.min(value.length(), hi);
        int lower = java.lang.Math.min(lo, upper);
        List<String> values = new ArrayList<String>(upper - lower + 1);

        for (int i = lower; i <= upper; i++) {
            values.add(String.format("'%s'", value.substring(0, i)));
        }
        result.append(StringUtils.join(values, ","));

        return result.toString();
    }

    private String buildBestMatchLenfields () {

        StringBuilder result = new StringBuilder();
        List<String> fields = new ArrayList<String>(bestMatching.size());

        for (FieldData field: bestMatching) {
            fields.add(String.format("LENGTH(%1$s) as %1$s_LEN", field.fieldName));
        }
        result.append(StringUtils.join(fields, ","));

        return result.toString();
    }

    private String buildOrderBy () {

        StringBuilder result = new StringBuilder();

        if (hasBestMatchingCriteria()) {
            result.append("ORDER BY ");
            List<String> fields = new ArrayList<String>(bestMatching.size());
            for (FieldData field: bestMatching) {
                fields.add(String.format("%1$s_LEN DESC", field.fieldName));
            }
            result.append(StringUtils.join(fields, ","));
        }
        return result.toString();
    }

    public Map<String, Integer> buildLengthsMap (SqlRowSet row) {

        Map<String, Integer> result = new HashMap<String, Integer>();

        for (FieldData field: bestMatching) {
            result.put(field.fieldName, row.getInt(String.format("%s_LEN", field.fieldName)));
        }
        return result;
    }

    public boolean isBestMatchLenghtsEqual (Map<String, Integer> bestMatchLenghts, SqlRowSet row) {
        for (FieldData field: bestMatching) {
            if (bestMatchLenghts.get(field.fieldName) != row.getInt(String.format("%s_LEN", field.fieldName))) {
                return false;
            }
        }
        return true;
    }
}
