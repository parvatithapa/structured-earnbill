package com.sapienter.jbilling.server.item;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.resources.AssetMetaFieldValueWS;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

@Transactional
public class AssetResourceHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;

    /**
     * Updates asset level meta fields
     * @param assetMetaFieldValueWS
     */
    public void updateAssetMetaFields(AssetMetaFieldValueWS assetMetaFieldValueWS) {
        MetaFieldValueWS[] metaFieldValues = MetaFieldHelper.convertAndValidateMFNameAndValueMapToMetaFieldValueWS(api.getCallerCompanyId(),
                EntityType.ASSET, assetMetaFieldValueWS.getMetaFieldValues());
        logger.debug("request meta fields {} converted to {}", assetMetaFieldValueWS.getMetaFieldValues(), metaFieldValues);
        Integer assetId = assetMetaFieldValueWS.getAssetId();
        api.updateAssetMetaFields(assetId, metaFieldValues);
        logger.debug("{} updated on Asset {}", metaFieldValues, assetId);
    }

    /**
     * Find assets by item id and status.
     * @param itemId
     * @param assetStatus
     * @param limit
     * @param offset
     * @return
     */
    public AssetRestWS[] getAssetsByItemAndStatus(Integer itemId, String assetStatus, Integer limit, Integer offset) {
        if(null == itemId) {
            logger.error("ItemId {}, is null", itemId);
            throw new SessionInternalError("Please provide item id parameter", new String [] { "Please enter itemId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(StringUtils.isEmpty(assetStatus)) {
            logger.error("assetStatus is null");
            throw new SessionInternalError("Please provide assetStatus parameter", new String [] { "Please enter assetStatus." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        try {
            ItemDAS itemDAS = new ItemDAS();
            if(!itemDAS.isIdPersisted(itemId)) {
                logger.error("Item {}, not found for entity {}", itemId, api.getCallerCompanyId());
                throw new SessionInternalError("item id not found for entity " + api.getCallerCompanyId(), new String [] { "Please enter valid itemId." },
                        HttpStatus.SC_NOT_FOUND);
            }
            ItemDTO item = itemDAS.findNow(itemId);
            List<String> statuses = item.getItemTypes()
                    .iterator()
                    .next()
                    .getAssetStatuses()
                    .stream()
                    .map(status-> status.getDescription(api.getCallerLanguageId()))
                    .collect(Collectors.toList());
            logger.debug("collected assetStatuses {} for Item {}", statuses, itemId);
            if(!statuses.contains(assetStatus)) {
                logger.error("Invalid asset status {}, passed", assetStatus);
                throw new SessionInternalError("Invalid asset status passed ", new String [] { "Please enter valid assetStatus.",
                        "Asset status " + assetStatus + " not present on item " + itemId + " category"},
                        HttpStatus.SC_BAD_REQUEST);
            }

            logger.debug("fetching assets for itemId {} with status {}", itemId, assetStatus);
            SearchCriteria criteria = new SearchCriteria();
            criteria.setMax(limit);
            criteria.setOffset(offset);
            criteria.setSort("id");
            criteria.setTotal(-1);
            criteria.setFilters(new BasicFilter[]{ new BasicFilter("status", FilterConstraint.EQ, assetStatus) });
            logger.debug("filter constructed {}", criteria);
            AssetSearchResult result = new AssetBL().findAssets(itemId, criteria);
            AssetWS[] assets = result.getObjects();
            if(ArrayUtils.isEmpty(assets)) {
                logger.debug("no asset found with criteria {} for item {}", criteria, itemId);
                return new AssetRestWS[0];
            }
            return Arrays.stream(assets)
                    .map(AssetBL::convertAssetWSToAssetRestWS)
                    .toArray(AssetRestWS[]::new);
        } catch(SessionInternalError error) {
            logger.error("validation failed", error);
            throw error;
        } catch (Exception e) {
            logger.error("getAssetsByItemAndStatus failed", e);
            throw new SessionInternalError("Error in getAssetsByItemAndStatus", e);
        }
    }
}
