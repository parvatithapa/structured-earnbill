package com.sapienter.jbilling.server.dt.reserve.mapper;


import com.sapienter.jbilling.catalogue.DtPlanWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DtReserveInstanceWSMapper {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public DtPlanWS mapPlanToDtPlan(PlanWS plan, DtPlanWS dtPlanWS){
        ItemDTO planItemDTO = new ItemBL(plan.getItemId()).getEntity();
        PriceModelDTO planPriceModel = planItemDTO.getPrice(TimezoneHelper.companyCurrentDate(planItemDTO.getPriceModelCompanyId()),
                planItemDTO.getPriceModelCompanyId());
        ItemDTO productItemDTO = new ItemBL(plan.getPlanItems().get(0).getItemId()).getEntity();                  // considering the scenario that is reserve instance can have at most one product
        PlanItemDTO planProductItemDTO = PlanBL.getDTO(plan).findPlanItem(plan.getPlanItems().get(0).getItemId());
        PriceModelDTO planProductPriceModel = planProductItemDTO.getPrice(TimezoneHelper.companyCurrentDate(
                planItemDTO.getPriceModelCompanyId()));

        if( !planProductPriceModel.getType().equals(PriceModelStrategy.FLAT) || plan.getPlanItems().size() > 1){
            return null;
        }

        dtPlanWS.setProductCategory(productItemDTO.getItemTypes().iterator().next().getDescription());

        dtPlanWS.setEnPlanName(planItemDTO.getDescription(Constants.LANGUAGE_ENGLISH_ID));
        dtPlanWS.setDePlanName(planItemDTO.getDescription(Constants.LANGUAGE_GERMAN_ID));
        dtPlanWS.setActiveSince(planItemDTO.getActiveSince() == null ? "" : sdf.format(planItemDTO.getActiveSince()));
        dtPlanWS.setActiveUntil(planItemDTO.getActiveUntil() == null ? "" : sdf.format(planItemDTO.getActiveUntil()));
        dtPlanWS.setCurrencyCode(planPriceModel.getCurrency().getCode());
        dtPlanWS.setPlanPrice(planPriceModel.getRate());
        dtPlanWS.setProductElasticPrice(planProductPriceModel.getRate());
        dtPlanWS.setPlanId(plan.getId());
        dtPlanWS.setEnProductName(productItemDTO.getDescription(Constants.LANGUAGE_ENGLISH_ID));
        dtPlanWS.setDeProductName(productItemDTO.getDescription(Constants.LANGUAGE_GERMAN_ID));
        dtPlanWS.setProductData(getProductData(productItemDTO));
        return dtPlanWS;
    }

    private Map<String,String> getProductData(ItemDTO productItemDTO){
        Map<String,String> featureMap = new HashMap<>();
        MetaFieldValue metafieldValue = productItemDTO.getMetaField("Features");
        if(metafieldValue == null){
            return featureMap;
        }
        List<String> features = Arrays.asList(metafieldValue.getValue().toString().split(","));
        for(String f : features){
            try {
                String key = f.split(":")[0];
                if(key.equals("")){
                    throw new Exception();
                }
                String value = f.split(":")[1];
                featureMap.put(key, value);
            }catch(Exception e){
                logger.error("Product feature not set properly.");
            }
        }
        return featureMap;
    }

   public List<DtPlanWS> filterOnCategory(List<DtPlanWS> reservedInstanceList, BasicFilter[] filter){
        if(filter == null){
            return reservedInstanceList;
        }
       String category =  filter[0].getStringValue();
       String os = filter[1].getStringValue();
       String ram = filter[2].getStringValue();
       String cpu = filter[3].getStringValue();
       return reservedInstanceList.stream().filter(r -> {
               boolean foundCategory = category==null || r.getProductCategory().trim().equalsIgnoreCase(category.trim());
               boolean foundOS = os == null || r.getProductData().containsKey("os") && r.getProductData().get("os").trim().equalsIgnoreCase(os.trim());
               boolean foundRAM = ram == null || r.getProductData().containsKey("ram") && r.getProductData().get("ram").trim().equalsIgnoreCase(ram.trim());
               boolean foundCPU = cpu == null || r.getProductData().containsKey("cpu") && r.getProductData().get("cpu").trim().equalsIgnoreCase(cpu.trim());
               return foundCategory && foundOS && foundRAM && foundCPU;
       })
               .collect(Collectors.toList());

   }
}
