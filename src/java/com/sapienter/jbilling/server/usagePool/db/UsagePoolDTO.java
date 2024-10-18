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

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.util.*;

/**
 * UsagePoolDTO 
 * The domain object representing Free Usage Pool.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

@Entity
@TableGenerator(
        name = "usage_pool_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_pool",
        allocationSize = 100
)
@Table(name = "usage_pool")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UsagePoolDTO extends AbstractDescription {
	
	public static final Integer DEFAULT_PRECEDENCE = -1;
	
	private int id;
	private BigDecimal quantity;
	private Integer precedence = DEFAULT_PRECEDENCE;
	private String cyclePeriodUnit;
	private Integer cyclePeriodValue;
	private Set<ItemTypeDTO> itemTypes = new HashSet<ItemTypeDTO>(0);
	private Set<ItemDTO> items = new HashSet<ItemDTO>(0);
	private UsagePoolResetValueEnum usagePoolResetValue;
	private CompanyDTO entity;
	private int versionNum;
	private Date createdDate;
	private SortedMap<String, String> attributes = new TreeMap<String, String>();
    public Set<UsagePoolConsumptionActionDTO> consumptionActions = new HashSet<UsagePoolConsumptionActionDTO>();

    public UsagePoolDTO() {
		super();
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_pool_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Column(name="quantity", precision=17, scale=17)
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	@Column(name = "precedence", nullable = false, length = 3)
    public Integer getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Integer precedence) {
        this.precedence = precedence;
    }
	
	@Column(name = "cycle_period_unit")
	public String getCyclePeriodUnit() {
		return cyclePeriodUnit;
	}
	
	public void setCyclePeriodUnit(String cyclePeriodUnit) {
		this.cyclePeriodUnit = cyclePeriodUnit;
	}
	
	@Column(name = "cycle_period_value")
	public Integer getCyclePeriodValue() {
		return cyclePeriodValue;
	}
	
	public void setCyclePeriodValue(Integer cyclePeriodValue) {
		this.cyclePeriodValue = cyclePeriodValue;
	}
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "usage_pool_item_type_map",
               joinColumns = {@JoinColumn(name = "usage_pool_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "item_type_id", updatable = false)}
    )
	public Set<ItemTypeDTO> getItemTypes() {
		return itemTypes;
	}
	
	public void setItemTypes(Set<ItemTypeDTO> itemTypes) {
		this.itemTypes = itemTypes;
	}
	
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "usage_pool_item_map",
               joinColumns = {@JoinColumn(name = "usage_pool_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "item_id", updatable = false)}
    )
	public Set<ItemDTO> getItems() {
		return items;
	}
	
	public void setItems(Set<ItemDTO> items) {
		this.items = items;
	}
	
	@Enumerated(EnumType.STRING)
    @Column(name = "reset_value", nullable = false, length = 25)
	public UsagePoolResetValueEnum getUsagePoolResetValue() {
		return usagePoolResetValue;
	}
	
	public void setUsagePoolResetValue(UsagePoolResetValueEnum usagePoolResetValue) {
		this.usagePoolResetValue = usagePoolResetValue;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
	public CompanyDTO getEntity() {
		return entity;
	}
	
	public void setEntity(CompanyDTO entity) {
		this.entity = entity;
	}
	
	@Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }
    
    @Column(name = "created_date")
	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	@Transient
    protected String getTable() {
        return Constants.TABLE_USAGE_POOL;
    }
	
	@Transient
    public Integer getEntityId() {
        return getEntity() != null ? getEntity().getId() : null;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="usage_pool_id")
    public Set<UsagePoolConsumptionActionDTO> getConsumptionActions() { return consumptionActions; }

    public void setConsumptionActions(Set<UsagePoolConsumptionActionDTO> consumptionActions) {
        this.consumptionActions = consumptionActions;
    }

    /**
	 * This is a convenience method that returns all items
	 * from a usage pool. It also goes through all categories on this 
	 * usage pool and adds up all items on each of those categories.
	 * @return
	 */
	@Transient
	public List<ItemDTO> getAllItems() {
		List<ItemDTO> allItemsOnUsagePool = new ArrayList<ItemDTO>();
		for (ItemTypeDTO itemType : this.getItemTypes()) {
			allItemsOnUsagePool.addAll(itemType.getItems());
		}
		allItemsOnUsagePool.addAll(this.getItems());
		return allItemsOnUsagePool;
	}
	
	@Override
	public String toString() {
		return "UsagePoolDTO={id=" + id +
			",quantity=" + quantity + 
			", precedence=" + precedence +
			",cyclePeriodUnit=" + cyclePeriodUnit +
			",cyclePeriodValue=" + cyclePeriodValue + 
			",itemTypes=" + itemTypes +
			",items=" + items + 
			",usagePoolResetValue=" + usagePoolResetValue +
			",entity=" + entity.getDescription() +  
			"}";
	}
	
	@Override
	 public void setDescription(String content, Integer languageId) {
	        setDescription("name", languageId, content);
	    }
	
	@Override
	public void deleteDescription(int languageId) {
	        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
	        JbillingTable table = tableDas.findByName(getTable());

	        InternationalDescriptionDAS descriptionDas = (InternationalDescriptionDAS) Context
	                .getBean(Context.Name.DESCRIPTION_DAS);
	        
	        descriptionDas.delete(table.getId(), getId(), "name", languageId);
	}
	
	/**
     * A comparator that is used to sort usage pools based on precedence.
     * If precedence at usage pool level is same, then created date is considered.
     */
    @Transient
    public static final Comparator<UsagePoolDTO> UsagePoolsByPrecedenceOrCreatedDateComparator = new Comparator<UsagePoolDTO> () {
        @Override
        public int compare(UsagePoolDTO usagePool1, UsagePoolDTO usagePool2) {

            Integer precedence1 = usagePool1.getPrecedence();
            Integer precedence2 =  usagePool2.getPrecedence();
            if(precedence1.intValue() == precedence2.intValue()) {

                Date createDate1 = usagePool1.getCreatedDate();
                Date createDate2 =  usagePool2.getCreatedDate();

                return createDate1.compareTo(createDate2);
            }
            return precedence1.compareTo(precedence2);
        }
    };
}
