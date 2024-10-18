package com.sapienter.jbilling.appdirect.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PricingUnit implements Serializable {
    private static final long serialVersionUID = 5370444679965938439L;
    private String unit;
}
