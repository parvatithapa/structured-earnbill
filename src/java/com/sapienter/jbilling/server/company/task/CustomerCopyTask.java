package com.sapienter.jbilling.server.company.task;

import com.google.common.collect.ObjectArrays;
import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 30/10/14.
 */
public class CustomerCopyTask extends AbstractCopyTask {

    AccountInformationTypeDAS accountInformationTypeDAS = null;
    MetaFieldDAS metaFieldDAS = null;
    UserDAS userDAS = null;
    AccountTypeDAS accountTypeDAS = null;


    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CustomerCopyTask.class));

    private static final Class dependencies[] = new Class[]{
            MetaFieldsCopyTask.class,
            AccountInformationTypeCopyTask.class,
            AccountTypeCopyTask.class,
            BillingProcessConfigurationCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<UserDTO> userDTOList = userDAS.findAllCustomers(targetEntityId);
        return userDTOList != null && !userDTOList.isEmpty();
    }

    public CustomerCopyTask() {
        init();
    }

    private void init() {
        accountInformationTypeDAS = new AccountInformationTypeDAS();
        metaFieldDAS = new MetaFieldDAS();
        userDAS = new UserDAS();
        accountTypeDAS = new AccountTypeDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.

        List<UserDTO> userDTOList = userDAS.findAllCustomers(entityId);
        Integer index = 0;
        LOG.debug("customer coping has been Started.");
        Map<Integer, Integer> oldNewUserMap = CopyCompanyUtils.oldNewUserMap;
        for (UserDTO userDTO : userDTOList) {
            UserDTOEx userDTOEx = new UserDTOEx(userDTO);
            UserWS userWS = UserBL.getWS(userDTOEx);
            userWS.setId(0);
            userWS.setMainRoleId(Constants.TYPE_CUSTOMER);
            userWS.setAccountTypeId(userDTO.getCustomer().getAccountType().getId());

            List<AccountTypeDTO> accountTypeDTOList = accountTypeDAS.findAll(targetEntityId);

            AccountTypeDTO accountTypeDTO = accountTypeDAS.find(userWS.getAccountTypeId(), entityId);
            AccountTypeDTO copyAccountTypeDTO = new AccountTypeDTO();
            for (AccountTypeDTO dto : accountTypeDTOList) {
                if ((dto.getDescription() != null) && dto.getDescription().equals(accountTypeDTO.getDescription())) {
                    copyAccountTypeDTO = dto;
                }
            }
            Boolean isEmailMetaFieldValueExist = Boolean.FALSE;
            Integer groupId = 0;
            for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {

                metaFieldValueWS.setId(0);
                groupId = metaFieldValueWS.getGroupId();
                if (groupId != null) {
                    AccountInformationTypeDTO informationTypeDTO = accountInformationTypeDAS.find(groupId);
                    AccountInformationTypeDTO copyInformationTypeDTO = accountInformationTypeDAS.findByName(informationTypeDTO.getName(), targetEntityId, copyAccountTypeDTO.getId());
                    metaFieldValueWS.setGroupId(copyInformationTypeDTO.getId());
                }
                if (metaFieldValueWS.getFieldName().equals("contact.email")) {
                    isEmailMetaFieldValueExist = Boolean.TRUE;
                }
            }
            if (!isEmailMetaFieldValueExist) {
                AccountInformationTypeDTO copyInformationTypeDTO = accountInformationTypeDAS.findByName("Contact", targetEntityId, copyAccountTypeDTO.getId());

                if(copyInformationTypeDTO != null) {
                    MetaFieldValueWS fieldValueWS = new MetaFieldValueWS();
                    fieldValueWS.setId(0);
                    fieldValueWS.setFieldName("contact.email");
                    fieldValueWS.setValue(userDTO.getUserName() + "@fakemail.com");
                    fieldValueWS.setGroupId(copyInformationTypeDTO.getId());
                    MetaFieldValueWS[] metaFieldValueWSes = userWS.getMetaFields();
                    MetaFieldValueWS[] newMetaFieldValueWSes = ObjectArrays.concat(metaFieldValueWSes, new MetaFieldValueWS[]{fieldValueWS}, MetaFieldValueWS.class);
                    userWS.setMetaFields(newMetaFieldValueWSes);
                }
            }
            userWS.setEntityId(targetEntityId);
            userWS.setAccountTypeId(copyAccountTypeDTO.getId());


            IWebServicesSessionBean local = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            Integer copyUserId = local.createUserWithCompanyId(userWS, targetEntityId);
            oldNewUserMap.put(userDTO.getId(), copyUserId);
            /*if (index++ > 1)
                break;*/

            try {
                userWS.close();
            } catch (Exception e) {
                LOG.debug("Exception: "+e);
            }

        }
        LOG.debug("customer coping has been completed.");
    }

    public static MetaFieldValueWS[] convertMetaFieldsToWS(Integer entityId, MetaContent entity, boolean allMetaFields) {
        // If this is a customer we retrieve only meta fields of type CUSTOMER to avoid duplicates from all the available
        // Account Types. Redmine Issue #6458
        EntityType[] types;
        if (entity instanceof CustomerDTO) {
            types = new EntityType[]{EntityType.CUSTOMER};
        } else {
            types = entity.getCustomizedEntityType();
        }

        List<MetaField> availableMetaFields = new MetaFieldDAS().getAvailableFields(
                entityId, types, allMetaFields ? null : true);

        MetaFieldValueWS[] result = new MetaFieldValueWS[]{};
        if (availableMetaFields != null && !availableMetaFields.isEmpty()) {
            result = new MetaFieldValueWS[availableMetaFields.size()];
            int i = 0;
            for (MetaField field : availableMetaFields) {
                Integer groupId = null;
                MetaFieldValue value = entity.getMetaField(field.getName(), groupId);
                if (value == null) {
                    value = field.createValue();
                }
                MetaFieldValueWS metaFieldValueWS = MetaFieldBL.getWS(value, groupId);
                result[i++] = metaFieldValueWS;
            }
        }
        return result;
    }
}
