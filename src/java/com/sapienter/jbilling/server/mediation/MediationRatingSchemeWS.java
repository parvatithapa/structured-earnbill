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

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;

/**
 * Created by Andres Canevaro on 28/07/15.
 */
public class MediationRatingSchemeWS implements Serializable {

    private Integer id;
    private Integer entity;
    @NotEmpty(message="validation.error.notnull")
    private String name;
    @NotNull(message="validation.error.notnull")
    @Digits(integer=12, fraction=10, message="validation.message.error.invalid.pattern")
    private Integer initialIncrement;
    @NotNull(message="validation.error.notnull")
    @Digits(integer=5, fraction=0, message="validation.message.error.invalid.pattern")
    private Integer mainIncrement;
    private Integer initialRoundingMode;
    private Integer mainRoundingMode;
    private Boolean global;
    List<RatingSchemeAssociationWS> associations;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntity() {
        return entity;
    }

    public void setEntity(Integer entity) {
        this.entity = entity;
    }

    public Integer getInitialIncrement() {
        return initialIncrement;
    }

    public void setInitialIncrement(Integer initialIncrement) {
        this.initialIncrement = initialIncrement;
    }

    public Integer getMainIncrement() { return mainIncrement; }

    public void setMainIncrement(Integer mainIncrement) {
        this.mainIncrement = mainIncrement;
    }

    public Integer getMainRoundingMode() {
        return mainRoundingMode;
    }

    public void setMainRoundingMode(Integer mainRoundingMode) {
        this.mainRoundingMode = mainRoundingMode;
    }

    public Integer getInitialRoundingMode() {
        return initialRoundingMode;
    }

    public void setInitialRoundingMode(Integer initialRoundingMode) {
        this.initialRoundingMode = initialRoundingMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public List<RatingSchemeAssociationWS> getAssociations() {
        return associations;
    }

    public void setAssociations(List<RatingSchemeAssociationWS> associations) {
        this.associations = associations;
    }

}
