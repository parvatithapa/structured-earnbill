package com.sapienter.jbilling.appdirect.subscription;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentWS {

	private String id;

	private String parentSubscriptionId;

	private Date creationDate;

	private Date endDate;

	private String externalAccountId;

	private String status;

	private Integer maxUsers;

	private Integer assignedUsers;

	private OrderWS order;

	private OrderWS upcomingOrder;

	private LinkWS user;

	private LinkWS company;

	private LinkWS product;

	private LinkWS edition;

	private String redirectUrl;

	private String internalId;
}