/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import java.math.BigDecimal;
import java.util.*;

/**
 * PlanItemDTO business logic. Currently only contains static factory methods for converting
 * PlanItemDTO objects into their web-service equivalents.
 *
 * @author Brian Cowdery
 * @since 20-09-2010
 */
public class PlanItemBL {

    private PlanItemDAS planItemDas;

    public PlanItemBL() {
        init();
    }

    private void init() {
        this.planItemDas = new PlanItemDAS();
    }

    /**
     * Convert a given PlanItemWS web-service object into a PlanItemDTO.
     *
     * The PlanItemWS web-service object must have an affected item id, price model
     * and price model currency id or an exception will be thrown.
     *
     * @param ws ws object to convert
     * @return converted DTO object
     * @throws SessionInternalError if required field is missing
     */
    public static PlanItemDTO getDTO(PlanItemWS ws) {
        if (ws != null) {
            if (ws.getItemId() == null)
                throw new SessionInternalError("PlanItemWS must have an affected item.");

            // affected item
            ItemDTO item = new ItemBL(ws.getItemId()).getEntity();

            // price models
            SortedMap<Date, PriceModelDTO> models = PriceModelBL.getDTO(ws.getModels());

            // bundled items
            PlanItemBundleDTO bundle = PlanItemBundleBL.getDTO(ws.getBundle());

            return new PlanItemDTO(ws, item, models, bundle);
        }
        return null;
    }

    /**
     * Converts a list of PlanItemWS web-service objects into PlanItemDTO objects.
     *
     * @see #getDTO(PlanItemWS)
     *
     * @param objects ws objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     */
    public static List<PlanItemDTO> getDTO(List<PlanItemWS> objects) {
        List<PlanItemDTO> planItems = new ArrayList<PlanItemDTO>(objects.size());
        for (PlanItemWS ws : objects)
            planItems.add(getDTO(ws));
        return planItems;
    }


    /**
     * Convert a given PlanItemDTO into a PlanItemWS web-service object.
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static PlanItemWS getWS(PlanItemDTO dto) {
        if(null == dto) return null;

         PlanItemWS ws = new PlanItemWS();
    	 ws.setId(dto.getId());
         ws.setPrecedence(dto.getPrecedence());

         if (dto.getModels() != null) ws.setModels(PriceModelBL.getWS(dto.getModels()));
         if (dto.getBundle() != null) ws.setBundle(PlanItemBundleBL.getWS(dto.getBundle()));
         if (dto.getItem() != null) ws.setItemId(dto.getItem().getId());

        ws.setModel(PriceModelBL.getWsPriceForDate(ws.getModels(), TimezoneHelper.companyCurrentDate(dto.getItem().getEntityId())));
        return ws;
    }


    /**
     * Converts a list of PlanItemDTO objects into PlanItemWS web-service objects.
     *
     * @see #getWS(PlanItemDTO)
     *
     * @param objects dto objects to convert
     * @return a list of converted WS objects, or an empty list if dto objects list was empty.
     */
    public static List<PlanItemWS> getWS(List<PlanItemDTO> objects) {
        List<PlanItemWS> planItems = new ArrayList<PlanItemWS>(objects.size());
        for (PlanItemDTO dto : objects)
            planItems.add(PlanItemBL.getWS(dto));
        return planItems;
    }

    /**
     * Collects the individual plan items from all order lines that contain a plan. Used to
     * condense a list of "subscription order lines" down to the individual prices and bundled
     * items to be applied.
     *
     * @param lines order lines containing plan subscriptions
     * @return list of plan items from all plans in the order lines
     */
    public static Map<PlanItemDTO, BigDecimal> collectPlanItems(List<OrderLineDTO> lines) {
    	Map<PlanItemDTO, BigDecimal> planItems = new HashMap<PlanItemDTO, BigDecimal>();

        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0) {
                for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(line.getItemId())) {
                    if (plan.getEditable() == 0) {
                        for (PlanItemDTO planItem : plan.getPlanItems()) {
                            planItems.put(planItem, line.getQuantity());
                        }
                    }
                }
            }
        }

        return planItems;
    }

    public long findPlanItemsCountByItemId(int itemId) {
        return planItemDas.findPlanItemsCountByItemId(itemId);
    }

    /**
     * Collects the individual plan items from a plan, including nested plans
     *
     * @param plan for planItems retrieve
     * @return set of plan items from all plans (root and nested)
     */
    public static Set<PlanItemDTO> collectPlanItemsIncludingNested(PlanDTO plan) {
        Set<PlanItemDTO> result = new HashSet<PlanItemDTO>();
        for (PlanItemDTO currentPlanItem : plan.getPlanItems()) {
            result.add(currentPlanItem);
            if (currentPlanItem.getItem().isPlan()) {
                for (PlanDTO nestedPlan : currentPlanItem.getItem().getPlans()) {
                    result.addAll(collectPlanItemsIncludingNested(nestedPlan));
                }
            }
        }
        return result;
    }
}
