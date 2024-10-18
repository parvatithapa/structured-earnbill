package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.search.SearchResult;
import jbilling.RouteService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

/**
 * Created by aman on 1/3/16.
 */
public class UsageLimitPricingStrategy extends AbstractPricingStrategy {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UsageLimitPricingStrategy.class));

    public static final String PARAM_UPPER_LIMIT = "upper_limit";
    public static final String PARAM_LOWER_LIMIT = "lower_limit";
    public static final String PARAM_USAGE_LIMIT_TABLE = "usage_limit_table";

    public static final String DATA_TABLE_COLUMN_MONTH = "month";
    public static final String DATA_TABLE_COLUMN_EXPECTED_USAGE = "expected_usage";
    public static final String DATA_TABLE_COLUMN_EXPECTED_TIER_RATE = "expected_tier_rate";
    public static final String DATA_TABLE_COLUMN_UNDERAGE_TIER_RATE = "underage_tier_rate";
    public static final String DATA_TABLE_COLUMN_OVERAGE_TIER_RATE = "overage_tier_rate";

    public static final String DATE_FORMAT = "MM/yyyy";

    public UsageLimitPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_UPPER_LIMIT, INTEGER, true),
                new AttributeDefinition(PARAM_LOWER_LIMIT, INTEGER, true),
                new AttributeDefinition(PARAM_USAGE_LIMIT_TABLE, STRING, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
        setVariableUsagePricing(true);
    }

    private RouteService routeService;

    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        if (pricingOrder == null || pricingOrder.getUser() == null) {
            LOG.debug("User not found.");
            result.setPrice(BigDecimal.ZERO);
            return;
        }
        String upperLimitPercentage = planPrice.getAttributes().get(PARAM_UPPER_LIMIT).toString();
        String lowerLimitPercentage = planPrice.getAttributes().get(PARAM_LOWER_LIMIT).toString();

        String usageTableName = planPrice.getAttributes().get(PARAM_USAGE_LIMIT_TABLE).toString();

        Integer entityId = pricingOrder.getUser().getCompany().getId();

        // Check data table exists
        RouteDTO usageLimitTable = new RouteDAS().getRoute(entityId, usageTableName);

        if (usageLimitTable == null) {
            LOG.debug("Pricing Model configuration issue : Data tables not found. \n usage Limit Table : %s ", usageLimitTable);
            result.setPrice(BigDecimal.ZERO);
            return;
        }

        routeService = Context.getBean(Context.Name.ROUTE_SERVICE);

        SearchResult<String> records = fetchDataTableRecord(pricingOrder.getActiveUntil(), usageLimitTable.getId());
        if (records.getRows().size() == 0) {
            LOG.error("No record found in \""+usageLimitTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT).format(pricingOrder.getActiveUntil()));
            throw new SessionInternalError("No record found in \""+usageLimitTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT).format(pricingOrder.getActiveUntil()));
        }
        BigDecimal expectedUsage = routeService.fetchData(records, usageLimitTable, DATA_TABLE_COLUMN_EXPECTED_USAGE, BigDecimal.class);

        if (expectedUsage == null || expectedUsage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SessionInternalError("(Usage Limit Pricing Model) Expected usage is not valid : " + expectedUsage);
        }
        BigDecimal upperPercentage = new BigDecimal(upperLimitPercentage);
        BigDecimal lowerPercentage = new BigDecimal(lowerLimitPercentage);

        BigDecimal rate = null;
        if (quantity.compareTo(expectedUsage.multiply(upperPercentage.divide(new BigDecimal(100)))) > 0) {
            rate = routeService.fetchData(records, usageLimitTable, DATA_TABLE_COLUMN_OVERAGE_TIER_RATE, BigDecimal.class);
        } else if (quantity.compareTo(expectedUsage.multiply(lowerPercentage.divide(new BigDecimal(100)))) < 0) {
            rate = routeService.fetchData(records, usageLimitTable, DATA_TABLE_COLUMN_UNDERAGE_TIER_RATE, BigDecimal.class);
        } else {
            rate = routeService.fetchData(records, usageLimitTable, DATA_TABLE_COLUMN_EXPECTED_TIER_RATE, BigDecimal.class);
        }
        result.setPrice(rate);
    }

    private SearchResult<String> fetchDataTableRecord(Date effectiveDate, Integer usageLimitTableId) {
        Map<String, String> filters = new HashMap<String, String>();
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        String date = df.format(effectiveDate);
        filters.put(DATA_TABLE_COLUMN_MONTH, date);
        return routeService.getFilteredRecords(usageLimitTableId, filters);
    }


    public void validate(PriceModelDTO priceModel) {
        String upperLimitPercentage = priceModel.getAttributes().get(PARAM_UPPER_LIMIT);
        if (StringUtils.isNumeric(upperLimitPercentage)) {
            Integer upper = Integer.parseInt(upperLimitPercentage);
            if (upper < 100) {
                throw new SessionInternalError("Upper limit should be greater than  or equal to 100",
                        new String[]{"bean.UsageLimitPricingStrategy.upper.limit.less.than.100"});
            }
        }
        String lowerLimitPercentage = priceModel.getAttributes().get(PARAM_LOWER_LIMIT).toString();
        if (StringUtils.isNumeric(lowerLimitPercentage)) {
            Integer lower = Integer.parseInt(lowerLimitPercentage);
            if (lower > 100) {
                throw new SessionInternalError("Lower limit should be less than or equal to 100",
                        new String[]{"bean.UsageLimitPricingStrategy.lower.limit.greater.than.100"});
            }
        }
    }
}
