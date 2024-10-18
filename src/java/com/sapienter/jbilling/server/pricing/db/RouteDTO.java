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

import com.sapienter.jbilling.server.pricing.Route;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;
import com.sapienter.jbilling.server.util.Constants;

import javax.persistence.*;
import javax.persistence.TableGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="route")
@TableGenerator(
        name = "route_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "route",
        allocationSize = 1
)
public class RouteDTO implements Route {

    public static final String TABLE_PREFIX = "route_";

    public static final List<com.sapienter.jbilling.server.util.sql.TableGenerator.Column> ROUTE_TABLE_COLUMNS = Arrays.asList(
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("id", "int", false, true),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("name", "varchar(255)", false, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("next_route", "varchar(255)", true, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("product", "varchar(50)", true, false)
    );

    public static final List<com.sapienter.jbilling.server.util.sql.TableGenerator.Column> NON_ROUTE_TABLE_COLUMNS = Arrays.asList(
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("id", "int", false, true)
    );

    public static final List<String> TABLE_COLUMNS_NAMES = Arrays.asList(new String[]{"id", "name", "next_route", "product"});

    private Integer id;
    private String name;
    private CompanyDTO company;
    private String tableName;
    private Boolean rootTable;
    private Boolean routeTable;
    private String outputFieldName;
    /** Next route to use in case nothing matches */
    private String defaultRoute;
    private Integer versionNum;
    private Set<MatchingFieldDTO> matchingFields = new HashSet<MatchingFieldDTO>(0);
    // transient
    private Set<RouteRecord> routeRecords = new HashSet<RouteRecord>(0);

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "route_GEN")
    @Column(name="id",unique = true,nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    @Column(name = "name")
    public String getName() {
        return name;
    }

    @Column(name = "default_route")
    public String getDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(String defaultRoute) {
        this.defaultRoute = defaultRoute;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "route_table")
    public Boolean getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(Boolean routeTable) {
        this.routeTable = routeTable;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }
    @Column(name="table_name", nullable = false, unique = true)
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    @Transient
    protected String getTable() {
        return Constants.TABLE_ROUTE;
    }

    @Column(name = "root_table")
    public Boolean getRootTable() {
        return rootTable;
    }

    public void setRootTable(Boolean root) {
        this.rootTable = root;
    }

    @Column(name = "output_field_name")
    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String outputFieldName) {
        this.outputFieldName = outputFieldName;
    }

    @Version
    @Column(name = "optlock")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "route")
    public Set<MatchingFieldDTO> getMatchingFields() {
        return matchingFields;
    }

    public void setMatchingFields(Set<MatchingFieldDTO> matchingFields) {
        this.matchingFields = matchingFields;
    }

    @Transient
    public Set<RouteRecord> getRouteRecords() {
        return routeRecords;
    }

    public void setRouteRecords(Set<RouteRecord> routeRecords) {
        this.routeRecords = routeRecords;
    }

    @Override
    public String toString() {
        return "RouteDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", company=" + company +
                ", tableName='" + tableName + '\'' +
                ", defaultRoute='" + defaultRoute + '\'' +
                '}';
    }

}
