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

package com.sapienter.jbilling.server.usagePool.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;

/**
 * CustomerUsagePoolDTO
 * The domain object representing the Customer Usage Pool association.
 * A customer can have one-to-many CustomerUsagePoolDTOs.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

@Entity
@TableGenerator(
        name = "customer_usage_pool_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "customer_usage_pool_map",
        allocationSize = 100
        )
@Table(name = "customer_usage_pool_map")
@NamedQueries({
    @NamedQuery(name = "CustomerUsagePoolDTO.findAllCustomerUsagePoolsByOrderId",
            query = "select cup from CustomerUsagePoolDTO cup where cup.order.id = :order_id"),
            @NamedQuery(name = "CustomerUsagePoolDTO.findAllCustomerUsagePoolsByCustomerId",
            query = "select cup from CustomerUsagePoolDTO cup where cup.customer.id = :customer_id")
})
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CustomerUsagePoolDTO implements Serializable {

    private int id;
    private CustomerDTO customer;
    private UsagePoolDTO usagePool;
    private PlanDTO plan;	// this is the plan that created this customer usage pool
    private BigDecimal quantity;
    private BigDecimal initialQuantity;
    private Date cycleEndDate;
    private int versionNum;
    private OrderDTO order;
    private Date cycleStartDate;
    private Date createDate = TimezoneHelper.serverCurrentDate();
    private BigDecimal lastRemainingQuantity;

    public CustomerUsagePoolDTO() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_usage_pool_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean hasId() {
        return getId() > 0;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_pool_id", nullable = false)
    public UsagePoolDTO getUsagePool() {
        return this.usagePool;
    }

    public void setUsagePool(UsagePoolDTO usagePool) {
        this.usagePool = usagePool;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    public PlanDTO getPlan() {
        return plan;
    }

    public void setPlan(PlanDTO plan) {
        this.plan = plan;
    }

    @Column(name="quantity", precision=17, scale=17)
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name="initial_quantity", precision=17, scale=17)
    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(BigDecimal initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    @Column(name="cycle_end_date")
    public Date getCycleEndDate() {
        return this.cycleEndDate;
    }

    public void setCycleEndDate(Date cycleEndDate) {
        this.cycleEndDate = cycleEndDate;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    public OrderDTO getOrder() {
        return order;
    }

    public void setOrder(OrderDTO order) {
        this.order = order;
    }

    @Column(name="cycle_start_date")
    public Date getCycleStartDate() {
        return cycleStartDate;
    }

    public void setCycleStartDate(Date cycleStartDate) {
        this.cycleStartDate = cycleStartDate;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Column(name = "create_date")
    private Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "last_remaining_quantity", precision = 17, scale = 17)
    public BigDecimal getLastRemainingQuantity() {
        return lastRemainingQuantity;
    }

    public void setLastRemainingQuantity(BigDecimal lastRemainingQuantity) {
        this.lastRemainingQuantity = lastRemainingQuantity;
    }

    @Transient
    public List<ItemDTO> getAllItems() {
        return this.getUsagePool().getAllItems();
    }

    @Transient
    public boolean isActive(Date activeSinceDate) {
        return (activeSinceDate.after(this.getCycleStartDate()) || activeSinceDate.equals(this.getCycleStartDate())) &&
                (activeSinceDate.before(this.getCycleEndDate()) || activeSinceDate.equals(this.getCycleEndDate()));
    }

    @Transient
    public boolean isExpired() {
        return this.getCycleEndDate().before(TimezoneHelper.serverCurrentDate());
    }

    @Override
    public String toString() {
        return "CustomerUsagePoolDTO={id=" + this.id +
                ",usagePool=" + this.usagePool +
                ",customer=" + this.customer.getId() +
                ",plan=" + this.plan.getId() +
                ",quantity=" + this.quantity +
                ",cycleEndDate=" + this.cycleEndDate +
                ",cycleStartDate=" + this.cycleStartDate +
                ",versionNum=" + this.versionNum +
                "}";
    }

    /**
     * A comparator that is used to sort customer usage pools based on precedence provided at system level usage pools.
     * If precedence at usage pool level is same, then created date for system level usage pools is considered.
     */
    @Transient
    public static final Comparator<CustomerUsagePoolDTO> CustomerUsagePoolsByPrecedenceOrCreatedDateComparator = new Comparator<CustomerUsagePoolDTO> () {
        @Override
        public int compare(CustomerUsagePoolDTO customerUsagePool1, CustomerUsagePoolDTO customerUsagePool2) {

            Integer precedence1 = customerUsagePool1.getUsagePool().getPrecedence();
            Integer precedence2 =  customerUsagePool2.getUsagePool().getPrecedence();
            if(precedence1.intValue() == precedence2.intValue()) {

                Date createDate1 = customerUsagePool1.getUsagePool().getCreatedDate();
                Date createDate2 =  customerUsagePool2.getUsagePool().getCreatedDate();

                return createDate1.compareTo(createDate2);
            }
            return precedence1.compareTo(precedence2);
        }
    };

    /**
     * A doesUsagePoolContainSkippedProduct method is used to check the Customer Usage Pool contains SkippedProduct or Not.
     * It returns boolean true when the customer usage pool contains SkippedProduct otherwise returns false.
     */
    @Transient
    public boolean doesUsagePoolContainSkippedProduct(Integer entityId) {
        boolean flag = false;
        for(ItemDTO item: getUsagePool().getItems()) {
            if(item.getInternalNumber().contains(PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_FREE_MINUTES_TOKEN_SKIP_PRODUCT_CODE))) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * Expires customer usage pool and set available
     * quantity zero and cycle start and end date as 1970-01-01.
     */
    @Transient
    public void expire() {
        Date epochDate = Util.getEpochDate();
        setCycleEndDate(epochDate);
        setCycleStartDate(epochDate);
        setQuantity(BigDecimal.ZERO);
    }
}
