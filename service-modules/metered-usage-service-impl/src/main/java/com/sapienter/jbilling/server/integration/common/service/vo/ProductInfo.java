package com.sapienter.jbilling.server.integration.common.service.vo;

import java.util.Date;
import java.util.SortedMap;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductInfo {
    private int entityId;
    private int productId;
    private String description;
    private String productCode;
    private int languageId;
    private SortedMap<Date, List<ProductRatingConfigInfo>> ratingConfig;
}
