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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.RetryConcurrentOperation;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.DTOFactory;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.db.EventLogDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * This is the session facade for the user. All interaction from the client
 * to the server is made through calls to the methods of this class. This
 * class uses helper classes (Business Logic -> BL) for the real logic.
 *
 * Had to implement IUserSessionBean to stop Spring related
 * ClassCastExceptions when getting the bean.
 *
 * @author emilc
 */
@Transactional( propagation = Propagation.REQUIRED )
public class UserSessionBean implements IUserSessionBean, PartnerSQL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UserSessionBean.class));

    private static final ConcurrentMap<Integer, Boolean> commissionRunning = new ConcurrentHashMap<Integer, Boolean>();


    // -------------------------------------------------------------------------
    // Methods
    // -------------------------------------------------------------------------

    /**
     * @return the new user id if everthing ok, or null if the username is already
     * taken, any other problems go as an exception
     */
    public Integer create(UserDTOEx newUser, ContactDTOEx contact)
            throws SessionInternalError {
        try {
            UserBL bl = new UserBL();
            if (!bl.exists(newUser.getUserName(), newUser.getEntityId())) {

                ContactBL cBl = new ContactBL();

                Integer userId = bl.create(newUser, null);
                if (userId != null) {
                    // children inherit the contact of the parent user
                    if (newUser.getCustomer() != null &&
                        newUser.getCustomer().getParent() != null) {
                        cBl.setFromChild(userId);
                        contact = cBl.getDTO();
                        LOG.debug("Using parent's contact %s", contact.getId());
                    }
                    cBl.createForUser(contact, userId,  null);
                    bl.createCredentialsFromDTO(newUser);
                } else {
                    // means that the partner doens't exist
                    userId = new Integer(-1);
                }
                return userId;
            }

            return null;

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public UserDTO getUserDTO(String userName, Integer entityId)
            throws SessionInternalError {
        UserDTO dto = null;
        try {
            UserBL user = new UserBL(userName, entityId);
            dto = user.getDto();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return dto;
    }


    public Locale getLocale(Integer userId)
            throws SessionInternalError {
        try {
            UserBL user = new UserBL(userId);
            return user.getLocale();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }


    public void delete(Integer executorId, Integer userId)
            throws SessionInternalError {
        if (userId == null) {
            throw new SessionInternalError("userId can't be null");
        }
        try {
            UserBL bl = new UserBL(userId);
            bl.delete(executorId);
        } catch(Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void delete(String userName, Integer entityId)
            throws SessionInternalError {
        if (userName == null) {
            throw new SessionInternalError("userId can't be null");
        }
        try {
            UserBL user = new UserBL(userName, entityId);
            user.delete(null);
        } catch(Exception e) {
            throw new SessionInternalError(e);
        }
    }


    /**
     * @param executorId The user that is doing this change, it could be
     * the same user or someone else in behalf.
     */
    public void update(Integer executorId, UserDTOEx dto)
            throws SessionInternalError {
        try {
            UserBL bl = new UserBL(dto.getUserId());
            bl.update(executorId, dto);
        } catch(Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void updatePartner(Integer executorId, PartnerDTO dto)
            throws SessionInternalError {
        try {
            if(dto.getParent() != null && !new EntityBL().isCompanyInHierarchy(dto.getBaseUser().getCompany(), dto.getParent().getBaseUser().getEntity())) {
                throw new SessionInternalError("Partner's parent is not in company hierarchy.",
                        new String[] { "PartnerWS,parent,validation.error.parent.invalid.hierarchy" });
            }
            PartnerBL bl = new PartnerBL(dto.getId());
            bl.update(executorId, dto);
        } catch(Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public ContactDTOEx getContactDTO(Integer userId)
            throws SessionInternalError {
        ContactBL bl = new ContactBL();
        bl.set(userId);
        if (bl.getEntity() != null) {
            return bl.getDTO();
        } else {
            return getVoidContactDTO(new UserDAS().find(userId).getCompany().getId());
        }
    }

    public ContactDTOEx getVoidContactDTO(Integer entityId)
            throws SessionInternalError {
        try {
            ContactBL bl = new ContactBL();
            return bl.getVoidDTO(entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setContact(ContactDTOEx dto, Integer userId)
            throws SessionInternalError {
        try {
            ContactBL cbl = new ContactBL();

            cbl.updateForUser(dto, userId, null);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public boolean addContact(ContactDTOEx dto, String username,
                              Integer entityId)
            throws SessionInternalError {
        try {
            UserBL user = new UserBL(username, entityId);
            ContactBL cbl = new ContactBL();

            return cbl.append(dto, user.getEntity().getUserId());
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public UserDTOEx getUserDTOEx(Integer userId)
            throws SessionInternalError {
        UserDTOEx dto = null;

        try {
            dto = DTOFactory.getUserDTOEx(userId);
            dto.touch();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return dto;
    }

    public Boolean isParentCustomer(Integer userId)
            throws SessionInternalError {
        try {
            UserBL user = new UserBL(userId);
            Integer isParent = user.getEntity().getCustomer().getIsParent();
            if (isParent == null || isParent.intValue() == 0) {
                return new Boolean(false);
            } else {
                return new Boolean(true);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    // Check if there is any Active Children under this client
    public Boolean hasSubAccounts(Integer userId)
            throws SessionInternalError {
        try {
            boolean hasSubAccounts = false;
            UserBL user = new UserBL(userId);
            Iterator childs = user.getEntity().getCustomer().getChildren().iterator();
            while( !hasSubAccounts && childs.hasNext() ){
                CustomerDTO child = (CustomerDTO)childs.next();
                if( child.getBaseUser().getDeleted() == 0 ){
                    hasSubAccounts = true;
                }
            }
            return hasSubAccounts;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public UserDTOEx getUserDTOEx(String userName, Integer entityId)
            throws SessionInternalError{
        UserDTOEx dto = null;

        try {
            UserBL bl = new UserBL();
            bl.set(userName, entityId);
            dto = DTOFactory.getUserDTOEx(bl.getEntity());
            dto.touch();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return dto;
    }

    public CurrencyDTO getCurrency(Integer userId)
            throws SessionInternalError{
        return new UserDAS().find(userId).getCurrency();
    }

    public Integer createCreditCard(Integer userId, PaymentInformationDTO dto) 
            throws SessionInternalError {
    	try {
            // add the base user to the given CreditCardDTO
            UserDTO user = new UserDAS().find(userId);
            dto.setUser(user);

            // create the cc record
            PaymentInformationBL ccBL = new PaymentInformationBL();
            PaymentInformationDTO saved = ccBL.create(dto);
            
            user.getPaymentInstruments().add(saved);
            
            if (null != user.getCustomer()) {
                user.getCustomer().setAutoPaymentType(Constants.AUTO_PAYMENT_TYPE_CC);
            }
            
            new UserDAS().save(user);

            return saved.getId();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setAuthPaymentType(Integer userId, Integer newMethod,
                                   Boolean use)
            throws SessionInternalError {
        try {
            UserBL user = new UserBL(userId);
            if (user.getEntity().getCustomer() == null) {
                LOG.warn("Trying to update the automatic payment type of a " +
                         "non customer");
                return;
            }
            Integer method = user.getEntity().getCustomer().
                    getAutoPaymentType();
            // it wants to use this one now
            if (use.booleanValue()) {
                user.getEntity().getCustomer().setAutoPaymentType(newMethod);
            }
            // it has this method, and doesn't want to use it any more
            if (method != null && method.equals(newMethod) &&
                !use.booleanValue()) {
                user.getEntity().getCustomer().setAutoPaymentType(null);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Integer getAuthPaymentType(Integer userId)
            throws SessionInternalError {
        try {
            UserBL user = new UserBL(userId);
            Integer method;
            if (user.getEntity().getCustomer() != null) {
                method = user.getEntity().getCustomer().getAutoPaymentType();
            } else {
                // this will be necessary as long as non-customers can have
                // a credit card
                method = new Integer(0);
            }
            return method;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * @return The path or url of the css to use for the given entity
     */
    public String getEntityPreference(Integer entityId, Integer preferenceId)
            throws SessionInternalError {
        try {
            String result = null;
            try {
                result = PreferenceBL.getPreferenceValue(entityId, preferenceId);
            } catch (EmptyResultDataAccessException e) {
                // it is missing, so it will pick up the default
            }

            if (result == null) {
                LOG.warn("Preference %s does not have a default.", preferenceId);
            }

            LOG.debug("result for %s = %s", preferenceId, result);
            return result;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Get the entity's contact information
     * @param entityId
     * @return
     * @throws SessionInternalError
     */
    public ContactDTOEx getEntityContact(Integer entityId)
            throws SessionInternalError {
        try {
            ContactBL bl = new ContactBL();
            bl.setEntity(entityId);
            return bl.getDTO();
        } catch (Exception e) {
            LOG.error("Exception retreiving the entity contact", e);
            throw new SessionInternalError("Customer primary contact");
        }
    }

    /**
     * This is really an entity level class, there is no user involved.
     * This means that the lookup of parameters will be based on the table
     * entity.
     *
     * @param ids
     * An array of the parameter ids that will be looked up and returned in
     * the hashtable
     * @return
     * The paramteres in "id - value" pairs. The value is of type String
     */
    public HashMap getEntityParameters(Integer entityId, Integer[] ids)
            throws SessionInternalError {
        HashMap retValue = new HashMap();

        try {
            for (int f = 0; f < ids.length; f++) {
                try {
                    retValue.put(ids[f], PreferenceBL.getPreferenceValue(entityId, ids[f]));
                } catch (EmptyResultDataAccessException e1) {
                    // do nothing
                }
            }
            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * @param entityId
     * @param params
     * @throws SessionInternalError
     */
    public void setEntityParameters(Integer entityId, HashMap params)
            throws SessionInternalError {
        try {
            PreferenceBL preference = new PreferenceBL();
            for (Iterator it = params.keySet().iterator(); it.hasNext();) {
                Integer preferenceId = (Integer) it.next();

                Object value = params.get(preferenceId);
                if (value != null) {
                    if (value instanceof Integer) {
                        preference.createUpdateForEntity(entityId, preferenceId, (Integer) value);

                    } else if (value instanceof String) {
                        preference.createUpdateForEntity(entityId, preferenceId, (String) value);

                    } else if (value instanceof Float) {
                        preference.createUpdateForEntity(entityId, preferenceId, new BigDecimal(value.toString()));

                    } else if (value instanceof BigDecimal) {
                        preference.createUpdateForEntity(entityId, preferenceId, (BigDecimal) value);
                    }

                } else {
                    preference.createUpdateForEntity(entityId, preferenceId, (String) null);
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * This now only working with String parameters
     *
     * @param entityId entity id
     * @param preferenceId preference Id
     * @param value String parameter value (optional)
     * @throws SessionInternalError
     */
    public void setEntityParameter(Integer entityId, Integer preferenceId, String value) throws SessionInternalError {
        try {
            LOG.debug("updating preference %s for entity %s", preferenceId, entityId);
            PreferenceBL preference = new PreferenceBL();
            preference.createUpdateForEntity(entityId, preferenceId, value);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setUserStatus(Integer executorId, Integer userId,
                              Integer statusId)
            throws SessionInternalError {
        try {
            AgeingBL age = new AgeingBL();
            age.setUserStatus(executorId, userId, statusId,
                              Calendar.getInstance().getTime());
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String getWelcomeMessage(Integer entityId, Integer languageId,
                                    Integer statusId)
            throws SessionInternalError {
        String retValue;
        try {
            LOG.warn("Using welcome default");
            retValue = "Welcome!";
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    /**
     * Describes the instance and its content for debugging purpose
     *
     * @return Debugging information about the instance and its content
     */
    public String toString() {
        return "UserSessionBean [ " + " ]";
    }

    public PartnerDTO getPartnerDTO(Integer partnerId)
            throws SessionInternalError {
        PartnerBL partnerBL = new PartnerBL(partnerId);
        PartnerDTO retValue = partnerBL.getDTO();
        retValue.touch();
        return retValue;
    }

    public PartnerPayout getPartnerLastPayoutDTO(Integer partnerId)
            throws SessionInternalError {
        try {
            PartnerBL partnerBL = new PartnerBL();
            return partnerBL.getLastPayoutDTO(partnerId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public PartnerPayout getPartnerPayoutDTO(Integer payoutId)
            throws SessionInternalError {
        try {
            PartnerBL partnerBL = new PartnerBL();
            partnerBL.setPayout(payoutId);
            return partnerBL.getPayoutDTO();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void notifyCreditCardExpiration(Date today)
            throws SessionInternalError {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            if (cal.get(Calendar.DAY_OF_MONTH) == 1) {
                PaymentInformationBL bl = new PaymentInformationBL();
                bl.notifyExipration(today);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void setUserBlacklisted(Integer executorId, Integer userId,
                                   Boolean isBlacklisted) throws SessionInternalError {
        try {
            UserBL bl = new UserBL(userId);
            bl.setUserBlacklisted(executorId, isBlacklisted);
        } catch(Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * @throws NumberFormatException
     * @throws NotificationNotFoundException
     * @throws SessionInternalError
     */
    @Deprecated
    public void sendLostPassword(String entityId, String username)
            throws NumberFormatException, SessionInternalError,
                   NotificationNotFoundException {
        UserBL user = new UserBL(username, Integer.valueOf(entityId));

        //todo: implement this method or erase it.
        //user.sendLostPassword(Integer.valueOf(entityId), user.getEntity().getUserId(),  user.getEntity().getLanguageIdField(), user.getEntity().getPassword());
    }

    @Deprecated
    public boolean isPasswordExpired(Integer userId) {
        UserBL user;
        user = new UserBL(userId);
        return user.isPasswordExpired();
    }

    public List<EventLogDTO> getEventLog(Integer userId) {
        List<EventLogDTO> events = new EventLogDAS().getEventsByAffectedUser(
                userId);
        for (EventLogDTO event : events) {
            event.touch();
        }
        return events;
    }

    @RetryConcurrentOperation( on = { HibernateOptimisticLockingFailureException.class, StaleObjectStateException.class, UnsupportedOperationException.class }, retries = 3)
    @Transactional( propagation = Propagation.REQUIRES_NEW)
    public void loginSuccess(String username, Integer entityId) {
        try {
            UserBL userBL = new UserBL(username, entityId);
            UserDTO user = userBL.getDto();
            if (null != user) {
                userBL.successLoginAttempt();
            }

            String successMsg = String.format("User %s;%d successfully logged in", username, entityId);
            LOG.info(fullLogMessage(LogConstants.STATUS_SUCCESS, successMsg));

        } catch(Exception ex) {
            LOG.error(fullLogMessage(LogConstants.STATUS_NOT_SUCCESS, ex.getMessage()));
            throw new SessionInternalError(ex);
        }
    }

    public boolean loginFailure(String username, Integer entityId)
            throws SessionInternalError {
        String loginFailureMessage = String.format("User %s;%d failed to login.", username, entityId);
        LOG.info(fullLogMessage(LogConstants.STATUS_NOT_SUCCESS, loginFailureMessage));

        try {
            UserBL userBL = new UserBL(username, entityId);
            if (null != userBL.getDto()) {
                return userBL.failedLoginAttempt();
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.error(fullLogMessage(LogConstants.STATUS_NOT_SUCCESS, e.getMessage()));
            throw new SessionInternalError(e);
        }
    }

    private String fullLogMessage(LogConstants status, String message) {
        return new LogMessage.Builder()
                .module(LogConstants.MODULE_LOGIN.toString())
                .action(LogConstants.ACTION_EVENT.toString())
                .status(status.toString())
                .message(message).build().toString();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean calculatePartnerCommissions (Integer entityId) throws SessionInternalError {
        commissionRunning.putIfAbsent(entityId, false);
        if (!commissionRunning.replace(entityId, false, true)) {
            LOG.warn("Failed to trigger commission process for entity: %s", entityId);
            return false;
        }

        LOG.debug("Triggered commission process for entity %s", entityId);

        try {

            PartnerBL partnerBL = new PartnerBL();
            partnerBL.calculateCommissions(entityId);

        } catch (Exception e) {
            throw new SessionInternalError(e);
        } finally {
            commissionRunning.put(entityId, false);
        }
        return true;

    }

    /**
     * Returns true if the Commission Process is running.
     * @param entityId
     */
    public boolean isPartnerCommissionRunning(Integer entityId) {
        if (entityId == null)
            return false;

        commissionRunning.putIfAbsent(entityId, false);
        return commissionRunning.get(entityId);
    }

    public boolean updateUserAccountExpiryStatus(UserDTOEx user, boolean status)
            throws SessionInternalError {
        try {
            UserBL userBL = new UserBL(user.getId());
            if (null != userBL.getDto()) {
                userBL.setAccountExpired(status, user.getAccountDisabledDate());
                return true;
            } else {
                LOG.debug("Error occurred while updating user : " + userBL.getEntity().getId());
                return false;
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    public void logout(Integer userId)
            throws SessionInternalError{
        try {
            UserBL userBL = new UserBL(userId);
            if (null != userBL.getDto()) {
                userBL.logout();
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    public void deletePasswordCode(ResetPasswordCodeDTO passCode) {
        new ResetPasswordCodeDAS().delete(passCode);
    }

    public void deleteRole(Integer roleId, Integer laguageId){
        try{
            RoleBL roleService = new RoleBL(roleId);
            if(!roleService.isUsedRole()){
                LOG.debug("Error occurred when try delete role : " + roleId);
                throw new SessionInternalError("Cannot delete a role");
            }

            roleService.deleteDescription(laguageId);
            roleService.deleteTitle(laguageId);
            roleService.delete();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void sendSSOEnabledUserCreatedEmailMessage(Integer entityId, Integer userId,Integer languageId)
            throws SessionInternalError, NotificationNotFoundException{
        new UserBL().sendSSOEnabledUserCreatedEmailMessage(entityId, userId, languageId);
    }

    public boolean deleteUserByEntityIdAndAppDirectUUID(Integer entityId, String appDirectUuid, String executorUuid){
        boolean deleted = false;

        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean("webServicesSession");
        UserDAS userDAS = new UserDAS();

        UserDTO executorDTO = userDAS.findUserByMetaFieldValue(executorUuid, entityId);
        if(null!=executorDTO) {
            Integer executorId = executorDTO.getId();
            UserDTO userDTO = userDAS.findUserByMetaFieldValue(appDirectUuid, entityId);
            Integer userId = userDTO != null ? userDTO.getId() : 0;
            if (null == userId || 0 == userId) {
                CustomerDAS customerDAS = new CustomerDAS();
                Integer customerId = customerDAS.getCustomerIdByAppDirectUUID(appDirectUuid, entityId);
                if (null != customerId) {
                    CustomerDTO customer = customerDAS.find(customerId);
                    userId = customer.getBaseUser().getId();
                    webServicesSessionSpringBean.deleteAppDirectUser(executorId,userId);
                    deleted = true;
                }
                if (null == userId || 0 == userId) {
                    PartnerDAS partnerDAS = new PartnerDAS();
                    Integer partnerId = partnerDAS.getPartnerIdByAppDirectUUID(appDirectUuid, entityId);
                    if (null != partnerId) {
                        PartnerBL bl = new PartnerBL(partnerId);
                        bl.delete(executorId);
                        deleted = true;
                    }
                }
            } else if (userId != 0) {
                webServicesSessionSpringBean.deleteAppDirectUser(executorId,userId);
                deleted = true;
            }
        }

        return deleted;
    }

    public List<Integer> findAdminUserIds(Integer entityId) {
        UserBL bl = new UserBL();
        return bl.findAdminUserIds(entityId);
    }
}
