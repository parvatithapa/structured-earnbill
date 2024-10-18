package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemBundleDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.strategy.TeaserPricingStrategy;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Fernando Sivila on 03/01/17.
 */
public class CanadianQuoteTaxCalculationTask extends PluggableTask implements QuoteTaxCalculationTask {


    @Override
    public PlanDTO calculateTax(PlanDTO plan, String province, String date, String languageId) {
        BigDecimal price = null;
        BigDecimal totalOneTime = BigDecimal.ZERO;
        BigDecimal totalRecurrent = BigDecimal.ZERO;
        for (PlanItemDTO planItem : plan.getPlanItems()) {
            if (planItem.hasBundle() && planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                SortedMap<Date, PriceModelDTO> models = planItem.getModels();
                PriceModelDTO priceModel = models.entrySet().stream().max(Map.Entry.comparingByKey()).get().getValue();
                if (PriceModelStrategy.TEASER_PRICING.equals(priceModel.getType())) {
                    price = priceModel.getAttributes().entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(TeaserPricingStrategy.FIRST_PERIOD))
                        .map(Map.Entry::getValue)
                        .map(BigDecimal::new)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);
                    priceModel.setRate(price);
                }
                if (PriceModelStrategy.FLAT.equals(priceModel.getType())) {
                    price = priceModel.getRate();
                }
                if (planItem.getBundle().getPeriod().getId() == Constants.ORDER_PERIOD_ONCE) {
                    totalOneTime = totalOneTime.add(price.multiply(planItem.getBundle().getQuantity()));
                } else {
                    totalRecurrent = totalRecurrent.add(price.multiply(planItem.getBundle().getQuantity()));
                    totalOneTime = totalOneTime.add(price.multiply(planItem.getBundle().getQuantity()));
                }
                planItem.setPlan(plan);
            }
        }
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route = routeDAS.getRoute(getEntityId(), "canadian_taxes");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        if (province != null) {
            criteria.setSort("date");
            criteria.setDirection(SearchCriteria.SortDirection.DESC);
            criteria.setFilters(new BasicFilter[]{
                    new BasicFilter("province", com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, province),
                    new BasicFilter("date", Filter.FilterConstraint.LE, date),

            });
        } else {
            criteria.setFilters(new BasicFilter[]{});
        }
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);
        List<List<String>> rows = queryResult.getRows();
        if (rows.size() > 0) {
            List<String> row = rows.get(0);
            BigDecimal GST = new BigDecimal(row.get(SpaConstants.GST));
            BigDecimal PST = new BigDecimal(row.get(SpaConstants.PST));
            BigDecimal HST = new BigDecimal(row.get(SpaConstants.HST));
            ItemDTO itemTax;
            if (GST.compareTo(BigDecimal.ZERO) > 0) {
                itemTax= getTaxItem(getRegNumber(row, SpaConstants.GST, languageId));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ONCE, itemTax, Util.getPercentage(totalOneTime, GST));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ALL_ORDERS, itemTax, Util.getPercentage(totalRecurrent, GST));
            }
            if (PST.compareTo(BigDecimal.ZERO) > 0) {
                itemTax= getTaxItem(getRegNumber(row, SpaConstants.PST, languageId));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ONCE, itemTax, Util.getPercentage(totalOneTime, PST));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ALL_ORDERS, itemTax, Util.getPercentage(totalRecurrent, PST));
            }
            if (HST.compareTo(BigDecimal.ZERO) > 0) {
                itemTax= getTaxItem(getRegNumber(row, SpaConstants.HST, languageId));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ONCE, itemTax, Util.getPercentage(totalOneTime, HST));
                addPlanItemTaxDTO(plan, Constants.ORDER_PERIOD_ALL_ORDERS, itemTax, Util.getPercentage(totalRecurrent, HST));
            }
        }

        return plan;
    }

    private String getRegNumber(List<String> row, int taxName, String languageID) {
        switch (taxName) {
            case SpaConstants.GST:
                return languageID.equals(SpaConstants.ENGLISH_LANGUAGE) ? row.get(SpaConstants.GST_REG_ENG) : row.get(SpaConstants.GST_REG_FR);
            case SpaConstants.PST:
                return languageID.equals(SpaConstants.ENGLISH_LANGUAGE) ? row.get(SpaConstants.PST_REG_ENG) : row.get(SpaConstants.PST_REG_FR);
            case SpaConstants.HST:
                return languageID.equals(SpaConstants.ENGLISH_LANGUAGE) ? row.get(SpaConstants.HST_REG_ENG) : row.get(SpaConstants.HST_REG_FR);
        }
        return null;
    }

    private ItemDTO getTaxItem(String taxName) {
        ItemDAS itemDAS = new ItemDAS();
       return itemDAS.findItemByInternalNumber(taxName, getEntityId());
    }


    /**
     * @param plan
     * @param type    can be either recurrent or one-time
     * @param taxItem
     * @param price
     */
    private void addPlanItemTaxDTO(PlanDTO plan, Integer type, ItemDTO taxItem, BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            PlanItemDTO planItem = new PlanItemDTO();
            OrderPeriodDAS orderPeriodDAS = new OrderPeriodDAS();
            OrderPeriodDTO orderPeriodDTO = orderPeriodDAS.find(type);

            PlanItemBundleDTO bundle = new PlanItemBundleDTO();
            bundle.setPeriod(orderPeriodDTO);
            bundle.setQuantity(BigDecimal.ONE);
            planItem.setBundle(bundle);

            PriceModelDTO priceModelDTO = new PriceModelDTO();
            priceModelDTO.setRate(price);
            priceModelDTO.setCurrency(new CurrencyDAS().find(Constants.PRIMARY_CURRENCY_ID));
            priceModelDTO.setType(PriceModelStrategy.FLAT);
            SortedMap<Date, PriceModelDTO> models = new TreeMap<>();
            models.put(CommonConstants.EPOCH_DATE, priceModelDTO);
            planItem.setModels(models);

            taxItem.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_TAX);
            planItem.setItem(taxItem);
            plan.getPlanItems().add(planItem);
        }
    }


}
