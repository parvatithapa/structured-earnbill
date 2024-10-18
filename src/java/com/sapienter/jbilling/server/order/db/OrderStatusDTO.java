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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;


@Entity
@TableGenerator(name = "order_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "order_status",
        allocationSize = 100)
@Table(name = "order_status")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class OrderStatusDTO  extends AbstractDescription implements java.io.Serializable {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderStatusDTO.class));

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="order_status_GEN")
    @Column(name="id", unique=true, nullable=false)
    private int id;

    @Column(name="order_status_flag", unique=true, nullable=false)
    private OrderStatusFlag orderStatusFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private CompanyDTO entity;
    public CompanyDTO getEntity() {
        return entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    public String getStatusValue(){
        return getDescription(entity.getLanguageId());
    }

    public void setStatusValue(String text){
        setDescription(text);
    }


    public void setId(int id) {
        this.id = id;
    }


    public int getId() {
        return id;
    }


    public OrderStatusDTO() {

    }

    public OrderStatusFlag getOrderStatusFlag() {
        return orderStatusFlag;
    }

    public void setOrderStatusFlag(OrderStatusFlag orderStatusFlag) {
        this.orderStatusFlag = orderStatusFlag;
    }


    @Transient
    protected String getTable() {
        return Constants.TABLE_ORDER_STATUS;
    }

	
    /*@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="status_id")
    public Set<OrderDTO> getPurchaseOrders() {
        return this.orderDTOs;
    }*/
    
    /*public void setPurchaseOrders(Set<OrderDTO> orderDTOs) {
        this.orderDTOs = orderDTOs;
    }*/
}


