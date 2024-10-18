package com.sapienter.jbilling.appdirect.subscription.companydetails;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.sapienter.jbilling.appdirect.subscription.HrefWS;

public class AppdirectCompanyWS {
	@Getter @Setter
	private String uuid;

	@Getter @Setter
	private String name;

	@Getter @Setter
	private String picture;

	@Getter @Setter
	private boolean vendor;

	@Getter @Setter
	private String phoneNumber;

	@Getter @Setter
	private String defaultRole;

	@Getter @Setter
	private boolean enabled;

	@Getter @Setter
	private List<HrefWS> links;
}
