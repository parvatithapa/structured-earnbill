package com.sapienter.jbilling.server.company.task;

import com.google.common.collect.ObjectArrays;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.account.AccountInformationTypeBL;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.metafields.db.*;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by vivek on 12/11/14.
 */
public class AccountInformationTypeCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AccountInformationTypeCopyTask.class));

    AccountTypeDAS accountTypeDAS = null;
    MetaFieldDAS metaFieldDAS = null;
    AccountInformationTypeDAS accountInformationTypeDAS = null;
    MetaFieldGroupDAS metaFieldGroupDAS = null;

    private static final Class dependencies[] = new Class[]{
            MetaFieldsCopyTask.class,
            AccountTypeCopyTask.class
    };

    private void init() {
        accountTypeDAS = new AccountTypeDAS();
        metaFieldDAS = new MetaFieldDAS();
        accountInformationTypeDAS = new AccountInformationTypeDAS();
        metaFieldGroupDAS = new MetaFieldGroupDAS();
    }

    public AccountInformationTypeCopyTask() {
        init();
    }

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<AccountInformationTypeDTO> accountInformationTypeDTOs = accountInformationTypeDAS.getAvailableAccountInformationTypes(targetEntityId);
        return accountInformationTypeDTOs != null && !accountInformationTypeDTOs.isEmpty();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create AccountInformationTypeCopyTask");
        copyAccountInformationType(entityId, targetEntityId);
    }

    public void copyAccountInformationType(Integer entityId, Integer targetEntityId) {
    	Integer languageId = new CompanyDAS().find(entityId).getLanguageId();
        List<AccountTypeDTO> accountTypeDTOsForEntity = accountTypeDAS.findAll(entityId);
        List<AccountTypeDTO> accountTypeDTOsForTargetEntity = accountTypeDAS.findAll(targetEntityId);
        for (AccountTypeDTO accountTypeDTO : accountTypeDTOsForEntity) {
            accountTypeDAS.reattach(accountTypeDTO);
            Integer copyAccountTypeId = 0;
            for (AccountTypeDTO copyAccountTypeDTO : accountTypeDTOsForTargetEntity) {
                if (copyAccountTypeDTO.getDescription(languageId).equals(accountTypeDTO.getDescription(languageId))) {
                    copyAccountTypeId = copyAccountTypeDTO.getId();
                }
            }

            AccountInformationTypeBL aitBL = new AccountInformationTypeBL();
            for (AccountInformationTypeDTO accountInformationTypeDTO : aitBL.getAccountInformationTypes(accountTypeDTO.getId())) {

                Set<AccountInformationTypeDTO> copyAccountInformationTypeDTOs = new HashSet<AccountInformationTypeDTO>();
                accountInformationTypeDAS.reattach(accountInformationTypeDTO);

                AccountInformationTypeDTO copyAccountInformationType = accountInformationTypeDAS.findByName(accountInformationTypeDTO.getName(), targetEntityId, copyAccountTypeId);
                if (copyAccountInformationType == null) {
                    AccountInformationTypeWS informationTypeWS = AccountInformationTypeBL.getWS(accountInformationTypeDTO);
                    informationTypeWS.setId(0);
                    informationTypeWS.setAccountTypeId(copyAccountTypeId);
                    informationTypeWS.setEntityId(targetEntityId);
                    Boolean isEmailMetaFieldExist = Boolean.FALSE;
                    MetaFieldWS[] oldMetaFields = informationTypeWS.getMetaFields();
                    List<MetaFieldWS> newMetaFields = new LinkedList<MetaFieldWS>();
                    MetaFieldBL metaFieldBL = new MetaFieldBL();
                    for (MetaFieldWS metaFieldWS : oldMetaFields) {
//                        if(metaFieldWS.getEntityType().equals(MetaFieldType.EMAIL)) {
//                            isEmailMetaFieldExist = Boolean.TRUE;
//                        }
                        if (metaFieldWS.getEntityId().equals(entityId)) {
                            MetaField metaField = metaFieldDAS.find(CopyCompanyUtils.oldNewMetaFieldMap.get(metaFieldWS.getId()));
                            newMetaFields.add(metaFieldBL.getWS(metaField));

//                            metaFieldWS.setEntityId(targetEntityId);
//                            metaFieldWS.setId(metaField.getId());
//
//                            if (metaFieldWS.getValidationRule() != null) {
//                                metaFieldWS.getValidationRule().setId(metaField.getValidationRule().getId());
//                            }
                        }
                    }

                    informationTypeWS.setMetaFields(newMetaFields.toArray(new MetaFieldWS[newMetaFields.size()]));
                    /*if (!isEmailMetaFieldExist) {
                        MetaFieldWS metaFieldWS = new MetaFieldWS();
                        metaFieldWS.setName("contact.email");
                        metaFieldWS.setEntityId(targetEntityId);
                        metaFieldWS.setDisplayOrder(1);
                        metaFieldWS.setFieldUsage(MetaFieldType.EMAIL);
                        metaFieldWS.setId(0);
                        metaFieldWS.setMandatory(true);
                        metaFieldWS.setDataType(DataType.STRING);
                        metaFieldWS.setDisabled(false);
                        metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
                        ValidationRuleWS validationRuleWS = new ValidationRuleWS();
                        validationRuleWS.setId(0);
                        validationRuleWS.setEnabled(false);
                        validationRuleWS.setRuleType(ValidationRuleType.EMAIL.name());
                        MetaFieldWS[] metaFieldWSes = ObjectArrays.concat(informationTypeWS.getMetaFields(), new MetaFieldWS[]{metaFieldWS}, MetaFieldWS.class);
                        informationTypeWS.setMetaFields(metaFieldWSes);
                    }*/


                    AccountInformationTypeDTO ait = createAccountInformationType(informationTypeWS, targetEntityId);


                    MetaFieldGroup metaFieldGroup = metaFieldGroupDAS.find(ait.getId());

                    copyAccountInformationTypeDTOs.add(ait);
                    accountTypeDTO.setInformationTypes(copyAccountInformationTypeDTOs);

                    accountTypeDTO = new AccountTypeDAS().save(accountTypeDTO);
                }
            }
        }
        LOG.debug("AccountInformationTypeCopyTask has been completed successfully");
    }

    public AccountInformationTypeDTO createAccountInformationType(AccountInformationTypeWS accountInformationType, Integer targetEntityId) {

        if (accountInformationType.getMetaFields() != null) {
            for (MetaFieldWS field : accountInformationType.getMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT) &&
                        (null == field.getFilename() || field.getFilename().isEmpty())) {
                    throw new SessionInternalError("Script Meta Fields must define filename", new String[]{
                            "AccountInformationTypeWS,metaFields,metafield.validation.filename.required"
                    });
                }
            }
        }
        AccountInformationTypeDTO dto = AccountInformationTypeBL.getDTO(accountInformationType, targetEntityId);
        dto = new AccountInformationTypeBL().create(dto, new HashMap<Integer, List<Integer>>());

        return dto;
    }
}
