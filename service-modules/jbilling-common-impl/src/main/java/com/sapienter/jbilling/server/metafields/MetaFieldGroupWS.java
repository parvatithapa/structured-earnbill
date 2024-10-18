package com.sapienter.jbilling.server.metafields;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;
import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.sapienter.jbilling.server.security.WSSecured;

@ApiModel(value = "MetaFieldGroup Data",
		description = "MetaFieldGroupWS Model",
		subTypes = {AccountInformationTypeWS.class})
public class MetaFieldGroupWS implements WSSecured, Serializable {

	private int id;
	private Date dateCreated;
	private Date dateUpdated;
    private Integer entityId;
	private EntityType entityType;
	private Integer displayOrder;
	
    @NotEmpty(message = "validation.error.notnull")
    @Valid
	private MetaFieldWS[] metaFields;
	
    @NotEmpty(message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(
            new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));

    public MetaFieldGroupWS(){
    }

	@ApiModelProperty(value = "The id of the meta field group entity")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ApiModelProperty(value = "Date of creation")
	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@ApiModelProperty(value = "The latest date when this entity was updated")
	public Date getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}

	@ApiModelProperty(value = "The id of the owner company")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

	@ApiModelProperty(value = "The type of entity for which this meta-field group is defined")
    public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	@ApiModelProperty(value = "Array of meta-fields that belongs to this meta-field group",
			required = true)
    public MetaFieldWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldWS[] metaFields) {
        this.metaFields = metaFields;
    }

	@ApiModelProperty(value = "The ordered number for this meta-field group")
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@ApiModelProperty(value = "Array of all descriptions regarding this meta-field group",
			required = true)
	public List<InternationalDescriptionWS> getDescriptions() {
		return descriptions;
	}

	@JsonIgnore
	public String getDescription() {
		//currently there is only default language is supported for description
		return descriptions.size()>0?descriptions.get(0).getContent():"";
	}

	public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
		this.descriptions = descriptions;
	}

	public void setName(String name){

		if (!updateDescriptionsNameForDefaultLanguageIfAny(name)){
			InternationalDescriptionWS nameDesc=new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, name);
			descriptions.add(nameDesc);
		}
	}

	private boolean updateDescriptionsNameForDefaultLanguageIfAny(String name){
		for (InternationalDescriptionWS description : getDescriptions()){
			if(descriptionContainsNameForDefaultLanguage(description)){
				description.setContent(name);
				return true;
			}
		}
		return false;
	}

	private boolean descriptionContainsNameForDefaultLanguage(InternationalDescriptionWS description){
		return Constants.LANGUAGE_ENGLISH_ID.equals(description.getLanguageId()) &&
				"description".equalsIgnoreCase(description.getPsudoColumn()) &&
				null != description.getContent() && !description.getContent().isEmpty();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MetaFieldGroupWS)) return false;

		MetaFieldGroupWS that = (MetaFieldGroupWS) o;

		if (id != that.id) return false;
		if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
		if (entityType != that.entityType) return false;
		if (dateCreated != that.dateCreated) return false;
		if (displayOrder != null ? !displayOrder.equals(that.displayOrder) : that.displayOrder != null) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(metaFields, that.metaFields)) return false;
		return !(descriptions != null ? !descriptions.equals(that.descriptions) : that.descriptions != null);

	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
		result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
		result = 31 * result + (dateCreated != null ? dateCreated.hashCode() : 0);
		result = 31 * result + (displayOrder != null ? displayOrder.hashCode() : 0);
		result = 31 * result + (metaFields != null ? Arrays.hashCode(metaFields) : 0);
		result = 31 * result + (descriptions != null ? descriptions.hashCode() : 0);
		return result;
	}
}
