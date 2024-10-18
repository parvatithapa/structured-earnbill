package com.sapienter.jbilling.appdirect.subscription;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurationWS implements Serializable {
	private static final long serialVersionUID = -3837625850430512492L;
	private Integer length;
	private String unit;
}
