package com.sapienter.jbilling.server.integration.common.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sapienter.jbilling.server.integration.common.service.vo.*;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.integration.common.service.vo.CompanyInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.CustomerInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ProductInfo;

public interface HelperDataAccessService {
    Optional<CompanyInfo> getCompanyInfo(int entityId);

    Optional<CustomerInfo> getCustomerInfo(int entityId, int userId, String metaFileName);

    Optional<ProductInfo> getProductInfo(int entity, int productId, int languageId);

    List<UsagePoolInfo> getUsagePoolsByPlanId(Integer entityId,Integer planId,Integer languageId);

    List<Integer> getProductIdsByPlanId(Integer entityId,Integer planId,Integer languageId);

    MetaFieldValueWS getPlanMetafieldValue(Integer entityId, Integer planId,  Integer metafieldNameId);

    MetaField getFieldByName(Integer entityId, EntityType entityType, String name);

    Map<Integer,BigDecimal> getCustomerPoolsWithUtilizedQty(Integer entityId, Integer orderId, Integer languageId);

    Integer getPlanAssociatedToCustomerPool(Integer entityId, Integer customerPoolId, Integer languageId);

    List<OrderLineTierInfo> getOrderLineTiers(Integer orderLineId);


    ReservedPlanInfo getReservedPlanInfo(Integer entityId, int planId, BigDecimal price);

    List<Integer> getUsersWithMediatedOrderAndPartition(Integer entityId, int orderStatusId, Date lastMediationRun, int partitions, int partition);

    List<OrderInfo> getMediatedOrdersByStatusAndUser(Integer entityId, Integer userId, int orderStatusId, Date lastMediationRun);

    List<Integer> getUsersWithReservedMonthlyPlans(Integer entityId,int orderStatusId, int partitions, int partition);

    List<OrderInfo> getReservedPlanOrdersByCustomer(Integer entityId, Integer userId);

    List<OrderLineInfo> getOrderLines(Integer entityId, int orderId);

    Integer getItemIdForPlan (Integer planId);

    MetaFieldValueWS getOrderMetafieldValue(Integer entityId, Integer orderId, Integer metafieldNameId);



}
