package com.sapienter.jbilling.server.diameter.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

@Entity
@TableGenerator(
        name="reserved_amount_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="reserved_amount",
        allocationSize = 100
)
// No cache, mutable and critical
@Table(name="reserved_amounts")
public class ReservedAmountDTO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int id;
    private ChargeSessionDTO session;
    private Date created = TimezoneHelper.serverCurrentDate();
    private CurrencyDTO currency;
    private BigDecimal amount = BigDecimal.ZERO;
    private ItemDTO item;
    private BigDecimal quantity;
    private String data;
    
    public ReservedAmountDTO() {
    }
    
    public ReservedAmountDTO(ChargeSessionDTO session, CurrencyDTO currency, 
    		BigDecimal amount, BigDecimal quantity, ItemDTO item, PricingField[] pricingFields) {
    	this.session = session;
    	this.currency = currency;
    	this.amount = amount;
        this.quantity = quantity;
        this.item = item;
        this.data = PricingField.setPricingFieldsValue(pricingFields);
    }
    
    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "reserved_amount_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="session_id", nullable=false)
	public ChargeSessionDTO getSession() {
		return session;
	}
	
	public void setSession(ChargeSessionDTO session) {
		this.session = session;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="ts_created", nullable=false)
	public Date getCreated() {
		return created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    public CurrencyDTO getCurrency() {
        return this.currency;
    }

    public void setCurrency(CurrencyDTO currency) {
        this.currency = currency;
    }
	
	@Column(name="reserved_amount", nullable=false, precision=17, scale=17)
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_id")
    public ItemDTO getItem() {
        return this.item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    @Column(name="quantity", nullable=false)
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name="data", length=10000)
    public String getData () {
        return data;
    }

    public void setData (String data) {
        this.data = data;
    }
    
    @Transient
    public PricingField[] getDataAsFields() {
    	return PricingField.getPricingFieldsValue(data);
    }
    
    public void setDataAsFields(PricingField[] fields) {
    	this.data = PricingField.setPricingFieldsValue(fields);
    }
}
