package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.pricing.Route;
import com.sapienter.jbilling.server.pricing.RouteRateCardRecord;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;
import com.sapienter.jbilling.server.util.Constants;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
@Entity
@Table(name="route_rate_card")
@TableGenerator(
        name = "route_rate_card_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "route_rate_card",
        allocationSize = 1
)
public class RouteRateCardDTO implements Route {

    public static final String TABLE_PREFIX = "route_rate_";

    public static final List<com.sapienter.jbilling.server.util.sql.TableGenerator.Column> TABLE_COLUMNS = Arrays.asList(
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("id", "int", false, true),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("name", "varchar(100)", false, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("surcharge", "varchar(100)", false, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("initial_increment", "varchar(100)", false, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("subsequent_increment", "varchar(100)", false, false),
            new com.sapienter.jbilling.server.util.sql.TableGenerator.Column("charge", "varchar(100)", false, false)
    );

    public static final List<String> TABLE_COLUMNS_NAMES = Arrays.asList("id","name","surcharge","initial_increment","subsequent_increment","charge");

    private Integer id;
    private String name;
    private CompanyDTO company;
    private String tableName;
    private RatingUnitDTO ratingUnit;
    private Integer versionNum;
    private Set<MatchingFieldDTO> matchingFields = new HashSet<>(0);
    // transient
    private Set<RouteRateCardRecord> routeRateCardRecords = new HashSet<>(0);

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "route_rate_card_GEN")
    @Column(name="id",unique = true, nullable = false)
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

    public void setName(String name) {
        this.name = name;
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
        return Constants.TABLE_ROUTE_RATE_CARD;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_unit_id")
    public RatingUnitDTO getRatingUnit() {
        return ratingUnit;
    }

    public void setRatingUnit(RatingUnitDTO ratingUnit) {
        this.ratingUnit = ratingUnit;
    }

    @Version
    @Column(name = "optlock")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "routeRateCard")
    public Set<MatchingFieldDTO> getMatchingFields() {
        return matchingFields;
    }

    public void setMatchingFields(Set<MatchingFieldDTO> matchingFields) {
        this.matchingFields = matchingFields;
    }

    @Transient
    public Set<RouteRateCardRecord> getRouteRateCardRecords() {
        return routeRateCardRecords;
    }

    public void setRouteRateCardRecords(Set<RouteRateCardRecord> routeRateCardRecords) {
        this.routeRateCardRecords = routeRateCardRecords;
    }

    @Override
    public String toString() {
        return "RouteRateCardDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", company=" + company +
                ", tableName='" + tableName + '\'' +
                ", versionNum=" + versionNum +
                '}';
    }
}
