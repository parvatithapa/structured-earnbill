package com.sapienter.jbilling.server.mediation.movius.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.timezone.TimezoneHelper;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name            = "org_count_position_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "org_count_position"
)
@Table(name = "org_count_position")
public class OrgCountPositionDTO implements Serializable {

    private int id;
    private String orgId;  
    private String billableOrgId;
    private Date lastUpdatedDate;
    private BigDecimal count;
    private BigDecimal oldCount;
    private int deleted;
    private int version;
    private int orderId;
    private int itemId;
    private int entityId;
    
    
    public OrgCountPositionDTO() {
        //Default constructor 
    }

    public OrgCountPositionDTO(String orgId, String billableOrgId) {
        this.orgId = orgId;
        this.billableOrgId = billableOrgId;
        this.lastUpdatedDate = TimezoneHelper.serverCurrentDate();
    }

    public OrgCountPositionDTO(String orgId, String billableOrgId, BigDecimal count, BigDecimal oldCount, int orderId, int itemId, int entityId) {
        this.orgId = orgId;
        this.billableOrgId = billableOrgId;
        this.count = count;
        this.oldCount = oldCount;
        this.orderId = orderId;
        this.lastUpdatedDate = TimezoneHelper.serverCurrentDate();
        this.itemId = itemId;
        this.entityId = entityId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "org_count_position_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "org_id", unique = false, nullable = false)
    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    @Column(name = "billable_org_id", unique = false, nullable = false)
    public String getBillableOrgId() {
        return billableOrgId;
    }

    public void setBillableOrgId(String billableOrgId) {
        this.billableOrgId = billableOrgId;
    }

    @Column(name = "last_updated_date", unique = false, nullable = false)
    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Column(name = "old_count", unique = false, nullable = true)
    public BigDecimal getOldCount() {
        return oldCount;
    }

    public void setOldCount(BigDecimal oldCount) {
        this.oldCount = oldCount;
    }

    @Column(name = "deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
    @Column(name = "order_id", unique = false, nullable = false)
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    @Column(name = "count", unique = false, nullable = true)
    public BigDecimal getCount() {
        return count;
    }

    public void setCount(BigDecimal count) {
        this.count = count;
    }

    @Column(name = "item_id", unique = false, nullable = false)
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    @Column(name = "entity_id", unique = false, nullable = false)
    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return "OrgCountPositionDTO [id=" + id + ", orgId=" + orgId
                + ", billableOrgId=" + billableOrgId + ", lastUpdatedDate="
                + lastUpdatedDate + ", count=" + count + ", oldCount="
                + oldCount + ", orderId=" + orderId + ", itemId=" + itemId
                + "]";
    }


}
