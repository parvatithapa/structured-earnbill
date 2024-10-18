package com.sapienter.jbilling.appdirect.vo;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "usageItem")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageItemBean implements Serializable {
    private static final long serialVersionUID = 7935998093136955903L;

    /**
     * Pricing unit, may be null if custom pricing.
     */
    private PricingUnit unit;

    /**
     * Custom unit, may be null if not custom pricing.
     * It is used to identify each meter with custom price.
     */
    private String customUnit;

    /**
     * The quantity.
     */
    private BigDecimal quantity;

    /**
     * Price is used for custom pricing items.
     */
    private BigDecimal price;

    /**
     * Description of the usage item.
     */
    private String description;
}
