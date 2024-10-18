/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.db;


import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.sql.JDBCUtils;
import com.sapienter.jbilling.server.util.sql.TableGenerator;
import org.apache.commons.lang.StringUtils;

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
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rate_card")
@javax.persistence.TableGenerator(
        name = "rate_card_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "rate_card",
        allocationSize = 10
)
// no cache
public class RateCardDTO implements Serializable {

    public static final String TABLE_PREFIX = "rate_";

    public static final List<TableGenerator.Column> TABLE_COLUMNS = Arrays.asList(
            new TableGenerator.Column("id", "int", false, true),
            new TableGenerator.Column("match_column", "varchar(150)", false, false),
            new TableGenerator.Column("comment", "varchar(255)", true, false),
            new TableGenerator.Column("rate", "numeric(22,10)", false, false)
    );

    private Integer id;
    private String name;
    private String tableName;
    private CompanyDTO company;
    private Set<CompanyDTO> childCompanies = new HashSet<CompanyDTO>(0);
    private boolean global = false;

    public RateCardDTO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "rate_card_GEN")
    @Column(name = "id", nullable = false, unique = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "table_name", nullable = false, unique = true)
    public String getTableName() {
        if (StringUtils.isBlank(tableName) && StringUtils.isNotBlank(name)) {
            tableName = JDBCUtils.getDatabaseObjectName(name);
            if(tableName.startsWith(TABLE_PREFIX)) {
                tableName = TABLE_PREFIX + tableName;
            }
        }
        if (StringUtils.isNotBlank(tableName) && !tableName.startsWith(TABLE_PREFIX)) {
            tableName = TABLE_PREFIX + tableName;
        }
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = true)
    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "rate_card_child_entity_map", joinColumns = { 
    		@JoinColumn(name = "rate_card_id", updatable = false) }, inverseJoinColumns = { 
    		@JoinColumn(name = "entity_id", updatable = false) })
    public Set<CompanyDTO> getChildCompanies() {
        return this.childCompanies;
    }

    public void setChildCompanies(Set<CompanyDTO> childCompanies) {
        this.childCompanies = childCompanies;
    }

    @Column(name = "global", nullable = false)
    public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}
	
    @Override
    public String toString() {
        return "RateCardDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tableName='" + tableName + '\'' +
                ", company=" + company +
                '}';
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getCompany().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
