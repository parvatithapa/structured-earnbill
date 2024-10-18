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
package com.sapienter.jbilling.server.util.db;


import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CurrencyWS;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "currency")
@TableGenerator(
        name="currency_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="currency",
        allocationSize = 10
)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CurrencyDTO extends AbstractDescription implements Serializable, IDeletable {

    private int id;
    private String symbol;
    private String code;
    private String countryCode;
    private Set<CurrencyExchangeDTO> currencyExchanges = new HashSet<CurrencyExchangeDTO>(0);
    private Set<Integer> entities_1 = new HashSet<>();
    private Integer versionNum;

    // from EX
    private String name = null;
    private Boolean inUse = null;
    private String rate = null; // will be converted to float
    private BigDecimal sysRate = null;

    public CurrencyDTO() {
    }

    // for stubs
    public CurrencyDTO(Integer id) {
        this.id = id;
    }

    public CurrencyDTO(int id, String symbol, String code, String countryCode) {
        this.id = id;
        this.symbol = symbol;
        this.code = code;
        this.countryCode = countryCode;
    }

    public CurrencyDTO(int id, String symbol, String code, String countryCode, Set<CurrencyExchangeDTO> currencyExchanges) {
        this.id = id;
        this.symbol = symbol;
        this.code = code;
        this.countryCode = countryCode;
        this.currencyExchanges = currencyExchanges;
    }

    public CurrencyDTO(CurrencyWS ws) {
        if (ws.getId() != null) {
            this.id = ws.getId();
        }

        this.symbol = ws.getSymbol();
        this.code = ws.getCode();
        this.countryCode = ws.getCountryCode();
        this.inUse = ws.getInUse();

        if (StringUtils.isNotBlank(ws.getRate())) this.rate = ws.getRate();
        if (StringUtils.isNotBlank(ws.getSysRate())) this.sysRate = ws.getSysRateAsDecimal();
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_CURRENCY;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "currency_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "symbol", nullable = false, length = 10)
    public String getSymbol() {
        return this.symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Column(name = "code", nullable = false, length = 3)
    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(name = "country_code", nullable = false, length = 2)
    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "currency")
    public Set<CurrencyExchangeDTO> getCurrencyExchanges() {
        return this.currencyExchanges;
    }

    public void setCurrencyExchanges(Set<CurrencyExchangeDTO> currencyExchanges) {
        this.currencyExchanges = currencyExchanges;
    }

//    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinTable(name = "currency_entity_map",
//            joinColumns = {@JoinColumn(name = "currency_id", updatable = false)},
//            inverseJoinColumns = {@JoinColumn(name = "entity_id", updatable = false)}
//    )
    @ElementCollection
    @CollectionTable(
            name="currency_entity_map",
            joinColumns=@JoinColumn(name="currency_id")
    )
    @Column(name="entity_id")
    public Set<Integer> getEntities_1() {
        return this.entities_1;
    }

    public void setEntities_1(Set<Integer> entities_1) {
        this.entities_1 = entities_1;
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Transient
    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    @Transient
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Transient
    public String getRate() {
        return rate;
    }

    @Transient
    public BigDecimal getRateAsDecimal() {
        return (rate == null ? null : new BigDecimal(rate));
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    @Transient
    public BigDecimal getSysRate() {
        return sysRate;
    }

    public void setSysRate(BigDecimal sysRate) {
        this.sysRate = sysRate;
    }
    
    @Transient
    public boolean isDeletable() {
        //TODO MODULARIZATION, THIS SHOULD CHECK IF ANY ENTITY USE THIS CURRENCY ID
        return false;
//    	return (getEntities().isEmpty() && getBaseUsers().isEmpty()
//    			&& getPurchaseOrders().isEmpty()
//    			&& getPayments().isEmpty() && getInvoices().isEmpty()
//    			&& getPriceModels().isEmpty() && getProcessRunTotals().isEmpty() );
    }

    public String getAuditKey(Serializable id) {
        return id.toString();
    }
}


