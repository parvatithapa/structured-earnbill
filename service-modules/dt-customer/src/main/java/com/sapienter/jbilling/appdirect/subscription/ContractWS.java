package com.sapienter.jbilling.appdirect.subscription;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractWS implements Serializable {
	private static final long serialVersionUID = 6136644945443431009L;

	/**
	 * Minimum service length
	 */
	private Integer minimumServiceLength;

	/**
	 * Cancellation period limit
	 */
	private Integer cancellationPeriodLimit;

	/**
	 * End of contract grace period
	 */
	private Integer endOfContractGracePeriod;

	/**
	 * Is switch to shorter contract allowed
	 */
	private boolean blockSwitchToShorterContract = false;

	/**
	 * Is contract downgrade allowed
	 */
	private boolean blockContractDowngrades = false;

	/**
	 * Is contract upgrade allowed
	 */
	private boolean blockContractUpgrades = false;

	/**
	 * Is addon cycle start date contract aligned with parent cycle start date
	 */
	private boolean alignWithParentCycleStartDate = false;

	/**
	 * Grade period
	 */
	private DurationWS gracePeriod;

	/**
	 * Termination fee
	 */
	private TerminationFeeWS terminationFee;

	/**
	 * Auto extension pricing ID
	 */
	private Long autoExtensionPricingId;
}