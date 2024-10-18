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
package com.sapienter.jbilling.server.order.db;

import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.order.OrderSQL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;


/**
 * @author
 */
public class OrderStatusBL extends ResultList
implements OrderSQL {

	private OrderStatusDTO orderStatus;
	private OrderStatusDAS orderStatusDas;
	public OrderStatusBL(Integer orderStatus) {
		init();
		set(orderStatus);
	}

	public OrderStatusBL() {
		init();
	}

	public void set(Integer id) {
		orderStatus = orderStatusDas.find(id);
	}

	public void init() {
		//orderStatus=new OrderStatusDTO();
		orderStatusDas = new OrderStatusDAS();
	}


	public OrderStatusDTO getEntity() {
		return orderStatus;
	}

	public void delete(Integer entityId) {
		OrderStatusDAS orderStatusDas = new OrderStatusDAS();
		try {
			Integer count = orderStatusDas.findByOrderStatusFlag(orderStatus.getOrderStatusFlag(), entityId);
			if (count <= 1) {
				throw new SessionInternalError("There needs to be atleast one status of this type ",
						new String[]{"OrderStatusWS,statusExist,validation.error.status.should.exists"});
			} else if (count > 1) {
				orderStatusDas.delete(orderStatus);
			}
		} catch (Exception e) {
			throw new SessionInternalError(e);
		}
	}

	public static final OrderStatusWS getOrderStatusWS(OrderStatusDTO orderStatus) {
		if(null==orderStatus)return null;
		OrderStatusWS statusWS = new OrderStatusWS();
		statusWS.setId(orderStatus.getId());
		statusWS.setEntity(EntityBL.getCompanyWS(orderStatus.getEntity()));
		statusWS.setOrderStatusFlag(orderStatus.getOrderStatusFlag());
		statusWS.setDescription(orderStatus.getDescription(orderStatus.getEntity().getLanguageId()));
		return statusWS;
	}


	public static OrderStatusDTO getDTO(OrderStatusWS orderStatusWS) {
		OrderStatusDTO orderStatusDTO = new OrderStatusDTO();
		if (orderStatusWS.getId() != null) {
			orderStatusDTO.setId(orderStatusWS.getId());
		}
		orderStatusDTO.setOrderStatusFlag(orderStatusWS.getOrderStatusFlag());
		orderStatusDTO.setDescription(orderStatusWS.getDescription());
		if (orderStatusWS.getEntity() != null) {
			orderStatusDTO.setEntity(new CompanyDAS().find(orderStatusWS.getEntity().getId()));
		}
		return orderStatusDTO;
	}


	public static OrderStatusDTO getDTOWithTargetEntity(OrderStatusWS orderStatusWS) {
		OrderStatusDTO orderStatusDTO = new OrderStatusDTO();
		if (orderStatusWS.getId() != null)
			orderStatusDTO.setId(orderStatusWS.getId());
		orderStatusDTO.setOrderStatusFlag(orderStatusWS.getOrderStatusFlag());
		orderStatusDTO.setDescription(orderStatusWS.getDescription());
		orderStatusDTO.setEntity(new CompanyDAS().find(orderStatusWS.getEntity().getId()));
		return orderStatusDTO;
	}

	public Integer create(OrderStatusWS orderStatusWS, Integer entityId, Integer languageId) throws SessionInternalError

	{
		OrderStatusDTO newOrderStatus = getDTO(orderStatusWS);
		newOrderStatus.setEntity(new CompanyDAS().find(entityId));
		newOrderStatus = new OrderStatusDAS().createOrderStatus(newOrderStatus);
		newOrderStatus.setDescription(orderStatusWS.getDescription(), languageId);
		return newOrderStatus.getId();
	}


	public boolean isOrderStatusValid(OrderStatusWS orderStatusWS, Integer entityId, String name) {

		List<OrderStatusDTO> orderStatusDTOList = new OrderStatusDAS().findAll(entityId);
		List<String> descriptionList = new ArrayList<String>();
		for (OrderStatusDTO orderStatusDTO : orderStatusDTOList) {
			if (orderStatusWS.getOrderStatusFlag() == OrderStatusFlag.FINISHED || orderStatusWS.getOrderStatusFlag() == OrderStatusFlag.SUSPENDED_AGEING) {
				//save
				if (orderStatusWS.getId() == null && orderStatusDTO.getOrderStatusFlag() == orderStatusWS.getOrderStatusFlag()) {
					return false;
				}
				//update
				else if (orderStatusWS.getId() != null && orderStatusDTO.getOrderStatusFlag() == orderStatusWS.getOrderStatusFlag() && orderStatusDTO.getId() != orderStatusWS.getId()) {
					return false;
				}
			}
			if (orderStatusWS.getId() != null) {
				if (orderStatusWS.getId() != orderStatusDTO.getId()) {
					descriptionList.add(orderStatusDTO.getDescription());
				}

			} else {
				descriptionList.add(orderStatusDTO.getDescription());
			}
		}
		if (descriptionList.contains(name)) {
			String[] errmsgs = new String[1];
			errmsgs[0] = "OrderStatusWS,description,OrderStatusWS.error.unique.name";
			throw new SessionInternalError("There is an error in  data.", errmsgs);
		} else {
			return true;
		}

	}
}