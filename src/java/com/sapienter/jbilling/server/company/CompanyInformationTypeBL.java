package com.sapienter.jbilling.server.company;

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
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDTO;
import com.sapienter.jbilling.server.util.*;
import org.apache.log4j.Logger;

import javax.persistence.Transient;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic class for managing the company information types
 *
 * @author Aamir Ali
 * @since  02/21/2017
 */
public class CompanyInformationTypeBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CompanyInformationTypeBL.class));

    private CompanyInformationTypeDTO companyInformationTypeDTO = null;
    private CompanyInformationTypeDAS companyInformationTypeDAS = null;

    private void init() {
        companyInformationTypeDAS = new CompanyInformationTypeDAS();
        companyInformationTypeDTO = new CompanyInformationTypeDTO();
    }

    public CompanyInformationTypeBL() {
        init();
    }

    public CompanyInformationTypeBL(Integer companyInformationType) {
        init();
        setCompanyInformationType(companyInformationType);
    }

    public void setCompanyInformationType(Integer companyInformationType) {
        companyInformationTypeDTO = companyInformationTypeDAS.find(companyInformationType);
    }

    public CompanyInformationTypeDTO getCompanyInformationType() {
        return companyInformationTypeDTO;
    }

    public CompanyInformationTypeDTO create(CompanyInformationTypeDTO companyInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {
		// Validate Duplicate CIT meta fields based on preference.
		validateDuplicateCITMetaFields(companyInformationType);

        CompanyInformationTypeDTO cit = companyInformationTypeDAS.findByName(companyInformationType.getName(),
                companyInformationType.getEntityId());
        if (cit != null) {
            throw new SessionInternalError("Company Information Type", new String[]{
                    "CompanyInformationTypeDTO,name,companyInformationType.name.exists"
            });
        }
        companyInformationType.setDateCreated(TimezoneHelper.companyCurrentDate(companyInformationType.getEntityId()));
        saveMetaFields(companyInformationType, dependencyMetaFieldMap);
        
        companyInformationType = companyInformationTypeDAS.save(companyInformationType);
        return companyInformationType;
    }

    public void update(CompanyInformationTypeDTO companyInformationTypeDTO, Map<Integer, List<Integer>> dependencyMetaFieldMap) {
		// Validate Duplicate CIT meta fields based on preference.
		validateDuplicateCITMetaFields(companyInformationTypeDTO);
    	
        CompanyInformationTypeDTO dto = companyInformationTypeDAS.findNow(companyInformationTypeDTO.getId());
	    //workaround: touch on company type to load it

        dto.setDateUpdated(TimezoneHelper.companyCurrentDate(companyInformationTypeDTO.getEntityId()));
        dto.setDisplayOrder(companyInformationTypeDTO.getDisplayOrder());
        dto.setName(companyInformationTypeDTO.getName());
        saveMetaFields(dto, companyInformationTypeDTO, dependencyMetaFieldMap);
        companyInformationTypeDAS.save(dto);
    }

    private void removeCITMetaFieldDependency(Set<MetaField> metaFields) {
        if (metaFields != null && metaFields.size() > 0) {
            List<Integer> ids = metaFields.stream().map(metaField -> (metaField.getId()))
                    .collect(Collectors.toList());
            if (ids.size() > 0) new MetaFieldDAS().removeMetaFieldDependency(ids);
        }
    }

    private void deleteCitMetaFields() {
        Set<MetaField> metaFields = companyInformationTypeDTO.getMetaFields();
        //release the dependent meta field dependency
        removeCITMetaFieldDependency(metaFields);
        if (metaFields != null) {
            MetaFieldBL bl = new MetaFieldBL();
            for (MetaField metaField : metaFields) {
                bl.delete(metaField.getId());
            }
        }
    }

    public boolean delete() {
        //first delete CIT metafield group for Dissociating one side of the many-to-many association
        companyInformationTypeDAS.delete(companyInformationTypeDTO);
        //delete CIT metafield associated with this group
        deleteCitMetaFields();
        return true;
    }

    private void saveMetaFields(CompanyInformationTypeDTO companyInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {

        Set<MetaField> metafields = new HashSet<MetaField>(companyInformationType.getMetaFields());
        companyInformationType.getMetaFields().clear();
        Map<String, MetaField> metaFieldsMap = new HashMap<String, MetaField>();
        for (MetaField mf : metafields) {
            if (mf.getId() <= 0) {
                metaFieldsMap.put(mf.getName(), new MetaFieldBL().create(mf));
            } else {
                new MetaFieldBL().update(mf);
                companyInformationType.getMetaFields().add(new MetaFieldDAS().find(mf.getId()));
            }
        }
        //set dependent metafield
        if (metaFieldsMap != null && metaFieldsMap.size() > 0) {
            for (MetaField metaField : metafields) {
                if (metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0) {
                    Set<MetaField> dependentMetafields = new HashSet<MetaField>();
                    MetaField rootMetaField = metaFieldsMap.get(metaField.getName());
                    for (MetaField dependentMetaField : metaField.getDependentMetaFields()) {
                        if (metaFieldsMap.get(dependentMetaField.getName()) == null && dependentMetaField.getId() > 0){
                            //search meta field by company type for doing dependency in a same company type meta field.
                            dependentMetaField = new MetaFieldDAS().getMetaFieldByCompany(dependentMetaField.getId(), companyInformationType.getEntityId(), companyInformationType.getCompany().getId());
                        }else {
                            dependentMetaField = metaFieldsMap.get(dependentMetaField.getName());
                        }
                        if(dependentMetaField != null)dependentMetafields.add(dependentMetaField);
                    }
                    if (rootMetaField != null) rootMetaField.setDependentMetaFields(dependentMetafields);
                }
            }
            companyInformationType.getMetaFields().addAll(new HashSet<MetaField>(metaFieldsMap.values()));
        }
    }

    private void saveMetaFields(CompanyInformationTypeDTO persistedCompanyInformationType,
                                CompanyInformationTypeDTO companyInformationType, Map<Integer, List<Integer>> dependencyMetaFieldMap) {

        Map<Integer, Collection<MetaField>> diffMap = Util.calculateCollectionDifference(
                persistedCompanyInformationType.getMetaFields(),
                companyInformationType.getMetaFields(),
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

        persistedCompanyInformationType.getMetaFields().clear();

        for (MetaField mf : diffMap.get(0)) {
            new MetaFieldBL().update(mf);
            MetaField metaField = new MetaFieldDAS().find(mf.getId());
            persistedCompanyInformationType.getMetaFields().add(metaField);
            //remove meta-field dependency, if meta-field doesn't have any dependent meta-field
            if ((mf.getDependentMetaFields() == null || mf.getDependentMetaFields().size() == 0) && metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0) {
                metaField.setDependentMetaFields(null);
                new MetaFieldDAS().save(metaField);
            }
        }

        for (MetaField mf : diffMap.get(-1)) {
            //check the meta field dependency exits in newly updated meta field
            boolean isDependencyExistInPersistedMf = persistedCompanyInformationType.getMetaFields().stream().filter(metaField -> metaField.getDependentMetaFields() != null && metaField.getDependentMetaFields().size() > 0
                    && metaField.getDependentMetaFields().stream().filter(dependentMetaField -> dependentMetaField.getId() == mf.getId()).findAny().isPresent()).findAny().isPresent();

            if(new MetaFieldDAS().isDependencyExist(mf.getId()) && isDependencyExistInPersistedMf) {
                diffMap.clear();
                throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object",
                        new String[]{"metafield.dependency.error," + mf.getName()});
            }
            mf.setDependentMetaFields(null);
            new MetaFieldBL().delete(mf.getId());
            persistedCompanyInformationType.getMetaFields().remove(mf);
        }

        for (MetaField mf : diffMap.get(1)) {
            Integer fakeId = mf.getId();
            MetaField metaField = new MetaFieldBL().create(mf);
            if(fakeId < 0) {
                Set<Integer> mapSet = new HashSet<Integer>(dependencyMetaFieldMap.keySet());
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
            persistedCompanyInformationType.getMetaFields().add(metaField);
        }
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        for(Map.Entry<Integer, List<Integer>> entry : dependencyMetaFieldMap.entrySet()) {
            MetaField metaField = metaFieldDAS.find(entry.getKey());
            Set<MetaField> dependentMetFieldList = new HashSet<MetaField>();
            for(Integer dependentMetaFieldId: entry.getValue()) {
                dependentMetFieldList.add(metaFieldDAS.find(dependentMetaFieldId));
            }
            metaField.setDependentMetaFields(dependentMetFieldList);
            metaFieldDAS.save(metaField);
        }
    }

    public CompanyInformationTypeWS getWS() {
		CompanyInformationTypeWS companyInformationTypeWS = getWS(companyInformationTypeDTO);

		return companyInformationTypeWS;
    }
    
    public static final CompanyInformationTypeWS getWS(CompanyInformationTypeDTO dto) {
		CompanyInformationTypeWS companyInformationTypeWS = getCompanyInformationTypeWS(dto);

        //MetaFieldGroupBL.getWS(dto);
		companyInformationTypeWS.setName(dto.getName());
		companyInformationTypeWS.setCompanyId(dto.getCompany().getId());

		return companyInformationTypeWS;
    }

    public static final CompanyInformationTypeWS getCompanyInformationTypeWS(MetaFieldGroup dto){
        CompanyInformationTypeWS ws = new CompanyInformationTypeWS();
        if(null != dto){

            ws.setId(dto.getId());

            ws.setDateCreated(dto.getDateCreated());
            ws.setDateUpdated(dto.getDateUpdated());
            ws.setDisplayOrder(dto.getDisplayOrder());
            ws.setEntityId(dto.getEntityId());
            ws.setEntityType(dto.getEntityType());

            if(dto.getMetaFields()!=null && dto.getMetaFields().size()>0){
                Set<MetaFieldWS> tmpMetaFields=new HashSet<MetaFieldWS>();
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
	public static final CompanyInformationTypeDTO getDTO(CompanyInformationTypeWS ws,Integer entityId) {

		CompanyInformationTypeDTO cit = new CompanyInformationTypeDTO();

        cit.setDisplayOrder(ws.getDisplayOrder());
        cit.setEntityId(entityId);
        cit.setEntityType(null == ws.getEntityType() ? ws.getEntityType() : EntityType.COMPANY_INFO);
        cit.setId(ws.getId());
        try {

            MetaField metaField;
            Set<MetaField> metafieldsDTO = new HashSet<MetaField>();
            Map<Integer, MetaField> metaFieldsMap = new HashMap<Integer, MetaField>();
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
                            Set<MetaField> dependentMetafields = new HashSet<MetaField>();
                            MetaField rootMetaField = metaFieldsMap.get(metafieldWS.getId());
                            for (Integer dependentMetaFieldId : metafieldWS.getDependentMetaFields()) {
                                MetaField dependentMetafield = null;
                                if (metaFieldsMap.get(dependentMetaFieldId) == null && dependentMetaFieldId > 0){
                                    //search meta field by company type for doing dependency in a same company type meta field.
                                    dependentMetafield = new MetaFieldDAS().getMetaFieldByCompany(dependentMetaFieldId, entityId, ws.getCompanyId());
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
            cit.setMetaFields(metafieldsDTO);


            if (ws.getId() > 0) {
                List<InternationalDescriptionWS> descriptions = ws.getDescriptions();
                for (InternationalDescriptionWS description : descriptions) {
                    if (description.getLanguageId() != null
                            && description.getContent() != null) {
                        if (description.isDeleted()) {
                            cit.deleteDescription(description
                                    .getLanguageId());
                        } else {
                            cit.setDescription(description.getContent(),
                                    description.getLanguageId());
                        }
                    }
                }
            }

            cit.setName(ws.getName());

            if(ws.getCompanyId() != null){
                CompanyDTO companyTypeDTO = new CompanyDAS().find(ws.getCompanyId());
                if(companyTypeDTO!=null){
                    cit.setCompany(companyTypeDTO);
                }
            }
        } catch (Exception e) {
        	
            throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object", e,
                    new String[] { "MetaFieldGroupWS,metafieldGroups,cannot.convert.metafieldgroupws.error" });
        }

		return  cit;
	}
    public List<CompanyInformationTypeDTO> getCompanyInformationTypes(Integer companyTypeId) {
        return companyInformationTypeDAS.getInformationTypesForCompany(companyTypeId);
    }
    
    public static boolean checkDuplicateCIT(CompanyInformationTypeDTO  cit) {
		 HashSet<MetaFieldType> unique = new HashSet<MetaFieldType>();
		 boolean isDuplicate = false;
		 if(cit.getMetaFields() != null) {
			 outer:
			 for(MetaField metaField : cit.getMetaFields()) {
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

    public static Map<Integer, List<Integer>> getMetaFieldDependency(CompanyInformationTypeWS companyInformationTypeWS) {
        MetaFieldWS[] metaFieldWSes = companyInformationTypeWS.getMetaFields();
        Map<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
        for (MetaFieldWS metaFieldWS : metaFieldWSes) {
            if (metaFieldWS.getDependentMetaFields() != null && metaFieldWS.getDependentMetaFields().length > 0) {
                map.put(metaFieldWS.getFakeId(), Arrays.asList(metaFieldWS.getDependentMetaFields()));
            }
        }
        return map;
    }

    private void validateDuplicateCITMetaFields(CompanyInformationTypeDTO companyInformationType) {
		Integer entityId = companyInformationType.getCompany().getId();
		if(null != companyInformationType.getCompany().getParent()){
			entityId = companyInformationType.getCompany().getParent().getId();
		}

		Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, com.sapienter.jbilling.client.util.Constants.PREFERENCE_ALLOW_DUPLICATE_META_FIELDS_IN_COPY_COMPANY);
		boolean shouldAllowCheckDuplicateCIT = (prefValue != null && prefValue.intValue() == 1) ? false : true;

		if( shouldAllowCheckDuplicateCIT && checkDuplicateCIT(companyInformationType) ) {
			throw new SessionInternalError("Company Information Type should be unique", new String[] {
		             "CompanyInformationTypeWS,metaFields,metafield.validation.cit.unique"
		     });
		}
    }
}
