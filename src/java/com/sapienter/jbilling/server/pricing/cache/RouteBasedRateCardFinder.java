/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.cache;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.ILoader;
import com.sapienter.jbilling.server.pricing.RouteRateCardRecord;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.pricing.strategy.AbstractPricingStrategy;


public class RouteBasedRateCardFinder extends AbstractRouteFinder<RouteRateCardRecord, RouteRateCardDTO> {


    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RouteBasedRateCardFinder.class));

    public RouteBasedRateCardFinder(JdbcTemplate template, ILoader loader) {
        super(template, loader);
    }

    @Override
    public void init() {
        // noop
    }

    /**
     *
     * @param routeRateCardDTO
     * @param fields
     * @param durationFieldName
     * @return
     */
    public BigDecimal findRoutePrice(RouteRateCardDTO routeRateCardDTO, List<PricingField> fields,
            String durationFieldName) {

        durationFieldName= (!StringUtils.isEmpty(durationFieldName)) ? durationFieldName : Constants.DEFAULT_DURATION_FIELD_NAME;

        PricingField durationField= AbstractPricingStrategy.find(fields, durationFieldName);
        Double duration = null != durationField ? durationField.getDoubleValue() : null;

        RouteRateCardRecord routeRecordFound = findMatchingRecord(routeRateCardDTO, fields);

        if (routeRecordFound == null) {
            return null;
        }

        return routeRecordFound.calculatePrice(BigDecimal.valueOf(duration));

    }

    /**
     *
     * @param routeRateCardDTO
     * @param fields
     * @param durationFieldName
     * @return
     */
    public RouteRateCardRecord findRouteRecord(RouteRateCardDTO routeRateCardDTO, List<PricingField> fields) {

        RouteRateCardRecord routeRecordFound = findMatchingRecord(routeRateCardDTO, fields);

        if (routeRecordFound == null) {
            return null;
        }

        return routeRecordFound;
    }


    /**
     * overloaded function for RouteBasedRateCardPricingStrategy
     * @param routeRateCardDTO
     * @param fields
     * @param durationFieldName
     * @param quantity
     * @return
     */
    public BigDecimal findRoutePrice(RouteRateCardDTO routeRateCardDTO, List<PricingField> fields,
            String durationFieldName, BigDecimal quantity, boolean isMediated) {

        Double duration = new Double(quantity.doubleValue());

        RouteRateCardRecord routeRecordFound = findMatchingRecord(routeRateCardDTO, fields);

        if (routeRecordFound == null) {
            return null;
        }

        return routeRecordFound.calculatePrice(BigDecimal.valueOf(duration), isMediated);

    }


    public BigDecimal findRoutePrice(RouteRateCardDTO dto, List<PricingField> fields) {
        return findRoutePrice(dto, fields, Constants.DEFAULT_DURATION_FIELD_NAME);
    }

    @Override
    protected RouteRateCardRecord buildRecord(SqlRowSet sqlRowSet, RouteRateCardDTO routeRateCardDTO) {

        RouteRateCardRecord routeRecord = new RouteRateCardRecord();
        routeRecord.setId(sqlRowSet.getInt("id"));
        routeRecord.setName(sqlRowSet.getString("name"));
        routeRecord.setInitialIncrement(new BigDecimal(sqlRowSet.getString("initial_increment")));
        routeRecord.setEventSurcharge(new BigDecimal(sqlRowSet.getString("surcharge")));
        routeRecord.setSubsequentIncrement(new BigDecimal(sqlRowSet.getString("subsequent_increment")));
        routeRecord.setCharge(new BigDecimal(sqlRowSet.getString("charge")));
        routeRecord.setRouteRateCard(routeRateCardDTO);
        routeRecord.setAttributes(buildAttributeMap(sqlRowSet, RouteRateCardDTO.TABLE_COLUMNS_NAMES.toArray(new String[]{})));

        return routeRecord;
    }

}
