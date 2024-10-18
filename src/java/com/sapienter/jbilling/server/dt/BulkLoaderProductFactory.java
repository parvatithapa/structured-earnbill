package com.sapienter.jbilling.server.dt;

import static com.sapienter.jbilling.server.dt.BulkLoaderUtility.validatePricingModel;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.exolab.castor.xml.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Created by Taimoor Choudhary on 12/13/17.
 */
public class BulkLoaderProductFactory {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer entityId;

    private IWebServicesSessionBean webServicesSessionBean;

    public BulkLoaderProductFactory(Integer entityId, IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
        this.entityId = entityId;
    }

    public IWebServicesSessionBean getApi() {
        return webServicesSessionBean;
    }

    public void createOrUpdateProductPrice(ProductFileItem fileItem) throws Exception{

        logger.debug("Bulk Upload - Default Price: Processing file item: {}", fileItem.toString());

        try {
            Integer itemId = getApi().getItemID(fileItem.getProductCode());
            Integer categoryEntityId = null;

            if (!StringUtils.isEmpty(fileItem.getCompany())) {
                CompanyDAS companyDAS = new CompanyDAS();
                CompanyDTO entityByName = companyDAS.findEntityByName(fileItem.getCompany());

                if(entityByName != null) {
                    categoryEntityId = entityByName.getId();
                }

            }

            ItemDTOEx itemDTOEx = new ItemDTOEx();

            if (null == itemId) {
                itemDTOEx.setNumber(fileItem.getProductCode());
            } else {
                itemDTOEx = webServicesSessionBean.getItem(itemId, null, null);
            }

            Integer[] categoryIds = new Integer[fileItem.getCategories().length];
            int iterator = 0;

            for (String category : fileItem.getCategories()) {
                ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
                ItemTypeDTO itemTypeDTO;

                itemTypeDTO = itemTypeDAS.findByGlobalDescription(this.entityId, category);

                if(null == itemTypeDTO){
                    List<ItemTypeDTO> itemTypeDTOList = itemTypeDAS.findByEntityId(this.entityId);

                    for(ItemTypeDTO tempItemTypeDTO : itemTypeDTOList){
                        if(null != tempItemTypeDTO && tempItemTypeDTO.getDescription().equals(category)){
                            itemTypeDTO = tempItemTypeDTO;
                            break;
                        }
                    }
                }

                //if category is not Global user findByDescription

                if (null == itemTypeDTO) {
                    ItemTypeWS itemType = new ItemTypeWS();
                    itemType.setDescription(category);
                    itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                    itemType.setAllowAssetManagement(0);
                    itemType.setGlobal(true);
                    Integer newCategoryId = webServicesSessionBean.createItemCategory(itemType);
                    categoryIds[iterator] = newCategoryId;

                } else {
                    categoryIds[iterator] = itemTypeDTO.getId();
                }

                iterator++;
            }

            if (!CollectionUtils.isEmpty(fileItem.getDescriptions())) {

                itemDTOEx.setDescriptions(fileItem.getDescriptions());
            }

            if (null != fileItem.getActiveSince()) {
                itemDTOEx.setActiveSince(fileItem.getActiveSince());
            }

            if (null != fileItem.getActiveUntil()) {
                itemDTOEx.setActiveUntil(fileItem.getActiveUntil());
            }

            if(null != itemDTOEx.getActiveSince() && null != itemDTOEx.getActiveUntil()
                    && itemDTOEx.getActiveSince().compareTo(itemDTOEx.getActiveUntil()) == 1 ){
                throw new ValidationException("Availability End Date should be greater than Availability Start Date");
            }

            if (null != categoryEntityId) {
                itemDTOEx.setEntityId(categoryEntityId);
                itemDTOEx.setGlobal(false);
            }else{
                itemDTOEx.setGlobal(true);
            }

            if (null != fileItem.getCurrencyId()) {
                itemDTOEx.setCurrencyId(fileItem.getCurrencyId());
            }

            itemDTOEx.setHasDecimals(fileItem.getAllowDecimalQuantity() ? 1 : 0);

            if(0 != categoryIds.length) {
                itemDTOEx.setTypes(categoryIds);
            }

            SortedMap<Date, PriceModel> priceModelTimeline = fileItem.getPriceModelTimeLine();
            SortedMap<Date, PriceModelWS> itemDefaultPrices = itemDTOEx.getDefaultPrices();

            for (Date date : priceModelTimeline.keySet()) {
                PriceModel priceModel = priceModelTimeline.get(date);

                if (!priceModel.isChained()) {
                    if (null != itemDefaultPrices.get(date)) {
                        itemDefaultPrices.remove(date);
                    }

                    itemDefaultPrices.put(date, priceModel.getPriceModelWS());
                } else {
                    if (null != itemDefaultPrices.get(date)) {
                        PriceModelWS priceModelWS = itemDefaultPrices.get(date);

                        while (true) {

                            validatePricingModel(priceModelWS, priceModel.getPriceModelWS());

                            if (priceModelWS.getNext() == null) {
                                priceModelWS.setNext(priceModel.getPriceModelWS());
                                break;
                            } else {
                                priceModelWS = priceModelWS.getNext();
                            }
                        }
                    } else {
                        itemDefaultPrices.put(date, priceModel.getPriceModelWS());
                    }
                }
            }

            String priceModelCompanyId = null;

            if(fileItem.getPrices().size() != 0){
                for(Price price : fileItem.getPrices()){

                    if(price.getDate() != null && !priceModelTimeline.keySet().contains(price.getDate())) {
                        PriceModelWS priceModelWs = itemDefaultPrices.get(price.getDate());

                        if (priceModelWs != null) {
                            priceModelWs.setCurrencyId(price.getCurrencyId());
                        }
                    }

                    if(price.getCompany() != null && priceModelCompanyId == null){
                        CompanyDAS companyDAS = new CompanyDAS();
                        CompanyDTO entityByName = companyDAS.findEntityByName(price.getCompany());

                        if(entityByName != null) {
                            itemDTOEx.setPriceModelCompanyId(entityByName.getId());
                        }
                    }

                }
            }

            // Add incoming Rating Units to existing ones
            ItemDTOEx finalItemDTOEx = itemDTOEx;
            fileItem.getRatingConfigurationTimeLine().forEach((date, ratingConfiguration) ->{

                // If Rating Configuration is NULL we don't need to add new, but need to remove if it exists
                if(ratingConfiguration == null){

                    if(finalItemDTOEx.getRatingConfigurations().containsKey(date)) {

                        finalItemDTOEx.getRatingConfigurations().remove(date);
                    }
                }else {

                    finalItemDTOEx.getRatingConfigurations().put(date, ratingConfiguration);
                }
            });

            // Add/Update MetaFields
            if(ArrayUtils.isNotEmpty(fileItem.getMetaFields())) {

                itemDTOEx.getMetaFieldsMap().put(this.entityId, fileItem.getMetaFields());
            }

            if (null == itemId) {

                logger.debug("Bulk Upload - Default Price: Creating new product. ItemDtoEx: {}", itemDTOEx);

                webServicesSessionBean.createItem(itemDTOEx);
            } else {

                logger.debug("Bulk Upload - Default Price: Updating product. ItemDtoEx: {}", itemDTOEx);

                webServicesSessionBean.updateItem(itemDTOEx);
            }

        }catch (Exception exception){
            logger.error("Bulk Upload - Default Price: Error while creating/updating product price. ", exception);
            throw new Exception("Error while creating/updating product price. " + exception.getMessage());
        }
    }

    @Transactional
    public Integer createPlan(PlanWS plan, ItemDTOEx product) {

        product.setIsPlan(true);
        Integer id = getApi().createItem(product);
        product.setId(id);
        plan.setItemId(id);
        return getApi().createPlan(plan);
    }

    @Transactional
    public void updatePlan(PlanWS plan, ItemDTOEx product) {

        product.setIsPlan(true);
        getApi().updateItem(product);
        getApi().updatePlan(plan);
    }
}
