package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderLinePlanItemDTOEx;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangePlanItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserHelperDisplayerDistributel;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * Created by pablo_galera on 06/02/17.
 */
public class MCFRatedPlansProvisioningTask extends AbstractProvisioningTask {

    private static final Logger LOG = Logger.getLogger(MCFRatedPlansProvisioningTask.class);

    @Override
    boolean isActionProvisionable(IProvisionable provisionable) {

        try{
            if (provisionable instanceof AssetDTO) {
                AssetDTO asset = (AssetDTO) provisionable;
                asset = new AssetDAS().findNow(asset.getId());
                MetaFieldValue mfPhoneNumber = asset.getMetaField(SpaConstants.MF_PHONE_NUMBER);
                if (mfPhoneNumber == null || mfPhoneNumber.getValue().toString().contains(SpaConstants.NEW_PHONE_NUMBER)) {
                    return false;
                }
                OrderDTO order = null;
                OrderChangeDTO orderChange = new OrderChangeDAS().findByOrderChangeByAssetIdInPlanItems(asset.getId());
                if (orderChange != null) {
                    for (OrderChangePlanItemDTO orderChangePlanItem : orderChange.getOrderChangePlanItems()) {
                        for (ItemTypeDTO category : orderChangePlanItem.getItem().getItemTypes()) {
                            if (SpaConstants.MC_RATED_CATEGORY.equals(category.getDescription())) {
                                order = orderChange.getOrder();
                            }
                        }
                    }
                }

                if (order == null && asset.getOrderLine() != null) {
                    ItemDAS itemDAS = new ItemDAS();
                    if (asset.getOrderLine().getOrderLinePlanItems() != null) {
                        for (OrderLinePlanItemDTOEx orderPlanItem : asset.getOrderLine().getOrderLinePlanItems()) {
                            ItemDTO item = itemDAS.find(orderPlanItem.getItemId());
                            for (ItemTypeDTO category : item.getItemTypes()) {
                                if (SpaConstants.MC_RATED_CATEGORY.equals(category.getDescription())) {
                                    order = asset.getOrderLine().getPurchaseOrder();
                                }
                            }
                        }
                    }
                    if (order == null) {
                        for (ItemTypeDTO category : asset.getOrderLine().getItem().getItemTypes()) {
                            if (SpaConstants.MC_RATED_CATEGORY.equals(category.getDescription())) {
                                order = asset.getOrderLine().getPurchaseOrder();
                            }
                        }
                    }
                }

                if (order == null) {
                    return false;
                }

                LocalDate today = DateConvertUtils.asLocalDate(TimezoneHelper.companyCurrentDate(order.getUser().getCompany()));
                LocalDate activeUntilLD = DateConvertUtils.asLocalDate(order.getActiveUntil());
                if (activeUntilLD == null || today.isBefore(activeUntilLD) || today.isEqual(activeUntilLD)) {
                    return true;
                }
            }

        }
        catch(Exception e){
            LOG.error("Exception in isActionProvisionable() method. Exception : ", e);
        }

        return false;
    }

    @Override
    void update(AssetDTO asset, CommandManager c) {
        OrderDTO order = null;
        if (asset.getOrderLine() != null) {
            order = asset.getOrderLine().getPurchaseOrder();
        } else {
            order = new OrderChangeDAS().findByOrderChangeByAssetIdInPlanItems(asset.getId()).getOrder();
        }

        MetaFieldValue businessUnitMF = order.getUser().getCompany().getMetaField(SpaConstants.BUSINESS_UNIT);
        asset = new AssetDAS().findNow(asset.getId());
        String billingIdentifier = asset.getMetaField(SpaConstants.MF_PHONE_NUMBER).getValue().toString();
        c.addCommand("new_service_subscription");
        c.addParameter(SpaConstants.PARAM_CUSTOMER_NUMBER, String.valueOf(order.getUser().getId()));
        c.addParameter(SpaConstants.PARAM_CUSTOMER_NAME, UserHelperDisplayerDistributel.getInstance().getDisplayName(order.getUser()));
        c.addParameter(SpaConstants.PARAM_BILLING_IDENTIFIER, billingIdentifier);
        c.addParameter(SpaConstants.PARAM_TIME_ZONE, getTimezoneByProvince(order.getUser().getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE).getMetaFieldValue().getValue().toString()));
        c.addParameter(SpaConstants.PARAM_EFFECTIVE_DATE, new SimpleDateFormat("yyyyMMdd").format(order.getActiveSince()));
        c.addParameter(SpaConstants.PARAM_LANGUAGE, SpaImportHelper.getLanguageByCode(order.getUser().getLanguage().getCode()));
        c.addParameter(SpaConstants.PARAM_TAGS, "null");
        c.addParameter(SpaConstants.PARAM_BUSINESS_UNIT, businessUnitMF != null ? (String) businessUnitMF.getValue() : "No Business Unit");
        LOG.info("Added command for provisioning when the asset is updated " + asset.getId());
        ItemDTO item = asset.getItem();
        if(null != item){
            c.addParameter(SpaConstants.PARAM_SERVICE_NUMBER, (null != item.getMetaField(SpaConstants.MF_SERVICE_ID)) ? String.valueOf(item.getMetaField(SpaConstants.MF_SERVICE_ID).getValue()) : "n/a" );
        }else{
            c.addParameter(SpaConstants.PARAM_SERVICE_NUMBER, "n/a");
        }
    }

    private String getTimezoneByProvince(String province) {
        RouteDAS routeDAS = new RouteDAS();
        RouteDTO route =  routeDAS.getRoute(getEntityId(), "province_timezone");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        criteria.setFilters(new BasicFilter[]{
                new BasicFilter("province",com.sapienter.jbilling.server.util.search.Filter.FilterConstraint.EQ, province)
        });
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);
        List<List<String>> rows = queryResult.getRows();
        if (rows.size() > 0) {
            return rows.get(0).get(2);
        }
        return null;
    }
}
