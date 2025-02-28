/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields.db;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationReport;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleModel;

import javax.persistence.*;

import java.io.Serializable;

/**
 * MetaFieldValue contains the user-defined value to store for a meta field name-value pair.
 *
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
@Entity
@Table(name = "meta_field_value")
@TableGenerator(
        name = "meta_field_value_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "meta_field_value",
        allocationSize = 10
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class MetaFieldValue<T> implements Serializable {

    private Integer id;
    private MetaField field;
    protected MetaFieldValue() {
    }

    protected MetaFieldValue(MetaField field) {
        this.field = field;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "meta_field_value_GEN")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meta_field_name_id", updatable = false, nullable = false)
    public MetaField getField() {
        return field;
    }
    
      public void setField(MetaField field) {
        this.field = field;
    }


    /*
        Value bindings to be implemented by subclasses. This ensures that each "type" of parameter
        can get and set a value with an appropriate data type.
     */

    @Transient
    abstract public T getValue();
    abstract public void setValue(T value);
    @Transient
    abstract public boolean isEmpty();

    @SuppressWarnings("unchecked")
    @Transient
    public void validate(Integer languageId, MetaContent source) {
        if (this.getField() != null
                && this.getField().isMandatory()
                && (null == this.getValue() || this.isEmpty())) {

            String error = "MetaFieldValue,value,value.cannot.be.null," + field.getName();
            throw new SessionInternalError("Field value failed validation.", new String[]{ error });
        }

        ValidationRule rule = getField().getValidationRule();
        if (rule == null) {
            return;
        }

        ValidationRuleModel validationModel = rule.getRuleType().getValidationRuleModel();
        MetaField metaField = getField();
        ValidationReport validationReport = validationModel.doValidation(source, this.getValue(), languageId, metaField);

        if (validationReport != null && !validationReport.getErrors().isEmpty()) {
            throw new SessionInternalError("Field value failed validation.",
                    validationReport.getErrors().toArray(new String[validationReport.getErrors().size()]));
        }
    }

    @Override
    public String toString() {
        String metaFieldValue = ( field.getFieldUsage() == MetaFieldType.PAYMENT_CARD_NUMBER || field.getFieldUsage() == MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED) ? "******" :String.valueOf(getValue());
        return "MetaFieldValue{" +
                "name=" + field +
                ", value=" + metaFieldValue +
                '}';
    }

    /**
     * Load all lazy dependencies of entity if needed
     */
    public void touch() {
        getValue();
        if (getField() != null) {
            field.getName();
        }
    }

    @Transient
    public String getFieldName() {
        return getField().getName();
    }
}
