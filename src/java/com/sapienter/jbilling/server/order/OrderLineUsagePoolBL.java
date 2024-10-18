/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.order;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDAS;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;

/**
 * OrderLineUsagePoolBL
 * This has the find by id method for OrderLineUsagePoolDTO.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class OrderLineUsagePoolBL {
	
	OrderLineUsagePoolDAS olUsagePooldas = null;
	OrderLineUsagePoolDTO olUsagePool = null;

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderLineUsagePoolBL.class));


    public OrderLineUsagePoolBL() {
        init();
    }

    private void init() {
        this.olUsagePooldas = new OrderLineUsagePoolDAS();
        this.olUsagePool = new OrderLineUsagePoolDTO();
    }
    
    /**
     * find by id method for OrderLineUsagePoolDTO.
     * @param olUsagePoolId
     * @return OrderLineUsagePoolDTO
     */
    public OrderLineUsagePoolDTO find(Integer olUsagePoolId) {
        return olUsagePooldas.find(olUsagePoolId);
    }
    
    /**
     * find by id method for OrderLineUsagePoolDTO.
     * @param olUsagePoolId
     * @return OrderLineUsagePoolDTO
     */
    public List<OrderLineUsagePoolDTO> findByCustomerUsagePoolId(Integer customerUsagePoolId) {
        return olUsagePooldas.findByCustomerUsagePoolId(customerUsagePoolId);
    }
    
    /**
     * This method returns the list of OrderLine Usage Pool 
     * associations for the given user and item.
     * @param userId
     * @param itemId
     * @return List<OrderLineUsagePoolDTO>
     */
    public List<OrderLineUsagePoolDTO> findByUserItem(Integer userId, Integer itemId, Date startDate) {
    	return olUsagePooldas.findByUserItem(userId, itemId, startDate);
    }
    
    /**
     * This method returns the total free usage quantity for the given user and item.
     * @param userId
     * @param itemId
     * @return BigDecimal
     */
    public BigDecimal getFreeUsageQuantityByUserItem(Integer userId, Integer itemId, Date startDate) {
    	BigDecimal freeUsageQuantity = BigDecimal.ZERO;
    	for (OrderLineUsagePoolDTO olPool : findByUserItem(userId, itemId, startDate)) {
    		freeUsageQuantity = freeUsageQuantity.add(olPool.getQuantity());
    	}
    	return freeUsageQuantity;
    }
}
