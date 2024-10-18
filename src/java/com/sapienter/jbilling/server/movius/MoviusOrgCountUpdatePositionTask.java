package com.sapienter.jbilling.server.movius;

import java.math.BigDecimal;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDAS;
import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

/**
 * UpdateMoviusOrgCountPositionTask
 * This task is movius specific.
 * This is an internal events task that updates count position of the order 
 * when createUpdateOrderChange API will trigger.
 * @author Manish Bansod
 * @since 01-04-2018
 */
public class MoviusOrgCountUpdatePositionTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger LOG = LoggerFactory.getLogger(MoviusOrgCountUpdatePositionTask.class);
	
	@SuppressWarnings("unchecked")
	private static final Class<Event> EVENTS[] = new Class[]{
		OrderChangeUpdatePositionEvent.class
	};
	
	@Override
	public Class<Event>[] getSubscribedEvents() {
		return EVENTS;
	}
	
	@Override
	public void process(Event event) throws PluggableTaskException {
		LOG.debug("Entering Order Change Update Position Event: {}", event);
		
		OrderChangeUpdatePositionEvent updateMoviusOrgCountPositionEvent = (OrderChangeUpdatePositionEvent) event;
		OrderDTO orderDTO = updateMoviusOrgCountPositionEvent.getOrderDTO();
		OrderLineDTO existingOrderLine = updateMoviusOrgCountPositionEvent.getExistingOrderLine();
		Integer entityId = updateMoviusOrgCountPositionEvent.getEntityId();
		BigDecimal newQuantity = updateMoviusOrgCountPositionEvent.getNewQuantity();
		
		setCountPosition(entityId, newQuantity, orderDTO, existingOrderLine);
	}
	
	public void setCountPosition(Integer entityId, BigDecimal newQuantity,
			OrderDTO orderDTO, OrderLineDTO existingOrderLine) {
		OrgCountPositionDAS orgCountPositionDAS = new OrgCountPositionDAS();
		String orgId = findOrgIdbyUserId(orderDTO.getUserId());
		OrgCountPositionDTO positionDTO = orgCountPositionDAS.findByOrgIdOrderIdAndItemId(orgId,
				orderDTO.getId(), existingOrderLine.getItemId(), entityId);
		if(Objects.nonNull(positionDTO) && (newQuantity.compareTo(positionDTO.getCount()) != 0)) {
		    updateOrgCountPositionRecord(positionDTO, newQuantity, positionDTO.getCount());
		}
	}
	
	public static String findOrgIdbyUserId(Integer userId) {
        UserBL bl = new UserBL(userId);
        CustomerDTO customer = bl.getEntity().getCustomer();
        MetaFieldValue<?> orgId = customer.getMetaField(MoviusConstants.ORG_ID);
        return Objects.nonNull(orgId) ? (String) orgId.getValue() : null;
    }
	
	private static Integer updateOrgCountPositionRecord(OrgCountPositionDTO record, BigDecimal count, BigDecimal oldCount) {
	    OrgCountPositionDAS das = new OrgCountPositionDAS();
	    record.setCount(count);
	    record.setOldCount(oldCount);
	    record.setLastUpdatedDate(TimezoneHelper.serverCurrentDate());
	    Integer recordId =  das.save(record).getId();
	    LOG.debug("Updated Org Count Record %s for order %d with Quantity %s", record, record.getOrderId(), count);
	    return recordId;
	}
}
