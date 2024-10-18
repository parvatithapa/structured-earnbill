package com.sapienter.jbilling.server.mediation.converter.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@TableGenerator(
        name = "error_usage_record_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "error_usage_record",
        allocationSize = 1
)
@Table(name="jm_error_usage_record")
public class JMErrorUsageRecordDao {

    private Integer id;
    private JbillingMediationErrorRecordDao errorRecord;
    private Integer itemId;
    private Integer userId;
    private String resourceId;
    private Date eventDate;
    private BigDecimal originalQuantity;
    private BigDecimal quantity;

    public JMErrorUsageRecordDao() {}

    public JMErrorUsageRecordDao(Integer itemId, Integer userId, String resourceId, Date eventDate,
                                 BigDecimal originalQuantity, BigDecimal quantity) {
        this.itemId = itemId;
        this.userId = userId;
        this.resourceId = resourceId;
        this.eventDate = eventDate;
        this.originalQuantity = originalQuantity;
        this.quantity = quantity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "error_usage_record_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @OneToOne
    @JoinColumns({
            @JoinColumn(name = "error_record_id", referencedColumnName = "id"),
            @JoinColumn(name = "entity_id", referencedColumnName = "jbilling_entity_id"),
            @JoinColumn(name = "mediation_cfg_id", referencedColumnName = "mediation_cfg_id"),
            @JoinColumn(name = "record_key", referencedColumnName = "record_key"),
    })
    public JbillingMediationErrorRecordDao getErrorRecord() {
        return errorRecord;
    }

    public void setErrorRecord(JbillingMediationErrorRecordDao jmErrorRecord) {
        this.errorRecord = jmErrorRecord;
    }

    @Column(name="item_id")
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @Column(name = "user_id")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Column(name = "resource_id")
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @Column(name = "event_date")
    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    @Column(name = "original_quantity")
    public BigDecimal getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(BigDecimal originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    @Column(name = "quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
