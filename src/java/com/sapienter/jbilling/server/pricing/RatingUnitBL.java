package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.db.IncrementUnit;
import com.sapienter.jbilling.server.pricing.db.PriceUnit;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDAS;
import com.sapienter.jbilling.server.pricing.db.RatingUnitDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *  Rating Unit BL
 *
 *  @author Panche Isajeski
 *  @since 27-Aug-2013
 */
public class RatingUnitBL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RatingUnitDTO ratingUnitDTO = null;
    private RatingUnitDAS ratingUnitDAS = null;

    private void init() {
        ratingUnitDAS = new RatingUnitDAS();
        ratingUnitDTO = new RatingUnitDTO();
    }

    public RatingUnitWS getWS() {
        return getWS(ratingUnitDTO);
    }
    public static final RatingUnitWS getWS(RatingUnitDTO dto){
    	
    	RatingUnitWS ws = new RatingUnitWS();
        ws.setId(dto.getId());
        ws.setEntityId(dto.getCompany().getId());
        ws.setName(dto.getName());
        ws.setPriceUnitName(dto.getPriceUnit().getName());
        ws.setIncrementUnitName(dto.getIncrementUnit().getName());
        ws.setIsCanBeDeleted(dto.isCanBeDeleted());
        ws.setIncrementUnitQuantityAsDecimal(dto.getIncrementUnit().getQuantity());
        return ws;
    }
    
    public static final RatingUnitDTO getDTO(RatingUnitWS ws,Integer entityId) {

        RatingUnitDTO ratingUnitDTO = new RatingUnitDTO();
        if (ws.getId() != null && ws.getId() > 0) {
            ratingUnitDTO.setId(ws.getId());
        }

        ratingUnitDTO.setCompany(new CompanyDTO(entityId));
        ratingUnitDTO.setName(ws.getName());

        PriceUnit priceUnit = new PriceUnit();
        priceUnit.setName(ws.getPriceUnitName());
        ratingUnitDTO.setPriceUnit(priceUnit);

        IncrementUnit incrementUnit = new IncrementUnit();
        incrementUnit.setName(ws.getIncrementUnitName());
        incrementUnit.setQuantity(ws.getIncrementUnitQuantityAsDecimal());
        ratingUnitDTO.setIncrementUnit(incrementUnit);

        return ratingUnitDTO;
    }

    public RatingUnitBL() {
        init();
    }

    public RatingUnitBL(Integer ratingUnitId) {
        init();
        setRatingUnit(ratingUnitId);
    }

    public void setRatingUnit(Integer ratingUnitId) {
        ratingUnitDTO = ratingUnitDAS.find(ratingUnitId);
    }

    public RatingUnitDTO getRatingUnit() {
        return ratingUnitDTO;
    }

    public boolean delete() {

        if (ratingUnitDTO.isCanBeDeleted()) {
            if(!ratingUnitDAS.isRatingUnitUsed(ratingUnitDTO.getId())) {
                ratingUnitDAS.delete(ratingUnitDTO);
                return true;
            } else {
                throw new SessionInternalError("Rating unit is in use, it can not be deleted", new String[]{"ratingUnit.in.use.can.not.deleted"});
            }
        }

        return false;
    }

    public RatingUnitDTO create(RatingUnitDTO ratingUnitDTO) {

        ratingUnitDTO = ratingUnitDAS.save(ratingUnitDTO);

        ratingUnitDAS.flush();
        ratingUnitDAS.clear();
        return ratingUnitDTO;
    }

    public void update(RatingUnitDTO ratingUnit) {

        RatingUnitDTO ratingUnitDTO = ratingUnitDAS.find(ratingUnit.getId());

        ratingUnitDTO.setIncrementUnit(ratingUnit.getIncrementUnit());
        ratingUnitDTO.setPriceUnit(ratingUnit.getPriceUnit());
        ratingUnitDTO.setName(ratingUnit.getName());

        ratingUnitDAS.save(ratingUnitDTO);

        ratingUnitDAS.flush();
        ratingUnitDAS.clear();
    }

    public static RatingUnitDTO getDefaultRatingUnit(Integer entityId) {
        return new RatingUnitDAS().getDefaultRatingUnit(entityId);
    }
    /**
     * Returns the given list of PriceModelDTO entities as WS objects.
     *
     * @param dtos list of PriceModelDTO to convert
     * @return plan prices as WS objects, or an empty list if source list is empty.
     */
    public static List<RatingUnitWS> getWS(List<RatingUnitDTO> ratingUnits) {
        if (ratingUnits == null) {
            return Collections.emptyList();
        }
        List<RatingUnitWS> ws = new ArrayList<RatingUnitWS>(ratingUnits.size());
        for (RatingUnitDTO ratingUnit : ratingUnits)
            ws.add(getWS(ratingUnit));
        return ws;
    }

    /**
     * Returns the given pricing time-line sorted map of PriceModelDTO entities as WS objects.
     *
     * @param dtos map of PriceModelDTO to convert
     * @return plan prices as WS objects, or an empty map if source map is empty.
     */
    public static SortedMap<Date, RatingUnitWS> getWS(SortedMap<Date, RatingUnitDTO> ratingUnits) {

        SortedMap<Date, RatingUnitWS> ws = new TreeMap<Date, RatingUnitWS>();

        if (ratingUnits == null) return ws;

        for (Map.Entry<Date, RatingUnitDTO> entry : ratingUnits.entrySet())
            ws.put(entry.getKey(), getWS(entry.getValue()));

        return ws;
    }
    public static SortedMap<Date, RatingUnitDTO> getDTO(SortedMap<Date, RatingUnitWS> ws) {
        SortedMap<Date, RatingUnitDTO> dto = new TreeMap<Date, RatingUnitDTO>();

        for (Map.Entry<Date, RatingUnitWS> entry : ws.entrySet())
            dto.put(entry.getKey(), getDTO(entry.getValue(), entry.getValue().getEntityId()));

        return dto;
    }

    public static RatingUnitDTO getRatingUnitForDate(SortedMap<Date, RatingUnitDTO> ratingUnits,Date date) {
        if (ratingUnits == null || ratingUnits.isEmpty()) {
            logger.debug("prices null or empty.");
            return null;
        }

        if (date == null) {
            logger.debug("returning first price from the prices list");
            return ratingUnits.get(ratingUnits.firstKey());
        }

        // list of prices in ordered by start date, earliest first
        // return the model with the closest start date
        Date forDate = CommonConstants.EPOCH_DATE;
        if (ratingUnits.firstKey().before(CommonConstants.EPOCH_DATE) ) {
            //Additionall, Epoch Date is irrelavent in the this case
            forDate= ratingUnits.firstKey();
        }
        logger.debug("First key " + ratingUnits.firstKey() + ", Price required for " + forDate);

        for (Date start : ratingUnits.keySet()) {
            if (start != null && start.after(date)) {
                logger.debug(start + " is after expected price date of " + date);
                break;
            }

            forDate = start;
        }
        logger.debug("For date is set to " + forDate + ", returning: " + (forDate != null ? ratingUnits.get(forDate) : ratingUnits.get(ratingUnits.firstKey())) );
        return forDate != null ? ratingUnits.get(forDate) : ratingUnits.get(ratingUnits.firstKey());
    }
    public static RatingUnitWS getWsRatingUnitForDate(SortedMap<Date, RatingUnitWS> ratingUnits, Date date) {
        if (ratingUnits == null || ratingUnits.isEmpty()) {
            return null;
        }

        if (date == null) {
            return ratingUnits.get(ratingUnits.firstKey());
        }

        // list of prices in ordered by start date, earliest first
        // return the model with the closest start date
        Date forDate = null;
        for (Date start : ratingUnits.keySet()) {
            if (start != null && start.after(date))
                break;

            forDate = start;
        }

        return forDate != null ? ratingUnits.get(forDate) : ratingUnits.get(ratingUnits.firstKey());
    }
    public static RatingUnitDTO getDefaultRatingUnitDTO() {
        RatingUnitDTO model = new RatingUnitDTO();

        PriceUnit priceUnit = new PriceUnit();
        priceUnit.setName("UNIT");
        IncrementUnit incrementUnit = new IncrementUnit();
        incrementUnit.setName("UNIT");
        incrementUnit.setQuantity(BigDecimal.ONE);

        model.setName("RATING.UNIT.IDENTITY");
        model.setIncrementUnit(incrementUnit);
        model.setPriceUnit(priceUnit);

        return model;
    }
}
