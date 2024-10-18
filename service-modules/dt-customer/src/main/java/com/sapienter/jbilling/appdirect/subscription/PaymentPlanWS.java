package com.sapienter.jbilling.appdirect.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentPlanWS {

	private String href;

	private long id;

	private String uuid;

	private String frequency;

	private ContractWS contract;

	private boolean allowCustomUsage;

	private boolean keepBillDateOnUsageChange;

	private boolean separatePrepaid;

	@JsonProperty(value="isPrimaryPrice")
	private boolean isPrimaryPrice;

	private Set<CostWS> costs;

	private float discount;

	private boolean primaryPrice;
}
