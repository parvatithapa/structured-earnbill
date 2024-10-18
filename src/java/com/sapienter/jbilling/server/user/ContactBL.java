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

import java.util.*;
import java.util.stream.Collectors;

import javax.naming.NamingException;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.validation.EmailValidationRuleModel;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactMapDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactMapDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

public class ContactBL {
    private static final Logger logger = LoggerFactory.getLogger(ContactBL.class);
    // contact types in synch with the table contact_type
    static public final Integer ENTITY = new Integer(1);
    
    // private methods
    private ContactDAS contactDas = null;
    private ContactDTO contact = null;
    private Integer entityId = null;
    private JbillingTableDAS jbDAS = null;
    private EventLogger eLogger = null;
    
    public ContactBL(Integer contactId)
            throws NamingException {
        init();
        contact = contactDas.find(contactId);
    }
    
    public ContactBL() {
        init();
    }
    
    public static final ContactWS getContactWS(ContactDTOEx other) {
    	
    	ContactWS ws = new ContactWS();
        ws.setId(other.getId());
        ws.setOrganizationName(other.getOrganizationName());
        ws.setAddress1(other.getAddress1());
        ws.setAddress2(other.getAddress2());
        ws.setCity(other.getCity());
        ws.setStateProvince(other.getStateProvince());
        ws.setPostalCode(other.getPostalCode());
        ws.setCountryCode(other.getCountryCode());
        ws.setLastName(other.getLastName());
        ws.setFirstName(other.getFirstName());
        ws.setInitial(other.getInitial());
        ws.setTitle(other.getTitle());
        ws.setPhoneCountryCode(null != other.getPhoneCountryCode() ? String.valueOf(other.getPhoneCountryCode()) : null);
        ws.setPhoneAreaCode(null != other.getPhoneAreaCode() ? String.valueOf(other.getPhoneAreaCode()) : null );
        ws.setPhoneNumber(other.getPhoneNumber());
        ws.setFaxCountryCode(other.getFaxCountryCode());
        ws.setFaxAreaCode(other.getFaxAreaCode());
        ws.setFaxNumber(other.getFaxNumber());
        ws.setEmail(other.getEmail());
        ws.setCreateDate(other.getCreateDate());
        ws.setDeleted(other.getDeleted());
        ws.setInclude(other.getInclude() != null && other.getInclude().equals(1) );
        return ws;
    }
    
    
    private void setEntityFromUser(Integer userId) {
        // id the entity
        if (userId != null) {
            try {
                entityId = new UserBL().getEntityId(userId);
            } catch (Exception e) {
                logger.error("Finding the entity {}", e);
            }
        }
    }
 
    public void set(Integer userId) {
        contact = contactDas.findContact(userId);
        setEntityFromUser(userId);
    }

    public void setEntity(Integer entityId) {
        this.entityId = entityId;
        contact = contactDas.findEntityContact(entityId);
    }

    public boolean setInvoice(Integer invoiceId) {
        boolean retValue = false;
        contact = contactDas.findInvoiceContact(invoiceId);
        InvoiceBL invoice = new InvoiceBL(invoiceId);
        if (contact == null) {
            set(invoice.getEntity().getBaseUser().getUserId());
        } else {
            entityId = invoice.getEntity().getBaseUser().getCompany().getId();
            retValue = true;
        }
        return retValue;
    }

    /**
     * Rather confusing considering the previous method, but necessary
     * to follow the convention
     * @return
     */
    public ContactDTO getEntity() {
        return contact;
    }
    
    
    public ContactDTOEx getVoidDTO(Integer myEntityId) {
        entityId = myEntityId;
        ContactDTOEx retValue = new ContactDTOEx();
        return retValue;
    }
    
    public ContactDTOEx getDTO() {

        ContactDTOEx retValue =  new ContactDTOEx(
            contact.getId(),
            contact.getOrganizationName(),
            contact.getAddress1(),
            contact.getAddress2(),
            contact.getCity(),
            contact.getStateProvince(),
            contact.getPostalCode(),
            contact.getCountryCode(),
            contact.getLastName(),
            contact.getFirstName(),
            contact.getInitial(),
            contact.getTitle(),
            contact.getPhoneCountryCode(),
            contact.getPhoneAreaCode(),
            contact.getPhoneNumber(),
            contact.getFaxCountryCode(),
            contact.getFaxAreaCode(),
            contact.getFaxNumber(),
            contact.getEmail(),
            contact.getCreateDate(),
            contact.getDeleted(),
            contact.getInclude());
        
        return retValue;
    }
    
    public List<ContactDTOEx> getAll(Integer userId)  {
        List<ContactDTOEx> retValue = new ArrayList<>();
        UserBL user = new UserBL(userId);
        entityId = user.getEntityId(userId);
        contact = contactDas.findContact(userId);
        if (contact != null) {
            ContactDTOEx dto = getDTO();
            retValue.add(dto);
        }
        return retValue;
    }

    private void init() {
        contactDas = new ContactDAS();
        jbDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        eLogger = EventLogger.getInstance();
    }
    

    /**
     * Finds what is the next contact type and creates a new
     * contact with it
     * @param dto
     */
    public boolean append(ContactDTOEx dto, Integer userId) 
                throws SessionInternalError {
        UserBL user = new UserBL(userId);
        set(userId);
        if (contact == null) {
            // this one is available
            createForUser(dto, userId, null);
            return true;
        }

        return false;
    }
    
    public Integer createForUser(ContactDTOEx dto, Integer userId, Integer executorUserId)
            throws SessionInternalError {
        try {
            //Null check for contact user_id field BugFix:5498
            if(dto.getUserId()==null){
                dto.setUserId(userId);
                return create(dto, Constants.TABLE_BASE_USER, userId, executorUserId);
            }
            return create(dto, Constants.TABLE_BASE_USER, userId, executorUserId);
        } catch (Exception e) {
            logger.debug("Error creating contact for user {}", userId);
            throw new SessionInternalError(e);
        }
    }
    
    public Integer createForInvoice(ContactDTOEx dto, Integer invoiceId) {
        return create(dto, Constants.TABLE_INVOICE, invoiceId, null);
    }
    
    /**
     * 
     * @param dto
     * @param table
     * @param foreignId
     * @return
     * @throws NamingException
     */
    public Integer create(ContactDTOEx dto, String table,  
            Integer foreignId, Integer executorUserId) {
        // first thing is to create the map to the user
        ContactMapDTO map = new ContactMapDTO();
        map.setJbillingTable(jbDAS.findByName(table));
        map.setForeignId(foreignId);
        map = new ContactMapDAS().save(map);
        
        // now the contact itself
        dto.setCreateDate(TimezoneHelper.serverCurrentDate());
        dto.setDeleted(0);
        dto.setVersionNum(0);
        dto.setId(0);

        contact = contactDas.save(new ContactDTO(dto)); // it won't take the Ex
        contact.setContactMap(map);
        map.setContact(contact);
        
        logger.debug("created {}", contact);

        // do an event if this is a user contact (invoices, companies, have
        // contacts too)
        if (table.equals(Constants.TABLE_BASE_USER)) {
            NewContactEvent event = new NewContactEvent(contact.getUserId(), contact, entityId);
            EventManager.process(event);

            if ( null != executorUserId) {
                eLogger.audit(executorUserId,
                        contact.getUserId(),
                        Constants.TABLE_CONTACT,
                        contact.getId(),
                        EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.ROW_CREATED, null, null, null);
            } else {
                eLogger.auditBySystem(entityId,
                                  contact.getUserId(),
                                  Constants.TABLE_CONTACT,
                                  contact.getId(),
                                  EventLogger.MODULE_USER_MAINTENANCE,
                                  EventLogger.ROW_CREATED, null, null, null);
            }
        }

        return contact.getId();
    }
    

    public void updateForUser(ContactDTOEx dto, Integer userId, Integer executorUserId)
            throws SessionInternalError {
        contact = contactDas.findContact(userId);
        if (contact != null) {
            if (entityId == null) {
                setEntityFromUser(userId);
            }
            update(dto, executorUserId);
        } else {
            try {
                createForUser(dto, userId, executorUserId);
            } catch (Exception e1) {
                throw new SessionInternalError(e1);
            }
        } 
    }
    
    private void update(ContactDTOEx dto, Integer executorUserId) {
        contact.setAddress1(dto.getAddress1());
        contact.setAddress2(dto.getAddress2());
        contact.setCity(dto.getCity());
        contact.setCountryCode(dto.getCountryCode());
        contact.setEmail(dto.getEmail());
        contact.setFaxAreaCode(dto.getFaxAreaCode());
        contact.setFaxCountryCode(dto.getFaxCountryCode());
        contact.setFaxNumber(dto.getFaxNumber());
        contact.setFirstName(dto.getFirstName());
        contact.setInitial(dto.getInitial());
        contact.setLastName(dto.getLastName());
        contact.setOrganizationName(dto.getOrganizationName());
        contact.setPhoneAreaCode(dto.getPhoneAreaCode());
        contact.setPhoneCountryCode(dto.getPhoneCountryCode());
        contact.setPhoneNumber(dto.getPhoneNumber());
        contact.setPostalCode(dto.getPostalCode());
        contact.setStateProvince(dto.getStateProvince());
        contact.setTitle(dto.getTitle());
        contact.setInclude(dto.getInclude());

        if (entityId == null) {
            setEntityFromUser(contact.getUserId());
        }

        NewContactEvent event = new NewContactEvent(contact.getUserId(), contact, entityId);
        EventManager.process(event);

        eLogger.auditBySystem(entityId,
                              contact.getUserId(),
                              Constants.TABLE_CONTACT,
                              contact.getId(),
                              EventLogger.MODULE_USER_MAINTENANCE,
                              EventLogger.ROW_UPDATED, null, null, null);
    }

    public void delete() {
        
        if (contact == null) return;
        
        logger.debug("Deleting contact {} ", contact.getId());
        // delete the map first
        new ContactMapDAS().delete(contact.getContactMap());

        // for the logger
        Integer entityId = this.entityId;
        Integer userId = contact.getUserId();
        Integer contactId = contact.getId();

        // the contact goes last
        contactDas.delete(contact);
        contact = null;

        // log event
        eLogger.auditBySystem(entityId,
                              userId,
                              Constants.TABLE_CONTACT,
                              contactId,
                              EventLogger.MODULE_USER_MAINTENANCE,
                              EventLogger.ROW_DELETED, null, null, null);
    }
    
    /**
     * Sets this contact object to that on the parent, taking the children id
     * as a parameter. 
     * @param userId
     */
    public void setFromChild(Integer userId) {
        UserBL customer = new UserBL(userId);
        set(customer.getEntity().getCustomer().getParent().getBaseUser().getUserId());
    }

    /**
     * Builds a contact object for a user from meta fields. The meta fields used
     * to build a contact object will always belong to one AIT group. If the
     * <code>groupId</code> parameter is null than this method will return
     * contcat object built from first AIT with non null email meta field.
     * If the <code>groupId</code> than this method will return contact object
     * build from the specified AIT group.
     *
     * @param userId - user for which we build contact object from meta fields
     * @param groupId - the designated AIT group from which we want to build contact object, Could be AIT ID in future,
     *                currently system defaults to 'use for notifications ait id'
     * @param effectiveDate	-	Date instance for which ait meta fields will be get
     * @return
     */
    public static ContactDTOEx buildFromMetaField(Integer userId, Integer groupId, Date effectiveDate){
        if(null == userId) {
            throw new IllegalArgumentException("userId argument can not be null");
        }

        CustomerDTO customer = new UserDAS().findNow(userId).getCustomer();
        new CustomerDAS().reattach(customer);

        ContactDTOEx contact = new ContactDTOEx();
        
        List<Integer> preferredAITIDs  = null;
        List<String> email = new ArrayList<>();
        Map<String, String> metaFieldValueByUsageTypes = new HashMap<>();
        Integer customerId      = null;
        if ( null != customer ) {
            preferredAITIDs = customer.getAccountType().getPreferredNotificationAitIds();
            customerId     = customer.getId();
        }

        if ( null != preferredAITIDs && null != customerId) {
            for (Integer preferredAITID : preferredAITIDs) {
                Map<String, String> metaFieldValueByUsageType = new MetaFieldDAS()
                .getCustomerAITMetaFieldValueMapByMetaFieldType(customerId, preferredAITID, effectiveDate);
                metaFieldValueByUsageTypes.putAll(metaFieldValueByUsageType);
                if(null != getFieldValuebyType(metaFieldValueByUsageType, MetaFieldType.EMAIL)){
                    email.add(getFieldValuebyType(metaFieldValueByUsageType, MetaFieldType.EMAIL));
                }
            }
            boolean emailPresent = !email.isEmpty();

            if ( emailPresent ) {
                
                contact.setEmail(email.stream().collect(Collectors.joining(",")));

                contact.setOrganizationName(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.ORGANIZATION));
                contact.setAddress1(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.ADDRESS1));
                contact.setAddress2(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.ADDRESS2));
                contact.setCity(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.CITY));
                contact.setStateProvince(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.STATE_PROVINCE));
                contact.setPostalCode(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.POSTAL_CODE));
                contact.setCountryCode(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.COUNTRY_CODE));

                contact.setFirstName(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.FIRST_NAME));
                contact.setLastName(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.LAST_NAME));
                contact.setInitial(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.INITIAL));
                contact.setTitle(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.TITLE));
                
                String phoneCountryCode = getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.PHONE_COUNTRY_CODE);
                contact.setPhoneCountryCode(phoneCountryCode!=null ? Integer.valueOf(phoneCountryCode): null);
                
                String phoneAreaCode = getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.PHONE_AREA_CODE);
                contact.setPhoneAreaCode(phoneAreaCode!=null ? Integer.valueOf(phoneAreaCode) : null);
                
                contact.setPhoneNumber(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.PHONE_NUMBER));

                String faxCountryCode = getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.FAX_COUNTRY_CODE);
                contact.setFaxCountryCode(faxCountryCode!=null ? Integer.valueOf(faxCountryCode) : null);
                
                String faxAreaCode = getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.FAX_AREA_CODE);
                contact.setFaxAreaCode(faxAreaCode!=null ? Integer.valueOf(faxAreaCode) : null);
                
                contact.setFaxNumber(getFieldValuebyType(metaFieldValueByUsageTypes, MetaFieldType.FAX_NUMBER));

            }
        }

        return contact;
    }

    public static ContactDTOEx buildFromMetaField(Integer userId, Date effectiveDate) {
        return buildFromMetaField(userId, null, effectiveDate);
    }

    public static List<String> getEmailList(String emailString) {
        List<String> emailList = new ArrayList<>();
        if(emailString != null && !emailString.trim().isEmpty()) {
            if (emailString.contains(EmailValidationRuleModel.COMMA) || emailString.contains(EmailValidationRuleModel.SEMI_COLON)) {
                String[] emailArray = emailString.contains(EmailValidationRuleModel.COMMA) ? emailString.split(EmailValidationRuleModel.COMMA) : emailString.split(EmailValidationRuleModel.SEMI_COLON);
                for (String e : emailArray) {
                    if(!e.isEmpty()) {
                        emailList.add(e.trim());
                    }
                }
            } else {
                emailList.add(emailString.trim());
            }
        }
        return emailList;
    }
    
    private static String getFieldValuebyType(Map<String, String> map , MetaFieldType type) {
        return map.get(type.toString());
    }

}
