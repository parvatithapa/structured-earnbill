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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Transient;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;

/**
 * Common class for extending by entities that can contain meta-fields. This class enforces a set
 * of convenience methods for accessing the meta data.
 *
 * @author Alexander Aksenov
 * @since 08.10.11
 */

public abstract class CustomizedEntity implements MetaContent, java.io.Serializable {

    private List<MetaFieldValue> metaFields = new LinkedList<MetaFieldValue>();

    @Transient
    protected List<MetaFieldValue> getMetaFieldsList() {
        return metaFields;
    }

    @Override
    public void setMetaFields(List<MetaFieldValue> fields) {
        this.metaFields = fields;
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param name meta field name
     * @return field if found, null if not set.
     */
    @Override
    @Transient
    public MetaFieldValue getMetaField(String name) {
        return getMetaField(name, null);
    }


    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param name meta field name
     * @return field if found, null if not set.
     */
    @Override
    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(this, name, groupId);
    }

    @Override
    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId, Integer companyId) {
        return MetaFieldHelper.getMetaField(this, name, groupId, companyId);
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param metaFieldNameId ID of meta field name
     * @return field if found, null if not set.
     */
    @Override
    @Transient
    public MetaFieldValue getMetaField(Integer metaFieldNameId) {
        return MetaFieldHelper.getMetaField(this, metaFieldNameId);
    }

    /**
     * Adds a meta field to this object. If there is already a field associated with
     * this object then the existing value should be updated.
     *
     * @param field field to update.
     */
    @Override
    @Transient
    public void setMetaField(MetaFieldValue field, Integer groupId) {
        MetaFieldHelper.setMetaField(this, field, groupId);
    }

    /**
     * Sets the value of a meta field that is already associated with this object. If
     * the field does not already exist, or if the value class is of an incorrect type
     * then an IllegalArgumentException will be thrown.
     *
     * @param name  field name
     * @param value field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    @Override
    @Transient
    public void setMetaField(Integer entityId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entityId, this, groupId, name, value);
    }

    /**
     * Usefull method for updating meta fields with validation before entity saving
     *
     * @param dto dto with new data
     */
    @Override
    @Transient
    public void updateMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer accountTypeId, MetaContent dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(languageId, entityId, accountTypeId, this, dto);
    }

    /**
     * Set the value of metaField to the value provided.
     *
     * @param metaField
     * @param value
     */
    public void setMetaField(MetaField metaField, Object value) {
        MetaFieldValue metaFieldValue = getMetaField(metaField.getName());
        if(metaFieldValue == null) {
            metaFieldValue = metaField.createValue();
        }
        metaFieldValue.setValue(value);
        setMetaField(metaFieldValue, null);
    }

    /**
     * Set the value of metaField to the value provided.
     *
     * @param metaField
     * @param value
     */
    public void setMetaField(MetaField metaField, Object value, Integer groupId) {
        MetaFieldValue metaFieldValue = getMetaField(metaField.getName(), groupId);
        if(metaFieldValue == null) {
            metaFieldValue = metaField.createValue();
        }
        metaFieldValue.setValue(value);
        setMetaField(metaFieldValue, groupId);
    }

    /**
     * Update the meta fields with values provided by dto. Only Meta Field as specified in {@code metaFields}
     * are updated
     *
     * @param metaFields
     * @param dto
     */
    public void updateMetaFieldsWithValidation(Integer languageId, Set<MetaField> metaFields, CustomizedEntity dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(languageId, metaFields, this, dto);
    }

    /**
     * Returns the meta field by name and type if it's been defined for this object.
     * @param name
     * @param type
     * @return
     */
    @Transient
    public Optional<MetaFieldValue<?>> getMetaFieldByType(MetaFieldType type) {
        return MetaFieldHelper.getMetaFieldByType(this, type);
    }
}
