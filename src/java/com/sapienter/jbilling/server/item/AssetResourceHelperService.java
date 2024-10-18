package com.sapienter.jbilling.server.item;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Lists;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.resources.AssetMetaFieldValueWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
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
    private static final AssetRestWS[] EMPTY_ARRAY = new AssetRestWS[0];
    private static final int BATCH_SIZE = 6;

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;
    @Resource
    private AssetDAS assetDAS;
    @Resource(name = "readOnlyTx")
    private TransactionTemplate readOnlyTransaction;
    @Resource(name = "asyncTaskExecutor")
    private AsyncTaskExecutor asyncTaskExecutor;
    @Resource
    private SessionFactory sessionFactory;

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
        validOffsetAndLimit(offset, limit);
        try {
            Integer entityId = api.getCallerCompanyId();
            ItemDAS itemDAS = new ItemDAS();
            if(!itemDAS.isIdPersisted(itemId)) {
                logger.error("Item {}, not found for entity {}", itemId, entityId);
                throw new SessionInternalError("item id not found for entity " + entityId, new String [] { "Please enter valid itemId." },
                        HttpStatus.SC_NOT_FOUND);
            }
            ItemDTO item = itemDAS.findNow(itemId);
            Integer languageId = api.getCallerLanguageId();
            List<String> statuses = item.getItemTypes()
                    .iterator()
                    .next()
                    .getAssetStatuses()
                    .stream()
                    .map(status-> status.getDescription(languageId))
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
                return EMPTY_ARRAY;
            }
            return Arrays.stream(assets)
                    .map(AssetBL::convertAssetWSToAssetRestWS)
                    .toArray(AssetRestWS[]::new);
        } catch(SessionInternalError error) {
            throw error;
        } catch (Exception e) {
            throw new SessionInternalError("Error in getAssetsByItemAndStatus", e);
        }
    }

    /**
     * creates {@link AssetRestWS} concurrently.
     * @param assetIds
     * @return
     */
    private AssetRestWS[] convertAssetRestWS(List<Integer> assetIds) {
        List<Future<List<AssetRestWS>>> assetFutureResult = new ArrayList<>();
        for(List<Integer> assetIdList : Lists.partition(assetIds, (assetIds.size() / BATCH_SIZE + 1))) {
            assetFutureResult.add(asyncTaskExecutor.submit(() -> readOnlyTransaction.execute(status -> {
                logger.debug("{} assets converting to asset WS by thread {}", assetIdList.size(), Thread.currentThread().getName());
                List<AssetRestWS> assets = new ArrayList<>();
                int count = 0;
                for(Integer assetId : assetIdList) {
                    if(Thread.interrupted()) {
                        logger.debug("thread {} interrupted", Thread.currentThread().getName());
                        break;
                    }
                    assets.add(AssetBL.getAssetWS(assetDAS.find(assetId)));
                    if(++count % 50 == 0) {
                        Session session = sessionFactory.getCurrentSession();
                        session.clear();
                        logger.debug("cleared session");
                    }
                }
                return assets;
            })));
        }
        List<AssetRestWS> assets = new ArrayList<>();
        for(Future<List<AssetRestWS>> assetFuture : assetFutureResult) {
            try {
                assets.addAll(assetFuture.get());
            } catch(Exception ex) {
                try {
                    // Canceling running task.
                    for(Future<List<AssetRestWS>> future: assetFutureResult) {
                        if(!future.isCancelled()) {
                            future.cancel(true);
                        }
                    }
                } catch(Exception e) {
                    logger.error("error during Canceling running task", e);
                }
                throw new SessionInternalError("Error during asset dto to ws conversion", ex);
            }
        }
        return assets.toArray(new AssetRestWS[0]);
    }

    /**
     * finds assets by category and given status.
     * @param categoryId
     * @param assetStatus
     * @param limit
     * @param offset
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public AssetRestWS[] getAssetsByCategoryAndStatus(Integer categoryId, String assetStatus, Integer limit, Integer offset) {
        long startTime = System.currentTimeMillis();
        if(null == categoryId) {
            logger.error("categoryId {}, is null", categoryId);
            throw new SessionInternalError("Please provide categoryId id parameter", new String [] { "Please enter categoryId." },
                    HttpStatus.SC_BAD_REQUEST);
        }

        if(StringUtils.isEmpty(assetStatus)) {
            logger.error("assetStatus is null");
            throw new SessionInternalError("Please provide assetStatus parameter", new String [] { "Please enter assetStatus." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        validOffsetAndLimit(offset, limit);
        AssetRestWS[] result = null;
        try {
            ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
            if(!itemTypeDAS.isIdPersisted(categoryId)) {
                logger.error("Invalid category id {}, passed", categoryId);
                throw new SessionInternalError("Invalid category id passed ", new String [] { "Please enter valid category id."},
                        HttpStatus.SC_NOT_FOUND);
            }
            ItemTypeDTO itemType = itemTypeDAS.find(categoryId);
            Integer languageId = api.getCallerLanguageId();
            long statusFetchTime = System.currentTimeMillis();
            Map<String, Integer> assetStatusMap = itemType
                    .getAssetStatuses()
                    .stream()
                    .collect(Collectors.toMap(status-> status.getDescription(languageId), AssetStatusDTO::getId));
            logger.debug("time {} taken to create assetStatusMap in milliseconds", (System.currentTimeMillis() - statusFetchTime));
            logger.debug("collected assetStatuses {} for Category {}", assetStatusMap, categoryId);
            if(!assetStatusMap.containsKey(assetStatus)) {
                logger.error("Invalid asset status {}, passed", assetStatus);
                throw new SessionInternalError("Invalid asset status passed ", new String [] { "Please enter valid assetStatus.",
                        "Asset status " + assetStatus + " not present on category " + categoryId},
                        HttpStatus.SC_BAD_REQUEST);
            }
            long assetFetchTime = System.currentTimeMillis();
            List<Integer> assets = assetDAS.getAssetsByCategoryAndStatus(categoryId, assetStatusMap.get(assetStatus), offset, limit);
            logger.debug("time {} taken to fetch assets in milliseconds", (System.currentTimeMillis() - assetFetchTime));
            if(CollectionUtils.isEmpty(assets)) {
                logger.debug("no asset found for category {} with status {}", categoryId, assetStatus);
                return EMPTY_ARRAY;
            }
            long assetConvertTime = System.currentTimeMillis();
            result = convertAssetRestWS(assets);
            logger.debug("time {} taken to convert assets in milliseconds", (System.currentTimeMillis() - assetConvertTime));
            return result;
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError("Error in getAssetsByCategoryAndStatus", ex);
        } finally {
            logger.debug("time {} taken to fetch assets {} in milliseconds", (System.currentTimeMillis() - startTime),
                    null == result ? 0 : result.length);
        }
    }

    private void validOffsetAndLimit(Integer offset, Integer limit) {
        if (null != offset && offset.intValue() < 0) {
            throw new SessionInternalError("Offset value can not be negative number.", HttpStatus.SC_BAD_REQUEST);
        }
        if (null != limit && limit.intValue() < 0) {
            throw new SessionInternalError("Limit value must be greater than zero.", HttpStatus.SC_BAD_REQUEST);
        }
    }
}
