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


import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.util.Constants;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="preference_type")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class PreferenceTypeDTO extends AbstractDescription implements Serializable {

    private int id;
    private String defaultValue;
    private Set<PreferenceDTO> preferences = new HashSet<PreferenceDTO>(0);
    private ValidationRule validationRule;

    public PreferenceTypeDTO() {
    }

    public PreferenceTypeDTO(int id) {
        this.id = id;
    }

    public PreferenceTypeDTO(int id, String defaultValue, Set<PreferenceDTO> preferences) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.preferences = preferences;
    }

    @Id
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name="def_value", length=200)
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="preferenceType")
    public Set<PreferenceDTO> getPreferences() {
        return this.preferences;
    }

    public void setPreferences(Set<PreferenceDTO> preferences) {
        this.preferences = preferences;
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_PREFERENCE_TYPE;
    }

    @Transient
    public String getInstructions() {
        return getDescription(Constants.LANGUAGE_ENGLISH_ID, "instruction");
    }

    @Transient
    public String getInstructions(Integer languageId) {
        return getDescription(languageId, "instruction");
    }

    public String getAuditKey(Serializable id) {
        return id.toString();
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "validation_rule_id", nullable = true)
    public ValidationRule getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(ValidationRule validationRule) {
        this.validationRule = validationRule;
    }

}


