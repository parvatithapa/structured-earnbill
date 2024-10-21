package com.sapienter.jbilling.server.adennet.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserRowMapper {
    private String loginName;
    private String status;
    private String governorate;
}
