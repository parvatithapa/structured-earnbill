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

package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.FlatPricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import org.apache.commons.validator.routines.BigDecimalValidator;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Brian Cowdery
 * @since 06-08-2010
 */
public class PriceModelBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PriceModelBL.class));

    /**
     * Returns the given PriceModelDTO entity as a WS object
     *
     * @param dto PriceModelDTO to convert
     * @return plan price as a WS object, null if dto is null
     */


    /**
     * Returns the given list of PriceModelDTO entities as WS objects.
     *
     * @param dtos list of PriceModelDTO to convert
     * @return plan prices as WS objects, or an empty list if source list is empty.
     */
    public static List<PriceModelWS> getWS(List<PriceModelDTO> dtos) {
        if (dtos == null)
            return Collections.emptyList();

        List<PriceModelWS> ws = new ArrayList<PriceModelWS>(dtos.size());
        for (PriceModelDTO planPrice : dtos)
            ws.add(getWS(planPrice));
        return ws;
    }

    /**
     * Returns the given pricing time-line sorted map of PriceModelDTO entities as WS objects.
     *
     * @param dtos map of PriceModelDTO to convert
     * @return plan prices as WS objects, or an empty map if source map is empty.
     */
    public static SortedMap<Date, PriceModelWS> getWS(SortedMap<Date, PriceModelDTO> dtos) {

        SortedMap<Date, PriceModelWS> ws = new TreeMap<Date, PriceModelWS>();

        if (dtos == null) return ws;

        for (Map.Entry<Date, PriceModelDTO> entry : dtos.entrySet())
            ws.put(entry.getKey(), getWS(entry.getValue()));

        return ws;
    }

    /**
     * Returns the given WS object as a PriceModelDTO entity. This method
     * does not perform any saves or updates, it only converts between the
     * two data structures.
     *
     * @param ws web service object to convert
     * @return PriceModelDTO entity, null if ws is null
     */
    public static PriceModelDTO getDTO(PriceModelWS ws) {
        if (ws != null) {
            PriceModelDTO root = null;
            PriceModelDTO model = null;

            for (PriceModelWS next = ws; next != null; next = next.getNext()) {
                if (model == null) {
                    model = root = new PriceModelDTO(next, new CurrencyBL(next.getCurrencyId()).getEntity());
                } else {
                    model.setNext(new PriceModelDTO(next, new CurrencyBL(next.getCurrencyId()).getEntity()));
                    model = model.getNext();
                }
            }

            return root;
        }
        return null;
    }

    /**
     * Returns the given list of WS objects as a list of PriceModelDTO entities.
     *
     * @param ws list of web service objects to convert
     * @return list of converted PriceModelDTO entities, or an empty list if source list is empty.
     */
    public static List<PriceModelDTO> getDTO(List<PriceModelWS> ws) {
        if (ws == null)
            return Collections.emptyList();

        List<PriceModelDTO> dto = new ArrayList<PriceModelDTO>(ws.size());
        for (PriceModelWS price : ws)
            dto.add(getDTO(price));
        return dto;
    }

    /**
     * Returns the given pricing time-line sorted map of WS objects as a list of PriceModelDTO entities.
     *
     * @param ws map of web service objects to convert
     * @return map of converted PriceModelDTO entities, or an empty map if source is empty.
     */
    public static SortedMap<Date, PriceModelDTO> getDTO(SortedMap<Date, PriceModelWS> ws) {
        SortedMap<Date, PriceModelDTO> dto = new TreeMap<Date, PriceModelDTO>();

        for (Map.Entry<Date, PriceModelWS> entry : ws.entrySet())
            dto.put(entry.getKey(), getDTO(entry.getValue()));

        return dto;
    }


    /**
     * Validates that the given pricing model has all the required attributes and that
     * the given attributes are of the correct type.
     *
     * @param models pricing models to validate
     * @throws SessionInternalError if attributes are missing or of an incorrect type
     */
    public static void validateAttributes(Collection<PriceModelDTO> models) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for (PriceModelDTO model : models) {
            for (PriceModelDTO next = model; next != null; next = next.getNext()) {
                try {
                    AttributeUtils.validateAttributes(next.getAttributes(), next.getStrategy());
                    validateRate(next);
                    model.getStrategy().validate(next);
                } catch (SessionInternalError e) {
                    errors.addAll(Arrays.asList(e.getErrorMessages()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Price model attributes failed validation.",
                                           errors.toArray(new String[errors.size()]));
        }
    }

    public static void validateAttributes(PriceModelDTO model) throws SessionInternalError {
        validateAttributes(Arrays.asList(model));
    }

    /**
     * Validates that the given pricing model WS object has all the required attributes and that
     * the given attributes are of the correct type.
     *
     * @param models pricing model WS objects to validate
     * @throws SessionInternalError if attributes are missing or of an incorrect type
     */
    public static void validateWsAttributes(Collection<PriceModelWS> models) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for (PriceModelWS model : models) {
            for (PriceModelWS next = model; next != null; next = next.getNext()) {
                try {
                    PriceModelStrategy type = PriceModelStrategy.valueOf(next.getType());
                    AttributeUtils.validateAttributes(next.getAttributes(), type.getStrategy());
                    validateModel(next);
                } catch (SessionInternalError e) {
                    errors.addAll(Arrays.asList(e.getErrorMessages()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Price model attributes failed validation.",
                                           errors.toArray(new String[errors.size()]));
        }
    }

    public static void validateWsAttributes(PriceModelWS model) {
        validateWsAttributes(Arrays.asList(model));
    }


    /**
     * Searches through the list of PriceModelDTO objects for the price that is active
     * on the given date.
     *
     * If the given date is null, or if the closest date could not be determined,
     * the first price will be returned.
     *
     * @param prices price models to search through
     * @param date date to find price for
     * @return found price for date, or null if no price found
     */
    public static PriceModelDTO getPriceForDate(SortedMap<Date, PriceModelDTO> prices, Date date) {
        if (prices == null || prices.isEmpty()) {
        	LOG.debug("prices null or empty.");
            return null;
        }

        if (date == null) {
        	LOG.debug("returning first price from the prices list");
            return prices.get(prices.firstKey());
        }

        // list of prices in ordered by start date, earliest first
        // return the model with the closest start date
        Date forDate = CommonConstants.EPOCH_DATE;
        if (prices.firstKey().before(CommonConstants.EPOCH_DATE) ) {
        	//Additionall, Epoch Date is irrelavent in the this case
        	forDate= prices.firstKey();
        }
        LOG.debug("First key " + prices.firstKey() + ", Price required for " + forDate);

        for (Date start : prices.keySet()) {
            if (start != null && start.after(date)) {
            	LOG.debug(start + " is after expected price date of " + date);
                break;
            }

            forDate = start;
        }
        LOG.debug("For date is set to " + forDate + ", returning: " + (forDate != null ? prices.get(forDate) : prices.get(prices.firstKey())) );
        return forDate != null ? prices.get(forDate) : prices.get(prices.firstKey());
    }

    /**
     * Searches through the list of PriceModelWS objects for the price that is active
     * on the given date.
     *
     * If the given date is null, or if the closest date could not be determined,
     * the first price will be returned.
     *
     * @param prices price models to search through
     * @param date date to find price for
     * @return found price for date, or null if no price found
     */
    public static PriceModelWS getWsPriceForDate(SortedMap<Date, PriceModelWS> prices, Date date) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }

        if (date == null) {
            return prices.get(prices.firstKey());
        }

        // list of prices in ordered by start date, earliest first
        // return the model with the closest start date
        Date forDate = null;
        for (Date start : prices.keySet()) {
            if (start != null && start.after(date))
                break;

            forDate = start;
        }

        return forDate != null ? prices.get(forDate) : prices.get(prices.firstKey());
    }

    public static void validateModel(PriceModelDTO model) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for (PriceModelDTO next = model; next != null; next = next.getNext()) {
            try {
                validateRate(next);
            } catch (SessionInternalError e) {
                errors.addAll(Arrays.asList(e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Price model failed validation.",
                    errors.toArray(new String[errors.size()]));
        }
    }

    public static void validateModel(PriceModelWS model) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for(PriceModelWS next = model; next != null; next = next.getNext()) {
            try {
                PriceModelDTO dto = getDTO(model);
                validateRate(dto);
            } catch (SessionInternalError e) {
                errors.addAll(Arrays.asList(e.getErrorMessages()));
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Price model failed validation.",
                    errors.toArray(new String[errors.size()]));
        }
    }

    public static void validateRate(PriceModelDTO dto){
        validateRate(dto.getRate(), dto.getStrategy().getClass().getSimpleName());
    }

    public static void validateRate(BigDecimal rate, String strategyName){
        //validate rate for range
        if (rate != null) {
            BigDecimalValidator validator = new BigDecimalValidator();
            Double min = 0d;
            Double max = 999999999999.9999999999d; //12 integer, 10 fraction
            boolean teaserOrFlatPricing = TeaserPricingStrategy.class.getSimpleName().equals(strategyName) ||
                FlatPricingStrategy.class.getSimpleName().equals(strategyName);

            LOG.debug("Rate : " + rate + " IsInRange : " + validator.isInRange(rate, min, max));

            // Negative prices are available for Teaser and Flat pricing models
            if ((teaserOrFlatPricing && !validator.maxValue(rate, max)) ||
                (!teaserOrFlatPricing && !validator.isInRange(rate, min, max))) {
                    throw new SessionInternalError("", new String[]{
                        strategyName+",rate,validation.error.invalid.rate.or.fraction"
                    });
                }
        }
    }

    public static PriceModelWS getWS (PriceModelDTO model) {
        PriceModelWS ws = new PriceModelWS();
        CurrencyDTO currency = model.getCurrency();
        ws.setId(model.getId());
        ws.setAttributes(model.getAttributes());
        if(currency!= null) {
            ws.setCurrencyId(currency.getId());
            ws.setCurrencySymbol(currency.getSymbol());
        }
        ws.setRate(model.getRate());
        if (model.getType() != null ) ws.setType(model.getType().name()) ;
        if (model.getNext() != null) ws.setNext(new PriceModelWS(PriceModelBL.getWS(model.getNext()))) ;
        return new PriceModelWS(ws);
    }
    /**
     *   Return the type of the Pricing
     *   model.
     *
     * @param ws
     * @return
     */
    public static final PriceModelStrategy getTypeEnum(PriceModelWS ws) {
        if (ws.getType() == null) {
            // todo: check if the type is okay
            return PriceModelStrategy.ZERO;
        }
        return PriceModelStrategy.valueOf(ws.getType());
    }

    /**
     * Searches through the list of PriceModelDTO objects to check if the customer defined
     * PrciceModel is effective or not.
     *
     * @param prices
     * @param pricingDate
     * @return true if Customer defined price model is effective for the given date.
     */
    public static boolean containsEffectiveModel(SortedMap<Date, PriceModelDTO> prices, Date pricingDate) {

        if (prices == null || prices.isEmpty()) {
            LOG.debug("prices null or empty.");
            return false;
        }

        Date start = prices.keySet()
                           .stream()
                           .filter(date -> date != null && date.compareTo(pricingDate) <= 0)
                           .findFirst()
                           .orElse(null);

        if (start != null) {
            LOG.debug("Effective model found for date this " + pricingDate);
            return true;
        }

        return false;
    }

    public static boolean isAllowedToUpdateOrderChange(ItemDTO item, Integer entityId, Date startDate) {
        if (item != null) {
            PriceModelDTO priceModel = item.getPrice(startDate, entityId);

            while (priceModel != null) {
                if (!priceModel.getType().isAllowedToUpdateOrderChange()) {
                    return false;
                }

                priceModel = priceModel.getNext();
            }
        }

        return true;
    }

    public static PriceModelWS getPrice(ItemDTO item, Date today, Integer entityId) {
        if (item == null) {
            throw new SessionInternalError("Please Provide Item!");
        }
        PriceModelDTO priceModelDTO = item.getPrice(today, entityId);
        return priceModelDTO != null ? PriceModelBL.getWS(priceModelDTO) : null;
    }
}
