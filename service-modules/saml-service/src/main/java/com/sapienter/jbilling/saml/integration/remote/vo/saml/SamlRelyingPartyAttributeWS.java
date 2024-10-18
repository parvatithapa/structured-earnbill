package com.sapienter.jbilling.saml.integration.remote.vo.saml;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Builder;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SamlRelyingPartyAttributeWS implements Serializable {
    private static final long serialVersionUID = -4262837867284367551L;

    private String type;
    private String value;
}
