package com.sapienter.jbilling.server.company;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class CompanyInformationTypeWS extends MetaFieldGroupWS implements Serializable {

	@NotNull
	private String name;

    @NotNull
	private Integer companyId;

	public CompanyInformationTypeWS() {
		super();
        setEntityType(EntityType.COMPANY_INFO);

	}

	public CompanyInformationTypeWS(MetaFieldGroupWS groupWS) {
		super();
		this.setDateCreated(groupWS.getDateCreated());
		this.setDateUpdated(groupWS.getDateUpdated());
		this.setDescriptions(groupWS.getDescriptions());
		this.setDisplayOrder(groupWS.getDisplayOrder());
		this.setEntityId(groupWS.getEntityId());
		this.setEntityType(groupWS.getEntityType());
		this.setMetaFields(groupWS.getMetaFields());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		super.setName(name);
		this.name = name;
	}

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

	@Override
	public String toString() {
		return "CompanyInformationTypeWS [name=" + name + ", company="
				+ companyId + ", getId()=" + getId() + "]";
	}

}
