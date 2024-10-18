package com.sapienter.jbilling.server.ratingUnit.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingUnit {

    public static RatingUnit NONE = RatingUnit.builder().id(-1).build();

    private String name;
    private int id;
    private BigDecimal incrementUnitQuantity;
}
