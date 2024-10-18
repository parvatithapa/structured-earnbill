package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;

import java.math.BigDecimal;
import java.util.List;

public class LinePercentagePricingStrategy extends AbstractPricingStrategy {

	public LinePercentagePricingStrategy() {
        setChainPositions(
                ChainPosition.START
        );
        
        setVariableUsagePricing(false);
    }
	
	/**
     * Sets the price to the plan rate.
     *  @param pricingOrder target order for this pricing request (not used by this strategy)
     * @param result pricing result to apply pricing to
     * @param fields pricing fields (not used by this strategy)
     * @param planPrice the plan price in the form of percentage to apply
     * @param quantity quantity of item being priced
     * @param usage for this strategy should be false
     * @param orderLineDTO
     */
	
	@Override
	public void applyTo(OrderDTO pricingOrder, PricingResult result,
                        List<PricingField> fields, PriceModelDTO planPrice,
                        BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {
		// TODO Auto-generated method stub
		
			result.setIsPercentage(true);
			result.setPrice(planPrice.getRate());
	}

}
