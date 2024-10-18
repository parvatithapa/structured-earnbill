package com.sapienter.jbilling.server.movius;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

public class MoviusOrderServiceImpl extends OrderServiceImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoviusOrderServiceImpl.class);

	@Override
	protected void processLines(OrderDTO order, Integer languageId,
			Integer entityId, Integer userId, Integer currencyId,
			String pricingFields, Integer itemId) {

		List<OrderLineDTO> lines = order.getLines()
				.stream()
				.filter(line -> line.getItemId().equals(itemId))
				.collect(Collectors.toList());

		LOGGER.debug("Collected Lines {} for item {}", lines, itemId);
		OrderBL orderBL = new OrderBL(order);
		for(OrderLineDTO line : lines) {
			orderBL.processLine(line, languageId, entityId, 
					userId, currencyId, pricingFields);
		}
	}


}
