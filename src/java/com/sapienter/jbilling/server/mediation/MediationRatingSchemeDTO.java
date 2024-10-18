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

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.io.Serializable;
import java.util.*;

/**
 * Created by Andres Canevaro on 28/07/15.
 */
@Entity
@TableGenerator(
        name = "rating_scheme_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "rating_scheme",
        allocationSize = 30
)
@Table(name = "rating_scheme")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class MediationRatingSchemeDTO implements Serializable {

    private Integer id;
    private CompanyDTO entity;
    private String name;
    private Integer initialIncrement;
    private Integer mainIncrement;
    private Integer initialRoundingMode;
    private Integer mainRoundingMode;
    private Boolean global;
    public Set<RatingSchemeAssociation> associations;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "rating_scheme_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entityId) {
        this.entity = entityId;
    }

    @Column(name = "name", length = 50)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "initial_increment")
        public Integer getInitialIncrement() {
        return this.initialIncrement;
    }

    public void setInitialIncrement(Integer initialIncrement) {
        this.initialIncrement = initialIncrement;
    }

    @Column(name = "main_increment")
    public Integer getMainIncrement() {
        return this.mainIncrement;
    }

    public void setMainIncrement(Integer mainIncrement) {
        this.mainIncrement = mainIncrement;
    }

    @Column(name = "initial_rounding_mode")
    public Integer getInitialRoundingMode() {
        return this.initialRoundingMode;
    }

    public void setInitialRoundingMode(Integer initialRoundingMode) {
        this.initialRoundingMode = initialRoundingMode;
    }

    @Column(name = "main_rounding_mode")
    public Integer getMainRoundingMode() {
        return this.mainRoundingMode;
    }

    public void setMainRoundingMode(Integer mainRoundingMode) {
        this.mainRoundingMode = mainRoundingMode;
    }

    @Column(name = "global")
    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "rating_scheme")
    public Set<RatingSchemeAssociation> getAssociations() {
        return associations;
    }

    public void setAssociations(Set<RatingSchemeAssociation> associations) {
        this.associations = associations;
    }

}
