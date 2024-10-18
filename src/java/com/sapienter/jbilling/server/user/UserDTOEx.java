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

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.List;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionPK;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;

import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionIdComparator;
import com.sapienter.jbilling.server.user.permisson.db.PermissionTypeIdComparator;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionTypeDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import org.apache.log4j.Logger;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author emilc
 */
public final class UserDTOEx extends UserDTO {

    // constants
    
    // user status in synch with db table user_status
    public static final Integer STATUS_ACTIVE = new Integer(1); // this HAS to be the very first status
    
    // subscriber status in synch with db table subscriber_status
    public static final Integer SUBSCRIBER_ACTIVE = new Integer(1); 
    public static final Integer SUBSCRIBER_PENDING_UNSUBSCRIPTION = new Integer(2);
    public static final Integer SUBSCRIBER_UNSUBSCRIBED = new Integer(3);
    public static final Integer SUBSCRIBER_PENDING_EXPIRATION= new Integer(4);
    public static final Integer SUBSCRIBER_EXPIRED = new Integer(5);
    public static final Integer SUBSCRIBER_NONSUBSCRIBED = new Integer(6);
    public static final Integer SUBSCRIBER_DISCONTINUED = new Integer(7);

    private List<PermissionDTO> allPermissions = null;
    private List<PermissionDTO> permissionsTypeId = null; // same as before but sorted by type
    private List<Integer> roles = null;
    private Integer mainRoleId = null;
    private Integer mainRoleType = null;
    private String mainRoleStr = null;
    private String languageStr = null;
    private Integer statusId = null;
    private String statusStr = null;
    private Integer subscriptionStatusId = null;
    private String subscriptionStatusStr = null;
    private Integer lastInvoiceId = null;
    private String currencySymbol = null;
    private String currencyName = null;
    private Locale locale = null;
    private List<String> blacklistMatches = null;
    private Boolean userIdBlacklisted = null;
    private BigDecimal balance = null; // calculated in real-time. Not a DB field

    private Map<Integer, ArrayList<Date>> timelineDatesMap = null;
    private Map<Integer, Date> effectiveDateMap = null;
    private Map<Integer, ArrayList<Date>> removedDatesMap = null;
    private boolean createCredentials = false;

    /**
     * Constructor for UserDTOEx.
     * @param userId
     * @param entityId
     * @param userName
     * @param password
     * @param deleted
     */
    public UserDTOEx(Integer userId, Integer entityId, String userName,
            String password, Integer deleted, Integer language, Integer roleId,
            Integer currencyId, Date creation, Date modified, Date lLogin, 
            Integer failedAttempts) {
        // set the base dto fields
        setId((userId == null) ? 0 : userId);
        setUserName(userName);
        setPassword(password);
        setDeleted((deleted == null) ? 0 : deleted);
        setLanguage(new LanguageDAS().find(language));
        setCurrency(new CurrencyDAS().find(currencyId));
        setCreateDatetime(creation);
        setLastStatusChange(modified);
        setLastLogin(lLogin);
        setFailedAttempts((failedAttempts == null) ? 0 : failedAttempts);
        // the entity id
        setEntityId(entityId);
        // the permissions are defaulted to nothing
        allPermissions = new ArrayList();
        roles = new ArrayList<Integer>();
        if (roleId != null) {
            // we ask for at least one role for this user
            roles.add(roleId);
            mainRoleId = roleId;
        }
    }
    
    public UserDTOEx(UserWS dto, Integer entityId) {
        setId(dto.getUserId());
        setCreateDatetime(dto.getCreateDatetime());
        setPassword(dto.getPassword());
        setDeleted(dto.getDeleted());
        setCreateDatetime(dto.getCreateDatetime());
        setLastStatusChange(dto.getLastStatusChange());
        setLastLogin(dto.getLastLogin());
        setUserName(dto.getUserName());

        RoleDTO role = new RoleBL().findByTypeOrId(dto.getMainRoleId(), dto.getEntityId());
        if(!Constants.TYPE_CUSTOMER.equals(role.getRoleTypeId()) && !Constants.TYPE_PARTNER.equals(role.getRoleTypeId())) {
            MetaFieldBL.fillMetaFieldsFromWS(entityId,
                    this, dto.getMetaFields());
        }
        setFailedAttempts(dto.getFailedAttempts());
        setCurrency(dto.getCurrencyId() == null ? null : new CurrencyDTO(dto.getCurrencyId()));
        mainRoleStr = dto.getRole();
        mainRoleId = dto.getMainRoleId();
        languageStr = dto.getLanguage();
        setLanguage(dto.getLanguageId() == null ? null : 
                new LanguageDAS().find(dto.getLanguageId()));
        statusStr = dto.getStatus();
        statusId = dto.getStatusId();
        subscriptionStatusId = dto.getSubscriberStatusId();
        setEntityId(entityId);

        if(Boolean.TRUE.equals( dto.isAccountLocked() ) ){
            setAccountLockedTime(TimezoneHelper.serverCurrentDate());
        } else {
            setAccountLockedTime(null);
        }
        setAccountExpired(dto.isAccountExpired());
        setAccountDisabledDate(dto.isAccountExpired() ? (dto.getAccountDisabledDate() != null ? dto.getAccountDisabledDate() : TimezoneHelper.companyCurrentDate(entityId)) : null);
        
        roles = new ArrayList<Integer>();
        roles.add(mainRoleId);
        if (role.getRoleTypeId().equals(Constants.TYPE_CUSTOMER)) {
            CustomerDTO customer = new CustomerDTO(entityId, dto);
            setCustomer(customer);
        }
        
        // timelines dates map and effective date map
        setTimelineDatesMap(dto.getTimelineDatesMap());
        setEffectiveDateMap(dto.getEffectiveDateMap());
        setRemovedDatesMap(dto.getRemovedDatesMap());
        
        if(dto.getPaymentInstruments() != null && dto.getPaymentInstruments().size() > 0) {
        	List<PaymentInformationDTO> paymentInstruments = new ArrayList<PaymentInformationDTO>(0);
        	for(PaymentInformationWS paymentInformation : dto.getPaymentInstruments()) {
               if (paymentInformation != null && paymentInformation.getPaymentMethodTypeId() != 0) {
                    PaymentInformationDTO paymentInformationDTO = new PaymentInformationDTO(paymentInformation, entityId);
                    // Find Payment method Id using credit card number
                    if (null != paymentInformationDTO) {
                    	setPaymentMenthodId(paymentInformationDTO);
                    }
                    paymentInstruments.add(paymentInformationDTO);
                }
            }
            setPaymentInstruments(paymentInstruments);
        }

        if(dto.getCommissionDefinitions() != null) {
            PartnerDAS partnerDAS = new PartnerDAS();
            for(CustomerCommissionDefinitionWS commissionDef : dto.getCommissionDefinitions()) {
                getCommissionDefinitions().add(
                        new CustomerCommissionDefinitionDTO(
                                new CustomerCommissionDefinitionPK(partnerDAS.find(commissionDef.getPartnerId()), this),
                                commissionDef.getRateAsDecimal())) ;
            }
        }

        setCreateCredentials(dto.isCreateCredentials());
    }

    /**
     * find out the payment method Id using payment method template.
     * @param paymentInformationDTO
     */
    public void setPaymentMenthodId(PaymentInformationDTO paymentInformationDTO) {
    	Integer paymentMethodId = null;
        String paymentMethodTemplate = paymentInformationDTO.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName();

        if(Constants.CHEQUE.equals(paymentMethodTemplate)){
    		paymentMethodId = new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_CHEQUE).getId();
        } else if(Constants.ACH.equals(paymentMethodTemplate)){
        	paymentMethodId = new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_ACH).getId();
        } else if(Constants.PAYMENT_CARD.equals(paymentMethodTemplate)){
        	char[] creditCardNumber = new PaymentInformationBL().getCharMetaFieldByType(paymentInformationDTO, MetaFieldType.PAYMENT_CARD_NUMBER);
    	    if (null != creditCardNumber && creditCardNumber.length!=0 && !PaymentInformationBL.paymentCardObscured(creditCardNumber)) {
    	    	Integer creditCardTypeId = Util.getPaymentMethod(creditCardNumber);
    	    	if (null != creditCardTypeId) {
    	    		paymentMethodId = new PaymentMethodDAS().find(creditCardTypeId).getId();
    	    	}
    	    	
    	    }
        } else {
        	paymentMethodId = new PaymentInformationBL().getPaymentMethodForPaymentMethodType(paymentInformationDTO);
        }
        if (null != paymentMethodId) {
            paymentInformationDTO.setPaymentMethodId(paymentMethodId);
        }
    }
    
    public UserDTOEx() {
        super();
    }
    
    public UserDTOEx(UserDTO user) {
       super(user); 
    }

    public List<PermissionDTO> getAllPermissions() {
        return this.allPermissions;
    }
    // this expects the List to be sorted already
    public void setAllPermissions(List<PermissionDTO> permissions) {
        this.allPermissions = permissions;
    }
    
    public boolean isGranted(Integer permissionId) {
        PermissionDTO permission = new PermissionDTO(permissionId);
        if (Collections.binarySearch(allPermissions, permission,
                new PermissionIdComparator()) >= 0) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Verifies that a permision for the given type/foreign_id has been
     * granted. This is defenetly an expensive 
     * @param typeId
     * @param foreignId
     * @return
     */
    public boolean isGranted(Integer typeId, Integer foreignId) {
        if (permissionsTypeId == null) {
            permissionsTypeId = new ArrayList<PermissionDTO>();
            permissionsTypeId.addAll(allPermissions);
            Collections.sort(permissionsTypeId, new PermissionTypeIdComparator());
          /*
            new FormatLogger(Logger.getLogger(UserDTOEx.class).debug("Permissions now = " +
                    permissionsTypeId);
                    */
        }
        boolean retValue;
        PermissionDTO permission = new PermissionDTO(0, new PermissionTypeDTO(typeId, null),
                foreignId, null, null);
        if (Collections.binarySearch(permissionsTypeId, permission,
                new PermissionTypeIdComparator()) >= 0) {
            retValue = true;
        } else {
            retValue = false;
        }
        
        /*
        new FormatLogger(Logger.getLogger(UserDTOEx.class).debug("permission for type = " + 
                typeId + " foreignId = " + foreignId + " result = " +
                retValue);
        */
        return retValue;
    }
    
    private boolean paymentInstrumentEntered(List<PaymentInformationDTO> paymentInstruments) {
    	if(paymentInstruments.size() < 2) {
    		for(MetaFieldValue value : paymentInstruments.iterator().next().getMetaFields()) {
    			if(value.getValue() != null &&  !value.getValue().toString().isEmpty()) {
    				return true;
    			}
    		}
    		return false;
    	}
    	return true;
    }

    /**
     * Returns the entityId.
     * @return Integer
     */
    public Integer getEntityId() {
        return getCompany().getId();
    }

    /**
     * Sets the entityId.
     * @param entityId The entityId to set
     */
    public void setEntityId(Integer entityId) {
        setCompany(new CompanyDAS().find(entityId));
    }

    /**
     * @return
     */
    public Integer getMainRoleId() {
        return mainRoleId;
    }

    /**
     * @return
     */
    public String getMainRoleStr() {
        return mainRoleStr;
    }

    /**
     * @param integer
     */
    public void setMainRoleId(Integer integer) {
        mainRoleId = integer;
        if (roles == null) {
            roles = new ArrayList<Integer>();
        }
        if (!roles.contains(integer)) {
            roles.add(integer);
        }
    }

    /**
     * @param string
     */
    public void setMainRoleStr(String string) {
        mainRoleStr = string;
    }

    /**
     * @return
     */
    public String getLanguageStr() {
        return languageStr;
    }

    /**
     * @param string
     */
    public void setLanguageStr(String string) {
        languageStr = string;
    }

    /**
     * @return
     */
    public Integer getStatusId() {
        return statusId;
    }

    /**
     * @return
     */
    public String getStatusStr() {
        return statusStr;
    }

    /**
     * @param integer
     */
    public void setStatusId(Integer integer) {
        statusId = integer;
    }

    /**
     * @param string
     */
    public void setStatusStr(String string) {
        statusStr = string;
    }

    /**
     * @return
     */
    public Integer getLastInvoiceId() {
        return lastInvoiceId;
    }

    /**
     * @param lastInvoiceId
     */
    public void setLastInvoiceId(Integer lastInvoiceId) {
        this.lastInvoiceId = lastInvoiceId;
    }

    /**
     * @return
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * @param currencySymbol
     */
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    /**
     * @return
     */
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * @param currencyName
     */
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
    
    public Locale getLocale() {
        return locale;
    }
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Integer getSubscriptionStatusId() {
        return subscriptionStatusId;
    }

    public void setSubscriptionStatusId(Integer subscriptionStatusId) {
        this.subscriptionStatusId = subscriptionStatusId;
    }

    public String getSubscriptionStatusStr() {
        return subscriptionStatusStr;
    }

    public void setSubscriptionStatusStr(String subscriptionStatusStr) {
        this.subscriptionStatusStr = subscriptionStatusStr;
    }
    
    public Integer getLanguageId() {
        if (getLanguage() != null) {
            return getLanguage().getId();
        }
        return null;
    }
    
    public void setUserId(Integer id) {
        setId(id);
    }
    
    public Integer getUserId() {
        return getId();
    }

    public List<String> getBlacklistMatches() {
        return blacklistMatches;
    }

    public void setBlacklistMatches(List<String> blacklistMatches) {
        this.blacklistMatches = blacklistMatches;
    }

    public Boolean getUserIdBlacklisted() {
        return userIdBlacklisted;
    }

    public void setUserIdBlacklisted(Boolean userIdBlacklisted) {
        this.userIdBlacklisted = userIdBlacklisted;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setTimelineDatesMap(Map<Integer, ArrayList<Date>> timelineDatesMap) {
    	this.timelineDatesMap =  timelineDatesMap;
    }
    
    public Map<Integer, ArrayList<Date>> getTimelineDatesMap() {
    	return timelineDatesMap;
    }
    
    public void setEffectiveDateMap(Map<Integer, Date> effectiveDateMap) {
    	this.effectiveDateMap =  effectiveDateMap;
    }
    
    public Map<Integer, Date> getEffectiveDateMap() {
    	return effectiveDateMap;
    }
    
    public void setRemovedDatesMap(Map<Integer, ArrayList<Date>> removedDatesMap) {
    	this.removedDatesMap =  removedDatesMap;
    }
    
    public Map<Integer, ArrayList<Date>> getRemovedDatesMap() {
    	return removedDatesMap;
    }

    public boolean isCreateCredentials() {
        return createCredentials;
    }

    public void setCreateCredentials(boolean createCredentials) {
        this.createCredentials = createCredentials;
    }
}
