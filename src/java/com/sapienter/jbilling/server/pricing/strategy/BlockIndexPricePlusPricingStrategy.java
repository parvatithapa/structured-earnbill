package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.EDIFileBL;
import com.sapienter.jbilling.server.ediTransaction.EDIFileFieldWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask;
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
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchResult;
import jbilling.RouteService;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask.MeterReadField;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

/**
 * Created by aman on 17/1/16.
 */
public class BlockIndexPricePlusPricingStrategy extends AbstractPricingStrategy {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BlockIndexPricePlusPricingStrategy.class));

    private static final String PARAM_BLOCK_TABLE = "block_table";
    private static final String PARAM_TD_CHARGES = "T&D_charges";
    private static final String PARAM_MARKET_CLEARING_PRICE = "market_clearing_price";

    private static final String PARAM_OVERAGE_MCP_FACTOR = "overage_mcp_factor";
    private static final String PARAM_UNDERAGE_MCP_FACTOR = "underage_mcp_factor";

    // Rates applicable on usage other than block defined
    public static final String PARAM_OVERAGE_FEE_PER_UNIT = "overage_fee_per_unit";
    private static final String PARAM_UNDERAGE_FEE_PER_UNIT = "underage_fee_per_unit";

    /**
     * One time charge over different usage.
     * Like if customer used extra units, he has to pay $2 anyway.
     * No matter how much extra usage is.
     */
    private static final String PARAM_OVERAGE_FEE = "overage_fee";
    private static final String PARAM_UNDERAGE_FEE = "underage_fee";

    private RouteService routeService;
    private static final Integer MINUTES_IN_DAY = 1440;
    private PriceModelDTO planPrice;

    public static final String DATE_FORMAT_MM_YYYY = "MM/yyyy";
    public static final String DATE_FORMAT_DD_MM_YYYY = "dd/MM/yyyy";

    public BlockIndexPricePlusPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(PARAM_BLOCK_TABLE, STRING, true),
                new AttributeDefinition(PARAM_TD_CHARGES, STRING, false),           // Optional

                new AttributeDefinition(PARAM_MARKET_CLEARING_PRICE, STRING, true),
                new AttributeDefinition(PARAM_OVERAGE_MCP_FACTOR, DECIMAL, false),           // Optional
                new AttributeDefinition(PARAM_UNDERAGE_MCP_FACTOR, DECIMAL, false),           // Optional

                new AttributeDefinition(PARAM_OVERAGE_FEE_PER_UNIT, DECIMAL, true),
                new AttributeDefinition(PARAM_UNDERAGE_FEE_PER_UNIT, DECIMAL, true),

                new AttributeDefinition(PARAM_OVERAGE_FEE, DECIMAL, false),           // Optional
                new AttributeDefinition(PARAM_UNDERAGE_FEE, DECIMAL, false)          // Optional
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
        setUsesDynamicAttributes(false);
        setVariableUsagePricing(false);
    }


    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {

        this.planPrice = planPrice;
        String blockTableName = planPrice.getAttributes().get(PARAM_BLOCK_TABLE).toString();
        String TDChargesName = planPrice.getAttributes().get(PARAM_TD_CHARGES).toString();
        String marketClearingPriceTableName = planPrice.getAttributes().get(PARAM_MARKET_CLEARING_PRICE).toString();

        if (pricingOrder == null || pricingOrder.getUser() == null) {
            LOG.debug("User not found.");
            result.setPrice(BigDecimal.ZERO);
            return;
        }
        CustomerDTO customerDTO = pricingOrder.getUser().getCustomer();
        Integer entityId = pricingOrder.getUser().getCompany().getId();

        // Check data tables exists
        RouteDTO blockTable = new RouteDAS().getRoute(entityId, blockTableName);
        RouteDTO marketClearingPriceTable = new RouteDAS().getRoute(entityId, marketClearingPriceTableName);

        // If provided only then fetch. Not mandatory
        RouteDTO TDCharges = TDChargesName != null && !TDChargesName.trim().isEmpty() ? new RouteDAS().getRoute(entityId, TDChargesName) : null;

        // Block and merketClearingPrice are mandatory data tables. If T&D provided and name is not valid then it wrong.
        if (blockTable == null || marketClearingPriceTable == null || (TDChargesName != null && !TDChargesName.trim().isEmpty() && TDCharges == null)) {
            LOG.debug("Pricing Model configuration issue : Data tables not found. \n blockTable : %s \n TDCharges : %s \n marketClearingPriceTable : %s", blockTable, TDCharges, marketClearingPriceTable);
            throw new SessionInternalError(String.format("Pricing Model configuration issue : Data tables not found. \n" +
                    " blockTable : %s \n" +
                    " TDCharges : %s \n" +
                    " marketClearingPriceTable : %s", blockTable != null ? blockTable.getName() : blockTable, TDCharges != null ? TDCharges.getName() : TDCharges, marketClearingPriceTable != null ? marketClearingPriceTable.getName() : marketClearingPriceTable));
        }

        // Find meter read edi file id in order
        MetaFieldValue ediFileMetaField = pricingOrder.getMetaField(MeterReadParserTask.MeterReadField.edi_file_id.toString());
        Integer meterReadFileId = null;
        if (ediFileMetaField != null && ediFileMetaField.getValue() != null) {
            meterReadFileId = Integer.parseInt(ediFileMetaField.getValue().toString());
        }

        if (meterReadFileId == null || meterReadFileId == 0) {
            throw new SessionInternalError("Meter read file id not exist");
        }
        List<IntervalRecordInfo> intervals = processMeterReadFile(meterReadFileId);;

        // Fetch zone meta field from customer
        MetaFieldValue zoneMetaField = customerDTO.getMetaField(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME);
        if (zoneMetaField == null || zoneMetaField.getValue() == null) {
            throw new SessionInternalError("Customer should belongs to a Zone");
        }

        if (intervals.size() == 0) {
            LOG.debug("No interval record found. Meter read file: " + meterReadFileId);
            throw new SessionInternalError("No interval record found");
        }

        BigDecimal totalQuantity = intervals.stream().map(record -> record.getQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);
        LOG.debug("Rate quantity : " + totalQuantity);
        if (totalQuantity == null || totalQuantity.equals(BigDecimal.ZERO)) {
            LOG.debug("Total Quantity is zero. Meter read file: " + meterReadFileId);
            result.setPrice(BigDecimal.ZERO);
            return;
        }

        BigDecimal overageFeePerUnit = new BigDecimal(planPrice.getAttributes().get(PARAM_OVERAGE_FEE_PER_UNIT));
        BigDecimal underageFeePerUnit = new BigDecimal(planPrice.getAttributes().get(PARAM_UNDERAGE_FEE_PER_UNIT));

        BigDecimal total = intervals.stream()
                .map(record -> calculatePricePerInterval(record, blockTable, TDCharges, marketClearingPriceTable, overageFeePerUnit, underageFeePerUnit, zoneMetaField.getValue().toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LOG.debug("Rate calculated : " + total);
        if (total == null || total.equals(BigDecimal.ZERO)) {
            LOG.debug("Total is zero. Meter read file: " + meterReadFileId);
            result.setPrice(BigDecimal.ZERO);
            return;
        }

        result.setPrice(total.divide(totalQuantity, MathContext.DECIMAL64));
    }

    /*
    * Find meter read file from order
    * Then find interval records
    * Then find the dates and quantity
    * */
    private List<IntervalRecordInfo> processMeterReadFile(Integer meterReadFileId) {
        EDIFileWS meterRead = new EDIFileBL(meterReadFileId).getWS();
        if (meterRead == null) {
            throw new SessionInternalError("Meter read file not exist");
        }
        EDIFileRecordWS[] records = meterRead.getEDIFileRecordWSes();
        int index = 0;
        List<IntervalRecordInfo> recordInfos = new LinkedList<IntervalRecordInfo>();
        Integer intervalType = null;

        List<EDIFileRecordWS> UMRRecords = Arrays.asList(records).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals("UMR")).collect(Collectors.toList());
        EDIFileRecordWS umrRecord = UMRRecords.get(0);
        intervalType = (Integer) getEDIField(umrRecord, MeterReadField.INTERVAL_TYPE.toString(), "Integer", true);

        //Finding Interval QTY record
        List<EDIFileRecordWS> ediFileRecordWSes = Arrays.asList(records).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals("QTY")).collect(Collectors.toList());
        ediFileRecordWSes.remove(0);

        for (EDIFileRecordWS QTYRecord : ediFileRecordWSes) {

            Date activeSince = (Date) getEDIField(QTYRecord, MeterReadField.INTERVAL_DT.toString(), "Date", true);
            String time = (String) getEDIField(QTYRecord, MeterReadField.INTERVAL_TIME.toString(), null, true);
            BigDecimal quantity = (BigDecimal) getEDIField(QTYRecord, MeterReadField.TOTAL_CONSUMPTION.toString(), "BigDecimal", true);

            if (time.length() != 4) {
                throw new SessionInternalError("Time field in QTY record is not valid");
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(activeSince);
            cal.set(Calendar.HOUR, Integer.parseInt(time.substring(0, 2)));
            cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2, 4)));
            activeSince = cal.getTime();
            recordInfos.add(IntervalRecordInfo.getInstance(activeSince, quantity, intervalType));

        }

        return recordInfos;
    }

    private Object getEDIField(EDIFileRecordWS record, String fieldName, String className, boolean mandatory) {

        Optional<EDIFileFieldWS> value = Arrays.stream(record.getEdiFileFieldWSes()).filter(field ->
                field.getKey().equals(fieldName)).findFirst();

        String valueAsString = value.isPresent() ? (value.get().getValue()) : null;
        if ((valueAsString == null || valueAsString.isEmpty()) && mandatory == true) {
            throw new SessionInternalError("Mandatory field not found : \"" + fieldName + "\"");
        }
        if (className == null) return valueAsString;
        return MeterReadParserTask.formatValue(fieldName, valueAsString, className);
    }

    private BigDecimal calculatePricePerInterval(IntervalRecordInfo recordInfo, RouteDTO blockTable, RouteDTO TDCharges,
                                                 RouteDTO marketClearingPriceTable, BigDecimal excessUsageFee, BigDecimal shortageUsageFee, String zone) {
        routeService = Context.getBean(Context.Name.ROUTE_SERVICE);

//        Fetch [Daily Block Size] and [Block Unit Price]
        SearchResult<String> result = fetchTableData(recordInfo.getActiveSince(), blockTable.getId(), false);

        if (result.getRows().size() == 0) {
            LOG.error("No record found in \""+blockTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY).format(recordInfo.getActiveSince()));
            throw new SessionInternalError("No record found in \""+blockTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY).format(recordInfo.getActiveSince()));
        }
        Integer dailyBlockSize = routeService.fetchData(result, blockTable, "block_size", Integer.class);
        BigDecimal unitPrice = routeService.fetchData(result, blockTable, "unit_price", BigDecimal.class);

//        [Interval Block Price] = [Daily Block Size]/[No. of Daily Intervals] * [Block Unit Price]
        BigDecimal blockSize = new BigDecimal(dailyBlockSize).multiply(new BigDecimal(recordInfo.getIntervalType())).divide(new BigDecimal(MINUTES_IN_DAY), MathContext.DECIMAL64);
        BigDecimal intervalBlockPrice = unitPrice.multiply(blockSize);

        // If T&D is not provided then interval exist in record will be the actual usage
        // Else calculate T&D for effective usage
        BigDecimal grossUsage = null;
        if (TDCharges == null) {
            grossUsage = recordInfo.getQuantity();

        } else {
            // [T&D Adjustment] = (1 + [Unaccounted for Usage]) / ((1 - [Transmission Loss]) * (1 - [Distribution Loss]))
            result = fetchTDChargesData(recordInfo.getActiveSince(), TDCharges.getId(), zone);
            if (result.getRows().size() == 0) {
                LOG.error("No record found in \""+TDCharges.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_MM_YYYY).format(recordInfo.getActiveSince()));
                throw new SessionInternalError("No record found in \""+TDCharges.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_MM_YYYY).format(recordInfo.getActiveSince()));
            }
            BigDecimal TDAdjustmentCharges = routeService.fetchData(result, TDCharges, "adjustment_amount", BigDecimal.class);

            // [Interval Gross Up Usage] = [Interval Usage] * [T&D Adjustment]
            grossUsage = recordInfo.getQuantity().multiply(TDAdjustmentCharges);
        }

//      [Residual Usage] = [Interval Gross Up Usage] - ([Daily Block Size]/[No. of Daily Intervals]
        BigDecimal residualUsage = grossUsage.subtract(blockSize);

//        [Market Clearing Price]
        result = fetchTableData(recordInfo.getActiveSince(), marketClearingPriceTable.getId(), true);
        if (result.getRows().size() == 0) {
            LOG.error("No record found in \""+marketClearingPriceTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY).format(recordInfo.getActiveSince()));
            throw new SessionInternalError("No record found in \""+marketClearingPriceTable.getName()+"\" data table for date "+new SimpleDateFormat(DATE_FORMAT_DD_MM_YYYY).format(recordInfo.getActiveSince()));
        }
        BigDecimal marketCleaningPrice = routeService.fetchData(result, marketClearingPriceTable, "zone", BigDecimal.class);

//        [Interval Residual Energy Price] = [Residual Usage] * [Market Clearing Price]
        BigDecimal intervalResidualEnergyPrice = residualUsage.multiply(marketCleaningPrice);

        // If MCP factor is defined then multiply it with Residual Price
        BigDecimal overageMCPFactor = planPrice.getAttributes().get(PARAM_OVERAGE_MCP_FACTOR) != null &&  !planPrice.getAttributes().get(PARAM_OVERAGE_MCP_FACTOR).trim().isEmpty()? new BigDecimal(planPrice.getAttributes().get(PARAM_OVERAGE_MCP_FACTOR)) : null;
        BigDecimal underageMCPFactor = planPrice.getAttributes().get(PARAM_UNDERAGE_MCP_FACTOR) != null &&  !planPrice.getAttributes().get(PARAM_UNDERAGE_MCP_FACTOR).trim().isEmpty()? new BigDecimal(planPrice.getAttributes().get(PARAM_UNDERAGE_MCP_FACTOR)) : null;
        if (residualUsage.compareTo(BigDecimal.ZERO) > 0 && overageMCPFactor != null && overageMCPFactor.compareTo(BigDecimal.ZERO) > 0) {
            intervalResidualEnergyPrice = intervalResidualEnergyPrice.multiply(overageMCPFactor);
        } else if (residualUsage.compareTo(BigDecimal.ZERO) < 0 && underageMCPFactor != null && underageMCPFactor.compareTo(BigDecimal.ZERO) > 0) {
            intervalResidualEnergyPrice = intervalResidualEnergyPrice.multiply(underageMCPFactor);
        }


        BigDecimal excessUsagePrice = BigDecimal.ZERO;
        if (residualUsage.compareTo(BigDecimal.ZERO) > 0) {
            // [Excess Usage Fee] =  IF( [Residual Usage]>0,[Residual Usage] * [Excess Fee Rate],0)
            if(excessUsageFee.compareTo(BigDecimal.ZERO) > 0) excessUsagePrice = residualUsage.multiply(excessUsageFee);

            // One time overage fee. If defined then use it.
            if (planPrice.getAttributes().get(PARAM_OVERAGE_FEE) != null && !planPrice.getAttributes().get(PARAM_OVERAGE_FEE).trim().isEmpty()) {
                BigDecimal overageFee = new BigDecimal(planPrice.getAttributes().get(PARAM_OVERAGE_FEE));
                if (overageFee.compareTo(BigDecimal.ZERO) > 0) {
                    excessUsagePrice = excessUsagePrice.add(overageFee
                            .divide(new BigDecimal(MINUTES_IN_DAY)
                                    .divide(new BigDecimal(recordInfo.getIntervalType()), MathContext.DECIMAL64), MathContext.DECIMAL64));
                }
            }
        }

        BigDecimal shortageUsagePrice = BigDecimal.ZERO;
        if (residualUsage.compareTo(BigDecimal.ZERO) < 0) {
            // [Shortage Usage Fee] = IF( [Residual Usage]<0, [Residual Usage] * [Shortage Fee Rate], 0)
           if(shortageUsageFee.compareTo(BigDecimal.ZERO) > 0) shortageUsagePrice = residualUsage.multiply(shortageUsageFee).negate();
            // One time underage fee. If defined then use it.
            if (planPrice.getAttributes().get(PARAM_UNDERAGE_FEE) != null && !planPrice.getAttributes().get(PARAM_UNDERAGE_FEE).trim().isEmpty()) {
                BigDecimal underageFee = new BigDecimal(planPrice.getAttributes().get(PARAM_UNDERAGE_FEE));
                if (underageFee.compareTo(BigDecimal.ZERO) > 0) {
                    shortageUsagePrice = shortageUsagePrice.add(underageFee
                            .divide(new BigDecimal(MINUTES_IN_DAY)
                                    .divide(new BigDecimal(recordInfo.getIntervalType()), MathContext.DECIMAL64), MathContext.DECIMAL64));
                }
            }
        }

//        [Interval Total Fee] = [Excess Usage Fee] + [Shortage Usage Fee]
        BigDecimal intervalTotalFee = excessUsagePrice.add(shortageUsagePrice);
//        [Interval Total Price] = [Interval Block Price] + [Interval Residual Energy Price] + [Interval Total Fee]
        BigDecimal total = intervalBlockPrice.add(intervalResidualEnergyPrice).add(intervalTotalFee);
        LOG.debug("Rate calculated for interval : " + total);
        return total;
    }

    private SearchResult<String> fetchTableData(Date effectiveDate, Integer tableId, boolean filterTime) {

        String dateFormat = filterTime ? "dd/MM/YY HH24:MI" : DATE_FORMAT_DD_MM_YYYY;
        String dateConversionMethod = filterTime ? "to_timestamp" : "to_date";
        Object value = filterTime ? new java.sql.Timestamp(effectiveDate.getTime()) : new java.sql.Date(effectiveDate.getTime());
        Map<String, Map<String, Object>> filters = new HashMap<String, Map<String, Object>>();
        Map<String, Object> startDateFilter = new HashMap<String, Object>();
        startDateFilter.put("key", dateConversionMethod + "(start_date,'" + dateFormat + "')");
        startDateFilter.put("value", value);
        startDateFilter.put("constraint", Filter.FilterConstraint.LE.toString());

        Map<String, Object> endDateFilter = new HashMap<String, Object>();
        endDateFilter.put("key", dateConversionMethod + "(end_date,'" + dateFormat + "')");
        endDateFilter.put("value", value);
        endDateFilter.put("constraint", Filter.FilterConstraint.GE.toString());

        filters.put("date_1", startDateFilter);
        filters.put("date_2", endDateFilter);

        return routeService.getFilteredRecords(tableId, filters);
    }

    private SearchResult<String> fetchTDChargesData(Date effectiveDate, Integer TDChargesId, String zone) {
        Map<String, String> filters = new HashMap<String, String>();
        DateFormat df = new SimpleDateFormat(DATE_FORMAT_MM_YYYY);
        String date = df.format(effectiveDate);
        filters.put("month", date);
        filters.put("zone", zone);
        return routeService.getFilteredRecords(TDChargesId, filters);
    }

    private SearchResult<String> fetchMarketClearingPriceData(Date effectiveDate, Integer marketClearingPriceTableId) {
        Map<String, String> filters = new HashMap<String, String>();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String date = df.format(effectiveDate);
        filters.put("date", date);
        return routeService.getFilteredRecords(marketClearingPriceTableId, filters);
    }

    private static class IntervalRecordInfo {
        private Date activeSince;
        private BigDecimal quantity;
        private Integer intervalType;

        private IntervalRecordInfo(Date activeSince, BigDecimal quantity, Integer intervalType) {
            this.activeSince = activeSince;
            this.quantity = quantity;
            this.intervalType = intervalType;
        }

        public static IntervalRecordInfo getInstance(Date activeSince, BigDecimal quantity, Integer intervalType) {
            return new IntervalRecordInfo(activeSince, quantity, intervalType);
        }

        public Date getActiveSince() {
            return activeSince;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public Integer getIntervalType() {
            return intervalType;
        }
    }
}
