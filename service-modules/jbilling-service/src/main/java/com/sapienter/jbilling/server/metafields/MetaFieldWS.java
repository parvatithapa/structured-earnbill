package com.sapienter.jbilling.server.metafields;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Arrays;

import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "MetaField Data", description = "MetaFieldWS Model")
public class MetaFieldWS implements WSSecured, Serializable {

    private static final long serialVersionUID = -1889507849327464287L;

    private int id;
    private Integer entityId;

    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 100, message = "validation.error.size,1,100")
    private String name;
    private Integer fakeId;
    private EntityType entityType;
    private DataType dataType;

    private boolean disabled = false;
    private boolean mandatory = false;

    private Integer displayOrder = 1;

    private Integer[] dependentMetaFields;
    Integer dataTableId;
    private String helpDescription;
    private String helpContentURL;


    @Valid
    private MetaFieldValueWS defaultValue = null;

    @Valid
    private ValidationRuleWS validationRule;

    //indicate whether the metafield is a primary field and can be used for creation of metafield groups and for providing
    //    a meta-fields to be populated for the entity type they belong to
    //Metafields created from the Configuration - MetaField menu will be considered as primary metafields by default. 
    //All other dynamic metafields created on the fly in the system (example: Account Information Type, Product Category) will not be considered as primary
    private boolean primary;
    private MetaFieldType fieldUsage;
    
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String filename;
    
	public MetaFieldWS() {
	}

    @ApiModelProperty(value = "The id of the meta-field entity")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The id of the owner company")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "The name of the meta-field", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "The type of the meta-field")
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    @ApiModelProperty(value = "The data type of the meta-field")
    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @ApiModelProperty(value = "Is this meta-field disabled or not")
    @JsonProperty(value = "disabled")
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @ApiModelProperty(value = "Is this meta-field mandatory or not")
    @JsonProperty(value = "mandatory")
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @ApiModelProperty(value = "The ordered number for this meta-field")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ApiModelProperty(value = "The default meta-field value for this meta-field")
    public MetaFieldValueWS getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(MetaFieldValueWS defaultValue) {
        this.defaultValue = defaultValue;
    }

    @ApiModelProperty(value = "Indicate whether the meta-field is a primary field and can be used for creation of meta-field groups and for providing " +
            "a meta-fields to be populated for the entity type they belong to",
            notes = "Meta-fields created from the Configuration - Meta-Field menu will be considered as primary metafields by default. " +
                    "All other dynamic meta-fields created on the fly in the system (example: Account Information Type, Product Category) will not be considered as primary.")
    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    @ApiModelProperty(value = "Validation rule used for meta-field, based on what MetaFieldType is used")
    public ValidationRuleWS getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(ValidationRuleWS validationRule) {
        this.validationRule = validationRule;
    }

    @ApiModelProperty(value = "Meta-field type usage")
    public MetaFieldType getFieldUsage() {
        return fieldUsage;
    }

    public void setFieldUsage(MetaFieldType fieldUsage) {
        this.fieldUsage = fieldUsage;
    }

    @ApiModelProperty(value = "File name of the javascript file used when DataType property is set to SCRIPT")
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Returns the entity ID of the company owning the secure object, or null
     * if the entity ID is not available.
     *
     * @return owning entity ID
     */
    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return entityId;
    }

    /**
     * Returns the user ID of the user owning the secure object, or null if the
     * user ID is not available.
     *
     * @return owning user ID
     */
    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @ApiModelProperty(value = "The ids of the meta-fields that have dependencies with this meta-field")
    public Integer[] getDependentMetaFields() {
        return dependentMetaFields;
    }

    public void setDependentMetaFields(Integer[] dependentMetaFields) {
        this.dependentMetaFields = dependentMetaFields;
    }

    @ApiModelProperty(value = "The id of the data table where a specific data is stored")
    public Integer getDataTableId() {
        return dataTableId;
    }

    public void setDataTableId(Integer dataTableId) {
        this.dataTableId = dataTableId;
    }

    @ApiModelProperty(value = "Fake id of this meta-field")
    public Integer getFakeId() {
        return fakeId;
    }

    public void setFakeId(Integer fakeId) {
        this.fakeId = fakeId;
    }

    @ApiModelProperty(value = "A helpful description of the information that is required by this meta-field")
    public String getHelpDescription() {
        return helpDescription;
    }

    public void setHelpDescription(String helpDescription) {
        this.helpDescription = helpDescription;
    }

    @ApiModelProperty(value = "A URL with more information about this meta-field")
    public String getHelpContentURL() {
        return helpContentURL;
    }

    public void setHelpContentURL(String helpContentURL) {
        this.helpContentURL = helpContentURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaFieldWS that = (MetaFieldWS) o;

        if (id != that.id) return false;
        if (disabled != that.disabled) return false;
        if (mandatory != that.mandatory) return false;
        if (primary != that.primary) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (fakeId != null ? !fakeId.equals(that.fakeId) : that.fakeId != null) return false;
        if (entityType != that.entityType) return false;
        if (dataType != that.dataType) return false;
        if (displayOrder != null ? !displayOrder.equals(that.displayOrder) : that.displayOrder != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(dependentMetaFields, that.dependentMetaFields)) return false;
        if (dataTableId != null ? !dataTableId.equals(that.dataTableId) : that.dataTableId != null) return false;
        if (helpDescription != null ? !helpDescription.equals(that.helpDescription) : that.helpDescription != null)
            return false;
        if (helpContentURL != null ? !helpContentURL.equals(that.helpContentURL) : that.helpContentURL != null)
            return false;
        if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
        if (validationRule != null ? !validationRule.equals(that.validationRule) : that.validationRule != null)
            return false;
        if (fieldUsage != that.fieldUsage) return false;
        return !(filename != null ? !filename.equals(that.filename) : that.filename != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (fakeId != null ? fakeId.hashCode() : 0);
        result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        result = 31 * result + (disabled ? 1 : 0);
        result = 31 * result + (mandatory ? 1 : 0);
        result = 31 * result + (displayOrder != null ? displayOrder.hashCode() : 0);
        result = 31 * result + (dependentMetaFields != null ? Arrays.hashCode(dependentMetaFields) : 0);
        result = 31 * result + (dataTableId != null ? dataTableId.hashCode() : 0);
        result = 31 * result + (helpDescription != null ? helpDescription.hashCode() : 0);
        result = 31 * result + (helpContentURL != null ? helpContentURL.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (validationRule != null ? validationRule.hashCode() : 0);
        result = 31 * result + (primary ? 1 : 0);
        result = 31 * result + (fieldUsage != null ? fieldUsage.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        return result;
    }
}
