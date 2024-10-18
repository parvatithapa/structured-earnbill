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

import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import java.io.Serializable;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

@Entity
@TableGenerator(
        name = "entity_item_price_map_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "entity_item_price_map",
        allocationSize = 1
)
@Table(name = "entity_item_price_map")
public class EntityItemPrice implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -649387332758012893L;

	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "entity_item_price_map_GEN")
    @Column(name = "id", unique = true, nullable = false)
	private int id;
	
	@ManyToOne
	@JoinColumn(name = "entity_id", nullable = true, updatable = false)
	private CompanyDTO entity;
	
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private ItemDTO item;
	
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn(name="start_date", nullable = true)
	@JoinTable(name = "item_price_timeline",
		joinColumns = {@JoinColumn(name = "model_map_id", updatable = false)},
		inverseJoinColumns = {@JoinColumn(name = "price_model_id", updatable = false)}		
	)
	@Sort(type = SortType.NATURAL)
	@Fetch(FetchMode.SELECT)
	private SortedMap<Date, PriceModelDTO> prices = new TreeMap<Date, PriceModelDTO>();
	
	public EntityItemPrice() {}
	
	public EntityItemPrice (ItemDTO item, CompanyDTO entity, SortedMap<Date, PriceModelDTO> prices) {
		this.item = item;
		this.entity = entity;
		this.prices = prices;
	}
	
	public ItemDTO getItem () {
		return this.item;
	}
	
	public CompanyDTO getEntity () {
		return this.entity;
	}
	
    public SortedMap<Date, PriceModelDTO> getPrices() {
        return prices;
    }

    public void setPrices(SortedMap<Date, PriceModelDTO> defaultPrices) {
        this.prices = defaultPrices;
    }
}


