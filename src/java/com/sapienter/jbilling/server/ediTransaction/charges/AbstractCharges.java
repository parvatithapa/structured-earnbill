/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.ediTransaction.charges;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;

/**
 * Created by hitesh on 26/8/16.
 */
public abstract class AbstractCharges {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractCharges.class));

    public UserDTO userDTO;
    public OrderDTO orderDTO;
    public Integer companyId;
    public BigDecimal totalConsumption;
    public String internalNumber;
    public String customerType;

    /**
     * This method used checking the user is applicable for  this charges.
     *
     * @return boolean.
     */
    public abstract boolean isApplicableForCharges();

    /**
     * This method used for apply charges.
     *
     * @return Nothing.
     */
    public abstract void applyCharge();

    /**
     * This method used for doing the charge configuration.
     *
     * @param userDTO          used for find the user is applicable for charges or not.
     * @param orderDTO         used for set the charges line.
     * @param companyId        used for find the item in this company.
     * @param totalConsumption used for set the quantity in charges line.
     * @return Nothing.
     */
    public void doChargeConfiguration(UserDTO userDTO, OrderDTO orderDTO, Integer companyId, BigDecimal totalConsumption, String customerType) {
        this.userDTO = userDTO;
        this.orderDTO = orderDTO;
        this.companyId = companyId;
        this.totalConsumption = totalConsumption;
        this.customerType = customerType;
        validate();
    }

    /**
     * This method used for apply validation on configuration data.
     *
     * @return Nothing.
     */
    public void validate() {
        if (userDTO == null || orderDTO == null || companyId == null || totalConsumption == null) {
            LOG.error("Charges configuration object cannot be null");
            throw new SessionInternalError("Charges configuration object cannot be null");
        }
    }

    /**
     * This method used create the order line.
     *
     * @param order        used for set the charges line.
     * @param itemId       used for bind the item with line.
     * @param quantity     used for set the quantity in charges line.
     * @param price        used for set the price.
     * @param isPercentage true/false.
     * @return Nothing.
     */
    public void createOrderLine(OrderDTO order, Integer itemId, String description, BigDecimal quantity, BigDecimal price, Boolean isPercentage) {
        LOG.debug("create the order line for charges");
        OrderLineDTO line = new OrderLineDTO();
        line.setDescription(description);
        line.setItemId(itemId);
        line.setQuantity(quantity);
        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
        line.setPurchaseOrder(order);
        if (isPercentage) line.setPercentage(isPercentage);
        order.getLines().add(line);
        if (price != null) {
            line.setPrice(price);
        }
    }

    public abstract void addNotes(String orderNotes);

}
