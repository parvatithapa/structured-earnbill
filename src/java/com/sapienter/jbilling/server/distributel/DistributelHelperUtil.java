package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public abstract class DistributelHelperUtil {

    private DistributelHelperUtil() {}

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String UPDATE_SQL = "UPDATE %s SET status = ? WHERE id = ?";

    public static void updateRequestStatus(DistributelPriceUpdateRequest request, String status, String tableName) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        // update status on request.
        jdbcTemplate.update(String.format(UPDATE_SQL, tableName), status, request.getId());
        BigDecimal price = request.getNewOrderLinePrice();
        if(BigDecimal.ZERO.compareTo(price)!=0) {
            logger.debug("Price {} updated on order {} for product {}", request.getNewOrderLinePrice(),
                    request.getOrderId(), request.getProductId());
        } else {
            logger.debug("Price reversed on order {} for product {}", request.getOrderId(),
                    request.getProductId());
        }
    }
}
