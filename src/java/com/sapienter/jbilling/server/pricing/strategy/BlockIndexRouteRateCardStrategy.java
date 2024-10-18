package com.sapienter.jbilling.server.pricing.strategy;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jbilling.RouteService;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchResult;

/**
 * The customer pays one rate for usage up to a threshold. All usage above the threshold gets billed at a second rate
 * and usage below the threshold gets subtracted at the same rate. The second rate will determined by using a route rate card.
 *
 * [Threshold] = Number of units that can be used during a period (cycle) that will be charged at the fixed rate.
 * [Rate1] = Fixed rate used for the usage under the established [Threshold] in effect for the duration of the contract.
 * [Rate2] = Rate to be used for the usage above the [Threshold] during the period.  This rate will change regularly.  The rate to be used is the rate in effect on the invoicing date.
 * [Usage] = Usage during the billing period in kWh
 *
 * Usage Charge = [Block Usage Charge] + [Index Usage Charge]
 *
 * [Block Usage Charge] = [Threshold] x [Rate1]
 * [Index Usage Charge] = ([Usage] - [Threshold]) x [Rate2]
 *
 * @author Gerhard Maree
 * @since 30-11-2015
 */
public class BlockIndexRouteRateCardStrategy extends RouteBasedRateCardPricingStrategy {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RouteBasedRateCardPricingStrategy.class));

    public static final String PARAM_BLOCK_QUANTITY = "block_quantity";
    public static final String PARAM_BLOCK_RATE = "block_rate";
    public static final String PARAM_ROUTE_RATE_CARD_ID = "route_rate_card_id";
    public static final String OVERAGE_MCP_FACTOR = "overage_mcp_factor";
    public static final String OVERAGE_FEE_PER_UNIT = "overage_fee_per_unit";
    public static final String OVERAGE_FEE = "overage_fee";
    public static final String UNDERAGE_MCP_FACTOR = "underage_mcp_factor";
    public static final String UNDERAGE_FEE_PER_UNIT = "underage_fee_per_unit";
    public static final String UNDERAGE_FEE = "underage_fee";
    public static final String MARKET_CLEARING_FEE = "market_clearing_fee";


    private Integer entityId;
    private OrderDTO order;

    private BigDecimal residualUsage;
    private BigDecimal usageQuantity;

    public static final String PARAM_EVENT_DATE = "event_date";

    public BlockIndexRouteRateCardStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_ROUTE_RATE_CARD_ID, INTEGER, true),
                new AttributeDefinition(PARAM_BLOCK_QUANTITY, DECIMAL, true),
                new AttributeDefinition(PARAM_BLOCK_RATE, DECIMAL, true),
                new AttributeDefinition(MARKET_CLEARING_FEE, STRING, false),
                new AttributeDefinition(OVERAGE_MCP_FACTOR, DECIMAL, false),
                new AttributeDefinition(OVERAGE_FEE_PER_UNIT, DECIMAL, false),
                new AttributeDefinition(OVERAGE_FEE, DECIMAL, false),
                new AttributeDefinition(UNDERAGE_MCP_FACTOR, DECIMAL, false),
                new AttributeDefinition(UNDERAGE_FEE_PER_UNIT, DECIMAL, false),
                new AttributeDefinition(UNDERAGE_FEE, DECIMAL, false),
                new AttributeDefinition(PARAM_EVENT_DATE, STRING, true)

                );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
                );

        setRequiresUsage(false);
        setVariableUsagePricing(false);
    }

    private RouteService routeService;

    private BigDecimal calculateMCPCharges(PriceModelDTO planPrice,Boolean overageUsed){
        LOG.debug("Calculating MCP Charges");
        //Calculating MCP charges

        BigDecimal blockQty = new BigDecimal(planPrice.getAttributes().get(PARAM_BLOCK_QUANTITY));
        BigDecimal mcpCharge=BigDecimal.ZERO;
        String marketClearingPriceTableId = planPrice.getAttributes().get(MARKET_CLEARING_FEE);

        BigDecimal mcpFactor=BigDecimal.ZERO;
        BigDecimal marketCleaningPrice=BigDecimal.ZERO;
        //if market clearing data table not defined then calculate returning zero
        if(StringUtils.isNotBlank(marketClearingPriceTableId)){
            RouteDTO marketClearingPriceTable = new RouteDAS().getRoute(entityId, marketClearingPriceTableId);
            if(marketClearingPriceTable==null){
                LOG.debug("Configuration issue : No data table configure with name "+marketClearingPriceTableId);
                throw new SessionInternalError("Configuration issue : No data table configure with name "+marketClearingPriceTableId);
            }

            try{
                SearchResult<String> resultData = fetchTableData(order.getActiveUntil(), marketClearingPriceTable.getId(), true);
                marketCleaningPrice = routeService.fetchData(resultData, marketClearingPriceTable, "zone", BigDecimal.class);
            }catch (Exception e){
                LOG.debug(e.getMessage());
                LOG.debug("No Market clearing fee found for date "+order.getActiveUntil());
                throw new SessionInternalError("Searching Market clearing fee for date "+order.getActiveUntil()+" : " +e.getMessage());
            }
        }

        //Calculating MCP factor
        if(overageUsed){
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(OVERAGE_MCP_FACTOR))){
                mcpFactor=new BigDecimal(planPrice.getAttributes().get(OVERAGE_MCP_FACTOR));
            }
        }else{
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(UNDERAGE_MCP_FACTOR))){
                mcpFactor= new BigDecimal(planPrice.getAttributes().get(UNDERAGE_MCP_FACTOR));
            }
        }

        LOG.debug("MCP component: mcpFactor : "+mcpFactor+", marketCleaningPrice : "+marketCleaningPrice);
        // calculating MCP charge
        //if MCP charges or MCP factor is defined then calculate MCP charges else MCP charges will ne 0
        if(marketCleaningPrice.compareTo(BigDecimal.ZERO)!=0 || mcpFactor.compareTo(BigDecimal.ZERO)!=0){
            mcpCharge=usageQuantity.subtract(blockQty);

            if(mcpFactor.compareTo(BigDecimal.ZERO)!=0){
                mcpCharge=mcpCharge.multiply(mcpFactor);
            }
            if(marketCleaningPrice.compareTo(BigDecimal.ZERO)!=0){
                mcpCharge=mcpCharge.multiply(marketCleaningPrice);
            }
        }

        return mcpCharge;
    }

    private BigDecimal calculateUsageFree(PriceModelDTO planPrice, Boolean overageUsed){
        LOG.debug("Calculating Usage Fee");
        BigDecimal usageFee=BigDecimal.ZERO;

        if(overageUsed){
            LOG.debug("overage Used : "+planPrice.getAttributes().get(OVERAGE_FEE));
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(OVERAGE_FEE))){
                usageFee=new BigDecimal(planPrice.getAttributes().get(OVERAGE_FEE));
            }
        }else{
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(UNDERAGE_FEE))){
                usageFee=new BigDecimal(planPrice.getAttributes().get(UNDERAGE_FEE));
            }
        }
        return usageFee;
    }

    private BigDecimal calculateIndexUsageCharges(PriceModelDTO planPrice, Boolean overageUsed){
        LOG.debug("Calculating Index Usage Fee");
        BigDecimal indexUsageCharges=null;
        if(overageUsed){
            BigDecimal unitPrice=null;
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(OVERAGE_FEE_PER_UNIT))){
                unitPrice=new BigDecimal(planPrice.getAttributes().get(OVERAGE_FEE_PER_UNIT));
            }

            LOG.debug("OverAge fee : "+unitPrice);
            if(unitPrice==null){
                unitPrice=calculateOverageFeeFromCustomer();
            }

            LOG.debug("Adder fee : "+unitPrice);

            if(unitPrice!=null){
                LOG.debug("Multiplying unit price with quantity");
                indexUsageCharges=residualUsage.multiply(unitPrice);
            }

        }else{
            if(StringUtils.isNotBlank(planPrice.getAttributes().get(UNDERAGE_FEE_PER_UNIT))){
                indexUsageCharges=residualUsage.multiply(new BigDecimal(planPrice.getAttributes().get(UNDERAGE_FEE_PER_UNIT)));
            }
        }

        return indexUsageCharges;

    }

    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {
        Map<FupKey, BigDecimal> fupResult = calculateFreeUsageQty(orderLineDTO, result, quantity);

        quantity = fupResult.get(FupKey.NEW_QTY);
        usageQuantity=quantity;
        BigDecimal blockQty = new BigDecimal(planPrice.getAttributes().get(PARAM_BLOCK_QUANTITY));
        BigDecimal blockRate = new BigDecimal(planPrice.getAttributes().get(PARAM_BLOCK_RATE));

        if (pricingOrder == null || pricingOrder.getUser() == null) {
            LOG.debug("User not found.");
            result.setPrice(BigDecimal.ZERO);
            return;
        }

        entityId = pricingOrder.getUser().getCompany().getId();
        order=pricingOrder;

        //caculating the residual usage
        boolean overageUsed=false;
        if(blockQty.compareTo(quantity) > 0) {
            residualUsage = blockQty.subtract(quantity);
        } else if(blockQty.compareTo(quantity) < 0){
            overageUsed = true;
            residualUsage = quantity.subtract(blockQty);
        }else {
            //if block quantity is equal to the usage quantity then return block price.
            result.setPrice(blockRate);
            return;
        }

        routeService = Context.getBean(Context.Name.ROUTE_SERVICE);
        //Calculating index usage fee
        BigDecimal calculatedPrice = BigDecimal.ZERO;
        BigDecimal indexUsageCharges=calculateIndexUsageCharges(planPrice, overageUsed);
        //if index usage charges in null then find price in route rate card
        if(indexUsageCharges==null){

            List<PricingField> pricingFields=new ArrayList<PricingField>(fields);
            String pricingFieldName=planPrice.getAttributes().get(PARAM_EVENT_DATE);
            PricingField.add(pricingFields, new PricingField(pricingFieldName, pricingOrder.getActiveUntil()));

            indexUsageCharges=calculatePrice(pricingOrder, result, pricingFields, planPrice, residualUsage);
            LOG.debug("indexUsageCharges : "+indexUsageCharges);
            if(indexUsageCharges==null){
                LOG.debug("No Index usage charge found ");
                result.setPrice(BigDecimal.ZERO);
                return;
            }
            if(!overageUsed){
                indexUsageCharges=indexUsageCharges.negate();
            }
        }

        // Calculating block Price
        BigDecimal blockPrice=blockRate.multiply(blockQty);
        LOG.debug("Block Price : "+blockPrice);

        //Calculating MCP Charges
        BigDecimal mcpCharges;
        mcpCharges=calculateMCPCharges(planPrice, overageUsed);

        // Calculating usage fee
        BigDecimal usageFee=calculateUsageFree(planPrice, overageUsed);
        LOG.debug("usageFee : "+usageFee);

        LOG.debug("BlockIndex price component: usageFee :"+usageFee+"indexUsageCharges : "+indexUsageCharges+", mcpCharges : "+mcpCharges+"blockPrice : "+blockPrice);

        calculatedPrice = blockPrice.add(indexUsageCharges).add(usageFee).add(mcpCharges);

        LOG.debug("Calculated Price : "+calculatedPrice);
        calculatedPrice = calculatedPrice.max(BigDecimal.ZERO);
        calculateUnitPrice(result, fupResult.get(FupKey.NEW_QTY), fupResult.get(FupKey.FREE_QTY), calculatedPrice);

    }

    private SearchResult<String> fetchTableData(Date effectiveDate, Integer tableId, boolean filterTime) {

        String dateFormat = filterTime?"dd/MM/YY HH24:MI":"dd/MM/yyyy";
        String dateConversionMethod = filterTime?"to_timestamp":"to_date";
        Object value = filterTime?new java.sql.Timestamp(effectiveDate.getTime()):new java.sql.Date(effectiveDate.getTime());
        Map<String, Map<String, Object>> filters = new HashMap<String, Map<String, Object>>();
        Map<String, Object> startDateFilter = new HashMap<String, Object>();
        startDateFilter.put("key", dateConversionMethod+"(start_date,'"+dateFormat+"')");
        startDateFilter.put("value", value);
        startDateFilter.put("constraint", Filter.FilterConstraint.LE.toString());

        Map<String, Object> endDateFilter = new HashMap<String, Object>();
        endDateFilter.put("key", dateConversionMethod+"(end_date,'"+dateFormat+"')");
        endDateFilter.put("value", value);
        endDateFilter.put("constraint", Filter.FilterConstraint.GE.toString());

        filters.put("date_1", startDateFilter);
        filters.put("date_2", endDateFilter);

        return routeService.getFilteredRecords(tableId, filters);
    }

    private BigDecimal calculateOverageFeeFromCustomer(){
        BigDecimal rate=null;

        // finding adder-fee form ait metafield
        Set<CustomerAccountInfoTypeMetaField> customerAccountInfoTypeMetaFields=order.getUser().getCustomer().getCustomerAccountInfoTypeMetaFields();
        for (CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField : customerAccountInfoTypeMetaFields) {
            MetaFieldValue metaField = customerAccountInfoTypeMetaField.getMetaFieldValue();
            if (FileConstants.ADDER_FEE_METAFIELD_NAME.equals(metaField.getField().getName())) {
                rate=(BigDecimal) metaField.getValue();
            }
        }

        LOG.debug("AIT Adder fee : "+rate);

        if(rate==null){
            MetaFieldValue metaFieldValue=order.getUser().getCustomer().getMetaField(FileConstants.ADDER_FEE_METAFIELD_NAME);
            if(metaFieldValue!=null && metaFieldValue.getValue()!=null){
                rate=(BigDecimal) metaFieldValue.getValue();
            }
        }

        return rate;
    }
}
