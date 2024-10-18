package com.sapienter.jbilling.server.diameter;

import java.math.BigDecimal;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;

/**
 * A mechanism for determining the amount of units that fit a given balance. The
 * algorithm probes pricing in binary search fashion (from 1 to the requested
 * unit amount).
 */
public class BinarySearchUnits {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BinarySearchUnits.class));

	private DiameterPriceLocator delegate;
	private UserDTO user;
	
	private BigDecimal ratedUnits;
	private BigDecimal ratedUnitPrice;
	private BigDecimal ratedTotalPrice;
	
	public BinarySearchUnits(Integer entityId, UserDTO user, DiameterPriceLocator delegate) {
		this.delegate = delegate;
		this.user = user;
	}
	
	/**
	 * Calculates the amount of units that fit a given price.
	 * 
	 * @param itemId ID of the item to rate.
	 * @param units number of requested units. The number of units returned will be the
	 * closest number of units that the user's current balance allows.
	 * @param fieldHelper pricing fields to apply during pricing.
	 * @return the number of units calculated to fit the user's balance.
	 */
	public BigDecimal calculateUnits(Integer itemId, BigDecimal units, PricingFieldsHelper fieldHelper) throws PriceNotFoundException{
        LOG.debug("Parameters received: itemId - %s, units - %s", itemId, units);
        for(PricingField pricingField : fieldHelper.getFields()) {
            LOG.debug("PricingField: %s - Value: %s", pricingField.getName(), pricingField.getStrValue());
        }

		ratedUnits = BigDecimal.ZERO;
		ratedUnitPrice = BigDecimal.ZERO;
		ratedTotalPrice = BigDecimal.ZERO;

		BigDecimal available = user.getCustomer().getDynamicBalance().add(user.getCustomer().getCreditLimit());
		if (BigDecimal.ZERO.compareTo(units) == 0) {
			return BigDecimal.ZERO;
		}

        LOG.debug("Available balance: %s", available);
		
		int max = units.intValue();
		int min = 1;
		ratedUnits = units;

        LOG.debug("max: %s, min: %s, ratedUnits: %s", max, min, ratedUnits);

		while (max >= min) {
			ratedUnitPrice = delegate.rate(itemId, ratedUnits, fieldHelper);
            ratedTotalPrice = ratedUnitPrice.multiply(ratedUnits);

            LOG.debug("ratedUnitPrice: %s, ratedTotalPrice: %s", ratedUnitPrice, ratedTotalPrice);
			
			int comparison = ratedTotalPrice.compareTo(available);
			if (comparison <= 0) {
				min = ratedUnits.intValue() + 1;
			} else if (comparison > 0) {
		        max = ratedUnits.intValue() - 1;
			} else {
				break;
			}
			ratedUnits = BigDecimal.valueOf((max + min) / 2);

            LOG.debug("max: %s, min: %s, ratedUnits: %s", max, min, ratedUnits);
		}

        ratedTotalPrice = ratedUnitPrice.multiply(ratedUnits);
        LOG.debug("ratedTotalPrice: %s", ratedTotalPrice);

		return ratedUnits;
	}
	
	public BigDecimal getRatedUnits() {
		return ratedUnits;
	}
	
	public BigDecimal getRatedUnitPrice() {
		return ratedUnitPrice;
	}
	
	public BigDecimal getRatedTotalPrice() {
		return ratedTotalPrice;
	}
}
