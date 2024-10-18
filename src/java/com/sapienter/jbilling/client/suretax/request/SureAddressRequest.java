package com.sapienter.jbilling.client.suretax.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;



@JsonIgnoreProperties(ignoreUnknown=true)
public class SureAddressRequest {
	@JsonProperty("ClientNumber")
	public String clientNumber;
	@JsonProperty("ValidationKey")
	public String validationKey;
	@JsonProperty("PrimaryAddressLine")
	public String address1;
	@JsonProperty("SecondaryAddressLine")
	public String address2="";
	@JsonProperty("City")
	public String city="";
	@JsonProperty("State")
	public String state="";
	@JsonProperty("ResponseType")
	public String responseType="S";
	public String zipcode;

	@Override
	public String toString() {
		return "SureAddressRequest{" +
				"clientNumber='" + clientNumber + '\'' +
				", validationKey='" + validationKey + '\'' +
				", address1='" + address1 + '\'' +
				", address2='" + address2 + '\'' +
				", city='" + city + '\'' +
				", state='" + state + '\'' +
				", responseType='" + responseType + '\'' +
				'}';
	}
}
