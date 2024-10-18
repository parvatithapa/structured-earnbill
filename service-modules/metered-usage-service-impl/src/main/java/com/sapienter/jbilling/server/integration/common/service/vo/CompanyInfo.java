package com.sapienter.jbilling.server.integration.common.service.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Locale;

@Data
@Builder
public class CompanyInfo {
    private Integer companyId;
    private Integer languageId;
    private Integer currencyId;
    private Locale  locale;
}
