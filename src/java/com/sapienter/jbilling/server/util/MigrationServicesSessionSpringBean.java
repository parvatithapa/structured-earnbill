package com.sapienter.jbilling.server.util;/*
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


import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationContext;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.db.*;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import grails.plugin.springsecurity.SpringSecurityService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Created by marcomanzi on 3/17/14.
 */
@Transactional( propagation = Propagation.REQUIRED )
public class MigrationServicesSessionSpringBean implements IMigrationServicesSessionBean{

    private SpringSecurityService springSecurityService;
    private IWebServicesSessionBean webServicesSessionBean;

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            this.springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

    public void setSpringSecurityService(SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }

    @Override
    public Integer getAccountTypeIdByDescription(Integer entityId, String accountType) {
        List<AccountTypeDTO> accountForEntity = new AccountTypeDAS().findAll(entityId);
        for (AccountTypeDTO accountTypeDTO: accountForEntity) {
            if (accountTypeDTO.getDescription().equals(accountType)) {
                return accountTypeDTO.getId();
            }
        }
        return 0;
    }

    @Override
    public List<MetaFieldValueWS> retrieveMetafieldForCustomer(Integer entityId, Integer accountTypeId) {
        List<EntityType> entities =  new ArrayList<EntityType>();
        entities.add(EntityType.CUSTOMER);
        List<MetaField> availableFieldsList = MetaFieldBL.getAvailableFieldsList(entityId, entities.toArray(new EntityType[1]));
        availableFieldsList.addAll(availableFieldsList);

        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList<MetaFieldValueWS>();
        for (MetaField metaField: availableFieldsList) {
            MetaFieldValueWS metaFieldValue =MetaFieldBL.getWS(metaField.createValue());
            metaFieldValueWS.add(metaFieldValue);
        }
        Map<Integer, List<MetaField>> availableAccountTypeFieldsMap = MetaFieldExternalHelper.getAvailableAccountTypeFieldsMap(accountTypeId);
        for (Integer groupId : availableAccountTypeFieldsMap.keySet()) {
            for (MetaField metaField: availableAccountTypeFieldsMap.get(groupId)) {
                MetaFieldValueWS metaFieldValue = MetaFieldBL.getWS(metaField.createValue());
                metaFieldValue.setGroupId(groupId);
                metaFieldValueWS.add(metaFieldValue);
            }
        }

        return metaFieldValueWS;
    }

    public UserWS retrieveUserWSByMetaField(Integer entityId, String metaFieldName, String metaFieldValue) {
        List<AccountTypeDTO> accountsForEntity = new AccountTypeDAS().findAll(entityId);
        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList<MetaFieldValueWS>();
        for (AccountTypeDTO accountType: accountsForEntity) {
            metaFieldValueWS.addAll(retrieveMetafieldForCustomer(entityId, accountType.getId()));
        }
        return new UserBL(new UserDAS().findByMetaFieldNameAndValue(entityId, metaFieldName, metaFieldValue)).getUserWS();
    }

    @Override
    public void saveMigrationCdr(JbillingMediationRecord mediationRecordLineWS) {
        MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
        MediationContext context = new MediationContext();
        MediationConfiguration mediationConfiguration = new MediationConfigurationDAS().find(mediationRecordLineWS.getMediationCfgId());
        context.setEntityId(mediationConfiguration.getEntityId());
        context.setMediationCfgId(mediationRecordLineWS.getMediationCfgId());
        context.setJobName(mediationConfiguration.getMediationJobLauncher());
        context.setRecordToProcess(mediationRecordLineWS);
        context.setProcessIdForMediation(mediationRecordLineWS.getProcessId());
        validateMediationContext(context);
        mediationService.launchMediation(context);
    }

    private void validateMediationContext(MediationContext mediationContext) {
        if (mediationContext.getEntityId() == null ||
                mediationContext.getMediationCfgId() == null ||
                mediationContext.getJobName() == null ||
                mediationContext.getProcessIdForMediation() == null)  {
            throw new IllegalArgumentException("To import a mediation records it needs that on it are set the following parameters: " +
                    "entityId:" + mediationContext.getEntityId() + "," +
                    "mediationConfigId:" + mediationContext.getMediationCfgId() + "," +
                    "jobName:" + mediationContext.getJobName() +
                    "processId:" + mediationContext.getProcessIdForMediation());
        }
    }

    /**
     * Returns the company ID of the authenticated user account making the web service call.
     *
     * @return caller company ID
     */
    @Transactional(readOnly=true)
    public Integer getCallerCompanyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService().getPrincipal();
        return details.getCompanyId();
    }

    @Override
    public UserWS findUserBy(Integer clientUserId) {
        /**
         * This method need to be customized for each customer, users imported by another system will probably have a
         * external ID that need to be used to understand where the user imported is in jBilling after the migration
         */
        throw new RuntimeException("Method to implement for each customer");
    }
//
//
//    private UserDTO findUserBy(CommonMediationRecordLineDTO mediationRecordLineDTO) {
//
//        /**
//         * This method need to be customized for each customer, users imported by another system will probably have a
//         * external ID that need to be used to understand where the user imported is in jBilling after the migration,
//         * this will be also on the rated cdrs migrated in jbilling
//         */
//        throw new RuntimeException("Method to implement for each customer");
//    }
//
//    public boolean isMediationRecordLineEventDateInTheOrder(OrderDTO order, Date eventDate) {
//        return order.getActiveSince().compareTo(eventDate) <= 0 &&
//                order.getActiveUntil().compareTo(eventDate) >= 0;
//    }

    @Override
    @Transactional()
    public UUID createMediationProcessForMigration(Integer mediationCfgId) {
        MediationProcessService processService = Context.getBean(MediationProcessService.BEAN_NAME);
        MediationConfiguration mediationConfiguration = new MediationConfigurationDAS().find(mediationCfgId);
        return processService.saveMediationProcess(mediationConfiguration.getEntityId(), mediationCfgId, null).getId();
    }

    @Override
    @Transactional()
    public Integer createChildOrderForUser(OrderWS order, OrderChangeWS[] orderChanges, Integer userId) throws SessionInternalError {
        OrderWS parentOrder = null;
        try {
            parentOrder = webServicesSessionBean.getLatestOrder(userId);
            fixOrderWithParentOrder(order, parentOrder);

            if (parentOrder.getParentOrderId() != null) {
                parentOrder = webServicesSessionBean.getOrder( Integer.parseInt(parentOrder.getParentOrderId()) );
            }
        } catch (SessionInternalError sessionInternalError) {
            throw new SessionInternalError("Can't find the subscription for userId:" + userId);
        }

        int orderCreated = webServicesSessionBean.createUpdateOrder(order, orderChanges);
        OrderWS orderSaved = webServicesSessionBean.getOrder(orderCreated);
        setOrderAsParentChild(parentOrder.getId(), orderSaved.getId());
        return orderSaved.getId();
    }

    private void fixOrderWithParentOrder(OrderWS order, OrderWS parentOrder) {
        order.setCancellationMinimumPeriod(parentOrder.getCancellationMinimumPeriod());
        order.setNextBillableDay(parentOrder.getNextBillableDay());
        if (parentOrder.getActiveUntil() != null && order.getActiveUntil() == null) {
            order.setActiveUntil(parentOrder.getActiveUntil());
            checkOrderDatesAreRight(order);
        }
        if (order.getActiveSince().before(parentOrder.getActiveSince())) {
            order.setActiveSince(parentOrder.getActiveSince());
            checkOrderDatesAreRight(order);
        }
        if (order.getActiveUntil().after(parentOrder.getActiveUntil())) {
            order.setActiveUntil(parentOrder.getActiveUntil());
            checkOrderDatesAreRight(order);
        }
    }

    private void checkOrderDatesAreRight(OrderWS order) {
        if (!order.getActiveUntil().after(order.getActiveSince())) {
            Calendar c = Calendar.getInstance();
            c.setTime(order.getActiveSince());
            c.add(Calendar.DATE, 1);
            order.setActiveUntil(c.getTime());
        }
        if (!order.getActiveSince().before(order.getActiveUntil())) {
            Calendar c = Calendar.getInstance();
            c.setTime(order.getActiveUntil());
            c.add(Calendar.DATE, -1);
            order.setActiveSince(c.getTime());
        }
    }

    @Override
    @Transactional()
    public Integer findParentOrder(Integer childId) {
        OrderDTO childOrder = new OrderDAS().find(childId);
        if (childOrder != null && childOrder.getParentOrder() != null) {
            return new OrderDAS().find(childId).getParentOrder().getId();
        }
        return null;
    }

    private void setOrderAsParentChild(Integer parentId, Integer childId) {
        OrderDAS das = new OrderDAS();
        OrderDTO childDto = das.find(childId);
        OrderDTO parentDto = das.find(parentId);
        childDto.setParentOrder(parentDto);
        parentDto.getChildOrders().add(childDto);
        das.save(parentDto);
        das.save(childDto);
    }

}
