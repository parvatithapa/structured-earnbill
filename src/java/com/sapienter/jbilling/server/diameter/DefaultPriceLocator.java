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

package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class DefaultPriceLocator implements DiameterPriceLocator {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DefaultPriceLocator.class));

	private Integer entityId;
	private Integer userId;
	
	public DefaultPriceLocator(Integer entityId, UserDTO user) {
		this.entityId = entityId;
		this.userId = user.getUserId();
	}

    @Override
    public BigDecimal rate(Integer itemId, BigDecimal units, PricingFieldsHelper params) throws PriceNotFoundException{
        List<PricingField> pricingFields = params.getFields();
    	ItemBL bl = new ItemBL(itemId);
    	bl.setPricingFields(pricingFields);
        BigDecimal price = bl.getPrice(userId, units, entityId);
        params.setFields(pricingFields);
        if(price == null){
            LOG.info("The price could not be determined.");
            throw new PriceNotFoundException("The price could not be determined.");
        }
        return price;
    }
}
