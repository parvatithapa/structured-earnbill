package com.sapienter.jbilling.server.pricing.cache;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.math.BigDecimal;

/**
 * Callback that gets called when a match has been found on the rate card
 */
public interface MatchCallback {
	
	public static final MatchCallback DO_NOTHING = new MatchCallback() {
		
		public BigDecimal onMatch(SqlRowSet set) {
			return null;
		}

        public Object onMatchObject(SqlRowSet set) {
            return null;
        }
    };
	
	abstract public BigDecimal onMatch(SqlRowSet set);
	abstract public Object onMatchObject(SqlRowSet set);
}
