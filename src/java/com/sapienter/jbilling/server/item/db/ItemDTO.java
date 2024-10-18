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
package com.sapienter.jbilling.server.item.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.audit.Auditable;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.AbstractDescription;



@Entity
@TableGenerator(
        name = "item_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "item",
        allocationSize = 1
        )
@Table(name = "item")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ItemDTO extends AbstractDescription implements MetaContent, Exportable, Auditable {

    private int id;
    private CompanyDTO entity;
    private Set<CompanyDTO> entities = new HashSet<>(0);
    private String internalNumber;
    private String glCode;
    private Set<EntityItemPrice> defaultPrices = new HashSet<>();
    private BigDecimal percentage;
    private Set<ItemTypeDTO> excludedTypes = new HashSet<>();
    private Integer deleted;
    private Integer hasDecimals;
    private Set<ItemTypeDTO> itemTypes = new HashSet<>(0);
    private Set<PlanDTO> plans = new HashSet<>(0);
    private List<MetaFieldValue> metaFields = new LinkedList<>();
    private Set<MetaField> orderLineMetaFields = new HashSet<>();

    private boolean standardAvailability = true;
    private boolean global = false;
    private List<AccountTypeDTO> accountTypeAvailability = new ArrayList<>();

    /** If the item will do asset management. Only possible if one linked ItemTypeDTO allows asset management */
    private Integer assetManagementEnabled;
    private int versionNum;

    private Set<ItemDependencyDTO> dependencies = new HashSet<>();
    private Set<UsagePoolDTO> itemUsagePools = new HashSet<>();

    // transient
    private Integer[] types = null;
    private Integer[] excludedTypeIds = null;
    private Integer[] accountTypeIds = null;
    private Integer[] dependencyIds = null;


    private List<Integer> childEntityIds = null;
    private List<Integer> parentAndChildIds = null;

    private Collection<String> strTypes = null; // for rules 'contains' operator
    private String promoCode = null;
    private Integer currencyId = null;
    private BigDecimal price = null;
    private Integer orderLineTypeId = null;

    private Integer priceModelCompanyId = null;

    private Date activeSince;
    private Date activeUntil;

    private BigDecimal standardPartnerPercentage;
    private BigDecimal masterPartnerPercentage;
    private boolean isPercentage;
    private Integer reservationDuration;

    private boolean isPlan=false ;
    private SortedMap<Date, RatingConfigurationDTO> ratingConfigurations = new TreeMap<Date, RatingConfigurationDTO>();

    public ItemDTO() {
    }

    public ItemDTO(int id) {
        this.id = id;
    }

    public ItemDTO(int id, String internalNumber, String glCode,BigDecimal percentage,
            Integer hasDecimals, Integer deleted, CompanyDTO entity, Integer assetManagementEnabled) {
        this.id = id;
        this.internalNumber = internalNumber;
        this.glCode = glCode;
        this.percentage = percentage;
        this.hasDecimals = hasDecimals;
        this.deleted = deleted;
        this.entity = entity;
        this.assetManagementEnabled = assetManagementEnabled;
    }

    public ItemDTO(int id, Integer deleted, Integer hasDecimals, Integer assetManagementEnabled) {
        this.id = id;
        this.deleted = deleted;
        this.hasDecimals = hasDecimals;
        this.assetManagementEnabled = assetManagementEnabled;
    }

    // ItemDTOEx
    public ItemDTO(int id, String number, String glCode, CompanyDTO entity, String description, Integer deleted,
            Integer currencyId, BigDecimal price, BigDecimal percentage, Integer orderLineTypeId,
            Integer hasDecimals, Integer assetManagementEnabled , boolean isPercentage) {

        this(id, number, glCode, percentage, hasDecimals, deleted, entity, assetManagementEnabled);
        setDescription(description);
        setCurrencyId(currencyId);
        setOrderLineTypeId(orderLineTypeId);
        setIsPercentage(isPercentage);
        setPrice(price);
    }

    @Override
    @Transient
    protected String getTable() {
        return Constants.TABLE_ITEM;
    }

    @Override
    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "item_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "item_entity_map", joinColumns = {
            @JoinColumn(name = "item_id", updatable = true) }, inverseJoinColumns = {
            @JoinColumn(name = "entity_id", updatable = true) })
    public Set<CompanyDTO> getEntities() {
        return this.entities;
    }

    public void setEntities(Set<CompanyDTO> entities) {
        this.entities = entities;
    }

    @Transient
    public List<Integer> getChildEntitiesIds() {
        if (this.childEntityIds == null) {
            this.childEntityIds = new ArrayList<Integer>();
            for(CompanyDTO dto : this.entities) {
                this.childEntityIds.add(dto.getId());
            }

        }
        return this.childEntityIds;
    }

    @Column(name = "asset_management_enabled")
    public Integer getAssetManagementEnabled() {
        return assetManagementEnabled;
    }

    public void setAssetManagementEnabled(Integer assetManagementEnabled) {
        this.assetManagementEnabled = assetManagementEnabled;
    }

    @Column(name = "internal_number", length = 50)
    public String getInternalNumber() {
        return this.internalNumber;
    }

    public void setInternalNumber(String internalNumber) {
        this.internalNumber = internalNumber;
    }

    @Column (name = "gl_code", length = 50)
    public String getGlCode() {
        return glCode;
    }

    public void setGlCode(String glCode) {
        this.glCode = glCode;
    }


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "item", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<EntityItemPrice> getDefaultPrices() {
        return defaultPrices;
    }

    public void setDefaultPrices(Set<EntityItemPrice> defaultPrices) {
        this.defaultPrices = defaultPrices;
    }

    @Transient
    public void setDefaultPricesByCompany(SortedMap<Date, PriceModelDTO> defaultPrices, CompanyDTO entity) {
        boolean updated = false;
        if(this.defaultPrices != null && this.defaultPrices.size() > 0) {
            for(EntityItemPrice price : this.defaultPrices) {
                if(entity == null) {
                    // prices are global, see if there already exist global price
                    if(price.getEntity() == null) {
                        price.setPrices(defaultPrices);
                        updated = true;
                    }
                } else {
                    //price is entity specfic, see if there exists price for that company
                    if(price.getEntity() != null && price.getEntity().getId() == entity.getId()) {
                        price.setPrices(defaultPrices);
                        updated = true;
                    }
                }
            }
        }

        // if prices have not been updated then create new entry
        if(!updated) {
            EntityItemPrice itemPrice = new EntityItemPrice(this, entity, defaultPrices);
            this.defaultPrices.add(itemPrice);
        }
    }

    /**
     * Removes price for a given company. If company is null then global price is removed
     *
     * @param entity	:	CompanyDTO
     */
    @Transient
    public void removeDefaultPricesByCompany(CompanyDTO entity) {
        EntityItemPrice removed = null;
        if(this.defaultPrices != null && this.defaultPrices.size() > 0) {
            for(EntityItemPrice price : this.defaultPrices) {
                if(entity == null) {
                    // prices are global, see if there already exist global price
                    if(price.getEntity() == null) {
                        removed = price;
                    }
                } else {
                    //price is entity specfic, see if there exists price for that company
                    if(price.getEntity() != null && price.getEntity().getId() == entity.getId()) {
                        removed = price;
                    }
                }
            }
        }

        if(removed != null) {
            this.defaultPrices.remove(removed);
        }
    }
    /**
     * This method looks up default prices by company id and if prices by company id are not found then
     * return null
     *
     * @param entity :	company id
     * @return	:	date, price model map if price is found
     */
    @Transient
    public SortedMap<Date, PriceModelDTO> getDefaultPricesByCompany (Integer entity) {
        for (EntityItemPrice price : this.defaultPrices) {
            if (price != null && entity != null) {
                if (price.getEntity() != null && price.getEntity().getId() == entity) {
                    return price.getPrices();
                }
            }
        }
        return null;
    }

    /**
     * Get global price of item if global price is not found then returns null
     *
     * @return	:	SortedMap<Date, PriceModelDTO>
     */
    @Transient
    public SortedMap<Date, PriceModelDTO> getGlobalDefaultPrices() {
        for(EntityItemPrice price : this.defaultPrices) {
            if(price.getEntity() == null) {
                return price.getPrices();
            }
        }
        return null;
    }

    /**
     * Return prices sorted by date, of the requested entity.
     * If the requested entityId is null, return the available global prices
     * sorted by date.
     *
     * @param entityId
     * @return
     */
    @Transient
    public SortedMap<Date, PriceModelWS> getPricesForSelectedEntity(Integer entityId) {

        SortedMap<Date, PriceModelDTO> prices;
        if(entityId != null) {
            prices = this.getDefaultPricesByCompany(entityId);
        } else {
            prices = this.getGlobalDefaultPrices();
        }

        return PriceModelBL.getWS(prices);
    }

    /**
     * Adds a new price to the default pricing list. If no date is given, then the
     * price it is assumed to be the start of a new time-line and the date will be
     * forced to 01-Jan-1970 (epoch).
     *
     * @param date date for the given price
     * @param price price
     */
    public void addDefaultPrice(Date date, PriceModelDTO price, Integer entityId) {
        Iterator<EntityItemPrice> iterator = getDefaultPrices().iterator();
        while(iterator.hasNext()) {
            EntityItemPrice itemPrice = iterator.next();

            if(entityId == null) {
                if(itemPrice.getEntity() == null) {
                    itemPrice.getPrices().put(date != null ? date : CommonConstants.EPOCH_DATE, price);
                }
            } else {
                if(itemPrice.getEntity() != null && itemPrice.getEntity().getId() == entityId) {
                    itemPrice.getPrices().put(date != null ? date : CommonConstants.EPOCH_DATE, price);
                }
            }
        }
    }

    @Transient
    public PriceModelDTO getPrice(Date today, Integer entityId) {
        EntityItemPrice global = null;
        EntityItemPrice specific = null;

        for (EntityItemPrice itemPrice : getDefaultPrices()) {
            if (itemPrice.getEntity() == null || entityId == null) {
                global = itemPrice;
            } else {
                if (itemPrice.getEntity() != null && itemPrice.getEntity().getId() == entityId.intValue()) {
                    specific = itemPrice;
                }
            }
        }

        if (specific != null) {
            return PriceModelBL.getPriceForDate(specific.getPrices(), today);
        } else if (global != null) {
            return PriceModelBL.getPriceForDate(global.getPrices(), today);
        } else {
            return PriceModelBL.getDTO(new PriceModelWS(PriceModelStrategy.ZERO.name(), BigDecimal.ZERO,
                    getCurrencyId() != null ? getCurrencyId() : getEntity().getCurrencyId()));
        }
    }

    @Transient
    public PriceModelDTO getPrice(Date today) {
        Iterator<EntityItemPrice> iterator = getDefaultPrices().iterator();
        EntityItemPrice global = null;
        EntityItemPrice specific = null;

        while (iterator.hasNext()) {
            EntityItemPrice itemPrice = iterator.next();
            if (itemPrice.getEntity() == null) {
                global = itemPrice;
            } else if (specific == null) {
                specific = itemPrice;
            }
        }

        if (global != null) {
            return PriceModelBL.getPriceForDate(global.getPrices(), today);
        } else if (specific != null) {
            return PriceModelBL.getPriceForDate(specific.getPrices(), today);
        } else {
            return null;
        }
    }

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinTable(name="item_rating_configuration_map",joinColumns = {@JoinColumn(name = "item_id")},inverseJoinColumns={@JoinColumn(name="rating_configuration_id")})
    @MapKeyColumn(name="start_date", nullable = false)
    @Fetch(FetchMode.SELECT)
    @SortNatural
    public SortedMap<Date,RatingConfigurationDTO> getRatingConfigurations(){return ratingConfigurations;}

    @Transient
    public void setRatingConfigurations(SortedMap<Date, RatingConfigurationDTO> ratingConfigurations) {
        this.ratingConfigurations = ratingConfigurations;
    }

    @Transient
    public BigDecimal getPercentage() {
        return this.percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "item_type_exclude_map",
    joinColumns = {@JoinColumn(name = "item_id", updatable = false)},
    inverseJoinColumns = {@JoinColumn(name = "type_id", updatable = false)}
            )
    public Set<ItemTypeDTO> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(Set<ItemTypeDTO> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    @Column(name = "deleted", nullable = false)
    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Column(name = "has_decimals", nullable = false)
    public Integer getHasDecimals() {
        return this.hasDecimals;
    }

    public void setHasDecimals(Integer hasDecimals) {
        this.hasDecimals = hasDecimals;
    }

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "item_type_map",
    joinColumns = {@JoinColumn(name = "item_id", updatable = false)},
    inverseJoinColumns = {@JoinColumn(name = "type_id", updatable = false)}
            )
    public Set<ItemTypeDTO> getItemTypes() {
        return this.itemTypes;
    }

    public void setItemTypes(Set<ItemTypeDTO> itemTypes) {
        this.itemTypes = itemTypes;
    }

    /**
     * Strips the given prefix off of item categories and returns the resulting code. This method allows categories to
     * be used to hold identifiers and other meta-data.
     * <p/>
     * Example: item = ItemDTO{ type : ["JB_123"] } item.getCategoryCode("JB") -> "123"
     *
     * @param prefix prefix of the category code to retrieve
     * @return code minus the given prefix
     */
    public String getCategoryCode(String prefix) {
        for (ItemTypeDTO type : getItemTypes()) {
            if (type.getDescription().startsWith(prefix)) {
                return type.getDescription().replaceAll(prefix, "");
            }
        }
        return null;
    }

    /**
     * List of all plans that use this item as the "plan subscription" item.
     *
     * @return plans
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "item")
    public Set<PlanDTO> getPlans() {
        return plans;
    }

    public void setPlans(Set<PlanDTO> plans) {
        this.plans = plans;
    }

    @Transient
    public boolean hasPlans() {
        return this.plans != null && !this.plans.isEmpty();
    }

    /**
     * This convenience method checks if the given item is a plan subscription item
     * and that if the plan has bundle items with bundle quantity > 0.
     * @return
     */
    @Transient
    public boolean hasPlanItems() {

        boolean hasPlanItems = false;
        if (hasPlans()) {
            for (PlanDTO plan : getPlans()) {
                for (PlanItemDTO planItem : plan.getPlanItems()) {
                    if (planItem.hasBundle() && planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        hasPlanItems = true;
                        break;
                    }
                }
                if (hasPlanItems) {
                    break;
                }
            }
        }
        return hasPlanItems;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "item_meta_field_map",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
            )
    @SortComparator(value = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return metaFields;
    }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "order_line_meta_fields_map",
    joinColumns = { @JoinColumn(name = "item_id", referencedColumnName="id") },
    inverseJoinColumns = { @JoinColumn(name = "meta_field_id", referencedColumnName="id", unique = true)}
            )
    @OrderBy("displayOrder")
    public Set<MetaField> getOrderLineMetaFields() {
        return orderLineMetaFields;
    }

    public void setOrderLineMetaFields(Set<MetaField> orderLineMetaFields) {
        this.orderLineMetaFields = orderLineMetaFields;
    }

    @Column(name = "standard_availability", nullable = false)
    public boolean isStandardAvailability() {
        return standardAvailability;
    }

    public void setStandardAvailability(boolean standardAvailability) {
        this.standardAvailability = standardAvailability;
    }

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "item_account_type_availability",
    joinColumns = {@JoinColumn(name = "item_id", updatable = false)},
    inverseJoinColumns = {@JoinColumn(name = "account_type_id", updatable = false)}
            )
    public List<AccountTypeDTO> getAccountTypeAvailability() {
        return accountTypeAvailability;
    }

    @Column(name = "global", nullable = false, updatable = true)
    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public void setAccountTypeAvailability(List<AccountTypeDTO> accountTypeAvailability) {
        this.accountTypeAvailability = accountTypeAvailability;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "item", orphanRemoval = true)
    public Set<ItemDependencyDTO> getDependencies() {
        return dependencies;
    }

    @Transient
    public List<ItemDTO> getDependItems() {
        List<ItemDTO> dependItems = new ArrayList<ItemDTO>();
        for(ItemDependencyDTO itemDependencyDTO : dependencies) {
            dependItems.add((ItemDTO) itemDependencyDTO.getDependent());
        }
        return dependItems;
    }

    @Column(name = "standard_partner_percentage")
    public BigDecimal getStandardPartnerPercentage () {
        return standardPartnerPercentage;
    }

    public void setStandardPartnerPercentage (BigDecimal standardPartnerPercentage) {
        this.standardPartnerPercentage = standardPartnerPercentage;
    }

    @Column(name = "master_partner_percentage")
    public BigDecimal getMasterPartnerPercentage () {
        return masterPartnerPercentage;
    }

    public void setMasterPartnerPercentage (BigDecimal masterPartnerPercentage) {
        this.masterPartnerPercentage = masterPartnerPercentage;
    }

    @Column(name = "reservation_duration")
    public Integer getReservationDuration() {
        return reservationDuration;
    }

    public void setReservationDuration(Integer reservationDuration) {
        this.reservationDuration = reservationDuration;
    }

    public void setDependencies(Set<ItemDependencyDTO> dependencies) {
        this.dependencies = dependencies;
        dependencyIds = null;
    }

    public void addDependency(ItemDependencyDTO dependencyDTO) {
        dependencies.add(dependencyDTO);
        dependencyDTO.setItem(this);
    }

    @Override
    public void setMetaFields(List<MetaFieldValue> fields) {
        this.metaFields = fields;
    }

    @Override
    @Transient
    public MetaFieldValue getMetaField(String name) {
        return MetaFieldHelper.getMetaField(this, name);
    }

    @Override
    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(this, name, groupId);
    }

    @Override
    @Transient
    public MetaFieldValue getMetaField(Integer metaFieldNameId) {
        return MetaFieldHelper.getMetaField(this, metaFieldNameId);
    }

    @Override
    @Transient
    public void setMetaField(MetaFieldValue field, Integer groupId) {
        MetaFieldHelper.setMetaField(this, field, groupId);
    }

    @Override
    @Transient
    public void setMetaField(Integer entitId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entitId, groupId, this, name, value);
    }

    @Override
    @Transient
    public void updateMetaFieldsWithValidation(Integer languageId, Integer entitId, Integer accountTypeId, MetaContent dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(languageId, entitId, accountTypeId, this, dto);
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.PRODUCT };
    }

    @Transient
    public String getNumber() {
        return getInternalNumber();
    }

    @Transient
    public void setNumber(String number) {
        setInternalNumber(number);
    }

    /*
        Transient fields
     */

    @Transient
    public Integer[] getTypes() {
        if (this.types == null && itemTypes != null) {
            Integer[] types = new Integer[itemTypes.size()];
            int i = 0;
            for (ItemTypeDTO type : itemTypes) {
                types[i++] = type.getId();
            }
            setTypes(types);
        }
        return types;
    }

    @Transient
    public void setTypes(Integer[] types) {
        this.types = types;

        strTypes = new ArrayList<String>(types.length);
        for (Integer i : types) {
            strTypes.add(i.toString());
        }
    }

    public boolean hasType(Integer typeId) {
        return Arrays.asList(getTypes()).contains(typeId);
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "usage_pool_item_map",
    joinColumns = {@JoinColumn(name = "item_id", updatable = false)},
    inverseJoinColumns = {@JoinColumn(name = "usage_pool_id", updatable = false)}
            )
    public Set<UsagePoolDTO> getItemUsagePools() {
        return this.itemUsagePools;
    }

    public void setItemUsagePools(Set<UsagePoolDTO> itemUsagePools) {
        this.itemUsagePools = itemUsagePools;
    }

    @Temporal(TemporalType.DATE)
    @Column(name="active_since", length=13)
    public Date getActiveSince() {
        return this.activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    @Temporal(TemporalType.DATE)
    @Column(name="active_until", length=13)
    public Date getActiveUntil() {
        return this.activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    @Transient
    public Integer[] getExcludedTypeIds() {
        if (this.excludedTypeIds == null && excludedTypes != null) {
            Integer[] types = new Integer[excludedTypes.size()];
            int i = 0;
            for (ItemTypeDTO type : excludedTypes) {
                types[i++] = type.getId();
            }
            setExcludedTypeIds(types);
        }
        return excludedTypeIds;
    }

    @Transient
    public void setExcludedTypeIds(Integer[] types) {
        this.excludedTypeIds = types;
    }

    public boolean hasExcludedType(Integer typeId) {
        return Arrays.asList(getExcludedTypeIds()).contains(typeId);
    }

    @Transient
    public Integer[] getAccountTypeIds() {
        if (this.accountTypeIds == null && accountTypeAvailability != null) {
            Integer[] types = new Integer[accountTypeAvailability.size()];
            int i = 0;
            for (AccountTypeDTO type : accountTypeAvailability) {
                types[i++] = type.getId();
            }
            setAccountTypeIds(types);
        }
        return accountTypeIds;
    }

    public void setAccountTypeIds(Integer[] accountTypeIds) {
        this.accountTypeIds = accountTypeIds;
    }

    /**
     * Return all ItemDependencyDTO objects contained in dependencies with the
     * given type.
     *
     * @param type
     * @return
     */
    public Collection<ItemDependencyDTO> getDependenciesOfType(ItemDependencyType type) {
        ArrayList<ItemDependencyDTO> result = new ArrayList<ItemDependencyDTO>();
        if(dependencies != null) {
            for(ItemDependencyDTO dependency : dependencies) {
                if(dependency.getType().equals(type)) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    @Transient
    public Integer[] getDependencyIds() {
        if (this.dependencyIds == null && dependencies != null) {
            Integer[] dependencyIds = new Integer[dependencies.size()];
            int i = 0;
            for (ItemDependencyDTO dependency : dependencies) {
                dependencyIds[i++] = dependency.getId();
            }
            setDependencyIds(types);
        }
        return dependencyIds;
    }

    /**
     * From the dependencies extract the ids of those of type {@code type }
     * which has a minimum required qty of 1
     *
     * @param type Type of dependecies to extract
     * @return
     */
    public Integer[] getMandatoryDependencyIdsOfType(ItemDependencyType type) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(dependencies != null) {
            for(ItemDependencyDTO dependency : dependencies) {
                if(dependency.getType().equals(type) && dependency.getMinimum() > 0) {
                    result.add(dependency.getDependentObjectId());
                }
            }
        }
        return result.toArray(new Integer[result.size()]);
    }

    @Transient
    public void setDependencyIds(Integer[] ids) {
        this.dependencyIds = types;
    }

    @Transient
    public List<Integer> getParentAndChildIds() {
        return parentAndChildIds;
    }

    public void setParentAndChildIds(List<Integer> parentAndChildIds) {
        this.parentAndChildIds = parentAndChildIds;
    }

    /**
     * Rules 'contains' operator only works on a collections of strings
     * @return collection of ItemTypeDTO ID's as strings.
     */
    @Transient
    public Collection<String> getStrTypes() {
        if (strTypes == null && itemTypes != null) {
            strTypes = new ArrayList<String>(itemTypes.size());
            for (ItemTypeDTO type : itemTypes) {
                strTypes.add(String.valueOf(type.getId()));
            }
        }

        return strTypes;
    }

    @Transient
    public String getPromoCode() {
        return promoCode;
    }


    @Transient
    public void setPromoCode(String string) {
        promoCode = string;
    }

    @Transient
    public Integer getEntityId() {
        return getEntity() != null ? getEntity().getId() : null;
    }

    @Transient
    public Integer getOrderLineTypeId() {
        return orderLineTypeId;
    }

    @Transient
    public void setOrderLineTypeId(Integer typeId) {
        orderLineTypeId = typeId;
    }

    @Transient
    public Integer getCurrencyId() {
        return currencyId;
    }

    @Transient
    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @Transient
    public BigDecimal getPrice() {
        return price;
    }

    @Transient
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Transient
    public Integer getPriceModelCompanyId() {
        return priceModelCompanyId;
    }

    @Transient
    public void setPriceModelCompanyId(Integer priceModelCompanyId) {
        this.priceModelCompanyId = priceModelCompanyId;
    }

    @Override
    public String toString() {
        return "ItemDTO: id=" + getId();
    }

    public ItemTypeDTO findItemTypeWithAssetManagement() {
        for(ItemTypeDTO type : itemTypes) {
            if(type.getAllowAssetManagement().intValue() == 1) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getEntity().getId())
        .append("-")
        .append(id);

        return key.toString();
    }

    @Transient
    public boolean isPercentage() {
        return isPercentage;
    }

    public void setIsPercentage(boolean isPercentage) {
        this.isPercentage = isPercentage;
    }

    @Override
    @Transient
    public String[] getFieldNames() {
        return new String[] {
                "productID",
                "productCode",
                "category",
                "hasDecimals",
                "percentage",
                "priceStrategy",
                "currency",
                "rate",
                "attributes"
        };
    }

    @Override
    @Transient
    public Object[][] getFieldValues() {
        // Now prices exist for each company so have to tell for which company you want to get the price, currently set to get global price
        PriceModelDTO currentPrice = getPrice(TimezoneHelper.companyCurrentDate(getEntityId()), null);

        return new Object[][]{
                {
                    id,
                    internalNumber,
                    itemTypes.stream().map(ItemTypeDTO::getDescription).collect(Collectors.joining(";")),
                    hasDecimals,
                    percentage,
                    currentPrice != null ? currentPrice.getType().name() : null,
                            currentPrice != null && currentPrice.getCurrency() != null
                            ? currentPrice.getCurrency().getDescription() : null,
                                    currentPrice != null ? currentPrice.getRate() : null,
                                            currentPrice != null && !currentPrice.getAttributes().isEmpty()
                                            ? currentPrice.getAttributes() : null,
                }
        };
    }

    /**
     * Return the Plan that this item is the subscription item for, or null
     * @return
     */
    public PlanDTO firstPlan() {
        if(isPlan()) {
            return getPlans().iterator().next();
        }
        return null;
    }

    @Transient
    public boolean isPlan() {
        return !getPlans().isEmpty();
    }

    @Transient
    public boolean isBundledItem(ItemDTO planSubscriptionItem) {

        boolean isBundledItem = false;
        for (PlanDTO plan : planSubscriptionItem.getPlans()) {
            for (PlanItemDTO planItem : plan.getPlanItems()) {
                if (this.getId() == planItem.getItem().getId() && planItem.hasBundle() && planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    isBundledItem = true;
                    break;
                }
            }
            if (isBundledItem) {
                break;
            }
        }

        return isBundledItem;
    }

    @Transient
    public boolean getIsPlan() {
        return isPlan;
    }

    @Transient
    public void setIsPlan(boolean isPlan) {
        this.isPlan = isPlan;
    }

    @Transient
    public boolean hasFreeUnits(List<CustomerUsagePoolDTO> newCustomerUsagePools) {

        for (CustomerUsagePoolDTO customerUsagePool : newCustomerUsagePools) {
            for (ItemDTO itemDto : customerUsagePool.getUsagePool().getAllItems()) {
                if (itemDto.getId() == this.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transient
    public boolean isProductAvailableToCompany(CompanyDTO company) {
        if(global) {
            return true;
        }
        if(entities != null) {
            for(CompanyDTO availableCompany : entities) {
                if(availableCompany.getId() == company.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transient
    public Boolean isTieredPricingProduct(){
        PriceModelDTO priceModelDto =  this.getPrice(new Date());
        if(priceModelDto != null){
            PriceModelStrategy strategyType = priceModelDto.getType();
            return (strategyType == PriceModelStrategy.TIERED || strategyType == PriceModelStrategy.CAPPED_GRADUATED ||
                    strategyType == PriceModelStrategy.GRADUATED);
        }
        return false;
    }

    @Transient
    public Boolean isFlatPricingProduct(){
        PriceModelDTO priceModelDto =  this.getPrice(new Date());
        return priceModelDto.getType().equals(PriceModelStrategy.FLAT);
    }

    @Transient
    public boolean isAssetEnabledItem() {
        Integer assetEnabled = getAssetManagementEnabled();
        return null!= assetEnabled && assetEnabled.intValue() == 1;
    }

    @Transient
    public Boolean isRouteRateCardPricingProduct(Date asOfDate){
        PriceModelDTO priceModelDto = this.getPrice(asOfDate);
        return null != priceModelDto ? priceModelDto.getType().equals(PriceModelStrategy.ROUTE_BASED_RATE_CARD)
                : Boolean.FALSE;
    }

    private static final List<PriceModelStrategy> EXCULDED_AMOUNT_CALCULATION_STRATEGY_LIST =
            Arrays.asList(PriceModelStrategy.CAPPED_GRADUATED, PriceModelStrategy.BLOCK_AND_INDEX, PriceModelStrategy.ROUTE_BASED_RATE_CARD);

    /**
     * return true if excluded pricing strategy found for given eventDate.
     * @param eventDate
     * @return
     */
    @Transient
    public Boolean excludeAmountCalculation(Date eventDate) {
        PriceModelDTO priceModel = this.getPrice(eventDate);
        if(null == priceModel) {
            return false;
        }
        PriceModelStrategy type = priceModel.getType();
        return EXCULDED_AMOUNT_CALCULATION_STRATEGY_LIST.contains(type);
    }

    @Transient
    public PlanDTO getPlan() {
        if(hasPlans()) {
            return plans.iterator().next();
        }
        return null;
    }

    @Transient
    public boolean belongsToCategory(Integer typeId) {
        Assert.notNull(typeId, "please provide not null typeId.");
        return ArrayUtils.contains(getTypes(), typeId);
    }
}
