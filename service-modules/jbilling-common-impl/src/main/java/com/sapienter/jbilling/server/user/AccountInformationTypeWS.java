package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AccountInformationType Data",
		description = "AccountInformationTypeWS Model",
		parent = MetaFieldGroupWS.class)
public class AccountInformationTypeWS extends MetaFieldGroupWS implements Serializable {
    
	@NotNull(message = "validation.error.notnull")
	private String name;

    @NotNull(message = "validation.error.notnull")
	private Integer accountTypeId;

    private boolean useForNotifications;

	public AccountInformationTypeWS() {
		super();
        setEntityType(EntityType.ACCOUNT_TYPE);
		
	}

	public AccountInformationTypeWS(MetaFieldGroupWS groupWS) {
		super();
		this.setDateCreated(groupWS.getDateCreated());
		this.setDateUpdated(groupWS.getDateUpdated());
		this.setDescriptions(groupWS.getDescriptions());
		this.setDisplayOrder(groupWS.getDisplayOrder());
		this.setEntityId(groupWS.getEntityId());
		this.setEntityType(groupWS.getEntityType());
		this.setMetaFields(groupWS.getMetaFields());
	}

	@ApiModelProperty(value = "The name of the AIT",
			required = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		super.setName(name);
		this.name = name;
	}
	@ApiModelProperty(value = "Is this AIT used for notifications")
	@JsonProperty(value = "useForNotifications")
	public boolean isUseForNotifications() {
		return useForNotifications;
	}

	public void setUseForNotifications(boolean useForNotifications) {
        this.useForNotifications = useForNotifications;
    }

	@ApiModelProperty(value = "The id of the account type in which this AIT belongs to.",
			required = true)
    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

	@Override
	public String toString() {
		return "AccountInformationTypeWS [name=" + name + ", accountType="
				+ accountTypeId + ", getId()=" + getId() + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!super.equals(o)) return false;
		if (!(o instanceof AccountInformationTypeWS)) return false;

		AccountInformationTypeWS that = (AccountInformationTypeWS) o;

		if (useForNotifications != that.useForNotifications) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		return !(accountTypeId != null ? !accountTypeId.equals(that.accountTypeId) : that.accountTypeId != null);

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (accountTypeId != null ? accountTypeId.hashCode() : 0);
		result = 31 * result + (useForNotifications ? 1 : 0);
		return result;
	}
}
