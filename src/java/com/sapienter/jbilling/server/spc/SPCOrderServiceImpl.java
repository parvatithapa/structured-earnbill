package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

/**
 * SPC Client specific OrderServiceImpl class to convert the Event Date time zone.
 * 
 * @author Ashwinkumar
 * @since 19-Sep-2019
 *
 */
public class SPCOrderServiceImpl extends OrderServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected Date getTimeZonedEventDate(Date eventDate, Integer entityId) {
        // get company level time zone
        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(entityId);
        Date newDate = Date.from(Instant.ofEpochMilli(eventDate.getTime()).atZone(ZoneId.of(companyTimeZone)).toLocalDateTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        logger.debug(
                "SPCOrderServiceImpl - company id : {}, time zone : {}, event date : {}, converted event date : {}",
                entityId, companyTimeZone, eventDate, newDate);
        return newDate;
    }

    @Override
    protected OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist, String processId, Integer entityId, PricingField[] pricingFields){
        return OrderBL.getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, persist, processId, entityId, getAssetIdentifier(pricingFields));
    }

    private String getAssetIdentifier(PricingField[] pricingFields) {
        List<PricingField> pricingList = Arrays.asList(pricingFields);
        PricingField assetIdentifier = PricingField.find(pricingList, SPCConstants.FROM_NUMBER);
        if(null == assetIdentifier) {
            assetIdentifier = PricingField.find(pricingList, SPCConstants.USER_NAME);
        }
        return null != assetIdentifier ? assetIdentifier.getStrValue() : null;
    }

}
