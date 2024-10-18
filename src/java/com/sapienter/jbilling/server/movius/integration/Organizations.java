package com.sapienter.jbilling.server.movius.integration;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Manish Bansod
 * @since 10-11-2017
 */
@XmlRootElement(name="organizations")  
public class Organizations {
	 
	private List<Organization> organizationList = new ArrayList<>();
	private String systemId;

	@XmlElements(@XmlElement(name = "org", type = Organization.class))
	public List<Organization> getOrganizations() {
		return organizationList;
	}

	public void setOrganizations(List<Organization> organizations) {
		this.organizationList = organizations;
	}

	@XmlAttribute(name = "system-id")
	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	@Override
	public String toString() {
		return String.format("Organizations [organizations=%s, systemId=%s]",
				organizationList, systemId);
	}

}
