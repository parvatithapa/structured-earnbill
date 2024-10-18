//TODO MODULARIZATION: MOVE IN THE MEDIATION SERVICE
package com.sapienter.jbilling.server.mediation.converter.common.steps;/*
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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.pricing.cache.RouteFinder;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by marcomanzi on 2/27/14.
 */
public abstract class AbstractRootRouteItemResolutionStep<T> extends AbstractItemResolutionStep<T>{

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractRootRouteItemResolutionStep.class));

    protected Integer resolveItemIdByRootRoute(Integer mediationCfgId, List<PricingField> pricingFields, Integer entityId) {
        LOG.setLogLevel(Level.DEBUG);
        RouteDTO rootRoute = new RouteDAS().getRootRouteForMediation(mediationCfgId);
        if (rootRoute == null) {
            LOG.debug("Root route table should exists for mediation config: %1$s", mediationCfgId);
            return null;
        }

        RouteRecord routeRecord;
        try {
            routeRecord = determineRoute(pricingFields, rootRoute);
        } catch (Exception e) {
            LOG.debug("Exception at determining route : "+e);
            return null;
        }

        if (routeRecord == null) {
            LOG.debug("Route record not found for mediation config:" + mediationCfgId);
            return null;
        }

        String productCode = routeRecord.getProduct();
        if (productCode == null || productCode.length() == 0) {
            LOG.debug("Product code not resolved: "+routeRecord);
            return null;
        }
        LOG.debug("Product code : "+productCode + "   "  + entityId);
        //determine the product
        Map<String, Object> itemMap = resolveItemByInternalNumber(entityId, productCode);
        for (String key: itemMap.keySet()) {
            LOG.debug("Item Map : "+key + "   "  + itemMap.get(key));
        }
        Integer itemId = (Integer)itemMap.get(MediationStepResult.ITEM_ID);

        if(itemId == null) {
            LOG.info("Item not resolved for entity["+entityId+"] product code ["+productCode+"]: "+routeRecord);
            return null;
        }

        return itemId;
    }

    /**
     * Determine the final route for the call.
     *
     * @param fields
     * @param routeDTO
     * @return
     */
    private RouteRecord determineRoute(List<PricingField> fields, RouteDTO routeDTO) throws Exception {
        RouteBL routeBL= new RouteBL(routeDTO);
        RouteFinder routeFinder = routeBL.getBeanFactory().getFinderInstance();
        RouteDTO route = routeBL.getEntity();

        if ( routeFinder != null ) {
            if ( route.getMatchingFields().size() > 0 ) {
                return  routeFinder.findTreeRoute(route, fields);
            }
        }

        return null;
    }

}
