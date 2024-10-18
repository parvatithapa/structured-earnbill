package com.sapienter.jbilling.server.pricing.cache;
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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.ILoader;
import com.sapienter.jbilling.server.pricing.RouteBL;
import com.sapienter.jbilling.server.pricing.RouteRecord;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.*;

public class RouteFinder extends AbstractRouteFinder<RouteRecord, RouteDTO> {
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RouteFinder.class));

    public RouteFinder(JdbcTemplate template, ILoader loader) {
        super(template, loader);
    }

    public void init() {
        // noop
    }

    /**
     *  This method will be used to find the route based on the configured matching field.
     *
     * @param routeDTO Based on the algorithm and required property query is build to find the route.
     * @param pricingFields This is matching field (K,V) pair.K is the matching field column in the table and the V is the value that is entered in the test screen
     * @param callback Callback is used in finding result based on Match algorithm using EXACT AND BEST MATCH
     * @return Route Record Result
     * @throws Exception 
     */

    public RouteRecord findRoute(RouteDTO routeDTO, List<PricingField> pricingFields, MatchCallback callback) throws SessionInternalError {
        return findMatchingRecord(routeDTO, pricingFields);
    }

    public RouteRecord findRoute(RouteDTO routeDTO, List<PricingField> pricingFields) throws SessionInternalError {
        return findRoute(routeDTO, pricingFields, MatchCallback.DO_NOTHING);
    }

    public RouteRecord findTreeRoute(RouteDTO rootRoute, List<PricingField> pricingFields) throws SessionInternalError {
        CompanyDTO company = rootRoute.getCompany();
        RouteDTO route = rootRoute;
        RouteRecord routeRecord = null;
        RouteDAS routeDAS = new RouteDAS();

        boolean stop = false;
        while (!stop) {
            RouteBL routeBL = new RouteBL(route);
            RouteFinder routeFinder = routeBL.getBeanFactory().getFinderInstance();
            RouteRecord prevRouteRecord = routeRecord;
            routeRecord = routeFinder.findRoute(route, pricingFields);

            if(null == routeRecord) {
                //set the previous record since we did
                //not manage to find next route record
                routeRecord = prevRouteRecord;
                stop = true;
            } else {
                //check if this route produces output
                //if yes, place the output as a pricing field
                if(null != routeRecord.getRouteId() &&
                        !routeRecord.getRouteId().isEmpty()){

                    String name = route.getOutputFieldName();
                    if(null != name && !name.isEmpty()){
                        PricingField field = findFieldWithName(pricingFields, name);
                        String  fieldValue = "";
                        String routeOutput=routeRecord.getRouteId().trim();
                        if (routeOutput.substring(0,1).equals("@")){
                            PricingField pricingFieldSelectByOutput=findFieldWithName(pricingFields, routeOutput.substring(1, routeOutput.length()));
                            if (pricingFieldSelectByOutput!=null){
                                fieldValue=pricingFieldSelectByOutput.getStrValue();
                            }else{
                                throw new SessionInternalError("No pricing field found with the name : "+routeOutput);
                            }
                        }else{
                            fieldValue = routeRecord.getRouteId();
                        }
                        if(null == field){

                            PricingField newField = new PricingField();
                            newField.setName(name);
                            newField.setType(PricingField.Type.STRING);
                            newField.setPosition(Integer.valueOf(-1));
                            newField.setStrValue(fieldValue);

                            //complement the pricing fields with new information
                            PricingField.add(pricingFields, newField);
                        } else {
                            //field with that name already exists
                            //so override the existing value
                            field.setStrValue(fieldValue);
                        }
                    }
                }

                if(null != routeRecord.getProduct() &&
                        !routeRecord.getProduct().isEmpty()) {
                    stop = true; //we got it
                } else {
                    if(null != routeRecord.getNextRoute() &&
                            !routeRecord.getNextRoute().isEmpty()){
                        route = routeDAS.getRouteByName(company.getId(), routeRecord.getNextRoute());
                        if(null == route){
                            LOG.error("Current data table: %s, route record:%s. Can NOT find next route: %s",
                                    route.getName(), routeRecord.toString(), routeRecord.getNextRoute());
                        }
                    } else if (null != route.getDefaultRoute() &&
                            !route.getDefaultRoute().isEmpty()){
                        route = routeDAS.getRouteByName(company.getId(), route.getDefaultRoute());
                        if(null == route){
                            LOG.error("Current data table: %s. Can NOT find default route: %s",
                                    route.getName(), route.getDefaultRoute());
                        }
                    } else {
                        //no product, no next route, no default
                        //default route then stop processing
                        route = null;
                    }
                }

                if (null == route) {
                    LOG.debug("No next route was found. Route: %s, RouteRecord: %s",
                            (null != route ? route.toString() : "null"),
                            (null != routeRecord ? routeRecord.toString() : "null"));
                    stop = true;
                }
            }
        }

        //product is resolved, add a new field
        if(null != routeRecord &&
                null != routeRecord.getProduct() &&
                !routeRecord.getProduct().isEmpty()){
            PricingField field = new PricingField();
            field.setName("jb_product");
            field.setType(PricingField.Type.STRING);
            field.setStrValue(routeRecord.getProduct());
            PricingField.add(pricingFields, field);
        }

        return routeRecord;
    }

    private PricingField findFieldWithName(List<PricingField> fields, String name){
        for(PricingField field : fields){
            if(field.getName().compareTo(name) == 0){
                return field;
            }
        }
        return null;
    }

    protected RouteRecord buildRecord(SqlRowSet sqlRowSet, RouteDTO routeDTO) {

        RouteRecord routeRecord = new RouteRecord();
        routeRecord.setId(sqlRowSet.getInt("id"));
        routeRecord.setName(sqlRowSet.getString("name"));
        routeRecord.setRouteId(sqlRowSet.getString(CommonConstants.ROUTE_ID_FIELD_NAME));
        routeRecord.setNextRoute(sqlRowSet.getString(CommonConstants.NEXT_ROUTE_FIELD_NAME));
        routeRecord.setProduct(sqlRowSet.getString(CommonConstants.PRODUCT_FIELD_NAME));
        routeRecord.setRouteTable(routeDTO);
        routeRecord.setAttributes(buildAttributeMap(sqlRowSet,
                "id", "name",
                CommonConstants.ROUTE_ID_FIELD_NAME,
                CommonConstants.NEXT_ROUTE_FIELD_NAME,
                CommonConstants.PRODUCT_FIELD_NAME));

        return routeRecord;
    }
}
