package com.sapienter.jbilling.server.pricing.strategy;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.DurationFieldType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.INTEGER;

/**
 * Created by neeraj on 14/6/16.
 */
public class LbmpPlusBlendedRatePricingStrategy extends DayAheadPricingStrategy {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(LbmpPlusBlendedRatePricingStrategy.class));

    private static final String EFFCTIVE_DATE="effective_date";
    public static final String ADDER_FEE = "adder_fee";

    public LbmpPlusBlendedRatePricingStrategy() {
        super();
    }

    BigDecimal calculateRate(OrderDTO pricingOrder, MetaFieldValue zoneMetaField, PricingResult result, BigDecimal quantity, List<PricingField> fields, PriceModelDTO planPrice, Date activeUntilDate){
        //additional fields(effectiveDate,zone) for searching
        PricingField zonePricingField=new PricingField(zoneMetaField.getField().getName(), zoneMetaField.getValue().toString());

        //calculate FUP quantities
        super.fupResult = calculateFreeUsageQty(pricingOrder, result, quantity);
        quantity = fupResult.get(FupKey.NEW_QTY);

        DateTime activeSince = new DateTime(pricingOrder.getActiveSince());
        DateTime activeUntil =new DateTime(pricingOrder.getActiveUntil());
        // Adding 1 to including active until date
        int days = Days.daysBetween(activeSince, activeUntil).getDays()+1;

        LOG.debug("Start Date " +activeSince);
        LOG.debug("End Date " +activeUntil);

        BigDecimal blendedRate=BigDecimal.ZERO;
        StringBuffer errorMessage=new StringBuffer();
        errorMessage.append("No rate find for these periods : ");

        Boolean hasError=false;
        SimpleDateFormat format=new SimpleDateFormat("MM/dd/yyyy");

        for (int i=0; i < days; i++) {
            List<PricingField> pricingFields = new ArrayList<PricingField>();
            pricingFields.add(zonePricingField);
            DateTime effectiveDate = activeSince.withFieldAdded(DurationFieldType.days(), i);
            LOG.debug("calculating rate for date " +effectiveDate);
            String effectiveStrDate = format.format(effectiveDate.toDate());
            pricingFields.add(new PricingField(EFFCTIVE_DATE, effectiveStrDate));
            BigDecimal price=BigDecimal.ZERO;
            try{
                /*blended rate will be calculated from route rate card.*/
                price = calculatePrice(pricingOrder, result, fields, pricingFields, planPrice, quantity, PARAM_ROUTE_RATE_CARD_ID);
            }catch (Exception e){
                LOG.debug("Error : " + e);
                hasError=true;
                errorMessage.append(effectiveStrDate+", ");
                continue;
            }

            LOG.debug("Price for date  " +effectiveDate +" : "+ price);
            if(price==null){
                errorMessage.append(effectiveStrDate+", ");
                hasError=true;
                continue;
            }

            blendedRate=blendedRate.add(price);
            LOG.debug("blendedRate : "+blendedRate);
        }

        if(hasError){
            throw new SessionInternalError(errorMessage.toString());
        }

        blendedRate=blendedRate.divide(new BigDecimal(days), MathContext.DECIMAL64);

        return blendedRate;

    }
}
