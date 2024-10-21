package com.sapienter.jbilling.server.distributel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;

@Transactional
public class PriceRecordValidator {

    private static final String USER_NOT_FOUND = "User [%s] not found!";
    private static final String ORDER_NOT_FOUND = "Order [%s] not found!";
    private static final String PRODUCT_NOT_FOUND = "Product [%s] not found!";
    private static final String PRODUCT_NOT_ON_ORDER_FOUND = "Product [%s] not found on Order [%s]!";

    public void validate(DistributelPriceUpdateRequest record) {
        List<String> errors = new ArrayList<>();
        if(!isUserPresent(record.getCustomerId())) {
            errors.add(String.format(USER_NOT_FOUND, record.getCustomerId()));
        }
        boolean isOrderFound = isOrderPresent(record.getOrderId());
        if(!isOrderFound) {
            errors.add(String.format(ORDER_NOT_FOUND, record.getOrderId()));
        }
        if(!isProductPresent(record.getProductId())) {
            errors.add(String.format(PRODUCT_NOT_FOUND, record.getProductId()));
        }
        if(isOrderFound && !isProductPresentOnOrder(record.getOrderId(), record.getProductId())) {
            errors.add(String.format(PRODUCT_NOT_ON_ORDER_FOUND, record.getProductId(), record.getOrderId()));
        }
        if(CollectionUtils.isNotEmpty(errors)) {
            String errorMessage = errors.stream()
                    .collect(Collectors.joining(","));
            throw new InvalidProductPriceUpdateRequestException(errorMessage);

        }

    }

    private boolean isUserPresent(Integer userId) {
        return (null != new UserDAS().findNow(userId));
    }

    private boolean isProductPresent(Integer productId) {
        return (null != new ItemDAS().findNow(productId));
    }

    private boolean isOrderPresent(Integer orderId) {
        return (null != new OrderDAS().findNow(orderId));
    }

    private boolean isProductPresentOnOrder(Integer orderId, Integer productId) {
        OrderDTO order = new OrderBL(orderId).getDTO();
        return order.getLines()
                .stream()
                .anyMatch(orderLine -> orderLine.getItemId().equals(productId));
    }
}
