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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.math.BigDecimal;

/**
 * MatchType specifies the logic used by {@link RateCardFinder} when determining if a pricing
 * field from mediation matches the 'match' column of the rating table when looking for a price.
 *
 * @author Brian Cowdery
 * @since 19-02-2012
 */
public enum MatchType {

    /**
     * Searches for an entry in the rating table where the entry exactly matches the search value
     */
    EXACT {
        public Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue,String column, MatchCallback callback) {
            LOG.debug("Searching for exact match '" + searchValue + "'");
            SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

            if (rs.next()) {
                return callback.onMatchObject(rs);
            }

            return null;
        }

        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue, MatchCallback callback) {
            LOG.debug("Searching for exact match '" + searchValue + "'");
            SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

            if (rs.next()) {
            	return callback.onMatch(rs);
            }

            return null;
        }
    },

    /**
     * Searches through the rating table looking for an entry using the search value as
     * a prefix. The BEST_MATCH continually shortens the prefix being used in the search
     * to find a match with the largest possible portion of the search string.
     */
    BEST_MATCH {

        public Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue,String column, MatchCallback callback) {
            int length = 10;

            searchValue = getCharacters(searchValue, length);

            while (length >= 0) {
                LOG.debug("Searching for prefix '" + searchValue + "'");
                SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

                if (rs.next()) {
                    return callback.onMatchObject(rs);
                } else {
                    length--;
                    searchValue = getCharacters(searchValue, length);
                }
            }
            return null;
        }

        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue, MatchCallback callback) {
            int length = 10;
            searchValue = getCharacters(searchValue, length);

            while (length >= 0) {
                LOG.debug("Searching for prefix '" + searchValue + "'");
                SqlRowSet rs = jdbcTemplate.queryForRowSet(query, searchValue);

                if (rs.next()) {
                    return callback.onMatch(rs);
                } else {
                    length--;
                    searchValue = getCharacters(searchValue, length);
                }
            }
            return null;
        }

        public String getCharacters(String number, int length) {
            if (length <= 0) return "*";
            return number.length() > length ? number.substring(0, length) : number;
        }
    },

    COUNTRY_CODE_MATCH {

        @Override
        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue, MatchCallback callback) {
            LOG.debug("Searching for country code match '" + searchValue + "'");
            if(!NumberUtils.isNumber(searchValue)) {
                return null;
            }
            String value = new String(searchValue.toCharArray());
            if(!searchValue.startsWith(PLUS_PREFIX)) {
                value = PLUS_PREFIX.concat(value);
            }
            try {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                String numericCountryCode = String.valueOf(phoneUtil.parse(value, "").getCountryCode());
                SqlRowSet rs = jdbcTemplate.queryForRowSet(query, PLUS_PREFIX+numericCountryCode);
                if(rs.next()) {
                    return callback.onMatch(rs);
                }
                else { // if the result set is null, check if 'restoftheworld' is defined in the ratecards.
					rs = jdbcTemplate.queryForRowSet(query, PLUS_PREFIX+"0");
					if(rs.next()) {
					    return callback.onMatch(rs);
					}
                }
            } catch (NumberParseException e) {
                throw new SessionInternalError(e);
            }
            return null;
        }

        @Override
        public Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue, String column, MatchCallback callback) {
            throw new UnsupportedOperationException("Country code match does not support findRoute Method!");
        }

    },

    COUNTRY_AREA_CODE_MATCH {

        @Override
        public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue, MatchCallback callback) {
            LOG.debug("Searching for country and area code match '" + searchValue + "'");
            if(!NumberUtils.isNumber(searchValue)) {
                return null;
            }
            String value = new String(searchValue.toCharArray());
            if(!searchValue.startsWith(PLUS_PREFIX)) {
                value = PLUS_PREFIX.concat(value);
            }
            try {
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                PhoneNumber number = phoneUtil.parse(value, "");
                String numericCountryCode = String.valueOf(number.getCountryCode());
                String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
                int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(number);
                String locationCode = nationalSignificantNumber.substring(0, areaCodeLength);
				if(areaCodeLength > 0){
					SqlRowSet rs = jdbcTemplate.queryForRowSet(query, PLUS_PREFIX+numericCountryCode+MINUS_PREFIX+locationCode);
				    if(rs.next()) {
				        return callback.onMatch(rs);
				    }
				    return COUNTRY_CODE_MATCH.findPrice(jdbcTemplate, query, searchValue);
				} else {
					String phoneNumber = PLUS_PREFIX+numericCountryCode+MINUS_PREFIX+nationalSignificantNumber;
					int countryCodeLength = numericCountryCode.length()+1;

					while(StringUtils.isNotEmpty(phoneNumber) &&  phoneNumber.length()>=countryCodeLength) {
						LOG.debug("Area code 'not found' searching for match using best match strategy '" + phoneNumber + "'");
						SqlRowSet rs = jdbcTemplate.queryForRowSet(query, phoneNumber);
						if (rs.next()) {
							return callback.onMatch(rs);
						}
						phoneNumber = StringUtils.chop(phoneNumber);
					}
					return null;
				}
            } catch (NumberParseException e) {
                throw new SessionInternalError(e);
            }
        }

        @Override
        public Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue, String column, MatchCallback callback) {
            throw new UnsupportedOperationException("Country and Area code match does not support findRoute Method!");
        }
    };

    private static final String PLUS_PREFIX = "+";
    private static final String MINUS_PREFIX = "-";
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MatchType.class));

    public abstract BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue, MatchCallback callback);
    public abstract Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue,String column,MatchCallback callback);
    
    public BigDecimal findPrice(JdbcTemplate jdbcTemplate, String query, String searchValue) {
    	return findPrice(jdbcTemplate, query, searchValue, new MatchCallback() {
			
    		public BigDecimal onMatch(SqlRowSet set) {
				return set.getBigDecimal("rate");
			}

            public Object onMatchObject(SqlRowSet set) {
                return null;
            }
        });
    }

    public Object findRoute(JdbcTemplate jdbcTemplate, String query, String searchValue,final String column) {
    	return findRoute(jdbcTemplate, query, searchValue, column, new MatchCallback() {

            public Object onMatchObject(SqlRowSet set) {
                return set.getObject(column);
            }

            public BigDecimal onMatch(SqlRowSet set) {
                return null;
            }
        });
    }
}
