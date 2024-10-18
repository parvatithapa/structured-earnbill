package com.sapienter.jbilling.server.movius.integration;


import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Manish Bansod
 * @since 10-11-2017
 */
@XmlRootElement(name = "org")  
public class Organization {
	
	private String name;
	private String id;
	private String subscription;
	private String count;
	private String timezone;
	private Integer billingPlanId;
	private String billingPlanName;
	private Boolean billable;
	private Organizations orgs;

	@XmlElement(name = "sub-orgs")
	public Organizations getOrganizations() {
		return orgs;
	}

	public void setOrganizations(Organizations organization) {
		this.orgs = organization;
	}

	public boolean hasSubOrgs(){
		return Objects.nonNull(orgs);
	}

	@XmlElement(name = "name", required = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement
	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	@XmlElement
	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		this.count = count;
	}

	@XmlElement
	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@XmlElement(name="billing-plan-id")
	public Integer getBillingPlanId() {
		return billingPlanId;
	}

	public void setBillingPlanId(Integer billingPlanId) {
		this.billingPlanId = billingPlanId;
	}

	@XmlElement(name="billing-plan-name")
	public String getBillingPlanName() {
		return billingPlanName;
	}

	public void setBillingPlanName(String billingPlanName) {
		this.billingPlanName = billingPlanName;
	}

	@XmlElement
	public Boolean isBillable() {
		return billable;
	}

	public void setBillable(Boolean billable) {
		this.billable = billable;
	}

	@Override
	public String toString() {
		return String.format(
				"Organization [name=%s, id=%s, subscription=%s, count=%s, timezone=%s, billingPlanId=%s, billingPlanName=%s, billable=%s]",
				name, id, subscription, count, timezone, billingPlanId, billingPlanName, billable);
	}

	
}
