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

package com.sapienter.jbilling.server.payment.blacklist;

import java.util.Date;
import java.util.List;
import java.util.List;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;

import org.springframework.dao.EmptyResultDataAccessException;

/**
 * Business logic class for the blacklist module.
 */
public class BlacklistBL {
    private BlacklistDAS blacklistDAS;
    private BlacklistDTO blacklistEntry;

    public BlacklistBL() {
        init();
    }

    public BlacklistBL(Integer blacklistEntryId) {
        init();
        set(blacklistEntryId);
    }

    public BlacklistBL(BlacklistDTO blacklistEntry) {
        init();
        set(blacklistEntry);
    }

    private void init() {
        blacklistDAS = new BlacklistDAS();
    }

    public void set(Integer blacklistEntryId) {
        blacklistEntry = blacklistDAS.find(blacklistEntryId);
    }

    public void set(BlacklistDTO blacklistEntry) {
        this.blacklistEntry = blacklistEntry;
    }

    public BlacklistDTO getDTO() {
        return blacklistEntry;
    }

    /**
     * Retuns true if the user id is blacklisted,
     * even if the UserIdFilter is disabled.
     */
    public static boolean isUserIdBlacklisted(Integer userId) {
        List<BlacklistDTO> blacklist = new BlacklistDAS().findByUserType(
                userId, BlacklistDTO.TYPE_USER_ID);

        return !blacklist.isEmpty();
    }

    /**
     * Instantiates the payment filter plug-in and checks the user 
     * against the enabled filters. Returns a vector of messages
     * returned by filters the user fails on, or null if the blacklist
     * preference isn't enabled.
     */
    public static List<String> getBlacklistMatches(Integer userId) {
        Integer entityId = new UserDAS().find(userId).getCompany().getId();

        PaymentFilterTask blacklist = instantiatePaymentFilter(entityId);

        if (blacklist != null) {
            return blacklist.getBlacklistMatches(userId);
        }
        return null;
    }

    /**
     * Returns the id of the payment filter plug-in set by the 
     * 'use blacklist' entity level preference.
     */
    public static Integer getBlacklistPluginId(Integer entityId) {
        // get the blacklist filter plug-in id from blacklist preference
    	Integer preferenceUseBlacklist = 0;
        try {
        	preferenceUseBlacklist = 
        		PreferenceBL.getPreferenceValueAsIntegerOrZero(
        			entityId, Constants.PREFERENCE_USE_BLACKLIST);
        } catch (EmptyResultDataAccessException fe) { 
            // use default
        }

        return preferenceUseBlacklist;
    }

    /**
     * Returns whether the blacklist is enabled.
     */
    public static boolean isBlacklistEnabled(Integer entityId) {
        return getBlacklistPluginId(entityId) != 0;
    }

    public BlacklistDTO save() {
        blacklistEntry = blacklistDAS.save(blacklistEntry);
        return blacklistEntry;
    }

    /**
     * Creates an entry in the blacklist.
     */
    public Integer create(CompanyDTO company, Integer type, Integer source, 
            PaymentInformationDTO creditCard, ContactDTO contact, UserDTO user, MetaFieldValue metaFieldValue) {
        BlacklistDTO entry = new BlacklistDTO();
        entry.setCompany(company);
        entry.setCreateDate(TimezoneHelper.serverCurrentDate());
        entry.setType(type);
        entry.setSource(source);
        entry.setCreditCard(creditCard);
        entry.setContact(contact);
        entry.setUser(user);
        entry.setMetaFieldValue(metaFieldValue);

        // save data
        blacklistEntry = blacklistDAS.save(entry);

        return blacklistEntry.getId();
    }

    /**
     * Instantiates the payment filter plug-in and returns the 
     * IP Address CCF Id or null if it can't be found.
     */
    public static Integer getIpAddressCcfId(Integer entityId) {
        PaymentFilterTask blacklist = instantiatePaymentFilter(entityId);

        if (blacklist != null) {
            return blacklist.getIpAddressCcfId();
        }
        return null;
    }

    /**
     * Returns an instance of the payment filter plug-in or
     * null if the blacklist preference isn't enabled.
     */
    public static PaymentFilterTask instantiatePaymentFilter(Integer entityId) {
        Integer blacklistPluginId = getBlacklistPluginId(entityId);

        if (blacklistPluginId == 0) {
            // blacklist isn't enabled
            return null;
        }

        // instantiate blacklist payment filter plug-in &
        // initialize its parameters
        PluggableTaskDTO blacklistPluginInfo = ((PluggableTaskDAS) Context.getBean(
                Context.Name.PLUGGABLE_TASK_DAS)).find(blacklistPluginId);
        PaymentFilterTask blacklist = new PaymentFilterTask();
        try {
            blacklist.initializeParamters(blacklistPluginInfo);
        } catch (PluggableTaskException pte) {
            throw new SessionInternalError("Error initilizing blacklist parameters",
                    BlacklistBL.class, pte);
        }

        return blacklist;
    }
}
