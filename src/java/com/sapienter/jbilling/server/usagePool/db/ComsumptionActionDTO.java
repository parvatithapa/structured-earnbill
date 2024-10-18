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
package com.sapienter.jbilling.server.usagePool.db;

import java.io.Serializable;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
/**
 * ComsumptionActionDTO 
 * The domain object representing Free Usage Pool Consumption.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

@Entity
@TableGenerator(
        name = "usage_pool_consumption_action_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_pool_consumption_action",
        allocationSize = 100
)
@Table(name = "usage_pool_consumption_action")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ComsumptionActionDTO implements Serializable {
	
	private int id;
	private String actionName;
	private String actionDescription;
	
	public ComsumptionActionDTO() {
		super();
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_pool_consumption_action_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Column(name = "action_name")
	public String getActionName() {
		return actionName;
	}
	
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
	
	@Column(name = "action_description")
	public String getActionDescription() {
		return actionDescription;
	}
	
	public void setActionDescription(String actionDescription) {
		this.actionDescription = actionDescription;
	}
	
	@Override
	public String toString() {
		return "UsagePoolDTO={actionName=" + actionName + 
				",actionDescription=" + actionDescription + 
				"}";
	}
}

