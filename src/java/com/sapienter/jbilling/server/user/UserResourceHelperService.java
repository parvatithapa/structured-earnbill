package com.sapienter.jbilling.server.user;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.Diff;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.resources.CustomerMetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
public class UserResourceHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GET = "get";
    private static final String SET = "set";
    private static final String PARSED = "Parsed";
    private static final String NID_FIELD_NAME = "nextInvoiceDate";

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;

    private void updateOrAddAITMetaField(Integer userId, List<MetaFieldValueWS> metaFields,
            String fieldName, Object value, MetaFieldGroup metaFieldGroup) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName) && ws.getGroupId() == metaFieldGroup.getId()) {
                logger.debug("meta field {} found on user {} and value set {} on user", fieldName, userId, value);
                ws.setValue(value);
                return;
            }
        }
        //If meta field value is not present then create a new metafield.
        for (MetaField field: metaFieldGroup.getMetaFields()) {
            if(field.getName().equalsIgnoreCase(fieldName)){
                MetaFieldValueWS newField = new MetaFieldValueWS();
                newField.setFieldName(field.getName());
                newField.setGroupId(metaFieldGroup.getId());
                newField.setMetaField(MetaFieldBL.getWS(field));
                newField.setValue(value);
                metaFields.add(newField);
                logger.debug("new meta field value {} created for user {}", newField, userId);
                return;
            }
        }
        logger.error("meta field {} not found on user", fieldName);
        throw new SessionInternalError("Metafield not found",
                new String[]{"No meta field found with name "+ fieldName }, HttpStatus.SC_BAD_REQUEST);
    }

    public UserWS updateAITMetaField(ContactInformationWS contactInformation) {
        UserWS user = getUserWS(contactInformation.getUserId());

        MetaFieldGroup metaFieldGroup = new AccountInformationTypeDAS().getGroupByNameAndEntityId(user.getEntityId()
                , EntityType.ACCOUNT_TYPE, contactInformation.getGroupName() , user.getAccountTypeId());

        if(null == metaFieldGroup) {
            logger.debug("no group name {} found for account type {}", contactInformation.getGroupName(), user.getAccountTypeId());
            throw new SessionInternalError("AIT not found",
                    new String[]{"No AIT group found with name "+ contactInformation.getGroupName()}, HttpStatus.SC_NOT_FOUND);
        }
        List<MetaFieldValueWS> usersMetaFields = new ArrayList<>();
        usersMetaFields.addAll(Arrays.asList(user.getMetaFields()));
        contactInformation.getMetaFields().forEach((fieldName,fieldValue)->
        updateOrAddAITMetaField(user.getId(), usersMetaFields, fieldName, fieldValue, metaFieldGroup));
        user.setMetaFields(usersMetaFields.toArray(new MetaFieldValueWS[0]));
        user.setPassword(null);
        api.updateUser(user);
        return getUserWS(user.getId());
    }

    public UserWS updateCustomerAttributes(Integer userId, CustomerRestWS reqCustomerRestWS) {
        CustomerRestWS userRestWS = getCustomerAttributes(userId);
        if (!userId.equals(reqCustomerRestWS.getUserId())) {
            throw new SessionInternalError("Validation failed",
                    new String [] {String.format("UserId mismatch, the JSON body and Api parameter both should have the same user id.")}, HttpStatus.SC_BAD_REQUEST);
        }
        DiffResult diffResult = userRestWS.diff(reqCustomerRestWS);
        if (diffResult == null || CollectionUtils.isEmpty(diffResult.getDiffs())) {
            logger.debug("There were no differences between existing object and supplied one for user {}", userId);
            return null;
        }
        try {
            UserWS userWS = api.getUserWS(userId);
            Class targetCls = userWS.getClass();
            Class sourceCls = reqCustomerRestWS.getClass();
            for (Diff<?> field : diffResult.getDiffs()) {
                if (field.getRight() != null) {
                    String fieldName = StringUtils.capitalize(field.getFieldName());
                    String getFieldName = NID_FIELD_NAME.equals(field.getFieldName()) ? fieldName + PARSED : fieldName;
                    Method getMethod = sourceCls.getDeclaredMethod(GET + getFieldName);
                    Object fieldValue = getMethod.invoke(reqCustomerRestWS);
                    Method setMethod = targetCls.getDeclaredMethod(SET + fieldName, new Class[] {getMethod.getReturnType()});
                    setMethod.invoke(userWS, new Object[] {fieldValue});
                }
            }
            return userWS;
        } catch(Exception ex) {
            logger.error("Exception occurred while updating customer attributes of user {}.", userId);
            throw new SessionInternalError(ex);
        }
    }

    public CustomerRestWS getCustomerAttributes(Integer userId) {
        UserWS userWS = api.getUserWS(userId);
        CustomerRestWS userRestWS = new CustomerRestWS();
        userRestWS.setUserId(userWS.getId());
        userRestWS.setCurrencyId(userWS.getCurrencyId());
        userRestWS.setLanguageId(userWS.getLanguageId());
        userRestWS.setStatusId(userWS.getStatusId());
        userRestWS.setSubscriberStatusId(userWS.getSubscriberStatusId());
        userRestWS.setAutomaticPaymentType(userWS.getAutomaticPaymentType());
        userRestWS.setPartnerIds(userWS.getPartnerIds());
        userRestWS.setParentId(userWS.getParentId());

        userRestWS.setIsParent(userWS.getIsParent());
        userRestWS.setInvoiceChild(userWS.getInvoiceChild());
        userRestWS.setUseParentPricing(userWS.getUseParentPricing());
        userRestWS.setExcludeAgeing(userWS.getExcludeAgeing());
        userRestWS.setIsAccountLocked(userWS.isAccountLocked());
        userRestWS.setAccountExpired(userWS.isAccountExpired());
        userRestWS.setUserCodeLink(userWS.getUserCodeLink());

        userRestWS.setCreditLimit(userWS.getCreditLimitAsDecimal());
        userRestWS.setAutoRecharge(userWS.getAutoRechargeAsDecimal());
        userRestWS.setRechargeThreshold(userWS.getRechargeThresholdAsDecimal());
        userRestWS.setLowBalanceThreshold(userWS.getLowBalanceThresholdAsDecimal());
        userRestWS.setMonthlyLimit(userWS.getMonthlyLimitAsDecimal());

        userRestWS.setInvoiceDeliveryMethodId(userWS.getInvoiceDeliveryMethodId());
        userRestWS.setDueDateValue(userWS.getDueDateValue());
        userRestWS.setDueDateUnitId(userWS.getDueDateUnitId());
        userRestWS.setMainSubscription(userWS.getMainSubscription());
        userRestWS.setNextInvoiceDate(formatDate(userWS.getNextInvoiceDate()));
        userRestWS.setInvoiceTemplateId(userWS.getInvoiceTemplateId());
        return userRestWS;
    }

    private UserWS getUserWS(Integer userId) {
        UserDTO user = new UserDAS().findNow(userId);
        if (null == user){
            throw new SessionInternalError("User not found",
                    new String[]{ "No user found with accountId "+ userId }, HttpStatus.SC_NOT_FOUND);
        }
        UserBL bl = new UserBL(user);
        return bl.getUserWS();
    }

    /**
     * Updates customer level meta fields
     * @param customerMetaFieldValueWS
     * @return
     */
    public CustomerMetaFieldValueWS updateCustomerMetaFields(CustomerMetaFieldValueWS customerMetaFieldValueWS) {
        MetaFieldValueWS[] metaFieldValues = MetaFieldHelper.convertAndValidateMFNameAndValueMapToMetaFieldValueWS(api.getCallerCompanyId(), EntityType.CUSTOMER,
                customerMetaFieldValueWS.getMetaFieldValues());
        logger.debug("request meta fields {} converted to {}", customerMetaFieldValueWS.getMetaFieldValues(), metaFieldValues);
        Integer userId = customerMetaFieldValueWS.getUserId();
        api.updateCustomerMetaFields(userId, metaFieldValues);
        logger.debug("{} updated on user {}", metaFieldValues, userId);
        return api.getCustomerMetaFields(userId);
    }

    /**
     * fetches all user's active {@link CustomerUsagePoolWS}
     * @param userId
     * @return
     */
    public CustomerUsagePoolWS[] getCustomerUsagePoolsByUserId(Integer userId) {
        if(null == userId) {
            logger.error("User {}, is null or empty", userId);
            throw new SessionInternalError("Please provide user id parameter", new String [] { "Please enter userId." },
                    HttpStatus.SC_BAD_REQUEST);
        }
        Integer entityId = api.getCallerCompanyId();
        UserDTO user = new UserDAS().findNow(userId);
        if(null == user) {
            logger.error("User {}, not found for entity {}", userId, entityId);
            throw new SessionInternalError("user id not found for entity " + entityId, new String [] { "Please enter valid user id." },
                    HttpStatus.SC_NOT_FOUND);
        }
        logger.debug("fetching customer usage pools for user {}", userId);
        return api.getCustomerUsagePoolsByCustomerId(user.getCustomer().getId());
    }

    private String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return  date != null ? formatter.format(date) : null;
    }
}
