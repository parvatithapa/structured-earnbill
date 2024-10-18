package com.sapienter.jbilling.server.fullcreative;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Component
@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class MediationQuantityHelperService {

    private static final FreeCallCounterResult NONE_RESULT = new FreeCallCounterResult(0, FreeCallCounterLimitLevelType.NONE);

    @ToString
    static class FreeCallCounterResult {
        Integer freeCallLimit;
        FreeCallCounterLimitLevelType type;

        FreeCallCounterResult(Integer freeCallLimit, FreeCallCounterLimitLevelType type) {
            this.freeCallLimit = freeCallLimit;
            this.type = type;
        }

    }

    enum FreeCallCounterLimitLevelType {
        CUSTOMER, PLAN, COMPANY, NONE
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private AssetDAS assetDAS;
    @Autowired
    private UserDAS userDAS;
    @Autowired
    private OrderLineDAS orderLineDAS;

    public int isQauntityExceeded(Integer userId, String assetIdentifier) {
        FreeCallCounterResult callLimitResult = getNumberOfFreeCalls(userId, assetIdentifier);
        logger.debug("found free call limit for user {} for asset identifier {} is {}", userId, assetIdentifier, callLimitResult);
        if(callLimitResult.type.equals(NONE_RESULT.type) ) {
            return -1;
        }
        Long freeCallCounter = getAllFreeCallCount(assetIdentifier, userId, callLimitResult.type);
        logger.debug("free call counter {} for user {} for asset identifier {}", freeCallCounter, userId, assetIdentifier);
        return freeCallCounter >= callLimitResult.freeCallLimit ? 1 : 0;
    }

    private FreeCallCounterResult getNumberOfFreeCalls(Integer userId, String assetIdentifier) {
        try {
            //preference : 1
            UserDTO user = userDAS.findNow(userId);
            Integer customersFreeCallLimit = user.getCustomer().getNumberOfFreeCalls();
            if(Optional.ofNullable(customersFreeCallLimit).orElse(0) != 0) {
                return new FreeCallCounterResult(customersFreeCallLimit, FreeCallCounterLimitLevelType.CUSTOMER);
            }

            //preference : 2
            if(StringUtils.isNotBlank(assetIdentifier)) {
                AssetDTO asset = assetDAS.findAssetsByIdentifier(assetIdentifier).get(0);
                OrderDTO subscriptionOrder = asset.getOrderLine().getPurchaseOrder();
                Optional<ItemDTO> planItem = subscriptionOrder.getLines()
                        .stream()
                        .map(OrderLineDTO::getItem)
                        .filter(Objects::nonNull)
                        .filter(ItemDTO::isPlan)
                        .findFirst();
                if(planItem.isPresent()) {
                    PlanDTO plan = planItem.get().getPlans().iterator().next();
                    if(Optional.ofNullable(plan.getNumberOfFreeCalls()).orElse(0) != 0) {
                        return new FreeCallCounterResult(plan.getNumberOfFreeCalls(), FreeCallCounterLimitLevelType.PLAN);
                    }
                }
            }

            //preference : 3
            Integer companyFreeCallLimit = user.getEntity().getNumberOfFreeCalls();
            if(Optional.ofNullable(companyFreeCallLimit).orElse(0) != 0) {
                return new FreeCallCounterResult(companyFreeCallLimit, FreeCallCounterLimitLevelType.COMPANY);
            }

            return NONE_RESULT;
        } catch(Exception ex) {
            throw new SessionInternalError("error in getNumberOfFreeCalls", ex);
        }
    }

    private Long getAllFreeCallCount(String assetIdentifier, Integer userId, FreeCallCounterLimitLevelType countLevelType) {
        try {
            Long callCounter;
            if(countLevelType.equals(FreeCallCounterLimitLevelType.PLAN)) {
                AssetDTO asset = assetDAS.findAssetsByIdentifier(assetIdentifier).get(0);
                OrderLineDTO orderLineDTO = orderLineDAS.find(asset.getOrderLine().getId());
                OrderDTO purchaseOrder = orderLineDTO.getPurchaseOrder();
                List<String> assetIdentifiers = getIdentifiersFromOrder(purchaseOrder);
                callCounter = orderLineDAS.findFreeCallCounterForAcitveMediatedOrderByAssetIdentifiers(assetIdentifiers);
            } else {
                callCounter = orderLineDAS.findFreeCallCounterForActiveMediatedOrderForUser(userId);
            }
            return callCounter == null ? 0L : callCounter;
        } catch(Exception ex) {
            throw new SessionInternalError("error in getAllFreeCallCount", ex);
        }
    }

    private List<String> getIdentifiersFromOrder(OrderDTO order) {
        List<String> assetIdentifiers = new ArrayList<>();
        for(OrderLineDTO orderLine : order.getLines()) {
            for(AssetDTO asset : orderLine.getAssets()) {
                assetIdentifiers.add(asset.getIdentifier());
            }
        }
        for(OrderDTO childOrder : order.getChildOrders()) {
            assetIdentifiers.addAll(getIdentifiersFromOrder(childOrder));
        }
        return assetIdentifiers;
    }

}
