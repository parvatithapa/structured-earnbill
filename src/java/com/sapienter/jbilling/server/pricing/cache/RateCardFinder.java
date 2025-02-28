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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.PriceNotFoundException;
import com.sapienter.jbilling.server.mediation.cache.AbstractFinder;
import com.sapienter.jbilling.server.mediation.cache.ILoader;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

/**
 * RateCardFinder
 *
 * @author Brian Cowdery
 * @since 18-02-2012
 */
public class RateCardFinder extends AbstractFinder {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RateCardFinder.class));

    public RateCardFinder(JdbcTemplate template, ILoader loader) {
        super(template, loader);
    }

    public void init() {
        // noop
    }

    public BigDecimal findPrice(MatchType matchType, String searchValue, MatchCallback callback) {
        LOG.debug("Rating '" + searchValue + "' using " + matchType);

        // find price
        BigDecimal price = BigDecimal.ZERO;

        if (matchType != null) {
            String sql = "select * from " + loader.getTableName() + " where match_column = ?";
            price = matchType.findPrice(getJdbcTemplate(), sql, searchValue, callback);
        }

        LOG.debug(searchValue + " rated to $" + price + "/unit");
        if (price == null) {
            throw new PriceNotFoundException(CommonConstants.PRICE_NOT_FOUND_IN_RATE_CARD);
        }

        return price;
    }
    
    public BigDecimal findPrice(MatchType matchType, String searchValue) {
    	return findPrice(matchType, searchValue, MatchCallback.DO_NOTHING);
    }
}
