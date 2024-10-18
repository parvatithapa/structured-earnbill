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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

/**
 *
 * This is the session facade for the user. All interaction from the client
 * to the server is made through calls to the methods of this class. This 
 * class uses helper classes (Business Logic -> BL) for the real logic.
 *
 * This interface was created to stop Spring related 
 * ClassCastExceptions when getting the bean. 
 *
 * @author emilc
 */
public interface IUserSessionBean {

    /**
     * @return the new user id if everthing ok, or null if the username is already 
     * taken, any other problems go as an exception
     */
    public Integer create(UserDTOEx newUser, ContactDTOEx contact) 
            throws SessionInternalError;

    public UserDTO getUserDTO(String userName, Integer entityId) 
            throws SessionInternalError;

    public Locale getLocale(Integer userId) throws SessionInternalError;

    public void delete(Integer executorId, Integer userId) 
            throws SessionInternalError;

    public void delete(String userName, Integer entityId) 
            throws SessionInternalError;

    /**
     * @param userId The user that is doing this change, it could be
     * the same user or someone else in behalf.
     */
    public void update(Integer executorId, UserDTOEx dto) 
            throws SessionInternalError;

    public void updatePartner(Integer executorId, PartnerDTO dto)
            throws SessionInternalError;

    public ContactDTOEx getContactDTO(Integer userId)
             throws SessionInternalError;
 
    public ContactDTOEx getVoidContactDTO(Integer entityId)
            throws SessionInternalError;

    public void setContact(ContactDTOEx dto, Integer userId)
            throws SessionInternalError;

    public boolean addContact(ContactDTOEx dto, String username,
            Integer entityId) throws SessionInternalError;

    public UserDTOEx getUserDTOEx(Integer userId) 
            throws SessionInternalError;
    
    public Boolean isParentCustomer(Integer userId) 
            throws SessionInternalError;
    
    public UserDTOEx getUserDTOEx(String userName, Integer entityId) 
            throws SessionInternalError;
    
    public Boolean hasSubAccounts(Integer userId)
            throws SessionInternalError;
    
    public CurrencyDTO getCurrency(Integer userId) throws SessionInternalError;

    public Integer createCreditCard(Integer userId, PaymentInformationDTO dto) 
            throws SessionInternalError;
    
    public void setAuthPaymentType(Integer userId, Integer newMethod, 
            Boolean use) throws SessionInternalError;
    
    public Integer getAuthPaymentType(Integer userId) 
            throws SessionInternalError;
    
    /**
     * @return The path or url of the css to use for the given entity
     */
    public String getEntityPreference(Integer entityId, Integer preferenceId) 
            throws SessionInternalError;
    
    /**
     * Get the entity's contact information
     * @param entityId
     * @return
     * @throws SessionInternalError
     */
    public ContactDTOEx getEntityContact(Integer entityId) 
            throws SessionInternalError;
    
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
            throws SessionInternalError;
    
    /**
     * @param entityId
     * @param params
     * @throws SessionInternalError
     */
    public void setEntityParameters(Integer entityId, HashMap params) 
            throws SessionInternalError;

    /**
     * This now only working with String parameters
     * 
     * @param entityId entity id
     * @param preferenceId preference Id
     * @param value String parameter value (optional)
     * @throws SessionInternalError
     */
    public void setEntityParameter(Integer entityId, Integer preferenceId, String value)
            throws SessionInternalError;

    public void setUserStatus(Integer executorId, Integer userId, 
            Integer statusId) throws SessionInternalError; 
    
    public String getWelcomeMessage(Integer entityId, Integer languageId, 
            Integer statusId) throws SessionInternalError;

    /**
    * Describes the instance and its content for debugging purpose
    *
    * @return Debugging information about the instance and its content
    */
    public String toString();

    public PartnerDTO getPartnerDTO(Integer partnerId)
            throws SessionInternalError;

    public PartnerPayout getPartnerLastPayoutDTO(Integer partnerId) 
            throws SessionInternalError;

    public PartnerPayout getPartnerPayoutDTO(Integer payoutId) 
            throws SessionInternalError;

    public void notifyCreditCardExpiration(Date today)
            throws SessionInternalError;

    public void setUserBlacklisted(Integer executorId, Integer userId, 
            Boolean isBlacklisted) throws SessionInternalError;

    /**
     * @throws NumberFormatException 
     * @throws NotificationNotFoundException 
     * @throws SessionInternalError 
     */
    public void sendLostPassword(String entityId, String username) 
        throws NumberFormatException, SessionInternalError,
        NotificationNotFoundException;
    
    public boolean isPasswordExpired(Integer userId);

    public List<EventLogDTO> getEventLog(Integer userId);

    public void loginSuccess(String username, Integer entityId);

    public boolean loginFailure(String username, Integer entityId)
            throws SessionInternalError;

    public boolean calculatePartnerCommissions(Integer entityId)
                throws SessionInternalError;

    public boolean isPartnerCommissionRunning(Integer entityId);

    public boolean updateUserAccountExpiryStatus(UserDTOEx user, boolean status)
            throws SessionInternalError;

    public void deletePasswordCode(ResetPasswordCodeDTO passCode) throws SessionInternalError;

    
    public void logout(Integer userId)
            throws SessionInternalError;

    public void deleteRole(Integer roleId, Integer laguageId) throws SessionInternalError;

    public void sendSSOEnabledUserCreatedEmailMessage(Integer entityId, Integer userId,Integer languageId)
            throws SessionInternalError, NotificationNotFoundException;

    public boolean deleteUserByEntityIdAndAppDirectUUID(Integer entityId, String appDirectUuid, String executorUuid);

    public List<Integer> findAdminUserIds(Integer entityId);
    
}
