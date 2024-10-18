package com.sapienter.jbilling.server.account;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic class for managing the account information types
 *
 * @author Panche Isajeski
 * @since 05/23/2013
 */
public class AccountInformationTypeBL {

    private static final Logger logger = LoggerFactory.getLogger(AccountInformationTypeBL.class);

    private AccountInformationTypeDTO accountInformationTypeDTO = null;
    private AccountInformationTypeDAS accountInformationTypeDAS = null;
   
    private void init() {
        accountInformationTypeDAS = new AccountInformationTypeDAS();
        accountInformationTypeDTO = new AccountInformationTypeDTO();
    }

    public AccountInformationTypeBL() {
        init();
    }

    public AccountInformationTypeBL(Integer accountInformationType) {
        init();
        setAccountInformationType(accountInformationType);
    }

    public void setAccountInformationType(Integer accountInformationType) {
        accountInformationTypeDTO = accountInformationTypeDAS.find(accountInformationType);
    }

    public AccountInformationTypeDTO getAccountInformationType() {
        return accountInformationTypeDTO;
    }

    public AccountInformationTypeDTO create(AccountInformationTypeDTO accountInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {
		// Validate Duplicate AIT meta fields based on preference.
		validateDuplicateAITMetaFields(accountInformationType);

        AccountInformationTypeDTO ait = accountInformationTypeDAS.findByName(accountInformationType.getName(),
                accountInformationType.getEntityId(), accountInformationType.getAccountType().getId());
        if (ait != null) {
            throw new SessionInternalError("Account Information Type", new String[]{
                    "AccountInformationTypeDTO,name,accountInformationType.name.exists"
            });
        }
        accountInformationType.setDateCreated(TimezoneHelper.companyCurrentDate(accountInformationType.getEntityId()));
        saveMetaFields(accountInformationType, dependencyMetaFieldMap);
        boolean useForNotifications=accountInformationType.isUseForNotifications();
        accountInformationType = accountInformationTypeDAS.save(accountInformationType);
        accountInformationType.setUseForNotifications(useForNotifications);
        return accountInformationType;
    }

    public void update(AccountInformationTypeDTO accountInformationTypeDTO, Map<Integer, List<Integer>> dependencyMetaFieldMap) {
		// Validate Duplicate AIT meta fields based on preference.
		validateDuplicateAITMetaFields(accountInformationTypeDTO);
    	
        AccountInformationTypeDTO dto = accountInformationTypeDAS.findNow(accountInformationTypeDTO.getId());

        dto.setDateUpdated(TimezoneHelper.companyCurrentDate(accountInformationTypeDTO.getEntityId()));
        dto.setDisplayOrder(accountInformationTypeDTO.getDisplayOrder());
        dto.setName(accountInformationTypeDTO.getName());
        dto.setUseForNotifications(accountInformationTypeDTO.isUseForNotifications());
        saveMetaFields(dto, accountInformationTypeDTO, dependencyMetaFieldMap);
        accountInformationTypeDAS.save(dto);
    }

    private void removeAITMetaFieldDependency(Set<MetaField> metaFields) {
        if (metaFields != null && metaFields.size() > 0) {
            List<Integer> ids = metaFields.stream().map(metaField -> (metaField.getId()))
                    .collect(Collectors.toList());
            if (ids.size() > 0) new MetaFieldDAS().removeMetaFieldDependency(ids);
        }
    }

    private void deleteAitMetaFields() {
        Set<MetaField> metaFields = accountInformationTypeDTO.getMetaFields();
        //release the dependent meta field dependency
        removeAITMetaFieldDependency(metaFields);
        if (metaFields != null) {
            MetaFieldBL bl = new MetaFieldBL();
            for (MetaField metaField : metaFields) {
                bl.delete(metaField.getId());
            }
        }
    }

    public boolean delete() {
        //first delete AIT metafield group for Dissociating one side of the many-to-many association
        accountInformationTypeDAS.delete(accountInformationTypeDTO);
        // delete AIT metafield associated with this group
        deleteAitMetaFields();
        return true;
    }

    private void saveMetaFields(AccountInformationTypeDTO accountInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {

        Set<MetaField> metafields = new HashSet<MetaField>(accountInformationType.getMetaFields());
        accountInformationType.getMetaFields().clear();
        Map<String, MetaField> metaFieldsMap = new HashMap<>();
        for (MetaField mf : metafields) {
            if (mf.getId() <= 0) {
                metaFieldsMap.put(mf.getName(), new MetaFieldBL().create(mf));
            } else {
                new MetaFieldBL().update(mf);
                accountInformationType.getMetaFields().add(new MetaFieldDAS().find(mf.getId()));
            }
        }
        //set dependent metafield
        if (metaFieldsMap != null && metaFieldsMap.size() > 0) {
            for (MetaField metaField : metafields) {
                if (metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0) {
                    Set<MetaField> dependentMetafields = new HashSet<>();
                    MetaField rootMetaField = metaFieldsMap.get(metaField.getName());
                    for (MetaField dependentMetaField : metaField.getDependentMetaFields()) {
                        if (metaFieldsMap.get(dependentMetaField.getName()) == null && dependentMetaField.getId() > 0){
                            //search meta field by account type for doing dependency in a same account type meta field.
                            dependentMetaField = new MetaFieldDAS().getMetaFieldByAccountType(dependentMetaField.getId(), accountInformationType.getEntityId(), accountInformationType.getAccountType().getId());
                        }else {
                            dependentMetaField = metaFieldsMap.get(dependentMetaField.getName());
                        }
                        if(dependentMetaField != null)dependentMetafields.add(dependentMetaField);
                    }
                    if (rootMetaField != null) rootMetaField.setDependentMetaFields(dependentMetafields);
                }
            }
            accountInformationType.getMetaFields().addAll(new HashSet<MetaField>(metaFieldsMap.values()));
        }
    }

    private void saveMetaFields(AccountInformationTypeDTO persistedAccountInformationType,
                                AccountInformationTypeDTO accountInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {

        Map<Integer, Collection<MetaField>> diffMap = Util.calculateCollectionDifference(
                persistedAccountInformationType.getMetaFields(),
                accountInformationType.getMetaFields(),
                new Util.IIdentifier<MetaField>() {

                    @Override
                    public boolean evaluate(MetaField input, MetaField output) {
                        if (input.getId() != 0 && output.getId() != 0) {
                            return input.getId() == output.getId();
                        } else {
                            return input.getName().equals(output.getName());
                        }
                    }

                    @Override
                    public void setIdentifier(MetaField input, MetaField output) {
                        output.setId(input.getId());
                    }
                });

        persistedAccountInformationType.getMetaFields().clear();

        for (MetaField mf : diffMap.get(0)) {
            new MetaFieldBL().update(mf);
            MetaField metaField = new MetaFieldDAS().find(mf.getId());
            persistedAccountInformationType.getMetaFields().add(metaField);
            //remove meta-field dependency, if meta-field doesn't have any dependent meta-field
            if ((mf.getDependentMetaFields() == null || mf.getDependentMetaFields().size() == 0) && metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0) {
                metaField.setDependentMetaFields(null);
                new MetaFieldDAS().save(metaField);
            }
        }

        for (MetaField mf : diffMap.get(-1)) {
            //check the meta field dependency exits in newly updated meta field
            boolean isDependencyExistInPersistedMf = persistedAccountInformationType.getMetaFields().stream().filter(metaField -> metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0
                    && metaField.getDependentMetaFields().stream().filter(dependentMetaField -> dependentMetaField.getId() == mf.getId()).findAny().isPresent()).findAny().isPresent();

            if(new MetaFieldDAS().isDependencyExist(mf.getId()) && isDependencyExistInPersistedMf) {
                diffMap.clear();
                throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object",
                        new String[]{"metafield.dependency.error," + mf.getName()});
            }
            mf.setDependentMetaFields(null);
            new MetaFieldBL().delete(mf.getId());
            persistedAccountInformationType.getMetaFields().remove(mf);
        }

        for (MetaField mf : diffMap.get(1)) {
            Integer fakeId = mf.getId();
            MetaField metaField = new MetaFieldBL().create(mf);
            if(fakeId < 0) {
                Set<Integer> mapSet = new HashSet<>(dependencyMetaFieldMap.keySet());
                for(Integer key: mapSet) {
                    if(key.equals(fakeId)) {
                        List<Integer> metaFieldList = dependencyMetaFieldMap.remove(fakeId);
                        if(metaFieldList.contains(fakeId)) {
                            metaFieldList.set(metaFieldList.indexOf(fakeId), metaField.getId());
                        }
                        dependencyMetaFieldMap.put(metaField.getId(), metaFieldList);
                    } else {
                        List<Integer> metaFieldList = dependencyMetaFieldMap.remove(key);
                        if(metaFieldList.contains(fakeId)) {
                            metaFieldList.set(metaFieldList.indexOf(fakeId), metaField.getId());
                        }
                        dependencyMetaFieldMap.put(key, metaFieldList);
                    }
                }
            }
            persistedAccountInformationType.getMetaFields().add(metaField);
        }
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        for(Map.Entry<Integer, List<Integer>> entry : dependencyMetaFieldMap.entrySet()) {
            MetaField metaField = metaFieldDAS.find(entry.getKey());
            Set<MetaField> dependentMetFieldList = new HashSet<>();
            for(Integer dependentMetaFieldId: entry.getValue()) {
                dependentMetFieldList.add(metaFieldDAS.find(dependentMetaFieldId));
            }
            metaField.setDependentMetaFields(dependentMetFieldList);
            metaFieldDAS.save(metaField);
        }
    }

    public AccountInformationTypeWS getWS() {
		AccountInformationTypeWS accountInformationTypeWS = getWS(accountInformationTypeDTO);

		return accountInformationTypeWS;
    }
    
    public static final AccountInformationTypeWS getWS(AccountInformationTypeDTO dto) {
		AccountInformationTypeWS accountInformationTypeWS = getAccountInformationTypeWS(dto);

		//MetaFieldGroupBL.getWS(dto);
		accountInformationTypeWS.setName(dto.getName());
		accountInformationTypeWS.setAccountTypeId(dto.getAccountType().getId());
		accountInformationTypeWS.setUseForNotifications(dto.isUseForNotifications());

		return accountInformationTypeWS;
    }

    public static final AccountInformationTypeWS getAccountInformationTypeWS(MetaFieldGroup dto){
        AccountInformationTypeWS ws = new AccountInformationTypeWS();
        if(null != dto){

            ws.setId(dto.getId());

            ws.setDateCreated(dto.getDateCreated());
            ws.setDateUpdated(dto.getDateUpdated());
            ws.setDisplayOrder(dto.getDisplayOrder());
            ws.setEntityId(dto.getEntityId());
            ws.setEntityType(dto.getEntityType());

            if(dto.getMetaFields()!=null && dto.getMetaFields().size()>0){
                Set<MetaFieldWS> tmpMetaFields=new HashSet<>();
                for(MetaField metafield:dto.getMetaFields()){
                    tmpMetaFields.add(MetaFieldBL.getWS(metafield));
                }
                ws.setMetaFields(tmpMetaFields.toArray(new MetaFieldWS[tmpMetaFields.size()]));
            }
            if(dto.getDescription(Constants.LANGUAGE_ENGLISH_ID)!=null){
                List<InternationalDescriptionWS> tmpDescriptions=new ArrayList<InternationalDescriptionWS>(1);
                tmpDescriptions.add(DescriptionBL.getInternationalDescriptionWS(dto.getDescriptionDTO(Constants.LANGUAGE_ENGLISH_ID)));
                ws.setDescriptions(tmpDescriptions);
            }

        }
        return ws;
    }
    
    @Transient
	public static final AccountInformationTypeDTO getDTO(AccountInformationTypeWS ws,Integer entityId) {

		AccountInformationTypeDTO ait = new AccountInformationTypeDTO();

        ait.setDisplayOrder(ws.getDisplayOrder());
        ait.setEntityId(entityId);
        ait.setEntityType(null == ws.getEntityType() ? ws.getEntityType() : EntityType.ACCOUNT_TYPE);
        ait.setId(ws.getId());
        try {

            MetaField metaField;
            Set<MetaField> metafieldsDTO = new HashSet<>();
            Map<Integer, MetaField> metaFieldsMap = new HashMap<>();
            if (ws.getMetaFields() != null) {
                Map<Integer, List<Integer>> dependencyMap = getMetaFieldDependency(ws);
                //TODO: remove this dependencyStatus check and use metafield name as a key in a metaFieldsMap.
                boolean dependencyStatus = dependencyMap != null && dependencyMap.size() > 0 ? true : false;
                for (MetaFieldWS metafieldWS : ws.getMetaFields()) {
                    metaField = MetaFieldBL.getDTO(metafieldWS,entityId);
                    if(dependencyStatus){
                        metaFieldsMap.put(metaField.getId(), metaField);
                    }else {
                        metafieldsDTO.add(metaField);
                    }
                }
                //set metafield with dependency
                if (metaFieldsMap != null && metaFieldsMap.size() > 0) {
                    for (MetaFieldWS metafieldWS : ws.getMetaFields()) {
                        if (metafieldWS.getDependentMetaFields() != null && metafieldWS.getDependentMetaFields().length > 0) {
                            Set<MetaField> dependentMetafields = new HashSet<>();
                            MetaField rootMetaField = metaFieldsMap.get(metafieldWS.getId());
                            for (Integer dependentMetaFieldId : metafieldWS.getDependentMetaFields()) {
                                MetaField dependentMetafield = null;
                                if (metaFieldsMap.get(dependentMetaFieldId) == null && dependentMetaFieldId > 0){
                                    //search meta field by account type for doing dependency in a same account type meta field.
                                    dependentMetafield = new MetaFieldDAS().getMetaFieldByAccountType(dependentMetaFieldId, entityId, ws.getAccountTypeId());
                                }else {
                                    dependentMetafield = metaFieldsMap.get(dependentMetaFieldId);
                                }
                                if(dependentMetafield != null)dependentMetafields.add(dependentMetafield);
                            }
                            if (rootMetaField != null) rootMetaField.setDependentMetaFields(dependentMetafields);
                        }
                    }
                    metafieldsDTO = new HashSet<MetaField>(metaFieldsMap.values());
                }
            }
            ait.setMetaFields(metafieldsDTO);


            if (ws.getId() > 0) {
                List<InternationalDescriptionWS> descriptions = ws.getDescriptions();
                for (InternationalDescriptionWS description : descriptions) {
                    if (description.getLanguageId() != null
                            && description.getContent() != null) {
                        if (description.isDeleted()) {
                            ait.deleteDescription(description
                                    .getLanguageId());
                        } else {
                            ait.setDescription(description.getContent(),
                                    description.getLanguageId());
                        }
                    }
                }
            }

            ait.setName(ws.getName());
            ait.setUseForNotifications(ws.isUseForNotifications());

            if(ws.getAccountTypeId() !=null){
                AccountTypeDTO accountTypeDTO = new AccountTypeDAS().find(ws.getAccountTypeId());
                if(accountTypeDTO!=null){
                    ait.setAccountType(accountTypeDTO);
                }
            }
        } catch (Exception e) {
        	
            throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object", e,
                    new String[] { "MetaFieldGroupWS,metafieldGroups,cannot.convert.metafieldgroupws.error" });
        }

		return  ait;
	}
    public List<AccountInformationTypeDTO> getAccountInformationTypes(Integer accountTypeId) {
        return accountInformationTypeDAS.getInformationTypesForAccountType(accountTypeId);
    }
    
    public static boolean checkDuplicateAIT(AccountInformationTypeDTO  ait) {
		 HashSet<MetaFieldType> unique = new HashSet<>();
		 boolean isDuplicate = false;
		 if(ait.getMetaFields() != null) {
			 outer:
			 for(MetaField metaField : ait.getMetaFields()) {
				 if(metaField.getFieldUsage() != null) {
                     if(!unique.add( metaField.getFieldUsage() )) {
                        isDuplicate = true;
                        break outer;
                     }
				 }
			 }
		 }	 
		 return isDuplicate;
	}


    public static Map<Integer, List<Integer>> getMetaFieldDependency(AccountInformationTypeWS accountInformationTypeWS) {
        MetaFieldWS[] metaFieldWSes = accountInformationTypeWS.getMetaFields();
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (MetaFieldWS metaFieldWS : metaFieldWSes) {
            if (metaFieldWS.getDependentMetaFields() != null && metaFieldWS.getDependentMetaFields().length > 0) {
                map.put(metaFieldWS.getFakeId(), Arrays.asList(metaFieldWS.getDependentMetaFields()));
            }
        }
        return map;
    }

    private void validateDuplicateAITMetaFields(AccountInformationTypeDTO accountInformationType) {
		Integer entityId = accountInformationType.getAccountType().getCompany().getId();
		if(null != accountInformationType.getAccountType().getCompany().getParent()){
			entityId = accountInformationType.getAccountType().getCompany().getParent().getId();
		}

		Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, com.sapienter.jbilling.client.util.Constants.PREFERENCE_ALLOW_DUPLICATE_META_FIELDS_IN_COPY_COMPANY);
		boolean shouldAllowCheckDuplicateAIT = (prefValue != null && prefValue.intValue() == 1) ? false : true;

		if( shouldAllowCheckDuplicateAIT && checkDuplicateAIT(accountInformationType) ) {
			throw new SessionInternalError("Account Information Type should be unique", new String[] {
		             "AccountInformationTypeWS,metaFields,metafield.validation.ait.unique"
		     });
		}
    }
}
