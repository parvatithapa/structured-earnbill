package com.sapienter.jbilling.appdirect.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceWS {

	private String type;

	private String uuid;

	private String url;

	private ContentWS content;

}
