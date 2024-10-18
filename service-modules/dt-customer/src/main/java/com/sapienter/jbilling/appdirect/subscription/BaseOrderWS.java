package com.sapienter.jbilling.appdirect.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * Created by rvaibhav on 19/12/17.
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseOrderWS {

	@JsonProperty("id")
	private Long apiId;

	private Date startDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate serviceStartDate;

	private Date endDate;

	private Date nextBillingDate;

	private Date endOfDiscountDate;

	private String status;

	private String frequency;

	private String currency;

	private String type;

	private BigDecimal totalPrice;

	private LinkWS user;

	private LinkWS salesSupportUser;

	private LinkWS salesSupportCompany;

	private LinkWS company;

	private String referenceCode;

}
