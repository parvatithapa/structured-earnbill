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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;
import java.util.Optional;

import javax.naming.NamingException;
import javax.sql.rowset.CachedRowSet;

import com.sapienter.jbilling.server.adennet.MessageIdMap;
import com.sapienter.jbilling.server.customer.event.CustomerBillingCycleChangeEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder;
import com.sapienter.jbilling.client.authentication.util.UsernameHelper;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.creditnote.CreditNoteBL;
import com.sapienter.jbilling.server.customer.event.UpdateCustomerEvent;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationRestWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.AgeingDTOEx;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.task.security.AgeingTask;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.balance.CustomerProperties;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDTO;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDAS;
import com.sapienter.jbilling.server.user.db.UserCodeDAS;
import com.sapienter.jbilling.server.user.db.UserCodeObjectType;
import com.sapienter.jbilling.server.user.db.UserPasswordDTO;
import com.sapienter.jbilling.server.user.db.UserPasswordDAS;
import com.sapienter.jbilling.server.user.db.UserCodeCustomerLinkDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserCodeDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.SubscriberStatusDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.event.AchUpdateEvent;
import com.sapienter.jbilling.server.user.event.NewCreditCardEvent;
import com.sapienter.jbilling.server.user.event.UserDeletedEvent;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionPK;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionUserDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionIdComparator;
import com.sapienter.jbilling.server.user.tasks.IValidatePurchaseTask;
import com.sapienter.jbilling.server.util.DTOFactory;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.credentials.PasswordService;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

public class UserBL extends ResultList implements UserSQL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private UserDTO                   user                  = null;
    private EventLogger               eLogger               = null;
    private Integer                   mainRole              = null;
    private UserDAS                   das                   = null;
    private PaymentInformationDAS     paymentInformationDAS = null;
    private PasswordService passwordService = null;
    private static final String TEXT_NOTIFICATION_NOT_FOUND = "Notification not found for sending deleted user notification";

    private SessionRegistry sessionRegistry = Context.getBean(Context.Name.SESSION_REGISTRY);

    public UserBL (Integer userId) {
        init();
        set(userId);
    }

    public UserBL () {
        init();
    }

    public UserBL (UserDTO entity) {
        user = entity;
        init();
    }

    public UserBL (String username, Integer entityId) {
        init();
        user = das.findByUserName(username, entityId);
    }

    public static UserDTO getUserEntity(Integer userId) {
        return new UserDAS().find(userId);
    }

    private static Integer selectMainRole(Collection allRoleIds){
        // the main role is the smallest of them, so they have to be ordered in the
        // db in ascending order (small = important);
        Integer result = null;
        for (Iterator roleIds = allRoleIds.iterator(); roleIds.hasNext();){
            Integer nextId = (Integer)roleIds.next();
            if (result == null || nextId.compareTo(result) < 0) {
                result = nextId;
            }
        }
        return result;
    }

    public static boolean validate(UserWS userWS) {
        return validate(new UserDTOEx(userWS, null));
    }

    /**
     * Validates the user info and the credit card if present
     * @param dto
     * @return
     */
    public static boolean validate(UserDTOEx dto) {
        boolean retValue = true;

        if (dto == null || dto.getUserName() == null ||
                dto.getPassword() == null || dto.getLanguageId() == null ||
                dto.getMainRoleId() == null || dto.getStatusId() == null) {
            retValue = false;
            logger.debug("Invalid {}", dto);
        } else if (dto.getPaymentInstruments() != null) {
            //retValue = can validate paymentInstruments here
        }

        return retValue;
    }

    public static boolean isUserBalanceEnoughToAge (UserDTO user) {
        //check if balance below minimum balance to ignore ageing
        BigDecimal minBalanceToIgnore = BigDecimal.ZERO;
        try {
            minBalanceToIgnore =
                    PreferenceBL.getPreferenceValueAsDecimalOrZero(user.getEntity().getId(), Constants.PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);

            logger.debug("Minimum balance to ignore ageing preference set to {}", minBalanceToIgnore);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("Preference minimum balance to ignore ageing not set.");
        }

        Integer userId = user.getUserId();
        BigDecimal userBalance = getBalance(userId);

        logger.debug("Checking user balance {} against {} for userId : {}", userBalance, minBalanceToIgnore, userId);

        //Return 'true' if user balance above min to ignore preference value, or zero if preference not set
        return userBalance.compareTo(minBalanceToIgnore) > 0;
    }

    public void set(Integer userId) {
        user = das.find(userId);
    }

    public void set (String userName, Integer entityId) {
        user = das.findByUserName(userName, entityId);
    }

    public void set (UserDTO user) {
        this.user = user;
    }

    public void setRoot (String userName) {
        user = das.findRoot(userName);
    }

    private void init () {
        eLogger = EventLogger.getInstance();
        das = new UserDAS();
        paymentInformationDAS = new PaymentInformationDAS();
        passwordService = Context.getBean(Context.Name.PASSWORD_SERVICE);
    }

    /**
     * @param executorId
     *            This is the user that has ordered the update
     * @param dto
     *            This is the user that will be updated
     */
    public void update (Integer executorId, UserDTOEx dto) throws SessionInternalError {
        String msg = "Updating User: " + dto;
        String log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE);
        logger.info(log);
        // password is the only one that might've not been set
        String changedPassword = dto.getPassword();
        Date date = DateConvertUtils.asUtilDate(LocalDate.now());
        CustomerProperties oldCustomer = this.getCustomerProperties();
        if (changedPassword != null){
            //encrypt it based on the user role
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(getMainRole());
            String encPassword = user.getPassword();
            boolean matches = JBCrypto.passwordsMatch(passwordEncoderId, encPassword, changedPassword);

            if (!matches) {
                eLogger.audit(executorId, dto.getId(), Constants.TABLE_BASE_USER,
                        user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.PASSWORD_CHANGE, null, user.getPassword(), null);

                user.setChangePasswordDate(date);
                user.setPassword(JBCrypto.encodePassword(passwordEncoderId, changedPassword));
                user.setEncryptionScheme(passwordEncoderId);
                savePasswordHistory();
                oldCustomer.setNewPassword(user.getPassword());
                oldCustomer.setOldPassword(encPassword);
            }
        }

        if (dto.getUserName() != null && !user.getUserName().equals(dto.getUserName())) {
            user.setUserName(dto.getUserName());
        }

        if (dto.getLanguageId() != null && !user.getLanguageIdField().equals(dto.getLanguageId())) {

            user.setLanguage(new LanguageDAS().find(dto.getLanguageId()));
        }

        if (user.getEntity().getId() != dto.getEntityId()) {

            user.setCompany(new CompanyDAS().find(dto.getEntityId()));
        }

        if (dto.getStatusId() != null && user.getStatus().getId() != dto.getStatusId()) {
            AgeingBL age = new AgeingBL();
            age.setUserStatus(executorId, user.getUserId(), dto.getStatusId(), Calendar.getInstance().getTime());
            UserStatusDTO status = new UserStatusDAS().find(dto.getStatusId());

            if (status.getAgeingEntityStep() != null && status.getAgeingEntityStep().getSuspend() == 1) {
                AgeingTask.suspendUser(user, DateConvertUtils.getNow(), status);
            }
        }

        updateSubscriptionStatus(dto.getSubscriptionStatusId(), executorId);
        if (dto.getCurrencyId() != null && !user.getCurrencyId().equals(dto.getCurrencyId())) {
            user.setCurrency(new CurrencyDAS().find(dto.getCurrencyId()));
        }

        setAccountExpired(validateAccountExpired(dto.getAccountDisabledDate()), dto.getAccountDisabledDate());
        Integer oldRole = user.getRoles().stream().findFirst().get().getId();
        Integer newRole = dto.getMainRoleId();
        if(newRole != null && !oldRole.equals(newRole))  {
            user.getPermissions().clear();
        }

        RoleDTO role = new RoleBL().findByTypeOrId(dto.getMainRoleId(), dto.getEntityId());
        user.getRoles().clear();
        user.getRoles().add(role);
        user.setAccountLockedTime(dto.getAccountLockedTime());
        if(!role.getRoleTypeId().equals(Constants.TYPE_CUSTOMER) && !role.getRoleTypeId().equals(Constants.TYPE_PARTNER)) {
            user.updateMetaFieldsWithValidation(dto.getLanguageId(), dto.getEntityId(), null, dto);
        }

        MetaFieldValueWS[] oldMetaFields = new MetaFieldValueWS[0];
        CustomerDTO customerDTO = dto.getCustomer();
        CustomerDTO customerToUpdate = user.getCustomer();

        if (customerDTO != null && customerToUpdate != null) {
            if (customerDTO.getInvoiceDeliveryMethod() != null) {
                customerToUpdate.setInvoiceDeliveryMethod(customerDTO.getInvoiceDeliveryMethod());
            }

            customerToUpdate.setDueDateUnitId(customerDTO.getDueDateUnitId());
            customerToUpdate.setDueDateValue(customerDTO.getDueDateValue());
            customerToUpdate.setDfFm(customerDTO.getDfFm());

            try {
                EntityBL entityBL = new EntityBL();
                Set<PartnerDTO> partners = new HashSet<>(0);
                for (PartnerDTO partner : customerDTO.getPartners()) {
                    if (partner.getBaseUser().getEntity() != null) {
                        partners.add(partner);

                        if (!entityBL.isCompanyInHierarchy(dto.getCompany(), partner.getBaseUser().getEntity())) {
                            msg = "Partner's parent is not in company hierarchy.";
                            log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS,
                                    LogConstants.ACTION_UPDATE);
                            logger.error(log);
                            throw new SessionInternalError("Partner's parent is not in company hierarchy.",
                                    new String[]{"UserWS,partnerId,validation.error.partner.invalid.hierarchy"});
                        }
                    }
                }
                customerToUpdate.setPartners(partners);
            } catch (SessionInternalError ex) {
                throw ex;
            } catch (Exception ex) {
                msg = "Partner ids are not all valid.";
                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                logger.error(log);
                throw new SessionInternalError("Partner ids are not all valid.", ex,
                        new String[]{"UserWS,partnerId,validation.error.partner.does.not.exist"});
            }

            customerToUpdate.setExcludeAging(customerDTO.getExcludeAging());
            customerToUpdate.setCreditLimit(customerDTO.getCreditLimit());
            customerToUpdate.setAutoRecharge(customerDTO.getAutoRecharge());
            customerToUpdate.setRechargeThreshold(customerDTO.getRechargeThreshold());
            customerToUpdate.setLowBalanceThreshold(customerDTO.getLowBalanceThreshold());
            customerToUpdate.setMonthlyLimit(customerDTO.getMonthlyLimit());
            customerToUpdate.setAutoPaymentType(customerDTO.getAutoPaymentType());
            customerToUpdate.setReissueCount(customerDTO.getReissueCount());
            customerToUpdate.setReissueDate(customerDTO.getReissueDate());

            // update the linked user code
            UserBL.updateAssociateUserCodesToLookLikeTarget(customerToUpdate, customerDTO.getUserCodeLinks(),
                    "CustomerWS,userCode");

            //update customer specific commissions
            updateCustomerCustomerCommission(user, dto);

            // set the sub-account fields
            customerToUpdate.setIsParent(customerDTO.getIsParent());
            if (customerDTO.getParent() != null) {
                // the API accepts the user ID of the parent instead of the customer ID
                try {
                    if (customerDTO.getParent() != null) {
                        customerToUpdate.setParent(
                                new UserDAS().find(customerDTO.getParent().getId()).getCustomer());
                    } else {
                        customerToUpdate.setParent(null);
                    }
                } catch (Exception ex) {
                    msg = "There doesn't exist a parent with the supplied id.";
                    log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                    logger.error(log);
                    throw new SessionInternalError("There doesn't exist a parent with the supplied id.",
                            new String[]{"UserWS,parentId,validation.error.parent.does.not.exist"});
                }

                // use parent pricing flag
                customerToUpdate.setUseParentPricing(customerDTO.useParentPricing());

                // log invoice if child changes
                Integer oldInvoiceIfChild = customerToUpdate.getInvoiceChild();
                customerToUpdate.setInvoiceChild(customerDTO.getInvoiceChild());

                eLogger.audit(executorId,
                        user.getId(),
                        Constants.TABLE_CUSTOMER,
                        customerToUpdate.getId(),
                        EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.INVOICE_IF_CHILD_CHANGE,
                        (oldInvoiceIfChild != null ? oldInvoiceIfChild : 0),
                        null, null);
            } else {

                customerToUpdate.setParent(null);
            }

            Integer periodUnitId = -1;
            int nextInvoiceDayOfPeriod = -1;

            if (null != customerDTO.getMainSubscription()
                    && null != customerDTO.getMainSubscription().getSubscriptionPeriod()
                    && null != customerDTO.getMainSubscription().getSubscriptionPeriod().getPeriodUnit()) {
                periodUnitId = customerDTO.getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId();
            }

            if (null != customerDTO.getMainSubscription()
                    && null != customerDTO.getMainSubscription().getNextInvoiceDayOfPeriod()) {
                nextInvoiceDayOfPeriod = customerDTO.getMainSubscription().getNextInvoiceDayOfPeriod();
            }

            Calendar nextInvoiceCalendar = Calendar.getInstance();
            if (customerDTO.getNextInvoiceDate() != null) {
                nextInvoiceCalendar.setTime(customerDTO.getNextInvoiceDate());
            }

            if (customerDTO.getNextInvoiceDate()!=null && customerToUpdate.getNextInvoiceDate()!=null &&
                    customerDTO.getNextInvoiceDate().compareTo(customerToUpdate.getNextInvoiceDate()) != 0)  {
                if (periodUnitId.compareTo(Constants.PERIOD_UNIT_MONTH) == 0) {
                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_MONTH) != nextInvoiceDayOfPeriod) {
                        msg = "Billing cycle value should match with next invoice date: " + customerDTO.getNextInvoiceDate().toString();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error(log);
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.monthly.error" });
                    }
                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_WEEK) != (nextInvoiceDayOfPeriod)) {
                        msg = "Billing cycle value should match with next invoice date: " + customerDTO.getNextInvoiceDate().toString();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error(log);
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.weekly.error" });
                    }
                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
                    // if this is leap year and NID > Feb 28, then decrement nextInvoiceDayOfPeriod by 1 accounting for Feb 29
                    if (DateConvertUtils.asLocalDate(nextInvoiceCalendar.getTime()).isLeapYear() && nextInvoiceDayOfPeriod > 59) {
                        nextInvoiceCalendar.add(Calendar.DATE, -1);
                    }
                    if (LocalDate.now().isLeapYear() && nextInvoiceDayOfPeriod > 60) {
                        nextInvoiceDayOfPeriod -= 1;
                    }

                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_YEAR) != nextInvoiceDayOfPeriod) {
                        msg = "Billing cycle value should match with next invoice date: " + customerDTO.getNextInvoiceDate().toString();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error(log);
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.yearly.error" });
                    }
                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
                    int otherPossibleNextInvoiceDay = CalendarUtils.getSemiMonthlyOtherPossibleNextInvoiceDay(nextInvoiceCalendar.getTime(), nextInvoiceDayOfPeriod);
                    int nextInvoiceDay = nextInvoiceCalendar.get(Calendar.DAY_OF_MONTH);
                    if (nextInvoiceDay != nextInvoiceDayOfPeriod && nextInvoiceDay != otherPossibleNextInvoiceDay) {
                        msg = "Billing cycle value should match with next invoice date: " + customerDTO.getNextInvoiceDate().toString();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error(log);
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.monthly.error" });
                    }
                }
            }

            int oldPeriodUnitId = -1;
            String oldPeriodString = "";
            if (null != customerToUpdate.getMainSubscription()
                    && null != customerToUpdate.getMainSubscription().getSubscriptionPeriod()
                    && null != customerToUpdate.getMainSubscription().getSubscriptionPeriod().getPeriodUnit()) {
                OrderPeriodDTO periodDto = customerToUpdate.getMainSubscription().getSubscriptionPeriod();
                oldPeriodUnitId = periodDto.getPeriodUnit().getId();
                oldPeriodString = periodDto.getDescription(customerToUpdate.getBaseUser().getLanguage().getId());
            }

            int oldNextInvoiceDayOfPeriod = -1;
            if (null != customerToUpdate.getMainSubscription() && null != customerToUpdate.getMainSubscription().getNextInvoiceDayOfPeriod()) {
                oldNextInvoiceDayOfPeriod = customerToUpdate.getMainSubscription().getNextInvoiceDayOfPeriod();
            }

            // update the main subscription
            MainSubscriptionDTO newMainSubscription = customerDTO.getMainSubscription();
            MainSubscriptionDTO oldMainSubscription = customerToUpdate.getMainSubscription();
            CustomerDTO parent = customerToUpdate.getParent();
            OrderPeriodDTO orderPeriodDTO = newMainSubscription.getSubscriptionPeriod();
            OrderPeriodDTO parentCustomerOrderPeriodDTO = null;
            int parentChildCustomerPreference = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY);
            if (parentChildCustomerPreference == 1 && parent != null) {
                parentCustomerOrderPeriodDTO = parent.getMainSubscription().getSubscriptionPeriod();
            }

            if (null != parent && (customerToUpdate.getInvoiceChild() == null || customerToUpdate.getInvoiceChild() == 0)
                    && ((parentCustomerOrderPeriodDTO != null && orderPeriodDTO.getPeriodUnit() != parentCustomerOrderPeriodDTO.getPeriodUnit())
                            || (parentCustomerOrderPeriodDTO != null && !orderPeriodDTO.getValue().equals(parentCustomerOrderPeriodDTO.getValue()))
                            || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(parent.getMainSubscription().getNextInvoiceDayOfPeriod()) != 0
                            || Util.truncateDate(dto.getCustomer().getNextInvoiceDate()).compareTo(Util.truncateDate(parent.getNextInvoiceDate())) != 0)) {
                msg = "Child Billing cycle values should match with Parent Billing cycle values";
                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                logger.error(log);
                throw new SessionInternalError("Child Billing cycle values should match with Parent Billing cycle values",
                        new String[] { "CustomerWS,billingCycleValue,child.billing.cycle.is.diffrent.than.parent.billing.cycle" });
            }

            customerToUpdate.setMainSubscription(customerDTO.getMainSubscription());
            Date oldNextInvoiceDate = customerToUpdate.getNextInvoiceDate();
            Date newNextInvoiceDate = customerDTO.getNextInvoiceDate();
            if (periodUnitId != oldPeriodUnitId || nextInvoiceDayOfPeriod != oldNextInvoiceDayOfPeriod) {
                setCustomerNextInvoiceDate(user,executorId);
                CustomerBillingCycleChangeEvent customerBillingCycleChangeEvent = new CustomerBillingCycleChangeEvent(dto.getEntityId(), user);
                EventManager.process(customerBillingCycleChangeEvent);
                addBillingCycleChangeAuditLog(executorId, oldPeriodString, oldNextInvoiceDayOfPeriod, oldNextInvoiceDate);
            }
            if (null != customerDTO.getNextInvoiceDate() && null != newNextInvoiceDate && !newNextInvoiceDate.equals(oldNextInvoiceDate)){
                customerToUpdate.setNextInvoiceDate(newNextInvoiceDate);
                addNIDChangeAuditLog(executorId, oldNextInvoiceDate);
            } else {
                if (null == customerDTO.getNextInvoiceDate()) {
                    msg = "Next Invoice Date should not be null: " ;
                    log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                    logger.error(log);
                    throw new SessionInternalError(msg, new String[] { "CustomerWS,billingCycleValue,next.invoice.date.not.null" });
                }
            }
            // When parent billing cycle is updated then update the billing cycle, invoice generation day and
            // next invoice date of the parent customer as well as all its sub-accounts with 'Invoice if Child' flag
            // unchecked.
            if (oldNextInvoiceDate!= null && newNextInvoiceDate != null && newNextInvoiceDate.compareTo(oldNextInvoiceDate) != 0
                    || newMainSubscription.getSubscriptionPeriod().getId() != oldMainSubscription.getSubscriptionPeriod().getId()
                    || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(oldMainSubscription.getNextInvoiceDayOfPeriod()) != 0) {
                // update the Next invoice date and billing cycle of childs
                updateBillingCycleOfChildAsPerParent(user);
            }

            oldMetaFields = new UserBL(user).getUserWS().getMetaFields();

            customerToUpdate.updateCustomerMetaFieldsWithValidation(dto.getEntityId(), customerDTO);

            // delete removed meta fields
            if (dto.getRemovedDatesMap() != null) {
                for (Map.Entry<Integer, ArrayList<Date>> entry : dto.getRemovedDatesMap().entrySet()) {
                    Integer aitId = entry.getKey();
                    for (Date dateKey : entry.getValue()) {
                        customerToUpdate.removeCustomerAccountInfoTypeMetaFields(aitId, dateKey);
                    }
                }
            }

            Integer accountTypeId = customerToUpdate.getAccountType().getId();
            Map<Integer, List<MetaField>> aitMetaField = MetaFieldExternalHelper.getAvailableAccountTypeFieldsMap(accountTypeId);
            Map<Integer, Map<String, Object>> oldAITMetaFieldValueMap = convertAITMetaFieldNameAndValueMap(customerToUpdate, aitMetaField);
            // Validating Customer MetaField
            updateMetaFieldsWithValidation(dto.getEntityId(), accountTypeId, customerDTO);

            // update/create ait meta fields with validation for given date
            customerToUpdate.updateAitMetaFieldsWithValidation(dto.getEntityId(), customerToUpdate.getAccountType().getId(), customerDTO);

            logger.debug("Setting ait meta fields");
            AccountInformationTypeDAS accountInfoTypeDas = new AccountInformationTypeDAS();
            AccountInformationTypeDTO accountInfoType;

            for (Map.Entry<Integer, List<MetaFieldValue>> entry : customerToUpdate.getAitMetaFieldMap().entrySet()) {
                accountInfoType = accountInfoTypeDas.find(entry.getKey());
                for (MetaFieldValue value : entry.getValue()) {
                    Date effectiveDate = dto.getEffectiveDateMap().get(entry.getKey());
                    // if there is no effective date, set it to default date
                    if (effectiveDate == null) {
                        effectiveDate = CommonConstants.EPOCH_DATE;
                    }
                    customerToUpdate.addCustomerAccountInfoTypeMetaField(value, accountInfoType, effectiveDate);
                }
            }

            MetaFieldExternalHelper.removeEmptyAitMetaFields(customerToUpdate);
            // JBAGL-7: InvoiceDesign field should not be updated by updateUser Method - JBAGL-7
            //customerToUpdate.setInvoiceDesign(customerDTO.getInvoiceDesign());
            customerToUpdate.setInvoiceTemplate(customerDTO.getInvoiceTemplate());
            Map<Integer, Map<String, Object>> newAITMetaFieldValueMap = convertAITMetaFieldNameAndValueMap(customerDTO, aitMetaField);
            Map<Integer, Map<String, ValueDifference<Object>>> oldNewAITValueMapByName = new HashMap<>();
            for(Entry<Integer, List<MetaField>> aitEntry : aitMetaField.entrySet()) {
                Map<String, Object> oldAIT = oldAITMetaFieldValueMap.get(aitEntry.getKey());
                Map<String, Object> newAIT = newAITMetaFieldValueMap.get(aitEntry.getKey());
                if(MapUtils.isEmpty(oldAIT) || MapUtils.isEmpty(newAIT)) {
                    continue;
                }
                Map<String, ValueDifference<Object>> metaFieldDiff =
                        Maps.difference(oldAIT, newAIT).entriesDiffering();
                if(MapUtils.isEmpty(metaFieldDiff)) {
                    continue;
                }
                oldNewAITValueMapByName.put(aitEntry.getKey(), metaFieldDiff);
            }
            UserDTO baseUser = customerToUpdate.getBaseUser();
            logger.debug("AIT Metafield diff calculated {} for user {}", oldNewAITValueMapByName, baseUser.getId());
            UpdateCustomerEvent event = new UpdateCustomerEvent(customerToUpdate.getId(), oldCustomer,
                    oldNewAITValueMapByName, baseUser.getCompany().getId());
            logger.debug("processing {} event", event);
            EventManager.process(event);
        }

        if(dto.getPaymentInstruments().size()==0 && getUserWS().getPaymentInstruments().size() !=0){
            PaymentInformationWS paymentInformationWS = getUserWS().getPaymentInstruments().remove(0);
            if(paymentInformationWS.getId() != null && paymentInformationWS.getId() != 0) {
                PaymentInformationDAS das = new PaymentInformationDAS();
                PaymentInformationDTO paymentInformationDTO = das.findNow(paymentInformationWS.getId());
                if(paymentInformationDTO != null) {
                    try {
                        Integer index = getPaymentInformationIndex(paymentInformationDTO.getId(), user.getPaymentInstruments());
                        if(null!= index) {
                            user.getPaymentInstruments().remove(index.intValue());
                        }
                        das.updatePayment(paymentInformationDTO.getId());
                        das.delete(paymentInformationDTO);
                        msg = "Payment instrument: " + paymentInformationDTO.getId() + " has been deleted.";
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error(log);
                    } catch(Exception e) {
                        msg = "Could not delete payment instrument: " + paymentInformationDTO;
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.error("{} Exception is: {}", log, e);
                    }

                    try {
                        paymentInformationDTO.close();
                    } catch (Exception exception) {
                        logger.debug("exception: {}", exception);
                    }
                }
            }
        }

        try {

            // payment instruments
            for(PaymentInformationDTO paymentInformation : dto.getPaymentInstruments()) {
                if(paymentInformation.isMetaFieldEmpty()){
                    continue;
                } // all meta fields are empty then continue.

                if(paymentInformation.getId() != null) {
                    // update payment information, get index of payment instrument in saved user instruments list
                    logger.debug("Existing user instruments are: {}", user.getPaymentInstruments().size());
                    Integer index = getPaymentInformationIndex(paymentInformation.getId(), user.getPaymentInstruments());

                    removeCCPreAuthorization(paymentInformation, user.getId());

                    // update saved payment information
                    logger.debug("Getting payment instrument to update at index: {}", index);
                    PaymentInformationDTO saved = user.getPaymentInstruments().get(index);
                    saved.setPaymentMethodId(paymentInformation.getPaymentMethodId());

                    // if we have changed payment method type for a payment instrument then old fields
                    // should be cleared
                    if(saved.getPaymentMethodType().getId() != paymentInformation.getPaymentMethodType().getId()) {
                        saved.setUpdateDateTime(TimezoneHelper.companyCurrentDate(user.getEntity()));
                        saved.setPaymentMethodType(paymentInformation.getPaymentMethodType());
                        saved.getMetaFields().clear();

                        eLogger.audit(executorId,
                                user.getId(),
                                Constants.TABLE_PAYMENT_INFORMATION,
                                saved.getId(),
                                EventLogger.MODULE_PAYMENT_INFORMATION_MAINTENANCE,
                                EventLogger.ROW_DELETED, null, null, null);
                        msg = "Payment instrument with ID: " + saved.getId() + " successfully cleared for user " + dto.getId();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE);
                        logger.info(log);
                    }

                    //check if processing order or metaField value of existing payment instrument has changed or not
                    if (!Optional.ofNullable(saved.getProcessingOrder()).equals(Optional.ofNullable(paymentInformation.getProcessingOrder()))){
                        saved.setUpdateDateTime(TimezoneHelper.companyCurrentDate(user.getEntity()));
                        Map<String, String> paymentInstrumentUpdatedValues = getPaymentInstrumentUpdatedValues(paymentInformation, saved);
                        eLogger.audit(executorId,
                                user.getId(),
                                Constants.TABLE_PAYMENT_INFORMATION,
                                saved.getId(),
                                EventLogger.MODULE_PAYMENT_INFORMATION_MAINTENANCE,
                                EventLogger.ROW_UPDATED, null, paymentInstrumentUpdatedValues.toString(), null);
                        msg = "Processing order of Payment instrument with ID: " + saved.getId() + " successfully changed for user " + dto.getId();
                        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE);
                        logger.info(log);

                    } else if (saved.getPaymentMethodType().getId() == paymentInformation.getPaymentMethodType().getId()
                            && isPaymentInstrumentUpdated(paymentInformation, saved)) {
                        saved.setUpdateDateTime(TimezoneHelper.companyCurrentDate(user.getEntity()));
                        Map<String, String> paymentInstrumentUpdatedValues = getPaymentInstrumentUpdatedValues(paymentInformation, saved);
                        eLogger.audit(executorId,
                                user.getId(),
                                Constants.TABLE_PAYMENT_INFORMATION,
                                paymentInformation.getId(),
                                EventLogger.MODULE_PAYMENT_INFORMATION_MAINTENANCE,
                                EventLogger.ROW_UPDATED, null, paymentInstrumentUpdatedValues.toString(), null);
                    }

                    saved.setProcessingOrder(paymentInformation.getProcessingOrder());
                    saved.updatePaymentMethodMetaFieldsWithValidation(dto.getEntityId(), paymentInformation);
                    setCreditCardType(saved);
                    paymentInformationDAS.save(saved);
                    if("ACH".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                        EventManager.process(new AchUpdateEvent(saved, user.getEntity().getId()));
                    }else if("Payment Card".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                        EventManager.process(new NewCreditCardEvent(saved, user.getEntity().getId()));
                    }
                    user.getPaymentInstruments().add(index, saved);
                    saved.close();
                    paymentInformation.close();
                } else {
                    // create a new one
                    paymentInformation.setUser(user);
                    PaymentInformationDTO saved = paymentInformationDAS.create(paymentInformation, dto.getEntityId());
                    setCreditCardType(paymentInformation);

                    if("ACH".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                        EventManager.process(new AchUpdateEvent(saved, user.getEntity().getId()));
                    }else if("Payment Card".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                        EventManager.process(new NewCreditCardEvent(saved, user.getEntity().getId()));
                    }
                    user.getPaymentInstruments().add(saved);

                    eLogger.audit(executorId,
                            user.getId(),
                            Constants.TABLE_PAYMENT_INFORMATION,
                            saved.getId(),
                            EventLogger.MODULE_PAYMENT_INFORMATION_MAINTENANCE,
                            EventLogger.ROW_CREATED, null, null, null);
                    msg = "Payment instrument with ID: " + saved.getId() + " successfully added for user " + dto.getId();
                    log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_CREATE);
                    logger.info(log);

                    saved.close();
                }

                paymentInformation.close();
            }
        }catch(Exception exception){
            logger.debug("exception: {}", exception );
            throw new SessionInternalError(exception);
        }

        //audit log for updated values
        Arrays.stream(oldMetaFields).forEach(oldValue -> {
            Optional<MetaFieldValue> newValue = customerDTO.getMetaFields().stream()
                    .filter(metaFieldValue -> metaFieldValue.getFieldName().equals(oldValue.getFieldName()))
                    .findFirst();

            Object value = newValue.map(MetaFieldValue::getValue).orElse("");
            if (!oldValue.getValue().equals(value)) {
                String message = (value.toString().trim().isEmpty()) ? String.format("%s '%s' was removed.", oldValue.getFieldName(), oldValue.getValue()) :
                        String.format("%s was changed from '%s' to '%s'.", oldValue.getFieldName(), oldValue.getValue(), value);

                eLogger.auditLog(executorId, user.getId(), Constants.TABLE_CUSTOMER, user.getId(),
                        EventLogger.MODULE_USER_MAINTENANCE, getMessageId(oldValue.getFieldName()), null, message, null);
            }
        });

        //audit log for newly added values
        if (customerDTO != null) {
            List<MetaFieldValueWS> metaFieldValueWS = Arrays.asList(oldMetaFields);
            customerDTO.getMetaFields().forEach(metaFieldValue -> {
                if (metaFieldValueWS.stream().noneMatch(metaFieldValueWS1 -> metaFieldValueWS1.getMetaField().getName().equals(metaFieldValue.getField().getName()))) {
                    eLogger.auditLog(executorId, user.getId(), Constants.TABLE_CUSTOMER, user.getId(),
                        EventLogger.MODULE_USER_MAINTENANCE, getMessageId(metaFieldValue.getFieldName()), null,
                        String.format("%s '%s' was added.", metaFieldValue.getFieldName(), null != metaFieldValue.getValue() ? metaFieldValue.getValue().toString() : ""),
                        null);
                }
            });
        }

        eLogger.audit(executorId,
                user.getId(),
                Constants.TABLE_BASE_USER,
                user.getId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null, null);

        msg = "User updated successfully: " + dto.getId();
        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE);
        logger.debug(log);
    }

    private void addNIDChangeAuditLog(Integer executorId, Date oldNextInvoiceDate) {
        if (null != executorId) {
            eLogger.audit(executorId,
                    user.getId(),
                    Constants.TABLE_CUSTOMER, user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.NEXT_INVOICE_DATE_CHANGE,
                    null, null, oldNextInvoiceDate);
        } else {
            eLogger.auditBySystem(user.getEntity().getId(),
                    user.getId(),
                    Constants.TABLE_CUSTOMER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.NEXT_INVOICE_DATE_CHANGE,
                    null, null, oldNextInvoiceDate);
        }
    }

    private void addBillingCycleChangeAuditLog(Integer executorId, String oldPeriodString, int oldNextInvoiceDayOfPeriod, Date oldNextInvoiceDate) {
        if (null != executorId) {
            eLogger.audit(executorId,
                    user.getId(),
                    Constants.TABLE_CUSTOMER, user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.BILLING_CYCLE_CHANGE,
                    null, oldNextInvoiceDayOfPeriod + "-" + oldPeriodString, oldNextInvoiceDate);
        } else {
            eLogger.auditBySystem(user.getEntity().getId(),
                    user.getId(),
                    Constants.TABLE_CUSTOMER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.BILLING_CYCLE_CHANGE,
                    null, oldNextInvoiceDayOfPeriod + "-" + oldPeriodString, oldNextInvoiceDate);
        }
    }

    public void setSSOEnabledPCICompliantPassword(){
        String randPassword = generatePCICompliantPassword();
        JBillingPasswordEncoder passwordEncoder = new JBillingPasswordEncoder();
        user.setPassword(passwordEncoder.encodePassword(randPassword, null));
    }

    public static String getUserEnhancedLogMessage(String msg, LogConstants status, LogConstants action) {
        return new LogMessage.Builder()
        .module(LogConstants.MODULE_USER.toString())
        .status(status.toString())
        .action(action.toString())
        .message(msg)
        .build().toString();
    }

    private void setMonthAndYearFrom(Date newNextInvoiceDate, UserDTO user) {
        Calendar cal = new GregorianCalendar();
        Calendar newInvoiceCal = new GregorianCalendar();
        newInvoiceCal.setTime(newNextInvoiceDate);
        cal.setTime(user.getCustomer().getNextInvoiceDate());
        cal.set(Calendar.MONTH, newInvoiceCal.get(Calendar.MONTH));
        cal.set(Calendar.YEAR, newInvoiceCal.get(Calendar.YEAR));

        if (user.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId() == Constants.PERIOD_UNIT_WEEK) {
        	cal.set(Calendar.DAY_OF_MONTH, newInvoiceCal.get(Calendar.DAY_OF_MONTH));
        }


        user.getCustomer().setNextInvoiceDate(cal.getTime());
    }

    private Map<Integer, Map<String, Object>> convertAITMetaFieldNameAndValueMap(CustomerDTO customer, Map<Integer, List<MetaField>> aitMetaFieldMap) {
        Map<Integer, Map<String, Object>> aitValueMetaFieldMap = new HashMap<>();
        for(Entry<Integer, List<MetaField>> aitEntry : aitMetaFieldMap.entrySet()) {
            Map<String, Object> aitNameAndValueMap = aitValueMetaFieldMap.getOrDefault(aitEntry.getKey(), new HashMap<>());
            if(aitNameAndValueMap.isEmpty()) {
                aitValueMetaFieldMap.put(aitEntry.getKey(), aitNameAndValueMap);
            }
            for(MetaField mf : aitEntry.getValue()) {
                CustomerAccountInfoTypeMetaField  customerMetaField= customer.getCurrentCustomerAccountInfoTypeMetaField(mf.getName(), aitEntry.getKey());
                MetaFieldValue<?> value = null;
                if(null!= customerMetaField &&
                        null!= customerMetaField.getMetaFieldValue()) {
                    value = customerMetaField.getMetaFieldValue();
                } else {
                    value = customer.getMetaField(mf.getName(), aitEntry.getKey());
                }
                aitNameAndValueMap.put(mf.getName(), value!=null ? value.getValue() : null);
            }
        }
        return aitValueMetaFieldMap;
    }

    private void setCreditCardType (PaymentInformationDTO saved) {
        PaymentInformationBL piBl = new PaymentInformationBL();
        // if its a credit card, sets is type
        MetaFieldValue value = piBl.getMetaField(saved, MetaFieldType.CC_TYPE);
        char[] creditCardNumber = piBl.getCharMetaFieldByType(saved, MetaFieldType.PAYMENT_CARD_NUMBER);
        if (value != null && value.getField() != null) {
            logger.debug("Updating credit card type for instrument: {}" + saved.getId());
            if (null != creditCardNumber && creditCardNumber.length!=0 && !PaymentInformationBL.paymentCardObscured(creditCardNumber)) {
                Integer ccTypeId = piBl.getPaymentMethod(creditCardNumber);
                if (null != ccTypeId) {
                    saved.setMetaField(value.getField(), convertCreditCardType(ccTypeId));
                }
            }
        }
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

    /**
     * get Description of Credit Card type using integer ccType.
     * @param ccType
     * @return
     */
    private static String convertCreditCardType(int ccType) {
        switch(ccType) {
        case 2: return CreditCardType.VISA.toString();
        case 3: return CreditCardType.MASTER_CARD.toString();
        case 4: return CreditCardType.AMEX.toString();
        case 6: return CreditCardType.DISCOVER.toString();
        case 7: return CreditCardType.DINERS.toString();
        case 11: return CreditCardType.JCB.toString();
        case 13: return CreditCardType.MAESTRO.toString();
        case 14: return CreditCardType.VISA_ELECTRON.toString();
        }

        return "";
    }

    private int countDiffDay (Calendar c1, Calendar c2) {
        int returnInt = 0;
        while (!c1.after(c2)) {
            c1.add(Calendar.DAY_OF_MONTH, 1);
            returnInt++;
        }

        /*
         * if (returnInt > 0) { returnInt = returnInt - 1; }
         */

        return (returnInt);
    }

    public boolean exists (String userName, Integer entityId) {
        if (userName == null || entityId == null) {
            logger.debug("User name and entity ID are required, cannot check user existence");
            return true; // just in case this prompts them to try and create a user.
        }

        return new UserDAS().findByUserName(userName, entityId, true) != null;
    }

    public boolean exists (Integer userId, Integer entityId) {
        if (userId == null || entityId == null) {
            logger.debug("User ID and entity ID are required, cannot check user existence");
            return true; // just in case this prompts them to try and create a user.
        }

        return new UserDAS().exists(userId, entityId);
    }

    public Integer create (UserDTOEx dto, Integer executorUserId) throws SessionInternalError {

        Integer newId;
        String msg;
        String log;
        List<Integer> roles = new ArrayList<>();
        if (dto.getRoles() == null || dto.getRoles().size() == 0) {
            if (dto.getMainRoleId() != null) {
                roles.add(dto.getMainRoleId());
            } else {
                msg = "Creating user without any role...";
                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_EVENT);
                logger.warn(log);
            }
        } else {
            for (RoleDTO role : dto.getRoles()) {
                roles.add(role.getRoleTypeId());
            }
        }

        logger.info("Roles set for user");
        RoleDTO newUserRole = new RoleBL().findByTypeOrId(dto.getMainRoleId(), dto.getEntityId());
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(newUserRole.getRoleTypeId());
        dto.setEncryptionScheme(passwordEncoderId);

        // may be this is a partner
        if (dto.getPartner() != null) {
            if(dto.getPartner().getParent() != null && !new EntityBL().isCompanyInHierarchy(dto.getCompany(), dto.getPartner().getParent().getBaseUser().getEntity())) {
                msg = "Partner's parent is not in company hierarchy.";
                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError("Partner's parent is not in company hierarchy.",
                        new String[] { "PartnerWS,parent,validation.error.parent.invalid.hierarchy" });
            }

            newId = create(dto.getEntityId(), dto.getCreateDatetime(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(),
                    roles, dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());

            PartnerBL partner = new PartnerBL();
            Integer partnerId = partner.create(dto.getPartner());
            partner.getEntity().setId(partnerId);//        return dto;
            user.setPartner(partner.getEntity());
            partner.getEntity().setBaseUser(user);
            user.getPartner().updateMetaFieldsWithValidation(dto.getLanguageId(), dto.getEntityId(), null, dto.getPartner());

            createUserCode(user);
        } else if (dto.getCustomer() != null) {
            // link the partner
            Set<PartnerDTO> partners = new HashSet<>();
            if (dto.getCustomer().getPartners() != null) {
                try {
                    PartnerBL partnerBL = new PartnerBL();
                    for(PartnerDTO dtoPartner : dto.getCustomer().getPartners()) {
                        partnerBL.set(dtoPartner.getId());
                        PartnerDTO newPartner = partnerBL.getDTO();
                        if (newPartner.getUser().getEntity().getId() == dto.getEntityId() && newPartner.getUser().getDeleted() == 0) {
                            partners.add(newPartner);

                            if(!new EntityBL().isCompanyInHierarchy(dto.getCompany(), newPartner.getBaseUser().getEntity())) {
                                msg = "Partner's parent is not in company hierarchy.";
                                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                                logger.error(log);
                                throw new SessionInternalError("Partner's parent is not in company hierarchy.",
                                        new String[] { "UserWS,partnerId,validation.error.partner.invalid.hierarchy" });
                            }
                        }
                    }
                } catch (SessionInternalError ex) {
                    throw ex;
                } catch (Exception ex) {
                    msg = "A partner with the supplied id does not exist.";
                    log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                    logger.error(log);
                    throw new SessionInternalError("A partner with the supplied id does not exist.",
                            new String[] { "UserWS,partnerId,validation.error.partner.does.not.exist" }, HttpStatus.SC_INTERNAL_SERVER_ERROR);
                }
            }

            newId = create(dto.getEntityId(), dto.getCreateDatetime(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(),
                    roles, dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());

            user.setCustomer(new CustomerDAS().create());

            user.getCustomer().setBaseUser(user);
            user.getCustomer().setReferralFeePaid(dto.getCustomer().getReferralFeePaid());

            if (dto.getCustomer().getInvoiceDeliveryMethod() != null) {
                user.getCustomer().setInvoiceDeliveryMethod(dto.getCustomer().getInvoiceDeliveryMethod());
            }

            user.getCustomer().setPartners(partners);

            // set the sub-account fields
            user.getCustomer().setIsParent(dto.getCustomer().getIsParent());
            if (dto.getCustomer().getParent() != null) {
                // the API accepts the user ID of the parent instead of the customer ID
                user.getCustomer().setParent(new UserDAS().find(dto.getCustomer().getParent().getId()).getCustomer());
                user.getCustomer().setInvoiceChild(dto.getCustomer().getInvoiceChild());
                user.getCustomer().setUseParentPricing(dto.getCustomer().useParentPricing());
            }

            user.getCustomer().setDueDateUnitId(dto.getCustomer().getDueDateUnitId());
            user.getCustomer().setDueDateValue(dto.getCustomer().getDueDateValue());

            // set dynamic balance fields
            user.getCustomer().setCreditLimit(dto.getCustomer().getCreditLimit());
            user.getCustomer().setDynamicBalance(dto.getCustomer().getDynamicBalance());
            user.getCustomer().setAutoRecharge(dto.getCustomer().getAutoRecharge());

            user.getCustomer().setMonthlyLimit(dto.getCustomer().getMonthlyLimit());
            user.getCustomer().setRechargeThreshold(dto.getCustomer().getRechargeThreshold());
            user.getCustomer().setLowBalanceThreshold(dto.getCustomer().getLowBalanceThreshold());
            user.getCustomer().setReissueCount(dto.getCustomer().getReissueCount());
            user.getCustomer().setReissueDate(dto.getCustomer().getReissueDate());

            AccountTypeDTO accountType = dto.getCustomer().getAccountType();

            // set credit notification limit 1 & 2
            if (accountType != null) {
                user.getCustomer().setCreditNotificationLimit1(null != dto.getCustomer().getCreditNotificationLimit1() ?
                        dto.getCustomer().getCreditNotificationLimit1() : accountType.getCreditNotificationLimit1());
                user.getCustomer().setCreditNotificationLimit2(null != dto.getCustomer().getCreditNotificationLimit2() ?
                        dto.getCustomer().getCreditNotificationLimit2() : accountType.getCreditNotificationLimit2());
            }

            // additional customer fields
            user.getCustomer().setMainSubscription(dto.getCustomer().getMainSubscription());

            // set next Invoice Date Based on Last billing process
            setCustomerNextInvoiceDateBasedOnLastRanBillingProcess(user);

            // Validation if The Billing cycle of sub-accounts should match with parent account billing cycle.
            MainSubscriptionDTO newMainSubscription = dto.getCustomer().getMainSubscription();
            CustomerDTO parent = user.getCustomer().getParent();
            OrderPeriodDTO orderPeriodDTO = newMainSubscription.getSubscriptionPeriod();
            OrderPeriodDTO parentCustomerOrderPeriodDTO = null;
            int parentChildCustomerPreference = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY);
            if (parentChildCustomerPreference == 1 && parent != null) {
                parentCustomerOrderPeriodDTO= parent.getMainSubscription().getSubscriptionPeriod();
            }

            if (null != parent
                    && (user.getCustomer().getInvoiceChild() == null || user.getCustomer().getInvoiceChild() == 0)
                    && ((parentCustomerOrderPeriodDTO != null && orderPeriodDTO.getPeriodUnit() != parentCustomerOrderPeriodDTO.getPeriodUnit())
                            || (parentCustomerOrderPeriodDTO != null && orderPeriodDTO.getValue() != parentCustomerOrderPeriodDTO.getValue())
                            || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(
                                    parent.getMainSubscription().getNextInvoiceDayOfPeriod()) != 0)) {
                msg = "Child Billing cycle value should match with Parent Billing cycle value";
                log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE);
                logger.error(log);
                throw new SessionInternalError(
                        "Child Billing cycle value should match with Parent Billing cycle value",
                        new String[] { "CustomerWS,billingCycleValue,child.billing.cycle.is.different.than.parent.billing.cycle" }, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }

            user.getCustomer().setAutoPaymentType(dto.getCustomer().getAutoPaymentType());

            // save linked user codes
            UserBL.updateAssociateUserCodesToLookLikeTarget(user.getCustomer(), dto.getCustomer().getUserCodeLinks(), "CustomerWS,userCode");
            updateCustomerCustomerCommission(user, dto);

            // meta fields
            Integer accountTypeId = null != accountType ? accountType.getId() : null;
            updateMetaFieldsWithValidation(dto.getEntityId(), accountTypeId, dto.getCustomer());
            user.getCustomer().updateAitMetaFieldsWithValidation(dto.getEntityId(), accountTypeId, dto.getCustomer());

            // save ait meta field with given dates
            logger.debug("Setting AIT meta fields for given dates in customer");
            AccountInformationTypeDAS accountInfoTypeDas = new AccountInformationTypeDAS();
            AccountInformationTypeDTO accountInfoType;
            List<Date> timelineDates;

            for (Map.Entry<Integer, List<MetaFieldValue>> entry : user.getCustomer().getAitMetaFieldMap().entrySet()) {

                Integer aitId = entry.getKey();

                accountInfoType = accountInfoTypeDas.find(entry.getKey());
                timelineDates = dto.getTimelineDatesMap().get(aitId);

                // if no dates are provided for the given aitId then use default date
                if (timelineDates == null || timelineDates.size() < 1) {
                    timelineDates = new ArrayList<>(0);
                    timelineDates.add(CommonConstants.EPOCH_DATE);
                }

                for (MetaFieldValue value : entry.getValue()) {
                    logger.debug("Setting meta field: {}", value);
                    for (Date date : timelineDates) {
                        MetaFieldValue rigged = generateValue(value);
                        user.getCustomer().addCustomerAccountInfoTypeMetaField(rigged, accountInfoType, date);
                    }
                }
            }

            MetaFieldExternalHelper.removeEmptyAitMetaFields(user.getCustomer());

            user.getCustomer().setInvoiceDesign(dto.getCustomer().getInvoiceDesign());
            user.getCustomer().setInvoiceTemplate(dto.getCustomer().getInvoiceTemplate());
            user.getCustomer().setAccountType(accountType);

            for(PaymentInformationDTO paymentInformation : user.getPaymentInstruments()) {
                if(paymentInformation.isMetaFieldEmpty()){
                    continue;
                }

                if("ACH".equals(paymentInformation.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                    EventManager.process(new AchUpdateEvent(paymentInformation, user.getEntity().getId()));
                }else if("Payment Card".equals(paymentInformation.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                    EventManager.process(new NewCreditCardEvent(paymentInformation, user.getEntity().getId()));
                }
            }
        } else { // all the rest
            newId = create(dto.getEntityId(), dto.getCreateDatetime(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(), roles,
                    dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());

            createUserCode(user);
        }

        if(!newUserRole.getRoleTypeId().equals(Constants.TYPE_CUSTOMER) && !newUserRole.getRoleTypeId().equals(Constants.TYPE_PARTNER)) {
            user.updateMetaFieldsWithValidation(dto.getLanguageId(), dto.getEntityId(), null, dto);
        }

        //lock or unlock user account
        user.setAccountLockedTime(null);
        setAccountExpired(validateAccountExpired(dto.getAccountDisabledDate()), dto.getAccountDisabledDate());

        user = das.save(user);
        das.flush();

        if (user.getCustomer() != null && user.getUserStatus().getAgeingEntityStep() != null &&
                user.getUserStatus().getAgeingEntityStep().getSuspend() == 1) {
            AgeingTask.suspendUser(user, Util.truncateDate(user.getCreateDatetime()), user.getUserStatus());
        }

        msg = "Created new user with ID: " + newId;
        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_CREATE);
        logger.info(log);
        return newId;
    }

    private void updateCustomerCustomerCommission(UserDTO target, UserDTO source) {
        //map the partner id to the definition
        Map<Integer, CustomerCommissionDefinitionDTO> currentCommissions = new HashMap<>(target.getCommissionDefinitions().size() * 2);
        for(CustomerCommissionDefinitionDTO commissionDef : target.getCommissionDefinitions()) {
            currentCommissions.put(commissionDef.getId().getPartner().getId(), commissionDef);
        }
        //loop through new definitions
        for(CustomerCommissionDefinitionDTO srcDef : source.getCommissionDefinitions()) {
            CustomerCommissionDefinitionDTO current = currentCommissions.remove(srcDef.getId().getPartner().getId());
            //we have a definition for the partner, update id
            if(current != null) {
                if(!current.getRate().equals(srcDef.getRate())) {
                    current.setRate(srcDef.getRate());
                }
            } else { //no commission exists for the partner
                CustomerCommissionDefinitionDTO newDef = new CustomerCommissionDefinitionDTO(
                        new CustomerCommissionDefinitionPK(srcDef.getId().getPartner(), target),
                        srcDef.getRate());
                target.getCommissionDefinitions().add(newDef);
            }
        }

        //remove definitions in the target which are not in the source
        target.getCommissionDefinitions().removeAll(currentCommissions.values());

    }

    private Integer create (Integer entityId, Date createDateTime, String userName, String password, Integer languageId,
            List<Integer> roles, Integer currencyId, Integer statusId, Integer subscriberStatusId,
            Integer executorUserId, List<PaymentInformationDTO> paymentInstruments) throws SessionInternalError {

        // Default the language and currency to that one of the entity
        if (languageId == null) {
            EntityBL entity = new EntityBL(entityId);
            languageId = entity.getEntity().getLanguageId();
        }
        if (currencyId == null) {
            EntityBL entity = new EntityBL(entityId);
            currencyId = entity.getEntity().getCurrencyId();
        }

        // default the statuses
        if (statusId == null) {
            statusId = UserDTOEx.STATUS_ACTIVE;
        }
        if (subscriberStatusId == null) {
            subscriberStatusId = UserDTOEx.SUBSCRIBER_NONSUBSCRIBED;
        }

        UserDTO newUser = new UserDTO();
        newUser.setCompany(new CompanyDAS().find(entityId));
        newUser.setUserName(userName);
        newUser.setPassword(password);
        newUser.setLanguage(new LanguageDAS().find(languageId));
        newUser.setCurrency(new CurrencyDAS().find(currencyId));
        newUser.setUserStatus(new UserStatusDAS().find(statusId));
        newUser.setSubscriberStatus(new SubscriberStatusDAS().find(subscriberStatusId));
        newUser.setDeleted(0);
        newUser.setCreateDatetime(createDateTime != null ? createDateTime : Calendar.getInstance().getTime());
        newUser.setFailedAttempts(0);
        newUser.setEncryptionScheme(Integer.parseInt(Util.getSysProp(Constants.PASSWORD_ENCRYPTION_SCHEME)));

        user = das.save(newUser);
        String msg = "Creating new user: " + user;
        String log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_CREATE);
        logger.info(log);
        logger.debug("Changes flushed");

        // payment instruments
        try {
            for (PaymentInformationDTO paymentInformation : paymentInstruments) {
                paymentInformation.setUser(user);
                setCreditCardType(paymentInformation);
                PaymentInformationDTO saved = paymentInformationDAS.create(paymentInformation, entityId);
                saved.setCvv(paymentInformation.getCvv());
                boolean add = user.getPaymentInstruments().add(saved);
                if (add){
                    eLogger.audit(executorUserId,
                            user.getId(),
                            Constants.TABLE_PAYMENT_INFORMATION,
                            saved.getId(),
                            EventLogger.MODULE_PAYMENT_INFORMATION_MAINTENANCE,
                            EventLogger.ROW_CREATED, null, null, null);
                }
            }
        }catch (Exception exception){
            logger.debug("exception: {}", exception);
            throw new SessionInternalError(exception);
        }

        user.getRoles().clear();
        for (Integer roleId : roles) {
            RoleDTO role = new RoleDAS().findNow(roleId);
            if(role == null || role.getCompany() == null) {
                role = new RoleDAS().findByRoleTypeIdAndCompanyId(roleId, entityId);
            }
            user.getRoles().add(role);
        }

        if (null != executorUserId) {
            eLogger.audit(executorUserId, user.getId(), Constants.TABLE_BASE_USER, user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.ROW_CREATED, null, null, null);
        } else {
            eLogger.auditBySystem(entityId,
                    user.getId(),
                    Constants.TABLE_BASE_USER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        }


        return user.getUserId();
    }

    /**
     * Create the default user code.
     *
     * @param user
     */
    private void createUserCode (UserDTO user) {
        UserCodeDTO userCode = new UserCodeDTO();
        userCode.setIdentifier(user.getUserName() + "00001");
        userCode.setValidFrom(TimezoneHelper.companyCurrentDate(user.getEntity()));
        userCode.setUser(user);
        new UserCodeDAS().save(userCode);
    }

    @Deprecated
    public boolean validateUserNamePassword (UserDTOEx loggingUser, UserDTOEx db) {

        // the user status is not part of this check, as a customer that
        // can't login to the entity's service still has to be able to
        // as a customer to submit a payment or update her credit card
        if (db.getDeleted() == 0 && loggingUser.getEntityId().equals(db.getEntityId())) {

            String encodedPassword = db.getPassword();
            String plainPassword = loggingUser.getPassword();

            //using service specific for DB-user, loging one may not have its role set
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(new RoleBL().findByTypeOrId(db.getMainRoleId(), db.getEntityId()).getRoleTypeId());

            if (JBCrypto.passwordsMatch(passwordEncoderId, encodedPassword, plainPassword)){
                user = getUserEntity(db.getUserId());
                return true;
            }
        }

        return false;
    }

    /**
     * Tries to authenticate username/password for web services call. The user must be an administrator and have
     * permission 120 set. Returns the user's UserDTO if successful, otherwise null.
     */
    @Deprecated
    public UserDTO webServicesAuthenticate(String username, String plainPassword) {
        // try to get root user for this username that has web
        // services permission
        user = das.findWebServicesRoot(username);
        if (user == null) {
            logger.warn("Web services authentication: Username \"{} \" is either invalid, isn't an administrator or doesn't have web services permission granted (120).", username);
            return null;
        }

        // check password
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(Constants.TYPE_ROOT);
        if (JBCrypto.passwordsMatch(passwordEncoderId, user.getPassword(), plainPassword)) {
            return user;
        }

        logger.warn("Web services authentication: Invlid password for username \"{}\"", username);
        return null;
    }

    public UserDTO getEntity() {
        return user;
    }
    /**
     * sent the lost password to the user
     * @param entityId
     * @param userId
     * @param languageId
     * @throws SessionInternalError
     * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
     */
    public void sendLostPassword(Integer entityId, Integer userId, Integer languageId, String link) throws NotificationNotFoundException {
        NotificationBL notif = new NotificationBL();
        MessageDTO message = notif.getForgetPasswordEmailMessage(entityId, userId, languageId, link);
        INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        notificationSess.notify(userId, message);
    }

    /**
     * Sends the initial credentials
     *
     * @param entityId
     * @param userId
     * @param languageId
     * @throws SessionInternalError
     * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
     */
    public void sendCredentials(Integer entityId, Integer userId,
            Integer languageId, String link) throws SessionInternalError,
            NotificationNotFoundException {
        try{
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getInitialCredentialsEmailMessage(entityId, userId, languageId, link);
            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(userId, message);
        } catch (NotificationNotFoundException e){
            logger.error("Exception while sending notification : {}", e.getMessage());
            throw new SessionInternalError("Notification not found for sending lost password");
        }
    }

    /**
     * sent the lost password to the user
     * @param entityId
     * @param userId
     * @param languageId
     * @throws SessionInternalError
     * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
     */
    public void sendDeletedUser(Integer entityId, Integer userId,
            Integer languageId) throws SessionInternalError{
        try{
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getDeletedUSer(entityId, userId, languageId);
            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(userId, message);
        } catch (NotificationNotFoundException e){
            logger.error("Exception while sending notification : {}", e.getMessage());
            throw new SessionInternalError(TEXT_NOTIFICATION_NOT_FOUND, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public List<PermissionDTO> getPermissions() {
        List<PermissionDTO> ret = new ArrayList<PermissionDTO>();

        logger.debug("Reading permisions for user {}", user.getUserId());

        for (RoleDTO role: user.getRoles()) {
            // now get the permissions. They come sorted from the DB
            ret.addAll(role.getPermissions());
        }

        // now add / remove those privileges that were granted / revoked
        // to this particular user
        for(PermissionUserDTO permission : user.getPermissions()) {
            if (permission.isGranted()) {
                // see that this guy has it
                if (!ret.contains(permission.getPermission())) {
                    // not there, add it
                    ret.add(permission.getPermission());
                }
            } else {
                // make sure she doesn't
                if (ret.contains(permission.getPermission())) {
                    ret.remove(permission.getPermission());
                }
            }
        }

        // make sure the permissions are sorted
        Collections.sort(ret, new PermissionIdComparator());

        return ret;
    }

    /**
     * Sets the permissions for this user. Permissions that differ from the user's role will be saved as user specific
     * permissions that override the defaults.
     *
     * @param grantedPermissions
     *            a set of all permissions granted to this user
     * @return the list of implied permissions added
     */
    public List<PermissionDTO> setPermissions (Set<PermissionDTO> grantedPermissions, Integer callingUserId) {
        List<PermissionDTO> permissionsAdded = new ArrayList<>();

        Set<PermissionDTO> rolePermissions = new HashSet<>();

        // delete revoked permission
        das.removeUserPermission(user.getUserId());

        for (RoleDTO role : user.getRoles()) {
            rolePermissions.addAll(role.getPermissions());
        }

        //maps permission to all those which are dependent on it
        Map<PermissionDTO, Set<PermissionDTO>> permissionToChildrenMap = new HashMap<>();
        for(PermissionDTO permission : rolePermissions) {
            for(PermissionDTO impliedPermission : permission.rollUpImpliedPermissions()) {
                if(!permissionToChildrenMap.containsKey(impliedPermission)) {
                    permissionToChildrenMap.put(impliedPermission, new HashSet<>());
                }
                permissionToChildrenMap.get(impliedPermission).add(permission);
            }
        }

        Set<Integer> currentUserPermissions = new HashSet<>();
        for(PermissionUserDTO permissionUserDTO : user.getPermissions()) {
            currentUserPermissions.add(permissionUserDTO.getPermission().getId());
        }

        UserBL callerBL = new UserBL(callingUserId);

        Set<PermissionUserDTO> userPermissions = new HashSet<>();
        // add granted permissions
        for (PermissionDTO permission : grantedPermissions) {
            if (!rolePermissions.contains(permission) ) {
                boolean permitted = permission.isUserAssignable();
                if(permitted && !currentUserPermissions.contains(permission.getId()) && permission.getRequiredToAssign() != null) {
                    permitted = false;
                    permission.getRequiredToAssign().initializeAuthority();
                    String requiredAuth = permission.getRequiredToAssign().getAuthority();
                    for(PermissionDTO callerPermission : callerBL.getPermissions()) {
                        callerPermission.initializeAuthority();
                        if(callerPermission.getAuthority().equals(requiredAuth)) {
                            permitted = true;
                            break;
                        }
                    }
                }
                if(permitted) {
                    userPermissions.add(new PermissionUserDTO(user, permission, (short) 1));
                    //add all the implied permissions if the role does not already contain them
                    for(PermissionDTO implied : permission.rollUpImpliedPermissions()) {
                        if (!rolePermissions.contains(permission) ) {
                            userPermissions.add(new PermissionUserDTO(user, permission, (short) 1));
                        }
                    }
                } else {
                    throw new SessionInternalError("Calling user does not have permission to assign permission",
                            new String[] {"UserWS,permissions,validation.error.user.no.authority,"  + callingUserId + "," + permission.getAuthority()});
                }
            }
        }

        // add revoked permissions
        Set<PermissionDTO> revokedPermissions = new HashSet<>();
        for (PermissionDTO permission : rolePermissions) {
            if (!grantedPermissions.contains(permission)) {
                revokedPermissions.add(permission);
                userPermissions.add(new PermissionUserDTO(user, permission, (short) 0));
            }
        }

        //check for dependencies on revoked permissions.
        for(PermissionDTO revokedPermission : revokedPermissions) {
            if(permissionToChildrenMap.containsKey(revokedPermission) && !revokedPermissions.containsAll(permissionToChildrenMap.get(revokedPermission))) {
                for(PermissionUserDTO permissionUser : userPermissions) {
                    if(permissionUser.getPermission().equals(revokedPermission)) {
                        userPermissions.remove(permissionUser);
                        break;
                    }
                }
                permissionsAdded.add(revokedPermission);
            }
        }

        user.getPermissions().clear();
        user.getPermissions().addAll(userPermissions);

        this.user = das.save(user);

        String msg = String.format("%s overridden permissions for user %s", userPermissions.size(), user.getId());
        String fullSuccessMsg = new LogMessage.Builder().module(LogConstants.MODULE_PERMISSIONS.toString())
                .status(LogConstants.STATUS_SUCCESS.toString())
                .action(LogConstants.ACTION_UPDATE.toString())
                .message(msg).build().toString();
        logger.info(fullSuccessMsg);

        return permissionsAdded;
    }

    public UserWS getUserWS () throws SessionInternalError {
        UserDTOEx dto = DTOFactory.getUserDTOEx(user);
        UserWS retValue = getWS(dto);

        //Check whether account should be locked or not & update user account lock time to null if it should be unlocked
        if(null != dto.getAccountLockedTime()){
            retValue.setIsAccountLocked(isAccountLocked());
        } else {
            retValue.setIsAccountLocked(false);
        }

        //Check whether account should be inactive or not & update user account disabled time to null if it should be active
        if(null != dto.getAccountDisabledDate()){
            retValue.setAccountExpired(validateAccountExpired(dto.getAccountDisabledDate()));
        } else {
            retValue.setAccountExpired(false);
        }

        // the contact is not included in the Ex
        ContactBL bl = new ContactBL();

        bl.set(dto.getUserId());
        if (bl.getEntity() != null) { // this user has no contact ...
            retValue.setContact(ContactBL.getContactWS(bl.getDTO()));
        }

        List <CustomerNoteDTO> customerNoteDTOs=new CustomerNoteDAS().findByCustomer(retValue.getCustomerId(),dto.getEntityId());
        List <CustomerNoteWS> customerNoteWSList =new ArrayList<>();
        for(CustomerNoteDTO customerNoteDTO:customerNoteDTOs)
        {
            customerNoteWSList.add(convertCustomerNoteToWS(customerNoteDTO));
        }
        retValue.setCustomerNotes(customerNoteWSList.toArray(new CustomerNoteWS[customerNoteWSList.size()]));

        retValue.setAccessEntities(getAccessEntities(dto));
        return retValue;
    }

    public Integer getMainRole () {
        if (mainRole == null) {
            List roleIds = new LinkedList();
            for (RoleDTO nextRoleObject : user.getRoles()) {
                roleIds.add(nextRoleObject.getRoleTypeId());
            }
            mainRole = selectMainRole(roleIds);
        }
        return mainRole;
    }

    /**
     * Get the locale for this user.
     *
     * @return users locale
     */
    public Locale getLocale () {
        return user.getLanguage().asLocale();
    }

    public Integer getCurrencyId () {
        Integer retValue;

        if (user.getCurrencyId() == null) {
            retValue = user.getEntity().getCurrency().getId();
        } else {
            retValue = user.getCurrencyId();
        }

        return retValue;
    }

    /**
     * Will mark the user as deleted (deleted = 1), and do the same with all her orders, etc ... Not deleted for
     * reporting reasong: invoices, payments
     */
    public void delete (Integer executorId) throws SessionInternalError {
        String msg,log;
        int userId = user.getId();
        List<Integer> childList = das.findChildList(userId);
        if (CollectionUtils.isNotEmpty(childList)) {
            msg = "User Id " + userId + " cannot be deleted. Child users ID(s): " + childList + " exists.";
            log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_DELETE);
            logger.error(log);
            String[] errorMessages = new String[1];
            errorMessages[0] = "UserWS,childIds,validation.error.parent.user.cannot.be.deleted," + childList;
            throw new SessionInternalError("Cannot delete Parent User. Child ID(s) " + childList +" exists.", errorMessages, HttpStatus.SC_CONFLICT);
        }

        if(user.getCustomer() != null && user.getCustomer().getMetaFields() != null) {
            user.getCustomer().getMetaFields().clear();
        }

        user.setDeleted(1);
        // user deleted event triggered.
        EventManager.process(new UserDeletedEvent(user.getEntity().getId(), userId));
        user.setLastStatusChange(Calendar.getInstance().getTime());
        user.setSubscriberStatus(new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_EXPIRED));

        // orders
        for (OrderDTO order : user.getOrders()) {
            order.setDeleted(1);
            order.setDeletedDate(TimezoneHelper.companyCurrentDate(user.getEntity()));
        }
        // permissions
        user.getPermissions().clear();
        // roles
        user.getRoles().clear();

        CompanyUserDetails userDetails = new CompanyUserDetails(UsernameHelper.buildUsernameToken(user.getUserName(), user.getEntity().getId()), "", false, false, false, false, new ArrayList<>(), null, null, null, null, null, null);

        List<SessionInformation> sessionInformationList = sessionRegistry.getAllSessions(userDetails, false);

        logger.debug("Session Information Count = {}", sessionInformationList.size());

        for (SessionInformation sessionInformation : sessionInformationList) {
            sessionInformation.expireNow();
            logger.debug("Session of user : {} expired = {}", user.getUserName(), sessionInformation.isExpired());
        }

        if (executorId != null) {
            eLogger.audit(executorId, userId, Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.ROW_DELETED, null, null, null);
        }

        msg = "User with ID: " + userId + " has been deleted.";
        log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE);
        logger.info(log);

        // Delete AIT meta fields, uncomment following lines if
        // you want to remove ait meta fileds from database
        // when a user is deleted
        /*
         * if(user.getOrders().size() < 1) { user.getCustomer().removeAitMetaFields(); }
         */

        try {
            sendDeletedUser(user.getCompany().getId(), userId, 1);
        } catch (SessionInternalError e) {
            msg = TEXT_NOTIFICATION_NOT_FOUND;
            log = getUserEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_DELETE);
            logger.error(log);
            if(!e.getMessage().equals(TEXT_NOTIFICATION_NOT_FOUND)){
                logger.error(TEXT_NOTIFICATION_NOT_FOUND);
                throw e;
            }
        }
    }

    public UserDTO getDto () {
        return user;
    }

    /**
     * Verifies that both user belong to the same entity.
     *
     * @param rootUserName
     *            This has to be a root user
     * @param callerUserId
     * @return
     */
    public boolean validateUserBelongs (String rootUserName, Integer callerUserId) throws SessionInternalError {
        user = das.find(callerUserId);
        set(rootUserName, user.getEntity().getId());
        if (user == null) {
            return false;
        }
        if (user.getDeleted() == 1) {
            throw new SessionInternalError("the caller is set as deleted");
        }
        if (!getMainRole().equals(Constants.TYPE_ROOT)) {
            throw new SessionInternalError("can't validate but root users");
        }

        return true;
    }

    public UserWS[] convertEntitiesToWS (Collection dtos) throws SessionInternalError {
        try {
            UserWS[] ret = new UserWS[dtos.size()];
            int index = 0;
            for (Iterator it = dtos.iterator(); it.hasNext();) {
                user = (UserDTO) it.next();
                ret[index] = entity2WS();
                index++;
            }

            return ret;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public static CustomerNoteWS convertCustomerNoteToWS(CustomerNoteDTO customerNoteDTO) {

        if (customerNoteDTO == null) {
            return null;
        }
        return new CustomerNoteWS(customerNoteDTO.getNoteId(),customerNoteDTO.getNoteTitle(),
                customerNoteDTO.getNoteContent(),customerNoteDTO.getCreationTime(),customerNoteDTO.getCompany().getId(),
                customerNoteDTO.getCustomer().getId(),customerNoteDTO.getUser().getId());
    }
    public static MainSubscriptionWS convertMainSubscriptionToWS(MainSubscriptionDTO mainSubscription) {

        if (mainSubscription == null) {
            return null;
        }

        return new MainSubscriptionWS(mainSubscription.getSubscriptionPeriod().getId(),
                mainSubscription.getNextInvoiceDayOfPeriod());
    }

    public static MainSubscriptionDTO convertMainSubscriptionFromWS (MainSubscriptionWS mainSubscriptionWS,
            Integer entityId) {

        if (mainSubscriptionWS == null) {
            return MainSubscriptionDTO.createDefaultMainSubscription(entityId);
        }

        MainSubscriptionDTO mainSub = new MainSubscriptionDTO();
        mainSub.setSubsriptionPeriodFromPeriodId(mainSubscriptionWS.getPeriodId());
        mainSub.setNextInvoiceDayOfPeriod(mainSubscriptionWS.getNextInvoiceDayOfPeriod());
        return mainSub;
    }

    /**
     * It calculates billing until date to which the billing process evaluates the customer
     * <p>
     * This date determines how far the billing process sees in future based on user's main subscription
     *
     * @param nextInvoiceDate
     * @param billingDate
     * @return billing untill date
     */
    public Date getBillingUntilDate (Date nextInvoiceDate, Date billingDate) {

        logger.debug("Calculating billing until date based on the next invoice date {} and billing date {}", nextInvoiceDate, billingDate);

        MainSubscriptionDTO mainSubscription = getMainSubscription();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(nextInvoiceDate);

        if (!cal.getTime().after(billingDate)) {

            while (!cal.getTime().after(billingDate)) {
                if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
                    cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                } else {
                    cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                            mainSubscription.getSubscriptionPeriod().getValue());
                }
            }
        } else {
            if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
                cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
            } else {
                cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                        mainSubscription.getSubscriptionPeriod().getValue());
            }
        }

        return cal.getTime();
    }

    /**
     * Checks if the user has to be included in the billing process
     *
     * @param startDate
     * @return true if the user is billable
     */
    public boolean isNotBillable (Date startDate) {

        startDate = new Date(startDate.getTime()); // to escape problem in java.sql.Timestamp compare to java.util.Date
        Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();
        if (nextInvoiceDate == null) {
            return true;
        }

        nextInvoiceDate = new Date(nextInvoiceDate.getTime()); // same as for startDate

        BillingProcessConfigurationDTO config = new ConfigurationBL(user.getEntity().getId()).getDTO();

        PeriodUnit billingPeriodUnit = PeriodUnit.valueOfPeriodUnit(new DateTime(startDate).getDayOfMonth(), config.getPeriodUnit().getId());
        Date endDate = DateConvertUtils.asUtilDate(billingPeriodUnit.addTo(DateConvertUtils.asLocalDate(startDate), 1).minusDays(1));

        logger.debug("user[{}].nid: {}, interval: {} - {}", user.getId(), nextInvoiceDate, startDate, endDate);

        return nextInvoiceDate.before(startDate) || nextInvoiceDate.after(endDate);
    }

    private MainSubscriptionDTO getMainSubscription () {

        MainSubscriptionDTO mainSubscription = user.getCustomer().getMainSubscription();
        if (mainSubscription == null) {
            throw new SessionInternalError("Main Subscription is not set for customer: " + user);
        }

        return mainSubscription;
    }

    public UserWS entity2WS () {
        UserWS retValue = new UserWS();
        retValue.setCreateDatetime(user.getCreateDatetime());
        retValue.setCurrencyId(getCurrencyId());
        retValue.setDeleted(user.getDeleted());
        retValue.setLanguageId(user.getLanguageIdField());
        retValue.setLastLogin(user.getLastLogin());
        retValue.setLastStatusChange(user.getLastStatusChange());
        mainRole = null;
        retValue.setMainRoleId(getMainRole());
        if (user.getPartner() != null) {
            retValue.setPartnerRoleId(user.getPartner().getId());
        }
        retValue.setPassword(user.getPassword());
        retValue.setStatusId(user.getStatus().getId());
        retValue.setUserId(user.getUserId());
        retValue.setUserName(user.getUserName());
        // now the contact
        ContactBL contact = new ContactBL();
        contact.set(retValue.getUserId());
        retValue.setContact(ContactBL.getContactWS(contact.getDTO()));

        return retValue;
    }

    public CachedRowSet findActiveWithOpenInvoices (Integer entityId) throws SQLException {
        prepareStatement(UserSQL.findActiveWithOpenInvoices);
        cachedResults.setInt(1, entityId);

        execute();
        conn.close();
        return cachedResults;
    }

    public UserTransitionResponseWS[] getUserTransitionsById (Integer entityId, Integer last, Date to) {

        try {
            UserTransitionResponseWS[] result = null;
            java.sql.Date toDate = null;
            String query = UserSQL.findUserTransitions;
            if (last > 0) {
                query += UserSQL.findUserTransitionsByIdSuffix;
            }
            if (to != null) {
                query += UserSQL.findUserTransitionsUpperDateSuffix;
                toDate = new java.sql.Date(to.getTime());
            }

            int pos = 2;
            logger.info("Getting transaction list by Id. query --> {}", query);
            prepareStatement(query);
            cachedResults.setInt(1, entityId);

            if (last > 0) {
                cachedResults.setInt(pos, last);
                pos++;
            }
            if (toDate != null) {
                cachedResults.setDate(pos, toDate);
            }

            execute();
            conn.close();

            if (cachedResults == null || !cachedResults.next()) {
                return null;
            }

            // Load the results into a linked list.
            List tempList = new LinkedList();
            UserTransitionResponseWS temp;
            do {
                temp = new UserTransitionResponseWS();
                temp.setId(cachedResults.getInt(1));
                temp.setToStatusId(Integer.parseInt(cachedResults.getString(2)));
                temp.setTransitionDate(new Date(cachedResults.getDate(3).getTime()));
                temp.setUserId(cachedResults.getInt(5));
                temp.setFromStatusId(cachedResults.getInt(4));
                tempList.add(temp);
            } while (cachedResults.next());

            // The list is now ready. Convert into an array and return.
            result = new UserTransitionResponseWS[tempList.size()];
            int count = 0;
            for (Iterator i = tempList.iterator(); i.hasNext();) {
                result[count] = (UserTransitionResponseWS) i.next();
                count++;
            }
            return result;
        } catch (SQLException e) {
            throw new SessionInternalError("Getting transitions", UserBL.class, e);
        }
    }

    public static BigDecimal getBalance(Integer userId) {
        return new InvoiceBL().getTotalAmountOwed(userId)
                .subtract(new PaymentBL().getTotalBalanceByUser(userId))
                .subtract(new CreditNoteBL().getAvailableCreditNotesBalanceByUser(userId))
                .setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
    }

    @Deprecated
    public BigDecimal getTotalOwed (Integer userId) {
        return new InvoiceDAS().findTotalAmountOwed(userId);
    }

    public UserTransitionResponseWS[] getUserTransitionsByDate (Integer entityId, Date from, Date to) {

        try {

            UserTransitionResponseWS[] result;
            java.sql.Date toDate = null;
            String query = UserSQL.findUserTransitions;
            query += UserSQL.findUserTransitionsByDateSuffix;

            if (to != null) {
                query += UserSQL.findUserTransitionsUpperDateSuffix;
                toDate = new java.sql.Date(to.getTime());
            }

            logger.info("Getting transaction list by date. query --> {}", query);

            prepareStatement(query);
            cachedResults.setInt(1, entityId);
            cachedResults.setDate(2, new java.sql.Date(from.getTime()));
            if (toDate != null) {
                cachedResults.setDate(3, toDate);
            }
            execute();
            conn.close();

            if (cachedResults == null || !cachedResults.next()) {
                return null;
            }

            // Load the results into a linked list.
            List tempList = new LinkedList();
            UserTransitionResponseWS temp;
            do {
                temp = new UserTransitionResponseWS();
                temp.setId(cachedResults.getInt(1));
                temp.setToStatusId(Integer.parseInt(cachedResults.getString(2)));
                temp.setTransitionDate(new Date(cachedResults.getDate(3).getTime()));
                temp.setUserId(cachedResults.getInt(5));
                temp.setFromStatusId(cachedResults.getInt(4));
                tempList.add(temp);
            } while (cachedResults.next());

            // The list is now ready. Convert into an array and return.
            result = new UserTransitionResponseWS[tempList.size()];
            int count = 0;
            for (Iterator i = tempList.iterator(); i.hasNext();) {
                result[count] = (UserTransitionResponseWS) i.next();
                count++;
            }
            return result;
        } catch (SQLException e) {
            throw new SessionInternalError("Finding transitions", UserBL.class, e);
        }
    }

    public void updateSubscriptionStatus (Integer id, Integer executorId) {
        if (id == null || user.getSubscriberStatus().getId() == id) {
            // no update ... it's already there
            return;
        }
        if (null != executorId) {
            eLogger.audit(executorId, user.getId(), Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.SUBSCRIPTION_STATUS_CHANGE,
                    user.getSubscriberStatus().getId(), id.toString(), null);
        } else {
            eLogger.auditBySystem(user.getEntity().getId(), user.getId(), Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.SUBSCRIPTION_STATUS_CHANGE,
                    user.getSubscriberStatus().getId(), id.toString(), null);
        }

        try {
            user.setSubscriberStatus(new SubscriberStatusDAS().find(id));
        } catch (Exception e) {
            throw new SessionInternalError("Can't update a user subscription status", UserBL.class, e);
        }

        // make sure this is in synch with the ageing status of the user
        try {
            int preferenceLinkAgeingToSubscription = 0;
            try {
                preferenceLinkAgeingToSubscription = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                        Constants.PREFERENCE_LINK_AGEING_TO_SUBSCRIPTION);
            } catch (EmptyResultDataAccessException e) {
                // i'll use the default
            }

            if (preferenceLinkAgeingToSubscription == 1) {
                AgeingBL ageing = new AgeingBL();
                // todo:
                if (id.equals(UserDTOEx.SUBSCRIBER_ACTIVE)) {
                    // remove the user from the ageing
                    ageing.out(user, null, DateConvertUtils.getNow());
                } else if (id.equals(UserDTOEx.SUBSCRIBER_EXPIRED) || id.equals(UserDTOEx.SUBSCRIBER_DISCONTINUED)) {
                    AgeingDTOEx[] ageingStatuses = ageing.getOrderedSteps(user.getEntity().getId());
                    if (ageingStatuses != null && ageingStatuses.length > 0) {
                        ageing.setUserStatus(null, user.getUserId(), ageingStatuses[0].getStatusId(), DateConvertUtils.getNow());
                    } else {
                        // do nothing
                        logger.warn("User should be suspended by subscription expire, but ageing is not configured for entity {}", user.getEntity().getId());
                    }
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError("Can't update a user status", UserBL.class, e);
        }

        logger.debug("Subscription status updated to {}", id);
    }

    public boolean isPasswordExpired () {
        boolean retValue = false;
        try {
            int expirationDays = 0;
            // Check if password expiration is configured for the user's role
            RoleDTO userRole = new RoleDAS().find(user.getRoles().iterator().next().getId());
            if(userRole.isExpirePassword()) {
                if(userRole.getPasswordExpireDays() > 0) {
                    expirationDays = userRole.getPasswordExpireDays();
                }
            } else {
                // Retrieve expiration days configured by preference
                try {
                    expirationDays = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_PASSWORD_EXPIRATION);
                } catch (EmptyResultDataAccessException e) {
                    // go with default
                }
            }

            // zero means that this is not enforced
            if (expirationDays == 0) {
                return false;
            }

            Date lastChange = user.getChangePasswordDate() != null ? user.getChangePasswordDate() : user.getCreateDatetime();

            long days = (Calendar.getInstance().getTimeInMillis() - lastChange.getTime()) / (1000 * 60 * 60 * 24);
            if (days >= expirationDays) {
                retValue = true;
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    /**
     * Call this method when the user has provided the wrong password
     *
     * @return True if the account is now locked (maximum retries) or false if it is not locked.
     */
    public boolean failedLoginAttempt () {
        boolean retValue = false;
        int allowedRetries = 3;
        try {
            allowedRetries = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                    Constants.PREFERENCE_FAILED_LOGINS_LOCKOUT);
        } catch (EmptyResultDataAccessException e) {
            // go with default
        }

        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.FAILED_USER_LOGIN, null,
                String.format("User %s failed to log in due to incorrect username/password", user.getUserName()), null);

        int total = user.getFailedAttempts();

        // If the user's account locked time is expired, it means the account is unlocked, hence the account locked time
        // and failed attempts have to be reset to default values
        if(null != user.getAccountLockedTime() && !isAccountLocked()) {
            user.setAccountLockedTime(null);
            total = 0;
        }

        total++;
        user.setFailedAttempts(total);

        //log failed attempts of user
        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.FAILED_LOGIN_ATTEMPTS, total,
                null, null);

        if (total >= allowedRetries) {
            retValue = true;

            // Lock out the user
            setAccountLocked(true);

            eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                    Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ACCOUNT_LOCKED, null,
                    String.format("User %s account is locked, failed attempts %s ", user.getUserName(), total), null);

            logger.debug("Locked account for user {}", user.getUserId());
        }

        return retValue;
    }

    /**
     * Checks if the user has been set the 'lockout_password'. Checking
     * is always done by using the current hashing method configured for
     * that user. If the user is configured with lockout_password then
     * the account is considered as lockout out.
     *
     * @return true - if 'lockout_password' is set, false otherwise
     */
    public boolean isLockoutPasswordSet() {
        String lockoutPassword = Util.getSysProp(Constants.PROPERTY_LOCKOUT_PASSWORD);
        Integer userEncodeMethodId = getEntity().getEncryptionScheme();
        return JBCrypto.passwordsMatch(userEncodeMethodId, user.getPassword(), lockoutPassword);
    }

    public void successLoginAttempt () {
        das.refresh(user);
        user.setLastLogin(Calendar.getInstance().getTime());
        user.setFailedAttempts(0);
        user.setAccountLockedTime(null);
        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.SUCESSFULL_USER_LOGIN,null,
                String.format("User %s logged in successfully", user.getUserName()), null);
    }

    public boolean canInvoice () {
        // can't be deleted and has to be a customer
        if (user.getDeleted() == 1
                || user.getCustomer() == null
                || !getMainRole().equals(Constants.TYPE_CUSTOMER)) {
            return false;
        }

        // child accounts only get invoiced if the explicit flag is on
        if (user.getCustomer().getParent() != null &&
                (user.getCustomer().getInvoiceChild() == null ||
                user.getCustomer().getInvoiceChild() == 0)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the user has been invoiced for anything at the time given as a parameter.
     *
     * @return
     */
    public boolean isCurrentlySubscribed (Date forDate) {

        List<Integer> results = new InvoiceDAS().findIdsByUserAndPeriodDate(user.getUserId(), forDate);
        boolean retValue = !results.isEmpty();

        logger.debug(" user {} is subscribed result {}", user.getUserId(), retValue);

        return retValue;
    }

    public CachedRowSet getByStatus (Integer entityId, Integer statusId, boolean in) {
        try {
            if (in) {
                prepareStatement(UserSQL.findInStatus);
            } else {
                prepareStatement(UserSQL.findNotInStatus);
            }
            cachedResults.setInt(1, statusId);
            cachedResults.setInt(2, entityId);
            execute();
            conn.close();
            return cachedResults;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by status", UserBL.class, e);
        }
    }

    public CachedRowSet getByCCNumber (Integer entityId, String number) {
        try {

            prepareStatement(UserSQL.findByCreditCard);
            cachedResults.setString(1, number);
            cachedResults.setInt(2, entityId);
            execute();
            conn.close();

            return cachedResults;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by cc", UserBL.class, e);
        }
    }

    public Integer getByEmail (String email) {
        try {
            Integer retValue = null;
            prepareStatement(UserSQL.findByEmail);
            // this is being use for paypal subscriptions. It only has an email
            // so there is not way to limit by entity_id
            cachedResults.setString(1, email);
            execute();
            if (cachedResults.next()) {
                retValue = cachedResults.getInt(1);
            }
            cachedResults.close();
            conn.close();
            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by cc", UserBL.class, e);
        }
    }

    /**
     * Only needed due to the locking of entity beans. Remove when using JPA
     *
     * @param userId
     * @return
     * @throws SQLException
     * @throws NamingException
     */
    public Integer getEntityId (Integer userId) {
        if (userId == null) {
            userId = user.getUserId();
        }
        UserDTO user = das.find(userId);
        return user.getCompany().getId();

    }

    /**
     * Adds/removes blacklist entries directly related to this user.
     */
    public void setUserBlacklisted (Integer executorId, Boolean isBlacklisted) {
        BlacklistDAS blacklistDAS = new BlacklistDAS();
        List<BlacklistDTO> blacklist = blacklistDAS.findByUserType(user.getId(), BlacklistDTO.TYPE_USER_ID);
        if (isBlacklisted) {
            if (blacklist.isEmpty()) {
                // add a new blacklist entry
                logger.debug("Adding blacklist record for user id: {}", user.getId());

                BlacklistDTO entry = new BlacklistDTO();
                entry.setCompany(user.getCompany());
                entry.setCreateDate(TimezoneHelper.serverCurrentDate());
                entry.setType(BlacklistDTO.TYPE_USER_ID);
                entry.setSource(BlacklistDTO.SOURCE_CUSTOMER_SERVICE);
                entry.setUser(user);
                if(user.getPaymentInstruments().size()>=1) {
                    entry.setCreditCard(user.getPaymentInstruments().get(0));
                }
                entry = blacklistDAS.save(entry);
                blacklistDAS.flush();

                eLogger.audit(executorId, user.getId(), Constants.TABLE_BLACKLIST, entry.getId(),
                        EventLogger.MODULE_BLACKLIST, EventLogger.BLACKLIST_USER_ID_ADDED, null, null, null);
            }
        } else {
            if (!blacklist.isEmpty()) {
                // remove any blacklist entries found
                logger.debug("Removing blacklist records for user id: {}", user.getId());

                for (BlacklistDTO entry : blacklist) {
                    blacklistDAS.delete(entry);

                    eLogger.audit(executorId, user.getId(), Constants.TABLE_BLACKLIST, entry.getId(),
                            EventLogger.MODULE_BLACKLIST, EventLogger.BLACKLIST_USER_ID_REMOVED, null, null, null);
                }
            }
        }
    }


    public ValidatePurchaseWS validatePurchase(List<ItemDTO> items,
            List<BigDecimal> amounts,
            List<List<PricingField>> pricingFields) {

        if (user.getCustomer() == null) {
            return null;
        }

        logger.debug("validating purchase items: {} amounts {} customer {}", Arrays.toString(items.toArray()), amounts,
                user.getCustomer());

        ValidatePurchaseWS result = new ValidatePurchaseWS();

        // call plug-ins
        try {
            PluggableTaskManager<IValidatePurchaseTask> taskManager = new PluggableTaskManager<>(
                    user.getCompany().getId(), Constants.PLUGGABLE_TASK_VALIDATE_PURCHASE);
            IValidatePurchaseTask myTask = taskManager.getNextClass();

            while (myTask != null) {
                myTask.validate(user.getCustomer(), items, amounts, result, pricingFields);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            // log stacktrace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            logger.error("Validate Purchase error: {}\n{}", e.getMessage(), sw.toString());

            result.setSuccess(false);
            result.setAuthorized(false);
            result.setQuantity(BigDecimal.ZERO);
            result.setMessage(new String[] { "Error: " + e.getMessage() });
        }

        return result;
    }

    public Integer getLanguage () {
        return user.getLanguageIdField();
    }

    /**
     * Checks if the string passed is less than 1000 characters
     *
     * @param notes
     * @return boolean
     */
    public static boolean ifValidNotes (String notes) {
        return notes == null || notes.length() <= 1000;
    }

    public boolean isEmailUsedByOthers (String email) {

        try {
            Integer retValue = null;
            prepareStatement(UserSQL.findOthersByEmail);

            cachedResults.setString(1, email.toLowerCase());
            cachedResults.setInt(2, user.getId());
            cachedResults.setInt(3, user.getEntity().getId());
            execute();
            if (cachedResults.next()) {
                retValue = cachedResults.getInt(1);
            }
            cachedResults.close();
            conn.close();

            boolean used = retValue != null;
            if (used)
            {
                return used;// no further checks needed
            }

            // check if the email is defined in meta field
            return getEmailMetaFieldValueIds(user.getEntity().getId(), email.toLowerCase()).size() > 0;
        } catch (Exception e) {

            throw new SessionInternalError("Error getting user by email, id, entity_id", UserBL.class, e);
        }
    }

    public List<UserDTO> findUsersByEmail (String email, Integer entity) {
        UserDAS userDAS = new UserDAS();

        List<UserDTO> users = new ArrayList<>();
        users.addAll(userDAS.findByEmail(email, entity));

        List<UserDTO> metaFieldUsers = findByMetaFieldEmailValue(email, entity);
        if (null != metaFieldUsers) {
            users.addAll(metaFieldUsers);
        }

        return users;
    }

    public List<UserDTO> findByMetaFieldEmailValue (String email, Integer entityId) {
        List<Integer> valueIds = getEmailMetaFieldValueIds(entityId, email.toLowerCase());

        if (null != valueIds && valueIds.size() > 0) {
            UserDAS userDAS = new UserDAS();

            List<UserDTO> users = new ArrayList<>(0);
            List<UserDTO> mfUsers = userDAS.findByMetaFieldValueIds(entityId, valueIds);
            List<UserDTO> aitMFUsers = userDAS.findByAitMetaFieldValueIds(entityId, valueIds);

            if (mfUsers != null) {
                users.addAll(mfUsers);
            }

            if (aitMFUsers != null) {
                users.addAll(aitMFUsers);
            }

            return users;
        }

        return new ArrayList<>();
    }

    public List<Integer> getEmailMetaFieldValueIds (Integer entityId, String email) {
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

        List<Integer> emailMetaFieldIds = metaFieldDAS.getByFieldType(entityId,
                new MetaFieldType[] { MetaFieldType.EMAIL });

        if (null != emailMetaFieldIds && emailMetaFieldIds.size() > 0) {
            return metaFieldDAS.findByValueAndField(DataType.STRING, email.toLowerCase(), Boolean.FALSE,
                    emailMetaFieldIds);
        }

        return new ArrayList<>();
    }

    public AccountTypeDTO getAccountType () {
        if (user.getCustomer() != null) {
            return user.getCustomer().getAccountType();
        }

        return null;
    }

    public static UserWS getWS (UserDTOEx dto) {

        UserWS userWS = new UserWS();
        userWS.setId(dto.getId());
        userWS.setCurrencyId(dto.getCurrencyId());
        userWS.setPassword(dto.getPassword());
        userWS.setDeleted(dto.getDeleted());
        userWS.setCreateDatetime(dto.getCreateDatetime());
        userWS.setLastStatusChange(dto.getLastStatusChange());
        userWS.setLastLogin(dto.getLastLogin());
        userWS.setUserName(dto.getUserName());
        userWS.setFailedAttempts(dto.getFailedAttempts());
        userWS.setLanguageId(dto.getLanguageId());
        userWS.setRole(dto.getMainRoleStr());
        userWS.setMainRoleId(dto.getMainRoleId());
        userWS.setLanguage(dto.getLanguageStr());
        userWS.setStatus(dto.getStatusStr());
        userWS.setStatusId(dto.getStatusId());
        userWS.setSubscriberStatusId(dto.getSubscriptionStatusId());
        userWS.setAccessEntities(getAccessEntities(dto));

        userWS.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(dto.getEntityId(), dto));
        //may be overwritten later on in case of a customer user
        userWS.setEntityId(null != dto.getCompany() ? dto.getCompany().getId() : null);

        if(null != dto.getAccountDisabledDate()) {
            userWS.setAccountExpired(true);
            userWS.setAccountDisabledDate(dto.getAccountDisabledDate());
        }

        if (dto.getCustomer() != null) {
            userWS.setCustomerId(dto.getCustomer().getId());
            userWS.setPartnerIds((dto.getCustomer().getPartners() == null) ? new Integer[0] : PartnerBL.getPartnerIds(dto.getCustomer().getPartners()));
            userWS.setParentId((dto.getCustomer().getParent() == null) ? null : dto
                    .getCustomer()
                    .getParent()
                    .getBaseUser()
                    .getId());
            userWS.setMainSubscription(convertMainSubscriptionToWS(dto.getCustomer().getMainSubscription()));
            userWS.setIsParent(dto.getCustomer().getIsParent() != null && dto.getCustomer().getIsParent().equals(1));
            userWS.setInvoiceChild(dto.getCustomer().getInvoiceChild() != null
                    && dto.getCustomer().getInvoiceChild().equals(1));
            userWS.setUseParentPricing(dto.getCustomer().useParentPricing());
            userWS.setExcludeAgeing(dto.getCustomer().getExcludeAging() == 1);
            userWS.setNextInvoiceDate(dto.getCustomer().getNextInvoiceDate());

            Integer[] childIds = new Integer[dto.getCustomer().getChildren().size()];
            int index = 0;
            for (CustomerDTO customer : dto.getCustomer().getChildren()) {
                childIds[index] = customer.getBaseUser().getId();
                index++;
            }
            userWS.setChildIds(childIds);

            userWS.setDynamicBalance(dto.getCustomer().getDynamicBalance());
            userWS.setCreditLimit(dto.getCustomer().getCreditLimit());
            userWS.setAutoRecharge(dto.getCustomer().getAutoRecharge());
            userWS.setRechargeThreshold(dto.getCustomer().getRechargeThreshold());
            userWS.setLowBalanceThreshold(dto.getCustomer().getLowBalanceThreshold());
            userWS.setMonthlyLimit(dto.getCustomer().getMonthlyLimit());

            List <CustomerNoteDTO> customerNoteDTOs=new CustomerNoteDAS().findByCustomer(userWS.getCustomerId(),dto.getEntityId());
            List <CustomerNoteWS> customerNoteWSList = new ArrayList<>();
            for(CustomerNoteDTO customerNoteDTO: customerNoteDTOs) {
                customerNoteWSList.add(convertCustomerNoteToWS(customerNoteDTO));
            }
            userWS.setCustomerNotes(customerNoteWSList.toArray(new CustomerNoteWS[customerNoteWSList.size()]));

            userWS.setAutomaticPaymentType(dto.getCustomer().getAutoPaymentType());

            userWS.setInvoiceDeliveryMethodId(dto.getCustomer().getInvoiceDeliveryMethod() == null ? null : dto
                    .getCustomer()
                    .getInvoiceDeliveryMethod()
                    .getId());
            userWS.setDueDateUnitId(dto.getCustomer().getDueDateUnitId());
            userWS.setDueDateValue(dto.getCustomer().getDueDateValue());

            if (!dto.getCustomer().getUserCodeLinks().isEmpty()) {
                Set<UserCodeCustomerLinkDTO> userCodeLinks = dto.getCustomer().getUserCodeLinks();

                userWS.setUserCodeLink(userCodeLinks.iterator().next().getUserCode().getIdentifier());
            }

            CustomerCommissionDefinitionWS[] commissionDefinitionWSes = new CustomerCommissionDefinitionWS[dto.getCommissionDefinitions().size()];
            index = 0;
            for(CustomerCommissionDefinitionDTO commissionDefinitionDTO : dto.getCommissionDefinitions()) {
                CustomerCommissionDefinitionWS commissionDefinitionWS = new CustomerCommissionDefinitionWS();
                commissionDefinitionWS.setRate(commissionDefinitionDTO.getRate().toString());
                commissionDefinitionWS.setPartnerId(commissionDefinitionDTO.getId().getPartner().getId());
                commissionDefinitionWS.setCustomerId(commissionDefinitionDTO.getId().getUser().getId());
                commissionDefinitionWSes[index++] = commissionDefinitionWS;
            }
            userWS.setCommissionDefinitions(commissionDefinitionWSes);

            Integer entityId;
            if (null == dto.getCompany()) {
                entityId = new UserBL().getEntityId(dto.getCustomer().getId());
            } else {
                entityId = dto.getCompany().getId();
            }
            userWS.setEntityId(entityId);
            userWS.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(dto.getCustomer()));

            userWS.setAccountTypeId(dto.getCustomer().getAccountType() != null ? dto.getCustomer().getAccountType().getId() : null);
            userWS.setInvoiceDesign(dto.getCustomer().getInvoiceDesign());
            userWS.setInvoiceTemplateId(dto.getCustomer().getInvoiceTemplate() != null ? dto.getCustomer().getInvoiceTemplate().getId() : null);

            // convert to Map<ait,Map<date, values>> map and set it in UserWS
            Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = new HashMap<>();
            for (CustomerAccountInfoTypeMetaField accountInfoTypeField : dto
                    .getCustomer()
                    .getCustomerAccountInfoTypeMetaFields()) {
                Integer groupId = accountInfoTypeField.getAccountInfoType().getId();
                if (accountInfoTypeFieldsMap.containsKey(accountInfoTypeField.getAccountInfoType().getId())) {
                    Map<Date, ArrayList<MetaFieldValueWS>> metaFieldMap = accountInfoTypeFieldsMap
                            .get(accountInfoTypeField.getAccountInfoType().getId());
                    ArrayList<MetaFieldValueWS> valueList;

                    if (metaFieldMap.containsKey(accountInfoTypeField.getEffectiveDate())) {
                        valueList = metaFieldMap.get(accountInfoTypeField.getEffectiveDate());
                        valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    } else {
                        valueList = new ArrayList<>();
                        valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    }

                    metaFieldMap.put(accountInfoTypeField.getEffectiveDate(), valueList);
                    accountInfoTypeFieldsMap.put(accountInfoTypeField.getAccountInfoType().getId(),
                            (HashMap<Date, ArrayList<MetaFieldValueWS>>) metaFieldMap);
                } else {
                    HashMap<Date, ArrayList<MetaFieldValueWS>> metaFieldMap = new HashMap<>();
                    List<MetaFieldValueWS> valueList = new ArrayList<>();

                    valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    metaFieldMap.put(accountInfoTypeField.getEffectiveDate(), (ArrayList<MetaFieldValueWS>) valueList);

                    accountInfoTypeFieldsMap.put(accountInfoTypeField.getAccountInfoType().getId(), metaFieldMap);
                }
            }
            userWS.setAccountInfoTypeFieldsMap(accountInfoTypeFieldsMap);

            // set timelines dates map and effective dates map
            Map<Integer, ArrayList<Date>> timelineDatesMap = new HashMap<>();
            Map<Integer, Date> effectiveDatesMap = new HashMap<>();
            for (Map.Entry<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> entry : accountInfoTypeFieldsMap
                    .entrySet()) {
                List<Date> dates = new ArrayList<>(0);
                for (Map.Entry<Date, ArrayList<MetaFieldValueWS>> en : entry.getValue().entrySet()) {
                    dates.add(en.getKey());
                }
                Collections.sort(dates);
                timelineDatesMap.put(entry.getKey(), (ArrayList<Date>) dates);
                effectiveDatesMap.put(entry.getKey(), findEffectiveDate(dates, entityId));
            }
            userWS.setTimelineDatesMap(timelineDatesMap);
            userWS.setEffectiveDateMap(effectiveDatesMap);

            userWS.setIdentificationType(dto.getCustomer().getIdentificationType());
            userWS.setIdentificationText(dto.getCustomer().getIdentificationText());
            userWS.setIdentificationImage(dto.getCustomer().getIdentificationImage());
            userWS.setReissueCount(dto.getCustomer().getReissueCount());
            userWS.setReissueDate(dto.getCustomer().getReissueDate());

            // merge ait latest meta fields with customer meta fields
            List<MetaFieldValueWS> aitMetaFields = new ArrayList<>();
            for (Map.Entry<Integer, Date> entry : effectiveDatesMap.entrySet()) {
                aitMetaFields.addAll(accountInfoTypeFieldsMap.get(entry.getKey()).get(entry.getValue()));
            }
            logger.debug("Total ait meta fields found: {}", aitMetaFields.size());
            MetaFieldValueWS[] aitMetaFieldsArray = aitMetaFields.toArray(new MetaFieldValueWS[aitMetaFields.size()]);
            MetaFieldValueWS[] combined = new MetaFieldValueWS[userWS.getMetaFields().length
                                                               + aitMetaFieldsArray.length];
            System.arraycopy(userWS.getMetaFields(), 0, combined, 0, userWS.getMetaFields().length);
            System.arraycopy(aitMetaFieldsArray, 0, combined, userWS.getMetaFields().length, aitMetaFieldsArray.length);
            userWS.setMetaFields(combined);
        }

        // set payment information

        for (PaymentInformationDTO paymentInformation : dto.getPaymentInstruments()) {
            userWS.getPaymentInstruments().add(PaymentInformationBL.getWS(paymentInformation));
            String msg = "Retrieving user payment information with ID: " + paymentInformation.getId();
            logger.info(getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_GET));
        }

        if (dto.getPartner() != null) {
            userWS.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(dto.getCompany().getId(), dto.getPartner()));
        }

        userWS.setBlacklistMatches(dto.getBlacklistMatches() != null ? dto.getBlacklistMatches().toArray(
                new String[dto.getBlacklistMatches().size()]) : null);
        userWS.setUserIdBlacklisted(dto.getUserIdBlacklisted());

        if (null != dto.getCompany()) {

            userWS.setCompanyName(dto.getCompany().getDescription());
        }

        userWS.setOwingBalance(dto.getBalance());
        String msg = "Retrieving information for user with ID: " + userWS.getId();
        logger.info(getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_GET));
        return userWS;
    }

    private static List<Integer> getAccessEntities(UserDTOEx dto) {
        List<Integer> entityIds = new ArrayList<>();
        CompanyDTO company = dto.getEntity();
        while(company != null) {
            entityIds.add(company.getId());
            company = company.getParent();
        }
        return entityIds;
    }

    /**
     * Get first credit card from user's instrument list, returns null if no credit card is found
     *
     * @return PaymentInrormationDTO
     */
    public PaymentInformationDTO getCreditCard () {
        if (this.user.getPaymentInstruments() != null && !this.user.getPaymentInstruments().isEmpty()) {
            return new PaymentInformationBL().findCreditCard(this.user.getPaymentInstruments());
        }
        return null;
    }

    /**
     * Get all credit cards from user's instrument list, returns null if no credit card is found
     *
     * @return List<PaymentInrormationDTO>
     */
    public List<PaymentInformationDTO> getAllCreditCards () {
        if (user.getPaymentInstruments() != null && !user.getPaymentInstruments().isEmpty()) {
            return new PaymentInformationBL().findAllCreditCards(user.getPaymentInstruments());
        }
        return null;
    }

    /**
     * Determine if the parentId is not already in currentUserId's hierarchy
     *
     * @param currentUserId
     * @param parentId
     * @return
     */
    public boolean okToAddAsParent (int currentUserId, int parentId) {

        List<Integer> childList = das.findChildList(currentUserId);

        if (childList.isEmpty()) {
            return true;
        }

        for (Integer childId : childList) {
            if (childId.equals(parentId)) {
                return false;
            } else {
                return this.okToAddAsParent(childId, parentId);
            }
        }

        return true;
    }

    /**
     * Sets the Next Invoice Date to the Customer
     *
     * @param userDto
     */
    public void setCustomerNextInvoiceDate (UserDTO userDto, Integer executorId) {
        Date oldNextInvoiceDate = userDto.getCustomer().getNextInvoiceDate();
        MainSubscriptionDTO mainSubscriptionDTO = userDto.getCustomer().getMainSubscription();
        Date nextInvoiceDate = checkNIDAgaintMainSubscription(
                mainSubscriptionDTO.getSubscriptionPeriod().getPeriodUnit().getId(),
                mainSubscriptionDTO.getNextInvoiceDayOfPeriod(),
                getCustomerNextInvoiceDate(userDto)
                );
        logger.debug("Final next invoice date for user {} is: {} ", user.getId(), nextInvoiceDate);
        userDto.getCustomer().setNextInvoiceDate(nextInvoiceDate);
        if (null != oldNextInvoiceDate && null != nextInvoiceDate && !oldNextInvoiceDate.equals(nextInvoiceDate)) {
            addNIDChangeAuditLog(executorId, oldNextInvoiceDate);
        }
    }

    /**
     * Calculate the date that this customer can be expected to receive their next invoice. This method will return null
     * if the customer does not exist
     *
     * @param userDto
     */
    public Date getCustomerNextInvoiceDate (UserDTO userDto) {

        BillingProcessConfigurationDTO billingProcessConfiguration = new BillingProcessConfigurationDAS().findByEntity(userDto.getEntity());
        MainSubscriptionDTO mainSubscription = userDto.getCustomer().getMainSubscription();
        Integer customerDayOfInvoice = mainSubscription.getNextInvoiceDayOfPeriod();

        LocalDate nextRunDate = DateConvertUtils.asLocalDate(billingProcessConfiguration.getNextRunDate());
        Date currentNextInvoiceDate = userDto.getCustomer().getNextInvoiceDate();
        Date initialDate = currentNextInvoiceDate == null ? userDto.getCreateDatetime() : currentNextInvoiceDate;
        LocalDate initialNextInvoiceDate = DateConvertUtils.asLocalDate(initialDate);
        LocalDate nextInvoiceDate = DateConvertUtils.asLocalDate(initialDate);

        // when it is not a new customer
        if (currentNextInvoiceDate != null) {
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(customerDayOfInvoice, billingProcessConfiguration.getPeriodUnit().getId());
            nextRunDate = periodUnit.addTo(nextRunDate, 1);
        }
        OrderPeriodDTO periodDTO = mainSubscription.getSubscriptionPeriod();
        int periodUnitId = periodDTO.getPeriodUnit().getId();
        int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
        PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);

        int lastDayOfMonth = nextInvoiceDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        if (lastDayOfMonth <= customerDayOfInvoice && PeriodUnit.MONTHLY.equals(periodUnit)) {
            nextInvoiceDate = periodUnit.getForDay(nextInvoiceDate, lastDayOfMonth);
        } else {
            nextInvoiceDate = periodUnit.getForDay(nextInvoiceDate, customerDayOfInvoice);
        }

        while (nextRunDate.isAfter(nextInvoiceDate) || !(nextInvoiceDate.isAfter(initialNextInvoiceDate))) {
            nextInvoiceDate = periodUnit.addTo(nextInvoiceDate, periodDTO.getValue());
        }
        return DateConvertUtils.asUtilDate(nextInvoiceDate);
    }

    /**
     * Adds semi monthly period to given date, considering the day of invoice generation received from UI.
     *
     * @param cal
     * @param customerDayOfInvoice
     * @return
     */
    public Date addSemiMonthlyPeriod (GregorianCalendar cal, Integer customerDayOfInvoice) {
        Integer nextInvoiceDay = cal.get(Calendar.DAY_OF_MONTH);
        Integer sourceDay = cal.get(Calendar.DAY_OF_MONTH);

        if (sourceDay < customerDayOfInvoice) {
            nextInvoiceDay = customerDayOfInvoice;
        } else if (customerDayOfInvoice <= 14) {
            nextInvoiceDay = Math.min(customerDayOfInvoice + 15, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            if (sourceDay >= nextInvoiceDay) {
                // Lets say today is 30th and nextInvoiceDay is 29th after adding 15 days.
                // then next invoice date should be 14th of the next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        } else if (customerDayOfInvoice == 15 && sourceDay >= customerDayOfInvoice) {
            DateTime sourceDatetime = new DateTime(cal.getTime());
            sourceDatetime = sourceDatetime.withDayOfMonth(sourceDatetime.dayOfMonth().getMaximumValue());
            nextInvoiceDay = sourceDatetime.getDayOfMonth();

            if (sourceDay == nextInvoiceDay) {
                // Lets say today is 31st and nextInvoiceDay is 30 after adding 15 days
                // then next invoice date should be 15th of next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            } else if (sourceDay > customerDayOfInvoice) {
                // source day is 30th but not month end
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        }
        cal.set(Calendar.DAY_OF_MONTH, nextInvoiceDay);
        return cal.getTime();
    }

    /**
     * Update Sub-account next invoice date and billing cycle as same as Parent account.
     *
     * @param user
     */
    private void updateBillingCycleOfChildAsPerParent (UserDTO user) {

        if (user.getCustomer().getIsParent() != null) {

            UserBL parent = null;
            Iterator subAccountsIt = null;
            if (user.getCustomer().getIsParent() != null && user.getCustomer().getIsParent() == 1) {
                parent = new UserBL(user.getId());
                subAccountsIt = parent.getEntity().getCustomer().getChildren().iterator();
            }

            // see if there is any subaccounts to include in this invoice
            while (subAccountsIt != null) { // until there are no more subaccounts (subAccountsIt != null) {
                CustomerDTO customer = null;
                while (subAccountsIt.hasNext()) {
                    customer = (CustomerDTO) subAccountsIt.next();
                    if (customer.getInvoiceChild() == null || customer.getInvoiceChild() == 0) {
                        break;
                    } else {
                        logger.debug("Subaccount not included in parent's invoice {}", customer.getId());
                        customer = null;
                    }
                }
                if (customer != null) {
                    customer.setMainSubscription(parent.getMainSubscription());
                    customer.setNextInvoiceDate(parent.getDto().getCustomer().getNextInvoiceDate());

                    if (customer.getIsParent() != null && customer.getIsParent() == 1) {
                        parent = new UserBL(customer.getBaseUser().getUserId());
                        if (parent.getEntity() != null && parent.getEntity().getCustomer() != null && checkIfUserhasAnychildren(parent)) {
                            subAccountsIt = parent.getEntity().getCustomer().getChildren().iterator();
                        }
                    }
                } else {
                    subAccountsIt = null;
                    logger.debug("No more subaccounts to process");
                }
            }
        }
    }

    /**
     * Gets ait meta field values for currently effective date and adds to give list
     *
     * @param metaFields
     *            : Meta Fields list
     * @param aitTimelineMetaFieldsMap
     *            : MetaFields map
     */
    public void getCustomerEffectiveAitMetaFieldValues (List<MetaFieldValue> metaFields, Map<Integer, Map<Date, List<MetaFieldValue>>> aitTimelineMetaFieldsMap, Integer entityId) {
        if (!MapUtils.isEmpty(aitTimelineMetaFieldsMap)) {
            for (Map.Entry<Integer, Map<Date, List<MetaFieldValue>>> entry : aitTimelineMetaFieldsMap.entrySet()) {
                List<Date> timelineDates = new ArrayList<>();
                Map<Date, List<MetaFieldValue>> timeline = entry.getValue();

                for (Map.Entry<Date, List<MetaFieldValue>> inner : timeline.entrySet()) {
                    timelineDates.add(inner.getKey());
                }

                Collections.sort(timelineDates);
                metaFields.addAll(timeline.get(findEffectiveDate(timelineDates, entityId)));
            }
        }
    }

    private MetaFieldValue generateValue (MetaFieldValue value) {
        MetaFieldValue generated = value.getField().createValue();
        generated.setField(value.getField());
        generated.setValue(value.getValue());
        return generated;
    }

    private static Date findEffectiveDate (List<Date> dates, Integer entityId) {
        Date date = TimezoneHelper.companyCurrentDate(entityId);
        Date forDate = null;
        for (Date start : dates) {
            if (start != null && start.after(date)) {
                break;
            }

            forDate = start;
        }
        return forDate;
    }

    public List<UserCodeDTO> getUserCodesForUser (int userId) {
        return new UserCodeDAS().findForUser(userId);
    }

    public static UserCodeWS convertUserCodeToWS (UserCodeDTO userCode) {
        UserCodeWS ws = new UserCodeWS();
        ws.setId(userCode.getId());
        ws.setIdentifier(userCode.getIdentifier());
        ws.setTypeDescription(userCode.getTypeDescription());
        ws.setType(userCode.getType());
        ws.setExternalReference(userCode.getExternalReference());
        ws.setUserId(userCode.getUser().getId());
        ws.setValidFrom(userCode.getValidFrom());
        ws.setValidTo(userCode.getValidTo());

        return ws;
    }

    public static UserCodeWS[] convertUserCodeToWS (List<UserCodeDTO> userCodes) {
        UserCodeWS[] result = new UserCodeWS[userCodes.size()];
        for (int i = 0; i < userCodes.size(); i++) {
            result[i] = convertUserCodeToWS(userCodes.get(0));
        }
        return result;
    }

    public int createUserCode (UserCodeWS userCode) {
        UserCodeDAS userCodeDAS = new UserCodeDAS();
        UserCodeDTO dto = converUserCodeToDTO(userCode);

        if (dto.getId() != 0) {
            throw new SessionInternalError("New User Code has an id" + dto,
                    new String[] { "UserCodeWS,identifier,userCode.validation.new.id.notnull" });
        }
        // This call will ensure that the usercode is unique in the company
        UserCodeDTO persistentDto = new UserCodeDAS().findForIdentifier(dto.getIdentifier(), dto
                .getUser()
                .getEntity()
                .getId());

        // check that the identifier is unique and belongs to this object
        if (persistentDto != null) {
            throw new SessionInternalError("User Code is in use: " + dto,
                    new String[] { "UserCodeWS,identifier,userCode.validation.duplicate.identifier" });
        }

        verifyUserCodeIdentifierFormat(dto.getUser(), dto.getIdentifier());
        verifyUserCode(dto);

        dto = userCodeDAS.save(dto);

        return dto.getId();
    }

    public void updateUserCode (UserCodeWS userCode) {
        UserCodeDAS userCodeDAS = new UserCodeDAS();
        UserCodeDTO dto = converUserCodeToDTO(userCode);
        // check that nothing has changed if the usercode is used

        UserCodeDTO persistentDto = new UserCodeDAS().find(dto.getId());

        if( (!dto.getIdentifier().equals(persistentDto.getIdentifier())) ||
                (dto.getType() != null && !dto.getType().equals(persistentDto.getType())) ||
                (dto.getExternalReference() != null && !dto.getExternalReference().equals(persistentDto.getExternalReference())) ||
                (dto.getUser().getId() != persistentDto.getUser().getId()) ||
                (!dto.getValidFrom().equals(persistentDto.getValidFrom())) || (dto.getTypeDescription() != null && !dto.getTypeDescription().equals(persistentDto.getTypeDescription())) ) {


            if (!persistentDto.getUserCodeLinks().isEmpty()) {
                throw new SessionInternalError("Attempting to update a UserCodeDTO which is already linked: "
                        + persistentDto, new String[] { "userCode.validation.update.linked" });
            }

            if ((!dto.getIdentifier().equals(persistentDto.getIdentifier()))) {
                if (userCodeDAS.findForIdentifier(dto.getIdentifier(), dto.getUser().getEntity().getId()) != null) {
                    throw new SessionInternalError("User Code is in use: " + dto,
                            new String[] { "UserCodeWS,identifier,userCode.validation.duplicate.identifier" });
                }
            }

            persistentDto.setIdentifier(dto.getIdentifier());
            persistentDto.setType(dto.getType());
            persistentDto.setExternalReference(dto.getExternalReference());
            persistentDto.setValidFrom(dto.getValidFrom());
            persistentDto.setTypeDescription(dto.getTypeDescription());

            verifyUserCodeIdentifierFormat(persistentDto.getUser(), persistentDto.getIdentifier());
        }
        persistentDto.setValidTo(dto.getValidTo());

        verifyUserCode(persistentDto);

        dto = persistentDto;
        userCodeDAS.save(dto);
    }

    private void verifyUserCode (UserCodeDTO dto) {
        if (dto.getValidTo() != null && dto.getValidFrom() != null && dto.getValidTo().before(dto.getValidFrom())) {
            throw new SessionInternalError("User Code 'valid to' is before 'valid from'",
                    new String[] { "UserCodeWS,validTo,validation.validTo.before.validFrom" });
        }
    }

    public UserCodeDTO findUserCodeForIdentifier (String userCode, Integer companyId) {
        return new UserCodeDAS().findForIdentifier(userCode, companyId);
    }

    private void verifyUserCodeIdentifierFormat (UserDTO user, String identifier) {
        String regex = user.getUserName() + "\\d{5}";
        Pattern pattern = Pattern.compile(regex);
        if (identifier == null || !pattern.matcher(identifier).matches()) {
            throw new SessionInternalError("User Code identifier does not match pattern",
                    new String[] { "UserCodeWS,identifier,validation.identifier.pattern.fail" });
        }
    }

    public UserCodeDTO converUserCodeToDTO (UserCodeWS ws) {
        UserCodeDTO dto = new UserCodeDTO(ws.getId());
        dto.setIdentifier(ws.getIdentifier());
        dto.setTypeDescription(ws.getTypeDescription());
        dto.setType(ws.getType());
        dto.setExternalReference(ws.getExternalReference());
        dto.setUser(das.find(ws.getUserId()));
        dto.setValidFrom(ws.getValidFrom());
        dto.setValidTo(ws.getValidTo());
        return dto;
    }

    public List<Integer> getAssociatedObjectsByUserCodeAndType (String userCode, UserCodeObjectType objectType) {
        return new UserCodeLinkDAS().getAssociatedObjectsByUserCodeAndType(userCode, objectType);
    }

    public List<Integer> getAssociatedObjectsByUserAndType (int userId, UserCodeObjectType objectType) {
        return new UserCodeLinkDAS().getAssociatedObjectsByUserAndType(userId, objectType);
    }

    /**
     * Update the UserCodeAssociate to have the links defined in targetLinks
     *
     * @param associate
     * @param targetLinks
     */
    public static <T extends UserCodeLinkDTO> void updateAssociateUserCodesToLookLikeTarget (
            UserCodeAssociate<T> associate, Collection<T> targetLinks, String errorBeanAndProperty) {
        ConcurrentHashMap<String, T> userCodeLinkMap = new ConcurrentHashMap<>();

        Set<T> currentLinks = associate.getUserCodeLinks();
        if (currentLinks != null) {
            for (T link : currentLinks) {
                userCodeLinkMap.put(link.getUserCode().getIdentifier(), link);
            }
        }
        if (targetLinks != null) {
            for (T link : targetLinks) {
                if (userCodeLinkMap.remove(link.getUserCode().getIdentifier()) == null) {
                    if (link.getUserCode().hasExpired()) {
                        throw new SessionInternalError("The user code has expired and can not be linked to an object "
                                + link.getUserCode(),
                                new String[] { errorBeanAndProperty == null ? "UserCodeWS,validTo"
                                        : errorBeanAndProperty + ",userCode.validation.expired,"
                                        + link.getUserCode().getIdentifier() });
                    }
                    associate.addUserCodeLink(link);
                }
            }
        }

        currentLinks.removeAll(userCodeLinkMap.values());
    }

    /**
     * Convert a Set of UserCodeLinkDTO to an array of strings containing the UserCodeDTO.identifier
     *
     * @param userCodeLinks
     * @return
     */
    public static String[] convertToUserCodeStringArray (Set<? extends UserCodeLinkDTO> userCodeLinks) {
        if (userCodeLinks == null) {
            return new String[0];
        }

        String[] userCodes = new String[userCodeLinks.size()];
        int idx = 0;
        for (UserCodeLinkDTO link : userCodeLinks) {
            userCodes[idx++] = link.getUserCode().getIdentifier();
        }
        return userCodes;
    }

    private Integer getPaymentInformationIndex (Integer paymentId, List<PaymentInformationDTO> paymentInstruments) {
        Integer count = 0;

        for (PaymentInformationDTO dto : paymentInstruments) {
            if (dto.getId().equals(paymentId)) {
                return count;
            }
            count++;
        }
        // not found
        return null;
    }

    /**
     * This method removes pre authorization of credit card in case credit card is updated
     *
     * @param saved
     *            PaymentInformationDTO
     * @param userId
     *            id of the credit carduser
     */
    private void removeCCPreAuthorization (PaymentInformationDTO saved, Integer userId) {
        if (saved.getId() == null || saved.getId() < 1 ) {
            logger.debug("Saving a new instrument does not require further action here.");
            return;
        }
        PaymentInformationBL piBl = new PaymentInformationBL(saved.getId());

        if ( null == piBl.get() ) {
            logger.debug("Instrument id [{}] not found. This may be a new instrument. Aborting removeCCPreAuth for this instrument.", saved.getId());
            return;
        }

        // verify if new and the old one are credit card instruments
        if (!(piBl.isCreditCard(saved) &&  piBl.isCreditCard(piBl.get()))) {
            logger.debug("Either former or new Instrument [{}] being updated is not a credit card.", saved.getId());
            return;
        }

        // verify that its value has changed
        if (piBl.isCCUpdated(saved)) {
            logger.debug("Credit card [{}] has been updated. Removing pre authrization", saved.getId());
            if (userId != null) {
                PaymentBL paymentBl = new PaymentBL();
                for (PaymentDTO auth : paymentBl.getHome().findPreauth(userId)) {
                    paymentBl.set(auth);
                    paymentBl.delete();
                }
            }

            logger.debug("Done removing pre-auths");
        }
    }

    public void saveUserWithNewPasswordScheme(Integer userId, Integer entityId, String newPasswordEncoded, Integer newScheme) {

        eLogger.audit(userId, userId, Constants.TABLE_BASE_USER,
                user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.PASSWORD_CHANGE, null, user.getPassword(), null);
        Date date = DateConvertUtils.asUtilDate(LocalDate.now());
        user.setChangePasswordDate(date);
        user.setPassword( newPasswordEncoded );
        user.setEncryptionScheme(newScheme);
        savePasswordHistory();

    }

    public Boolean isEncryptionSchemeSame(Integer methodId){
        return methodId.equals(user.getEncryptionScheme());
    }

    public Integer getEncryptionSchemeOfUser(String userName, Integer entityId){
        return user.getEncryptionScheme();
    }

    public void setAccountLocked(boolean isAccountLocked){
        user.setAccountLocked(isAccountLocked);
        if( isAccountLocked ) {
            if(null == user.getAccountLockedTime()) {       //We only need to update the lock time if it is not already set
                user.setAccountLockedTime(TimezoneHelper.serverCurrentDate());
            }
        } else {
            user.setAccountLockedTime(null);
        }
    }

    /**
     * this method checks whether account is locked or not. It first checks
     * for preferences(39 & 54) being set and return false if not. Then it checks
     * whether user lockout time is greater than the lock out time set in preference
     * 39. if it is than it should not be locked else lock
     *
     * @return
     */
    public boolean isAccountLocked() {

        if ("".equals(user.getPassword())) {
            //If no credentials have been created the account is locked
            return true;
        }

        if (null == user.getAccountLockedTime()) {
            return false;
        }

        //convert preference time to milliseconds
        int lockoutTime = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_ACCOUNT_LOCKOUT_TIME);
        long lockoutTimeInMilliSeconds = lockoutTime * 60 * 1000; //minutes to ms
        long accountLockedTimeInMilliSeconds = (Calendar.getInstance().getTimeInMillis() -
                user.getAccountLockedTime().getTime());

        return lockoutTimeInMilliSeconds > accountLockedTimeInMilliSeconds;
    }

    public void updateMetaFieldsWithValidation(Integer entityId, Integer accountTypeId, MetaContent dto) {
        user.getCustomer().updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId,
                accountTypeId, dto);
    }

    public boolean checkIfUserhasAnychildren (UserBL parent) {
        return (parent.getEntity().getCustomer().getChildren() != null && !parent
                .getEntity()
                .getCustomer()
                .getChildren()
                .isEmpty());
    }

    /***
     * If value of accountExpired is true , function will set the accountDisabledDate to current date and audit log the date
     * else set accountDisabledDate to null
     * @param accountExpired
     *
     */
    public void setAccountExpired(boolean accountExpired, Date accountDisabledDate) {
        user.setAccountExpired(accountExpired);
        if(accountExpired) {
            user.setAccountDisabledDate(null != accountDisabledDate ? accountDisabledDate : TimezoneHelper.serverCurrentDate());
            eLogger.audit(user.getId(), user.getId(), Constants.TABLE_BASE_USER,
                    user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ACCOUNT_EXPIRED, null, user.getAccountDisabledDate().toString(),
                    null);
        }
        else {
            user.setAccountDisabledDate(null);
        }

        logger.debug("Account Expired set for user {} to expired = {}", user.getId(), accountExpired);
    }

    /***
     *
     * This method checks Constants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS preference value.
     * if value is 0 that means the in-active feature is disabled, returns false
     * if the value is greater then 0 then checks if accountDisabledDate of user, if null return false else true
     *
     */
    public boolean validateAccountExpired(Date accountDisableDate) {
        PreferenceBL pref = new PreferenceBL();
        Integer daysToDeActivateAccount;

        try {
            pref.set(user.getCompany().getId(), Constants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS);
            daysToDeActivateAccount = pref.getInt();
        } catch (Exception e) {
            logger.debug("Exception while reading preference");
            return false;
        }

        logger.debug("Number of days Set for Inactive User Activity are {}", daysToDeActivateAccount);

        // Account Expiry feature disabled
        if (daysToDeActivateAccount.equals(0)) {
            // check previous state in-case of user update and account in-active feature is not enabled
            if(null != user && null != user.getAccountDisabledDate()) {
                return true;
            } else {
                return false;
            }
        }

        // Account is currently in-active
        return null != accountDisableDate;
    }

    /**
     * Get parent company for user
     * @param entityId
     * @return
     */
    public Integer getParentId(Integer entityId){
        CompanyDAS companyDas = new CompanyDAS();
        CompanyDTO companyDto = companyDas.find(entityId);
        if(companyDto.getParent()!=null){
            return companyDto.getParent().getId();
        }
        return -1;
    }

    public Integer getParentCompany(Integer entityId){
        CompanyDAS companyDas = new CompanyDAS();
        return companyDas.getParentCompanyId(entityId);
    }

    private void savePasswordHistory() {
        UserPasswordDTO userPasswordDTO = new UserPasswordDTO(user, user.getPassword());
        UserPasswordDAS userPasswordDAS = new UserPasswordDAS();
        userPasswordDAS.save(userPasswordDTO);
    }

    /**
     * It creates credentials for the current user in the BL using the parameters from the DTOEx
     * @param dto the UserDTOEx that comes from the API call
     */
    public void createCredentialsFromDTO(UserDTOEx dto) {
        boolean shouldCreateCredentialsByDefault = PreferenceBL.getPreferenceValueAsBoolean(user
                .getEntity()
                .getId(), Constants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT);

        if (shouldCreateCredentialsByDefault || dto.isCreateCredentials()) {
            logger.debug("Credentials have been sent");
            passwordService.createPassword(user);
        }
    }

    public void logout() {
        //ToDo: Put any other pre-logout code here
        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.USER_LOGOUT,null,
                String.format("User %s logged out successfully", user.getUserName()), null);
    }

    /**
     * This method Generates a PCI Compliant Password following these rules:
     * <ol>
     *     <li>At least 1 Number</li>
     *     <li>At least 1 Capital Letter</li>
     *     <li>At least 1 Lower Letter</li>
     *     <li>At least 1 Special character</li>
     *     <li>Must have a minimum of 10 characters</li>
     * </ol>
     */
    public static String generatePCICompliantPassword() {
        String randPassword = RandomStringUtils.random(10, CommonConstants.PASSWORD_PATTERN);
        Pattern pattern = Pattern.compile(CommonConstants.PASSWORD_PATTERN_4_UNIQUE_CLASSES);
        Matcher matcher = pattern.matcher(randPassword);
        while (!matcher.matches()) {
            randPassword = RandomStringUtils.random(10, CommonConstants.PASSWORD_PATTERN);
            matcher = pattern.matcher(randPassword);
        }

        return randPassword;
    }

    public List<CustomerDTO> getUserByCustomerMetaField(String metaFieldValue, String metaFieldName,
            Integer entityId) {
        try {
            MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
            MetaField customerMetaField = metaFieldDAS.getFieldByName(entityId,
                    new EntityType[] { EntityType.CUSTOMER }, metaFieldName);

            List<Integer> customerIds = metaFieldDAS.findEntitiesByMetaFieldValue(customerMetaField, metaFieldValue);
            CustomerDAS customerDAS = new CustomerDAS();
            List<CustomerDTO> customerList = new ArrayList<>();
            for (int id : customerIds) {
                customerList.add(customerDAS.find(id));
            }
            return customerList;
        } catch (Exception ex) {
            logger.error("Error in getting custiomer:", ex);
            return null;
        }
    }
    public List<UserDTO> getUserByParentId(Integer parentId) {
        try {
            return new UserDAS().findChildDTOList(parentId);
        } catch (Exception ex) {
            logger.error("Error in getting custiomer:", ex);
            return null;
        }
    }

    private boolean validateMetaFieldValue(MetaFieldValue<?> value) {
        return (null == value.getValue() || value.isEmpty() ||
                value.getValue().toString().trim().isEmpty());
    }

    @SuppressWarnings("rawtypes")
    private MetaFieldValue getMetaFieldValueByMetaFieldType(List<MetaFieldValue> metaFieldValues, MetaFieldType type) {
        if(null!=metaFieldValues && !metaFieldValues.isEmpty()) {
            for(MetaFieldValue metaFieldValue : metaFieldValues) {
                if(metaFieldValue.getField().getFieldUsage().equals(type)) {
                    return metaFieldValue;
                }
            }
        }
        return null;
    }


    public static void setCimProfileError(UserWS userWS) {
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        MetaField metaField = metaFieldDAS.getFieldByName(userWS.getEntityId(), new EntityType [] {EntityType.COMPANY},
                MetaFieldName.PAYPAL_BILLING_GROUP_NAME.getMetaFieldName()
                , true);
        if(null == metaField) {
            return ;
        }

        String billingGroupNameValue = metaFieldDAS.getComapanyLevelMetaFieldValue(
                MetaFieldName.PAYPAL_BILLING_GROUP_NAME.getMetaFieldName(), userWS.getEntityId());
        if(null == billingGroupNameValue || billingGroupNameValue.isEmpty()) {
            userWS.setCimProfileError(CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR);
            return ;
        }
        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
        MetaFieldGroup metaFieldGroup = accountInformationTypeDAS.getAccountInformationTypeByName(userWS.getEntityId()
                , userWS.getAccountTypeId(), billingGroupNameValue);

        if(null==metaFieldGroup) {
            return ;
        }

        Map<String, String> userMetaFieldMapByMetaFieldType =
                metaFieldDAS.getCustomerAITMetaFieldValueMapByMetaFieldType(userWS.getCustomerId(), metaFieldGroup.getId(),TimezoneHelper.companyCurrentDate(userWS.getEntityId()));

        EnumSet<MetaFieldType> requiredMetaFieldTypes = EnumSet.of(MetaFieldType.ADDRESS1, MetaFieldType.POSTAL_CODE, MetaFieldType.BILLING_EMAIL);
        for(MetaFieldType type: requiredMetaFieldTypes) {
            if(!userMetaFieldMapByMetaFieldType.containsKey(type.toString())) {
                userWS.setCimProfileError(CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR);
                break;
            }
        }

        if(!(userMetaFieldMapByMetaFieldType.containsKey(MetaFieldType.FIRST_NAME.toString()) ||
                userMetaFieldMapByMetaFieldType.containsKey(MetaFieldType.ORGANIZATION.toString()))) {
            userWS.setCimProfileError(CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR);
        }

        if (!CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR.equals(userWS.getCimProfileError())) {
            List<PaymentInformationWS> paymentInstruments = userWS.getPaymentInstruments();
            if (null != paymentInstruments && !paymentInstruments.isEmpty()) {
                PaymentInformationDAS piDAS = new PaymentInformationDAS();
                PaymentInformationBL piBL = new PaymentInformationBL();
                for(PaymentInformationWS piWS: paymentInstruments){
                    if(CommonConstants.PAYMENT_METHOD_ACH.equals(piWS.getPaymentMethodId())){
                        @SuppressWarnings("rawtypes")
                        MetaFieldValue value = piBL.getMetaField(piDAS.find(piWS.getId()), MetaFieldType.GATEWAY_KEY);
                        if(value == null ){
                            userWS.setCimProfileError(CommonConstants.CIM_PROFILE_ERROR);
                        }else{
                            MetaFieldValueWS valueWS = MetaFieldBL.getWS(value);
                            if(valueWS.getCharValue() == null || valueWS.getCharValue().length <=0){
                                userWS.setCimProfileError(CommonConstants.CIM_PROFILE_ERROR);

                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Get current billing cycle overage charges of usage orders.
     * @return
     */
    public BigDecimal getOverageCharges() {
        BigDecimal overageCharges= BigDecimal.ZERO;
        if (null != user && null != user.getCustomer()) {
            OrderPeriodDTO orderPeriod = user.getCustomer().getMainSubscription().getSubscriptionPeriod();
            Date billingCyleEndDate = user.getCustomer().getNextInvoiceDate();
            Calendar billingCycleStartDate= Calendar.getInstance();
            billingCycleStartDate.setTime(billingCyleEndDate);
            billingCycleStartDate.add(MapPeriodToCalendar.map(orderPeriod.getUnitId()), orderPeriod.getValue() *(-1));
            OrderDAS orderDas = new OrderDAS();

            //Get all usage orders in current billing period of the user
            List<Integer> orderIds = orderDas.getCustomersAllUsageOrdersInCurrentBillingCycle(user.getUserId(),
                    billingCycleStartDate.getTime(), billingCyleEndDate);
            for (Integer orderId : orderIds) {
                OrderDTO order = orderDas.find(orderId);
                for (OrderLineDTO line : order.getLines()) {
                    overageCharges=overageCharges.add(line.getAmount());
                }
            }
        }
        return overageCharges;
    }

    /**
     * Get current billing cycle overage rate of usage orders.
     * @return
     */
    public BigDecimal getOverageRatePerMinute() {
        BigDecimal overageRatePerMinute= BigDecimal.ZERO;
        String skippedProductInternalNumber = PreferenceBL.getPreferenceValue(user.getEntity().getId(),
                Constants.PREFERENCE_FREE_MINUTES_TOKEN_SKIP_PRODUCT_CODE);
        if (null != user) {
            OrderDAS orderDas = new OrderDAS();

            //Get monthly orders in current billing period of the user
            List<Integer> orderIds = orderDas.getCustomerRecurringOrders(user.getUserId(), OrderStatusFlag.INVOICE);
            for (Integer orderId : orderIds) {
                OrderDTO order = orderDas.find(orderId);
                for (OrderLineDTO line : order.getLines()) {
                    if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().hasPlans()) {
                        for (PlanDTO plan : line.getItem().getPlans()) {
                            if (plan.getItem().getInternalNumber().contains(skippedProductInternalNumber)){
                                continue;
                            }

                            Set<Map.Entry<Date,PriceModelDTO>> entry = plan.getPlanItems().get(0).getModels().entrySet();
                            Date key = entry.iterator().next().getKey();
                            PriceModelDTO priceModel = plan.getPlanItems().get(0).getModels().get(key);
                            overageRatePerMinute = priceModel.getRate();

                            if (null != overageRatePerMinute && overageRatePerMinute.compareTo(BigDecimal.ZERO) > 0) {
                                break;
                            }
                        }
                    }
                }

            }
        }
        return overageRatePerMinute;
    }

    /**
     * Sends the SSOEnabledUserCreatedEmailMessage Notification
     *
     * @param entityId
     * @param userId
     * @param languageId
     * @throws SessionInternalError
     * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
     */
    public void sendSSOEnabledUserCreatedEmailMessage(Integer entityId, Integer userId, Integer languageId)
            throws SessionInternalError, NotificationNotFoundException {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getSSOEnabledUserCreatedEmailMessage(entityId, userId, languageId);
            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(userId, message);
        } catch (NotificationNotFoundException e) {
            logger.error("Exception while sending notification : {}", e.getMessage());
            throw new SessionInternalError("Notification not found for SSOEnabledUserCreatedEmailMessage");
        }
    }

    public CustomerProperties getCustomerProperties() {
        return new CustomerProperties(user.getUserName(), user.getLanguage().getCode());
    }

    public UserDTO findUsersByEmailAndUserName (String email, String userName, Integer entity) {
        return new UserDAS().findByEmailAndUserName(email, userName, entity);
    }

    public UserDTO findUsersByUserName(String userName, Integer entity) {
        return new UserDAS().findByUserName(userName, entity);
    }

    public List<PaymentInformationWS> getAllUserPaymentInstrumentWS() {
        return PaymentInformationBL.convertPaymentInformationDTOtoWS(user.getPaymentInstruments());
    }

    public static Date checkNIDAgaintMainSubscription(int period, int dayOfPeriod, Date nextInvoiceDate) {
        LocalDate localDate = DateConvertUtils.asLocalDate(nextInvoiceDate);
        if (Constants.PERIOD_UNIT_MONTH == period && localDate.getDayOfMonth() < dayOfPeriod &&
                localDate.getDayOfMonth() < localDate.lengthOfMonth()) {
            localDate = localDate.withDayOfMonth(dayOfPeriod);
        }

        return DateConvertUtils.asUtilDate(localDate);
    }

    /**
     * Calculates Customer's NextInvoiceDate on last ran billing process.
     * @param user
     */
    private void setCustomerNextInvoiceDateBasedOnLastRanBillingProcess(UserDTO user) {
        // next Invoice Date
        setCustomerNextInvoiceDate(user,null);
        Date lastBillingProcessDate = new BillingProcessDAS().getLastBillingProcessDate(user.getEntity().getId());
        if(null!= lastBillingProcessDate) {
            CustomerDTO customer = user.getCustomer();
            Date nextInvoiceDate = customer.getNextInvoiceDate();
            MainSubscriptionDTO mainSubscription = customer.getMainSubscription();
            int periodUnitId = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
            int dayOfMonth = mainSubscription.getNextInvoiceDayOfPeriod();
            PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
            int periodUnitValue = mainSubscription.getSubscriptionPeriod().getValue() * -1;
            Date expectedNextInvoiceDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(nextInvoiceDate), periodUnitValue));
            if(expectedNextInvoiceDate.after(lastBillingProcessDate)) {
                customer.setNextInvoiceDate(expectedNextInvoiceDate);
            }
            logger.debug("Customer NextInvoice Date Based On Last Billing Run is {}", customer.getNextInvoiceDate());
        }
    }

    public List<Integer> findAdminUserIds(Integer entityId) {
        return das.findAdminUserIds(entityId);
    }

    public UserDTO getUserByAssetId(Integer assetId) {
        if(null != assetId) {
            OrderLineDTO orderLine = new AssetBL().find(assetId).getOrderLine();
            if (null != orderLine) {
                return new OrderDAS().findNow(orderLine.getPurchaseOrder().getId()).getUser();
            }
        }
        return null;
    }

    public boolean matchPasswordForUser(UserDTO user, String rawPassword) {
        return JBCrypto.passwordsMatch(user.getEncryptionScheme(), user.getPassword(), rawPassword);
    }

    @SuppressWarnings("rawtypes")
    public static UserProfileWS getUserProfile(Integer userId) {
	UserDTO user = getUserEntity(userId);
	UserProfileWS userProfileWS = new UserProfileWS();
	CustomerDTO customerDTO = user.getCustomer();
	Map<String,String> customerAitMetaFieldMap = new LinkedHashMap<>();
	Integer languageId = user.getLanguage().getId();
	if(null != customerDTO) {
		Set<CustomerAccountInfoTypeMetaField> currentCustomerAitMetaFields = new LinkedHashSet<>();
		for (AccountInformationTypeDTO ait : customerDTO.getAccountType().getInformationTypes()) {
			Date effectiveDate = customerDTO.getEffectiveDateByGroupIdAndDate(ait.getId(), new Date());
			currentCustomerAitMetaFields.addAll(customerDTO.getCustomerAccountInfoTypeMetaFields(ait.getId(), effectiveDate));
		}

		for (CustomerAccountInfoTypeMetaField customerAitMetaField : currentCustomerAitMetaFields) {
			String parameterName = customerAitMetaField.getMetaFieldValue().getField().getName().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
			String parameterValue = customerAitMetaField.getMetaFieldValue().getValue() != null ? customerAitMetaField.getMetaFieldValue().getValue().toString() : "";
			if (customerAitMetaField.getAccountInfoType() != null && customerAitMetaField.getAccountInfoType().getName() != null) {
				String prefix = customerAitMetaField.getAccountInfoType().getName().replaceAll("[/!@#\\\\$%&\\\\*()\\s]", "_");
				customerAitMetaFieldMap.put(prefix + "_" + parameterName, parameterValue);
			}
		}

		List<MetaFieldValue> customerMetaFields = customerDTO.getMetaFields();
		if(CollectionUtils.isNotEmpty(customerMetaFields)) {
			Map<String,String> customerMetaFieldMap = new HashMap<>();
			customerMetaFields.stream()
			.filter( value -> null != value && null != value.getField())
			.forEach( value -> customerMetaFieldMap.put(value.getField().getName(), String.valueOf(value.getValue())));
			userProfileWS.setCustomerMetaFieldMap(customerMetaFieldMap);
		}

		MainSubscriptionDTO mainSubscription = customerDTO.getMainSubscription();
		if (null != mainSubscription) {
			userProfileWS.setBillingCycleDay(mainSubscription.getNextInvoiceDayOfPeriod());
			userProfileWS.setBillingCycleUnit(mainSubscription.getSubscriptionPeriod().getDescription(languageId));
		}

		userProfileWS.setParentId((user.getCustomer().getParent() == null) ? null : user
				.getCustomer()
				.getParent()
				.getBaseUser()
				.getId());
		userProfileWS.setAccountType(customerDTO.getAccountType().getDescription(languageId));
		userProfileWS.setUseParentPricing(customerDTO.useParentPricing());

		List<CustomerNoteDTO> customerNoteDTOs = new CustomerNoteDAS().findByCustomer(customerDTO.getId(), user.getCompany().getId());
		List<CustomerNoteWS> customerNoteWSList = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(customerNoteDTOs)) {
			customerNoteDTOs.stream()
			.filter(Objects::nonNull)
			.forEach( note -> customerNoteWSList.add(convertCustomerNoteToWS(note)));
		}

		userProfileWS.setNotes(customerNoteWSList.toArray(new CustomerNoteWS[0]));
		userProfileWS.setInvoiceTemplate(null != customerDTO.getInvoiceTemplate() ? customerDTO.getInvoiceTemplate().getName() : StringUtils.EMPTY);
	}

	userProfileWS.setCustomerAitMetaFieldMap(customerAitMetaFieldMap);
	Map<String,String> paymentInstrumentMap = new LinkedHashMap<>();

	AtomicInteger atomicInteger = new AtomicInteger(0);

	List<PaymentInformationDTO> instrumentList = user.getPaymentInstruments();

	if (CollectionUtils.isNotEmpty(instrumentList)) {
		instrumentList.stream()
		.filter(Objects::nonNull)
		.forEach( instrument -> {
			List<MetaFieldValue> fieldValues = instrument.getMetaFields();
			int prefix = atomicInteger.incrementAndGet();
			if (CollectionUtils.isNotEmpty(fieldValues)) {
				paymentInstrumentMap.put(String.format("%02d" , prefix) + "_" + "paymentInstrumentId", instrument.getId().toString());
				fieldValues.stream().filter( value -> null != value &&
						null != value.getField() &&
						!(CommonConstants.METAFIELD_NAME_CC_GATEWAY_KEY.equals(value.getField().getName()) ||
								CommonConstants.METAFIELD_NAME_ACH_GATEWAY_KEY.equals(value.getField().getName())))
								.forEach(value -> {
									if (value.getValue() instanceof char[]) {
										paymentInstrumentMap.put(String.format("%02d" , prefix) + "_" + value.getField().getName(),String.valueOf((char[])value.getValue()));
									} else {
										paymentInstrumentMap.put(String.format("%02d" , prefix) + "_" + value.getField().getName(),String.valueOf(value.getValue()));
									}
								});
			}
		});
	}
	userProfileWS.setPaymentInstrumentMap(paymentInstrumentMap);
	userProfileWS.setAccountId(user.getId());
	userProfileWS.setCustomerName(user.getUserName());
	userProfileWS.setStatus(user.getStatus().getDescription(user.getLanguageIdField()));
	userProfileWS.setCreateDateTime(user.getCreateDatetime());
	userProfileWS.setOwingBalance(getBalance(userId));
	userProfileWS.setCurrencyCode(null != user.getCurrency() ? user.getCurrency().getCode() : StringUtils.EMPTY);
	userProfileWS.setLanguageDescription(null != user.getLanguage() ? user.getLanguage().getDescription() : StringUtils.EMPTY);
	userProfileWS.setInvoiceAsChild(user.isInvoiceAsChild());
	userProfileWS.setNumberOfInvoices(new InvoiceDAS().getInvoiceCountByUserId(userId).intValue());
	userProfileWS.setNumberOfPayments(new PaymentDAS().getPaymentsCountByUserId(userId).intValue());
	JMRRepository jmrRepository = Context.getBean(JMRRepository.class);
	userProfileWS.setNumberOfUnbilledCallRecords(jmrRepository.getCountOfUnBilledMediationEventsByUser(userId).intValue());
	return userProfileWS;
}

    public boolean updatePassword(Integer userId, Integer entityId, String newPassword) throws SessionInternalError {
        boolean result = false;
        if (null != user) {
            //encrypt it based on the user role
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(getMainRole());
            String newPasswordEncoded = JBCrypto.encodePassword(passwordEncoderId, newPassword);
            String oldPassword = user.getPassword();
            //compare current password with last six
            List<String> passwords = new UserPasswordDAS().findLastSixPasswords(user, newPasswordEncoded);
            for(String password: passwords) {
                if(JBCrypto.passwordsMatch(passwordEncoderId, password, newPassword)) {
                    throw new SessionInternalError("Failed update password",
                            new String[] {"Password is similar to one of the last six passwords. Please enter a unique Password."});
                }
            }
            CustomerProperties oldCustomerProperties  = getCustomerProperties();
            oldCustomerProperties.setNewPassword(newPasswordEncoded);
            oldCustomerProperties.setOldPassword(oldPassword);
            saveUserWithNewPasswordScheme(userId, entityId, newPasswordEncoded, passwordEncoderId);
            UpdateCustomerEvent updateCustomerEvent = new UpdateCustomerEvent(user.getCustomer().getId(),
                    oldCustomerProperties, Collections.emptyMap(), entityId);
            EventManager.process(updateCustomerEvent);
            result = true;
        }
        return result;
    }

    public Integer addPaymentInstrument(PaymentInformationWS instrument) {
        try {
            Integer entityId = user.getEntity().getId();
            PaymentInformationDTO paymentInformationDTO = new PaymentInformationDTO(instrument, entityId);

            boolean isCCTypeAvailable = paymentInformationDTO.getMetaFields().stream()
                    .anyMatch(mf -> MetaFieldType.CC_TYPE.equals(mf.getField().getFieldUsage()));
            if (!isCCTypeAvailable) {
                MetaField ccTypeMF = MetaFieldExternalHelper
                        .findPaymentMethodMetaFieldByFieldUsage(MetaFieldType.CC_TYPE, instrument.getPaymentMethodTypeId());
                if (ccTypeMF != null) {
                    paymentInformationDTO.getMetaFields().add(ccTypeMF.createValue());
                }
            }

            paymentInformationDTO.setUser(user);
            setCreditCardType(paymentInformationDTO);
            setPaymentMenthodId(paymentInformationDTO);
            PaymentInformationDTO saved = paymentInformationDAS.create(paymentInformationDTO, entityId);
            user.getPaymentInstruments().add(saved);
            Integer savedInstrumentId = saved.getId();

            if("ACH".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                EventManager.process(new AchUpdateEvent(saved, entityId));
            }else if("Payment Card".equals(saved.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
                EventManager.process(new NewCreditCardEvent(saved, entityId));
            }

            user = das.save(user);
            String msg = "Payment instrument with id: " + saved.getId() + " successfully added for user " + user.getId();
            logger.info(getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE));
            saved.close();
            return savedInstrumentId;
        } catch(Exception ex){
            logger.debug("Exception occurred while adding payment instrument", ex);
            throw new SessionInternalError(ex);
        }
    }

    public static PaymentInformationRestWS[] getPaymentInformationWSForRestWS(PaymentInformationWS[] instrument) {
        List<PaymentInformationRestWS> informationRestWSs = new ArrayList<>();
        for (PaymentInformationWS paymentInformationWS : instrument) {
            PaymentInformationRestWS restWS = new PaymentInformationRestWS();
            restWS.setId(paymentInformationWS.getId());
            restWS.setUserId(paymentInformationWS.getUserId());
            restWS.setProcessingOrder(paymentInformationWS.getProcessingOrder());
            restWS.setPaymentMethodId(paymentInformationWS.getPaymentMethodId());
            restWS.setPaymentMethodTypeId(paymentInformationWS.getPaymentMethodTypeId());
            restWS.setCvv(paymentInformationWS.getCvv());
            restWS.setMetaFields(MetaFieldBL.getFieldsMap(paymentInformationWS.getMetaFields()));
            informationRestWSs.add(restWS);
        }
        return informationRestWSs.toArray(new PaymentInformationRestWS[0]);
    }

    private Predicate<Object> notNull= Objects::nonNull;

    private boolean isPaymentInstrumentUpdated(PaymentInformationDTO newDto, PaymentInformationDTO oldDto) {
        boolean updated;
        List<MetaFieldValue> newDtoMetaFields = newDto.getMetaFields();
        List<MetaFieldValue> oldDtoMetaFields = oldDto.getMetaFields();

        for (MetaFieldValue newDtoMetaField : newDtoMetaFields) {
            for (MetaFieldValue oldDtoMetaField : oldDtoMetaFields) {
                if (oldDtoMetaField.getField().getName().equals(newDtoMetaField.getField().getName())) {
                    if (notNull.test(newDtoMetaField.getValue()) && notNull.test(oldDtoMetaField.getValue())) {
                        if (newDtoMetaField.getValue() instanceof char[]){
                            updated = !(String.valueOf((char[]) newDtoMetaField.getValue())).equals(String.valueOf((char[]) oldDtoMetaField.getValue()));
                        }else {
                            updated = !(String.valueOf(newDtoMetaField.getValue())).equals(String.valueOf(oldDtoMetaField.getValue()));
                        }
                        if (updated) {
                            String msg = oldDtoMetaField.getField().getName()+" Value Changed from: " + oldDtoMetaField.getValue()
                                    +" To new Value : "+newDtoMetaField.getValue()+" of Payment Instrument Id: "+oldDto.getId();
                            logger.info(getUserEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE));
                            return updated;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Map<String, String> getPaymentInstrumentUpdatedValues(PaymentInformationDTO newDto,
            PaymentInformationDTO oldDto) {
        Map<String, String> updatedValues = new HashMap<>();
        List<MetaFieldValue> newDtoMetaFields = newDto.getMetaFields();
        List<MetaFieldValue> oldDtoMetaFields = oldDto.getMetaFields();
        for (MetaFieldValue newDtoMetaField : newDtoMetaFields) {
            for (MetaFieldValue oldDtoMetaField : oldDtoMetaFields) {
                if (oldDtoMetaField.getField().getName().equals(newDtoMetaField.getField().getName())) {
                    if (notNull.test(newDtoMetaField.getValue()) && notNull.test(oldDtoMetaField.getValue())) {
                        if (oldDtoMetaField.getValue() instanceof char[]) {
                            if (!(String.valueOf((char[]) oldDtoMetaField.getValue())).equals(String
                                    .valueOf((char[]) newDtoMetaField.getValue()))) {
                                updatedValues.put(oldDtoMetaField.getField().getName(),
                                        String.valueOf((char[]) oldDtoMetaField.getValue()));
                            }
                        } else if (oldDtoMetaField.getValue() instanceof BigDecimal) {
                            if (((BigDecimal) oldDtoMetaField.getValue()).compareTo((BigDecimal) newDtoMetaField
                                    .getValue()) != 0) {
                                updatedValues.put(oldDtoMetaField.getField().getName(),
                                        String.valueOf(oldDtoMetaField.getValue()));
                            }
                        } else if (oldDtoMetaField.getValue() instanceof Date) {
                            if (((Date) oldDtoMetaField.getValue()).compareTo((Date) newDtoMetaField
                                    .getValue()) != 0) {
                                updatedValues.put(oldDtoMetaField.getField().getName(),
                                        String.valueOf(oldDtoMetaField.getValue()));
                            }
                        } else if (!oldDtoMetaField.getValue().equals(newDtoMetaField.getValue())) {
                            updatedValues.put(oldDtoMetaField.getField().getName(),
                                    String.valueOf(oldDtoMetaField.getValue()));
                        }
                    }
                }
            }
        }
        return updatedValues;
    }

    public List<UserDTO> getAllUsers(Integer entityId){
        return das.getAllUsers(entityId);
    }
    private Integer getMessageId(String name) {
        MessageIdMap message = MessageIdMap.fromName(name);
        if (message == null) {
            logger.error("Unable to get messageId for property name: {}",name);
            return EventLogger.ROW_UPDATED;
        }
        return message.getValue();
    }
}
