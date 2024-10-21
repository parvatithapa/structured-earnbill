package com.sapienter.jbilling.server.spc;

import java.util.ArrayList;
import java.util.List;

import lombok.ToString;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;

@ToString
public class PlanBasedFreeCallInfo {
    private String planCode;
    private List<String> itemCodes;
    private Long freeCallCount;
    private List<Integer> items;

    public PlanBasedFreeCallInfo(String planCode, Long freeCallCount, List<String> itemCodes, Integer entityId) {
        Assert.notNull(planCode, "planCode can not be null");
        Assert.notNull(freeCallCount, "freeCallCount can not be null");
        Assert.isTrue(CollectionUtils.isNotEmpty(itemCodes), "itemCodes can not be null or empty");
        this.planCode = planCode;
        this.freeCallCount = freeCallCount;
        this.itemCodes = itemCodes;
        validate(entityId);
    }

    public String getPlanCode() {
        return planCode;
    }

    public List<String> getPlanItems() {
        return itemCodes;
    }

    public Long getFreeCallCount() {
        return freeCallCount;
    }

    /**
     * validates planCode and items.
     * @param entityId
     */
    private void validate(Integer entityId) {
        ItemDAS itemDAS = new ItemDAS();
        ItemDTO planItem = itemDAS.findItemByInternalNumber(planCode, entityId);
        if(null == planItem) {
            throw new SessionInternalError("invalid planCode "+ planCode + " passed");
        }
        PlanDTO plan = planItem.firstPlan();
        if(null == plan) {
            throw new SessionInternalError("planCode "+ planCode + "is not plan, it is product only");
        }
        items = new ArrayList<>();
        for(String itemCode : itemCodes) {
            ItemDTO item = itemDAS.findItemByInternalNumber(itemCode, entityId);
            if(null == item) {
                throw new SessionInternalError("invalid plan item " + itemCode + " entry found for plan "+ planCode);
            }
            if(!plan.doesPlanHaveItem(item.getId())) {
                throw new SessionInternalError("product "+ itemCode + " is not part of plan "+ planCode);
            }
            items.add(item.getId());
        }
    }

    public List<Integer> getItems() {
        return items;
    }
}
