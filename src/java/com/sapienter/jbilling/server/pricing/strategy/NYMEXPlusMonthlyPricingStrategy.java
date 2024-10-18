package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL;
import com.sapienter.jbilling.server.pricing.RouteRateCardRecord;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;

/**
 * Created by aman on 20/6/16.
 */
public class NYMEXPlusMonthlyPricingStrategy extends DayAheadPricingStrategy {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NYMEXPlusMonthlyPricingStrategy.class));
    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("MM/dd/yyyy");
    private Date from = null;
    private Date to = null;
    private BigDecimal totalUsage;
    private String zone;
//    private BigDecimal totalUsageByBreakPoint = new BigDecimal(BigInteger.ZERO);

    public NYMEXPlusMonthlyPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(ADDER_FEE, DECIMAL, false),
                new AttributeDefinition(PARAM_ROUTE_RATE_CARD_ID, INTEGER, true)
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
    }

    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice,
                        BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {
        fields = new LinkedList<PricingField>(fields);
        if (pricingOrder == null || pricingOrder.getUser() == null) {
            LOG.debug("User not found.");
            if (result != null) {
                result.setPrice(BigDecimal.ZERO);
            }
            return;
        }
        CustomerDTO customerDTO = pricingOrder.getUser().getCustomer();

        // Fetch zone meta field from customer
        MetaFieldValue zoneMetaField = customerDTO.getMetaField(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME);
        if (zoneMetaField == null || zoneMetaField.getValue() == null) {
            throw new SessionInternalError("Customer should belongs to a Zone");
        }
        //Fetch date
        from = pricingOrder.getActiveSince();
        LOG.debug("Mete read start date "+from);
        //excluding first date of the meter read because start date will be excluded for nymax
        from=new DateTime(from).plusDays(1).toDate();
        LOG.debug("start date of the period after exclusion of first day :  "+from);
        to = pricingOrder.getActiveUntil();
        if (to == null) {
            LOG.debug("Active Until Date cannot be blank for Nymex pricing model.");
            throw new SessionInternalError("Active Until Date cannot be blank for Nymex pricing model.");
        }

        LOG.debug("Start Date " + from);
        LOG.debug("End Date " + to);
        // Adding 1 to including active until date


        //additional fields(zone) for searching
        zone = zoneMetaField.getValue().toString();
        PricingField zonePricingField = new PricingField(zoneMetaField.getField().getName(), zoneMetaField.getValue().toString());
        fields.add(zonePricingField);

        //calculate FUP quantities
        Map<FupKey, BigDecimal> fupResult = calculateFreeUsageQty(pricingOrder, result, quantity);
        totalUsage = fupResult.get(FupKey.NEW_QTY);


        BigDecimal adderFee = calculateAdderFee(customerDTO, planPrice);
        LOG.debug("adderFee:" + adderFee);

        BigDecimal blendedRate = BigDecimal.ZERO;
        blendedRate = getPrice(planPrice, adderFee, fields);
        //[Adjusted Rate] = [blendedRate] + [Adder fee]

        //calculate unit price
        calculateUnitPrice(result, quantity, fupResult.get(FupKey.FREE_QTY), blendedRate);
    }

    /*
    * This method will calculate usage ratio for different spans divided by Break Point.
    *       For example, From : 27 Jan, To : 10 Feb. Total usage : 1000.
    *       And route rate card has rows :
    *       1 Jan - 1 Feb : 0.01
    *       1 Feb - 1 Mar : 0.02 NOTE : End date(right date is exclusive)
    * Here 1 Feb is break point.
    *
    * Usage will be divided as :
    *
    * Usage for 27 Jan to 31 Jan
    * (27 Jan - 31 Jan)*1000/(27 Jan - 10 Feb)
    * => 5*1000/15
    * => 333.33333333
    *
    * Usage for 1 Feb to 10 Feb
    * (1 Feb - 10 Feb)*1000/(27 Jan - 10 Feb)
    * => 10*1000/15
    * => 666.6666666667
    *
    * Rate will be calculated as :
    * For example, From : 27 Jan, To : 5 Feb.
    * Firstly it will pass 27 Jan as input and get January month rate. In that row, end date is 31 Jan. If that date is smaller or equal than TO date
    * which is 5 Feb, it will search rate for 1 Feb then. It will return rate 0.02
    *
    * Total price : (333.33333333*0.01)+(666.666666666*0.02)
    * */
    protected BigDecimal getPrice(PriceModelDTO pricingModel, BigDecimal adderFee, List<PricingField> fields) {
        BigDecimal price = new BigDecimal(BigInteger.ZERO);

        RouteRateCardDTO rateCard = getRoute(pricingModel, fields, PARAM_ROUTE_RATE_CARD_ID);
        if (rateCard == null) {
            throw new SessionInternalError("Route rate card defined in pricing model not found");
        }

        int numberOfDays = Days.daysBetween(new DateTime(from), new DateTime(to)).getDays()+1;
        LOG.debug("Number of days between start and end date : " + numberOfDays);
        BigDecimal usageByDay = totalUsage.divide(new BigDecimal(numberOfDays), MathContext.DECIMAL64);
        LOG.debug("Average usage per day : " + usageByDay);

        // Get column name for effective_date in route rate card. So that value can be tokenized
        // and deduce the end date from there. End date will help to find the break point.
        String columnName = getColumnName(rateCard);

        Date searchDate = from;
        BigDecimal totalUsageByBreakPoint = new BigDecimal(BigInteger.ZERO);
        while (searchDate != null) {
            try {
                // Bind search date as pricing field based on break point.
                PricingField.add(fields, new PricingField(EFFCTIVE_DATE, searchDate));

                // Search route rate card record for date
                RouteRateCardRecord record = findRouteRateCardRow(rateCard, fields);
                if (record == null) {
                    throw new SessionInternalError("No record found for date : " + searchDate + " and zone : "+ zone);
                }
                Date recordEndDate = getEndDate(record, columnName);

                LOG.debug("Record End Date : " + recordEndDate);

                // Get Usage
                BigDecimal usageUpToBreakPoint = null;
                if (searchDate.compareTo(recordEndDate) == 0) {
                    throw new SessionInternalError("Issue in route rate card. End date in Date range is exclusive");
                }
                if (recordEndDate != null && recordEndDate.compareTo(to) <= 0) {

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(recordEndDate);
                    cal.add(Calendar.DATE, -1);
                    Date breakPoint = cal.getTime();

                    LOG.debug("Found a break point : " + breakPoint);
                    LOG.debug("From : " + searchDate);
                    LOG.debug("To : " + breakPoint);


                    int days = Days.daysBetween(new DateTime(searchDate), new DateTime(breakPoint)).getDays()+1;
                    LOG.debug("Number of days up to break point : " + days);

                    usageUpToBreakPoint = usageByDay.multiply(new BigDecimal(days));

                    // check end date in row and make it new date.
                    searchDate = recordEndDate;

                    totalUsageByBreakPoint = totalUsageByBreakPoint.add(usageUpToBreakPoint);
                } else {
                    LOG.debug("Usage up to break point : " + totalUsageByBreakPoint);
                    usageUpToBreakPoint = totalUsage.subtract(totalUsageByBreakPoint);
                    searchDate = null;
                }

                LOG.debug("Usage up to break point : " + usageUpToBreakPoint);

                // Get rate
                BigDecimal rate = record.getCharge();
                if(rate==null){
                    throw new SessionInternalError("No rate found for date : " + searchDate + " and zone : "+ zone + " in table");
                }
                LOG.debug("Rate for this span from Route rate card : " + rate);


                price = calculateCharge(price, rate, adderFee, usageUpToBreakPoint);
            } catch (Exception e) {

                from = null;
                LOG.error("Exception occurred while processing route rate card for date :" + searchDate, e);
                throw new SessionInternalError("Exception occurred while processing route rate card for date :" + searchDate, e);
            }
        }
        return price;
    }

    public BigDecimal calculateCharge(BigDecimal price, BigDecimal rate, BigDecimal adderFee, BigDecimal usageUpToBreakPoint) {
        LOG.debug("adderFee:" + adderFee);
        rate = rate.add(adderFee);
        LOG.debug("Rate after adding adder fee for this span : " + rate);
        rate = rate.multiply(usageUpToBreakPoint);
        LOG.debug("Amount for this span : " + rate);
        return price.add(rate);
    }

    protected RouteRateCardDTO getRoute(PriceModelDTO pricingModel, List<PricingField> fields, String routeRateCardAttrName) {
        SortedMap<Integer, String> routeLabels = getRoutes(pricingModel.getAttributes());
        LOG.debug("Route table mapping" + routeLabels);

        if (null == fields) {
            fields = new ArrayList<PricingField>(2);
        } else {
            fields = new ArrayList<PricingField>(fields);
        }
        resolveRoutes(fields, routeLabels);

        // get and validate attributes
        Integer routeRateCardId = AttributeUtils.getInteger(pricingModel.getAttributes(), routeRateCardAttrName);

        if (routeRateCardId == null) {
            throw new SessionInternalError("No route rate card Id found in pricing model");
        }
        RouteRateCardDAS rateCardDAS = new RouteRateCardDAS();
        return rateCardDAS.find(routeRateCardId);
    }

    protected RouteRateCardRecord findRouteRateCardRow(RouteRateCardDTO rateCard, List<PricingField> fields) {
        //use RouteBasedRateCardFinder to resolve the row
        //include route information
        RouteBasedRateCardBL rateCardBL = new RouteBasedRateCardBL(rateCard);


        RouteRateCardRecord routeRecordFound = rateCardBL.getBeanFactory().getFinderInstance().findRouteRecord(rateCard, fields);

        LOG.debug("Record resolved " + routeRecordFound);

        return routeRecordFound;
    }

    // It will fetch the column name from matching field which is bind with effective_date
    private String getColumnName(RouteRateCardDTO routeRateCardDTO) {
        Set<MatchingFieldDTO> fields = routeRateCardDTO.getMatchingFields();
        if (fields != null) {
            Optional<MatchingFieldDTO> matchingField = fields.stream().filter((MatchingFieldDTO field) -> EFFCTIVE_DATE.equals(field.getMediationField())).findFirst();
            if (matchingField.isPresent()) {
                return matchingField.get().getMatchingField();
            }
        }
        return null;
    }


    // Get from-to date from route rate card and extract end date from there.
    private Date getEndDate(RouteRateCardRecord record, String columnName) {

        String activeDateRange = record.getAttributes().get(columnName);
        //tokenize date range and get end date
        String[] dateRange = activeDateRange.split("-");
        if (dateRange != null && dateRange.length > 1) {
            return dateFormat.parseDateTime(dateRange[1]).toDate();
        } else {
            throw new SessionInternalError("Date field is not valid in route rate card : " + activeDateRange);
        }
    }
}
