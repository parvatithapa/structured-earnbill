package com.sapienter.jbilling.server.integration.common.service.vo;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChargePeriod {

	private Date firstDay;
	private Date lastDay;
}
