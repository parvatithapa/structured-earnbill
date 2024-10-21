/*
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

package com.sapienter.jbilling.server.user;

import java.sql.SQLException;
import java.util.*;

import javax.naming.NamingException;
import javax.sql.rowset.CachedRowSet;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;

/**
 * @author Emil
 */
public class EntityBL extends ResultList 
        implements EntitySQL {
    private CompanyDAS das = null;
    private CompanyDTO entity = null;
    private EventLogger eLogger = null;
    
    public EntityBL()  {
        init();
    }
    
    public EntityBL(Integer id)  {
        init();
        entity = das.find(id);
    }

    /*
    public EntityBL(String externalId) 
            throws FinderException, NamingException {
        init();
        entity = entityHome.findByExternalId(externalId);
    }
    */
    
    public static final CompanyWS getCompanyWS(CompanyDTO companyDto) {
    	
    	CompanyWS ws = new CompanyWS();
        ws.setId(companyDto.getId());
        ws.setCurrencyId(companyDto.getCurrencyId());
        ws.setLanguageId(companyDto.getLanguageId());
        ws.setDescription(companyDto.getDescription());
        ws.setCustomerInformationDesign(companyDto.getCustomerInformationDesign());
        ws.setUiColor(companyDto.getUiColor());
        ws.setTimezone(companyDto.getTimezone());
        ws.setFailedEmailNotification(companyDto.getFailedEmailNotification());

        ws.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(ws.getId(), companyDto));

        if (null != companyDto.getCompanyInformationTypes() && companyDto.getCompanyInformationTypes().size() > 0) {
            List<Integer> informationTypeIds = new ArrayList<Integer>();
            for (CompanyInformationTypeDTO cit : companyDto.getCompanyInformationTypes()) {
                informationTypeIds.add(cit.getId());
            }
            if (!informationTypeIds.isEmpty()) {
                ws.setCompanyInformationTypes(informationTypeIds
                        .toArray(new Integer[informationTypeIds.size()]));
            }
        }

        // convert to Map<cit,values> map and set it in CompanyWS
        Map<Integer, ArrayList<MetaFieldValueWS>> companyInfoTypeFieldsMap = new HashMap<Integer, ArrayList<MetaFieldValueWS>>();
        for (CompanyInfoTypeMetaField companyInfoTypeField : companyDto.getCompanyInfoTypeMetaFields()) {
            Integer groupId = companyInfoTypeField.getCompanyInfoType().getId();
            if (companyInfoTypeFieldsMap.containsKey(companyInfoTypeField.getCompanyInfoType().getId())) {
                ArrayList<MetaFieldValueWS> metaFieldMap = companyInfoTypeFieldsMap
                        .get(companyInfoTypeField.getCompanyInfoType().getId());
                ArrayList<MetaFieldValueWS> valueList;

                if (metaFieldMap.contains(companyInfoTypeField)) {
                    valueList = metaFieldMap;
                    valueList.add(MetaFieldBL.getWS(companyInfoTypeField.getMetaFieldValue(), groupId));
                } else {
                    valueList = new ArrayList<MetaFieldValueWS>();
                    valueList.add(MetaFieldBL.getWS(companyInfoTypeField.getMetaFieldValue(), groupId));
                }

                List<MetaFieldValueWS> finalList = new ArrayList<MetaFieldValueWS>();
                finalList.addAll(metaFieldMap);
                finalList.addAll(valueList);
                companyInfoTypeFieldsMap.put(companyInfoTypeField.getCompanyInfoType().getId(),
                        (ArrayList<MetaFieldValueWS>) finalList);
            } else {
                ArrayList<MetaFieldValueWS> metaFieldMap = new ArrayList<MetaFieldValueWS>();
                List<MetaFieldValueWS> valueList = new ArrayList<MetaFieldValueWS>();

                valueList.add(MetaFieldBL.getWS(companyInfoTypeField.getMetaFieldValue(), groupId));
                metaFieldMap = (ArrayList<MetaFieldValueWS>) valueList;

                companyInfoTypeFieldsMap.put(companyInfoTypeField.getCompanyInfoType().getId(), metaFieldMap);
            }
        }
        ws.setCompanyInfoTypeFieldsMap(companyInfoTypeFieldsMap);

        // merge cit latest meta fields with customer meta fields
        List<MetaFieldValueWS> citMetaFields = new ArrayList<MetaFieldValueWS>();
        for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : companyInfoTypeFieldsMap.entrySet()) {
            citMetaFields.addAll(companyInfoTypeFieldsMap.get(entry.getKey()));
        }
        
        MetaFieldValueWS[] citMetaFieldsArray = citMetaFields.toArray(new MetaFieldValueWS[citMetaFields.size()]);
        MetaFieldValueWS[] combined = new MetaFieldValueWS[ws.getMetaFields().length
                + citMetaFieldsArray.length];
        System.arraycopy(ws.getMetaFields(), 0, combined, 0, ws.getMetaFields().length);
        System.arraycopy(citMetaFieldsArray, 0, combined, ws.getMetaFields().length, citMetaFieldsArray.length);
        ws.setMetaFields(combined);
        
        ContactDTO contact = new EntityBL(new Integer(ws.getId())).getContact();

        if (contact != null) {
            ws.setContact(new ContactWS(contact.getId(),
                                         contact.getAddress1(),
                                         contact.getAddress2(),
                                         contact.getCity(),
                                         contact.getStateProvince(),
                                         contact.getPostalCode(),
                                         contact.getCountryCode(),
                                         contact.getDeleted(),
                                         contact.getPhoneCountryCode() != null? String.valueOf(contact.getPhoneCountryCode()): "",
                                         contact.getPhoneAreaCode() != null? String.valueOf(contact.getPhoneAreaCode()): "",
                                         contact.getPhoneNumber(),
                                         contact.getEmail()));
        }
        return ws;
    }

    /**
     * This method converts a List of CompanyDTO to an Array of CompanyWS.
     *
     * @param companiesDto List to convert
     */
    public static CompanyWS[] getCompaniesWS(List<CompanyDTO> companiesDto) {
        CompanyWS[] companies = new CompanyWS[companiesDto.size()];

        for (int i = 0; i < companiesDto.size(); i++) {
            companies[i] = EntityBL.getCompanyWS(companiesDto.get(i));
        }

        return companies;
    }


    
    public static final  CompanyDTO getDTO(CompanyWS ws){
        CompanyDTO dto = new CompanyDAS().find(new Integer(ws.getId()));
        dto.setCurrency(new CurrencyDAS().find(ws.getCurrencyId()));
        dto.setLanguage(new LanguageDAS().find(ws.getLanguageId()));
        dto.setDescription(ws.getDescription());
        dto.setCustomerInformationDesign(ws.getCustomerInformationDesign());
        dto.setUiColor(ws.getUiColor());
        dto.setTimezone(ws.getTimezone());
        dto.setFailedEmailNotification(ws.getFailedEmailNotification());

        if (ws.getMetaFields() != null) {
            if (null != dto) {
                dto.getMetaFields().clear();
                Set<MetaField> metaFields = new HashSet<MetaField>(MetaFieldBL.getAvailableFieldsList(ws.getId(), new EntityType[]{EntityType.COMPANY}));
                metaFields.addAll(MetaFieldBL.getAllAvailableFieldsList(ws.getId(), new EntityType[]{EntityType.COMPANY_INFO}));
                MetaFieldHelper.fillMetaFieldsFromWS(metaFields, dto, ws.getMetaFields());
            }
        }
        return dto;
    }
    
    
    private void init() {
        das = new CompanyDAS();
        eLogger = EventLogger.getInstance();
    }
    
    public CompanyDTO getEntity() {
        return entity;
    }

    public Locale getLocale() {
        return entity.getLanguage().asLocale();
    }

    public ContactDTO getContact() {
        //get company contact
        ContactBL contact = new ContactBL();
        contact.setEntity(entity.getId());
        return contact.getEntity();
    }
    
    public Integer[] getAllIDs() 
            throws SQLException, NamingException {
        List list = new ArrayList();
        
        prepareStatement(EntitySQL.listAll);
        execute();
        conn.close();
        
        while (cachedResults.next()) {
            list.add(new Integer(cachedResults.getInt(1)));
        } 
        
        Integer[] retValue = new Integer[list.size()];
        list.toArray(retValue);
        return retValue;
    }
    
    public CachedRowSet getTables() 
            throws SQLException, NamingException {
        prepareStatement(EntitySQL.getTables);
        execute();
        conn.close();
        
        return cachedResults;
    }
    
    public Integer getRootUser(Integer entityId) {
        try {
        	RoleDTO rootRole = new RoleDAS().findByRoleTypeIdAndCompanyId(Constants.TYPE_ROOT, entityId);
            prepareStatement(EntitySQL.findRoot);
            cachedResults.setInt(1, entityId);
            cachedResults.setInt(2, rootRole.getId());

            execute();
            conn.close();
            
            cachedResults.next();
            return cachedResults.getInt(1);
        } catch (Exception e) {
            throw new SessionInternalError("Root user not found for entity " +
                    entityId, EntityBL.class, e);
        } 
    }

    public void updateEntityAndContact(CompanyWS companyWS, Integer entityId, Integer userId) {

        CompanyDTO existingCompany=new CompanyDAS().findEntityByName(companyWS.getDescription());
        if(existingCompany!=null && existingCompany.getId()!=companyWS.getId()){
            throw new SessionInternalError("Company name should be unique", new String[]{"Company name should be unique"});
        }

        CompanyDTO dto= EntityBL.getDTO(companyWS);
        //validating Meta-field
        for (MetaFieldValue metaFieldValue : dto.getMetaFields()){
            MetaFieldBL.validateMetaField(dto.getLanguageId(), metaFieldValue.getField(), metaFieldValue, dto);
        }

            ContactWS contactWs= companyWS.getContact();
            ContactBL contactBl= new ContactBL();
            contactBl.setEntity(entityId);
            ContactDTO contact= contactBl.getEntity();
            contact.setAddress1(contactWs.getAddress1());
            contact.setAddress2(contactWs.getAddress2());
            contact.setCity(contactWs.getCity());
            contact.setCountryCode(contactWs.getCountryCode());
            contact.setPostalCode(contactWs.getPostalCode());
            contact.setStateProvince(contactWs.getStateProvince());
            contact.setEmail(contactWs.getEmail());

        try {
            if (contactWs.getPhoneCountryCode() != null && !contactWs.getPhoneCountryCode().trim().equals("")) {
                contact.setPhoneCountryCode(Integer.valueOf(contactWs.getPhoneCountryCode()));
            }
            if (contactWs.getPhoneAreaCode() != null && !contactWs.getPhoneAreaCode().trim().equals("")) {
                contact.setPhoneAreaCode(Integer.valueOf(contactWs.getPhoneAreaCode()));
            }
            if (contactWs.getPhoneNumber() != null && !contactWs.getPhoneNumber().trim().equals("")) {
                contact.setPhoneNumber(contactWs.getPhoneNumber());
            }
        }catch (NumberFormatException e){
            throw new SessionInternalError("The phone should be a number",
                    new String[]{"ContactWS,contact.phone,validation.error.contact.phone"});
        }

        new ContactDAS().save(contact);
        eLogger.auditBySystem(entityId,
                userId, Constants.TABLE_CONTACT,
                contact.getId(),
                EventLogger.MODULE_WEBSERVICES,
                EventLogger.ROW_UPDATED, null, null, null);

        CompanyDTO companyDTO = new CompanyDAS().save(dto);

        // meta fields
        Integer companyId = null != companyDTO ? companyDTO.getId() : null;
        companyDTO.updateCitMetaFieldsWithValidation(companyDTO.getId(), companyId, companyDTO);

        // save ait meta field with given dates
        CompanyInformationTypeDAS companyInformationTypeDAS = new CompanyInformationTypeDAS();
        CompanyInformationTypeDTO companyInformationTypeDTO = null;

        for (Map.Entry<Integer, List<MetaFieldValue>> entry : companyDTO.getCitMetaFieldMap().entrySet()) {

            companyInformationTypeDTO = companyInformationTypeDAS.find(entry.getKey());

            for (MetaFieldValue value : entry.getValue()) {
                MetaFieldValue rigged = generateValue(value);
                companyDTO.addCompanyInfoTypeMetaField(rigged, companyInformationTypeDTO);
            }
        }

        new CompanyDAS().save(companyDTO);
        eLogger.auditBySystem(entityId,
                userId, Constants.TABLE_ENTITY,
                entityId,
                EventLogger.MODULE_WEBSERVICES,
                EventLogger.ROW_UPDATED, null, null, null);
    }

    private MetaFieldValue generateValue (MetaFieldValue value) {
        MetaFieldValue generated = value.getField().createValue();
        generated.setField(value.getField());
        generated.setValue(value.getValue());
        return generated;
    }

    public List<CompanyWS> getChildEntities(Integer parentId) {
        List<CompanyDTO> companyDTOs = das.findChildEntities(parentId);
        List<CompanyWS> childEntities = new ArrayList<CompanyWS>();
        for(CompanyDTO entity : companyDTOs) {
            childEntities.add(getCompanyWS(entity));
        }
        return childEntities;
    }

    /**
     * This method checks if the other company {@code otherCompany} is either a parent
     * or child of this company {@code thisCompany}.
     *
     * @param otherCompany
     * @param thisCompany
     * @return
     */
    public boolean isCompanyInHierarchy(CompanyDTO thisCompany, CompanyDTO otherCompany) {
        if(thisCompany == null) {
            thisCompany = entity;
        }
        if(otherCompany.getId() == thisCompany.getId()) {
            return true;
        }

        CompanyDTO parentCompany = thisCompany.getParent();
        while(parentCompany != null) {
            if(parentCompany.getId() == otherCompany.getId()) {
                return true;
            }
            parentCompany = parentCompany.getParent();
        }
        return isCompanyChild(thisCompany, otherCompany);
    }

    /**
     * Check if the other company {@code otherCompany} is a child of this company {@code thisCompany}
     * @param thisCompany
     * @param otherCompany
     * @return
     */
    private boolean isCompanyChild(CompanyDTO thisCompany, CompanyDTO otherCompany) {
        if(thisCompany == null) {
            thisCompany = entity;
        }

        List<CompanyDTO> children = das.findChildEntities(thisCompany.getId());
        for(CompanyDTO childCompany : children) {
            if(childCompany.getId() == otherCompany.getId()) {
                return true;
            }
            if(isCompanyChild(childCompany, otherCompany)) {
                return true;
            }
        }
        return false;
    }
}
