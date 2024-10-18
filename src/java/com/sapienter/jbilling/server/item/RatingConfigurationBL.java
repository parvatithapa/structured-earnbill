package com.sapienter.jbilling.server.item;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.sapienter.jbilling.server.item.db.RatingConfigurationDTO;
import com.sapienter.jbilling.server.pricing.RatingUnitBL;
import com.sapienter.jbilling.server.usageratingscheme.service.UsageRatingSchemeBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

public class RatingConfigurationBL {



    private RatingConfigurationDTO ratingConfiguration;

    public RatingConfigurationBL(){

    }

    public RatingConfigurationBL(RatingConfigurationDTO ratingConfiguration){

        this.ratingConfiguration=ratingConfiguration;
    }


    public static RatingConfigurationDTO getDTO(RatingConfigurationWS ws){

        if (ws == null) return null;

        RatingConfigurationDTO ratingConfiguration=new RatingConfigurationDTO();
        ratingConfiguration.setRatingUnit(ws.getRatingUnit()!=null ? ws.getRatingUnit().getId():null);
        ratingConfiguration.setUsageRatingScheme(ws.getUsageRatingScheme()!=null ? ws.getUsageRatingScheme().getId():null);
        ratingConfiguration.setId(ws.getId());
        ratingConfiguration.setActive(true);
        return ratingConfiguration;

    }

    public static RatingConfigurationWS getWS(RatingConfigurationDTO dto){

        if(dto==null) return null;

        RatingConfigurationWS ratingConfiguration=new RatingConfigurationWS();
        ratingConfiguration.setRatingUnit(dto.getRatingUnit()!=null?new RatingUnitBL(dto.getRatingUnit()).getWS():null);
        ratingConfiguration.setUsageRatingScheme(dto.getUsageRatingScheme()!=null?new UsageRatingSchemeBL(dto.getUsageRatingScheme()).getWS():null);
        ratingConfiguration.setId(dto.getId());
        ratingConfiguration.setPricingUnit(getAllPricingUnitDescriptions(dto.getId()));

        return ratingConfiguration;
    }

    public static SortedMap<Date, RatingConfigurationDTO> convertMapWSToDTO(SortedMap<Date, RatingConfigurationWS> ws) {
        SortedMap<Date, RatingConfigurationDTO> dtoMap = new TreeMap<>();
        ws.entrySet().forEach(entry->{
            if (entry.getValue() != null) {
                dtoMap.put(entry.getKey(),getDTO(entry.getValue()));
            }
        });
        return dtoMap;
    }

    public static SortedMap<Date, RatingConfigurationWS> convertMapDTOToWS(SortedMap<Date, RatingConfigurationDTO> dto) {
        SortedMap<Date, RatingConfigurationWS> wsMap = new TreeMap<>();
        dto.entrySet().forEach(entry->{
            if(entry.getValue() != null) {
                wsMap.put(entry.getKey(),getWS(entry.getValue()));
            }
        });

        return wsMap;
    }


    public static RatingConfigurationWS getWsRatingConfigurationForDate(SortedMap<Date, RatingConfigurationWS> ratingConfigurations, Date date) {
        if (ratingConfigurations == null || ratingConfigurations.isEmpty()) {
            return null;
        }

        if (date == null) {
            return ratingConfigurations.get(ratingConfigurations.firstKey());
        }

        Date forDate = null;
        for (Date start : ratingConfigurations.keySet()) {
            if (start != null && start.after(date))
                break;

            forDate = start;
        }

        return forDate != null ? ratingConfigurations.get(forDate) : ratingConfigurations.get(ratingConfigurations.firstKey());
    }


    public static RatingConfigurationWS getWS(Integer ratingId, Integer ratingSchemeId, List<InternationalDescriptionWS> pricingUnit){

        if(ratingId==null && ratingSchemeId==null&& CollectionUtils.isEmpty(pricingUnit))
            return null;

        RatingConfigurationWS ratingConfigurationWS=new RatingConfigurationWS();
        ratingConfigurationWS.setRatingUnit(ratingId!=null?new RatingUnitBL(ratingId).getWS():null);
        ratingConfigurationWS.setUsageRatingScheme(ratingSchemeId!=null?new UsageRatingSchemeBL(ratingSchemeId).getWS():null);
        ratingConfigurationWS.setPricingUnit(pricingUnit);

        return ratingConfigurationWS;
    }

    public void savePricingUnit(List<InternationalDescriptionWS> pricingUnitDescriptions) {

        pricingUnitDescriptions.forEach(pricingUnit -> {
            if (pricingUnit.getLanguageId() != null
                && pricingUnit.getContent() != null
                && !pricingUnit.isDeleted()
                ) {
                    ratingConfiguration.setDescription("pricing_unit", pricingUnit.getLanguageId(), pricingUnit.getContent());
                }
                //handle delete part
        });


    }


    private static List<InternationalDescriptionWS> getAllPricingUnitDescriptions(int ratingConfigurationId) {
        JbillingTableDAS tableDas = Context
                .getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_RATING_CONFIGURATION);
        InternationalDescriptionDAS descriptionDas = Context
                .getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas
                .findAll(table.getId(), ratingConfigurationId, "pricing_unit");
        return descriptionsDTO.stream()
          .map(DescriptionBL::getInternationalDescriptionWS)
          .collect(Collectors.toList());
    }
}



