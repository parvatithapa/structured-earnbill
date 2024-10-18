package com.sapienter.jbilling.appdirect.subscription;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderContractWS implements Serializable {
	/**
	 * Minimum service length
	 */
	private Integer minimumServiceLength;

	/**
	 * End date for contract
	 */
	private Date endOfContractDate;

	/**
	 * End date for grace period
	 */
	private Date gracePeriodEndDate;

	/**
	 * Cancellation period limit
	 */
	private Integer cancellationPeriodLimit;

	/**
	 * Grace period for end of contract
	 */
	private Integer endOfContractGracePeriod;

	/**
	 * Contract termination fee
	 */
	private OrderTerminationFeeWS terminationFee;

	/**
	 * Contract renewal
	 */
	private OrderContractRenewalWS renewal;
}
