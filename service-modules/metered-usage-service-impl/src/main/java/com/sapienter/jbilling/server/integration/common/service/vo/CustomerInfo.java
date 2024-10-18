package com.sapienter.jbilling.server.integration.common.service.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerInfo {
    private Integer userId;
    private Integer languageId;
    private Integer currencyId;
    private String  externalAccountIdentifier;
}
