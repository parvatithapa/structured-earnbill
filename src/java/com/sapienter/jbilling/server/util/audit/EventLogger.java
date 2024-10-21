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

package com.sapienter.jbilling.server.util.audit;

import java.util.Date;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.db.EventLogDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.audit.db.EventLogMessageDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogModuleDAS;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import org.apache.log4j.Logger;

public class EventLogger {

    // these are the messages constants, in synch with the db (event_log_message)
    // billing process
    public static final Integer BILLING_PROCESS_UNBILLED_PERIOD = new Integer(1);
    public static final Integer BILLING_PROCESS_NOT_ACTIVE_YET = new Integer(2);
    public static final Integer BILLING_PROCESS_ONE_PERIOD_NEEDED = new Integer(3);
    public static final Integer BILLING_PROCESS_RECENTLY_BILLED = new Integer(4);
    public static final Integer BILLING_PROCESS_WRONG_FLAG_ON = new Integer(5);
    public static final Integer BILLING_PROCESS_EXPIRED = new Integer(6);
    public static final Integer BILLING_REVIEW_NOT_APPROVED = new Integer(10);
    public static final Integer BILLING_REVIEW_NOT_GENERATED = new Integer(11);
    // user maintenance
    public static final Integer PASSWORD_CHANGE = new Integer(8);
    public static final Integer STATUS_CHANGE = new Integer(12);
    public static final Integer NO_FURTHER_STEP = new Integer(14);
    public static final Integer CANT_PAY_PARTNER = new Integer(15);
    public static final Integer SUBSCRIPTION_STATUS_CHANGE = new Integer(20);
    public static final Integer SUBSCRIPTION_STATUS_NO_CHANGE = new Integer(32);
    public static final Integer ACCOUNT_LOCKED = new Integer(21);
    public static final Integer ACCOUNT_EXPIRED = new Integer(22);
    public static final Integer DYNAMIC_BALANCE_CHANGE = new Integer(33);
    public static final Integer INVOICE_IF_CHILD_CHANGE = new Integer(34);
    public static final Integer ORDER_CREATED_FOR_RESELLER_IN_ROOT = new Integer(35);
    public static final Integer NEXT_INVOICE_DATE_CHANGE= new Integer(43);
    public static final Integer BILLING_CYCLE_CHANGE= new Integer(42);
    public static final Integer INVOICE_DESIGN_CHANGE = 41;

    public static final Integer SUCESSFULL_USER_LOGIN= new Integer(38);
    public static final Integer USER_LOGOUT= new Integer(39);
    public static final Integer FAILED_USER_LOGIN= new Integer(40);
    public static final Integer FAILED_LOGIN_ATTEMPTS = new Integer(37);
    // order maintenance
    public static final Integer ORDER_STATUS_CHANGE = new Integer(13);
    public static final Integer ORDER_LINE_UPDATED = new Integer(17);
    public static final Integer ORDER_NEXT_BILL_DATE_UPDATED = new Integer(18);
    public static final Integer ORDER_MAIN_SUBSCRIPTION_UPDATED = new Integer(22);
    public static final Integer ORDER_CANCEL_AND_CREDIT = new Integer(26);
    // payment
    public static final Integer PAYMENT_INSTRUMENT_NOT_FOUND = new Integer(24);
    // invoice related message
    public static final Integer INVOICE_ORDER_APPLIED = new Integer(16);
    // mediation
    public static final Integer CURRENT_ORDER_FINISHED = new Integer(23);
    // blacklist
    public static final Integer BLACKLIST_USER_ID_ADDED = new Integer(27);
    public static final Integer BLACKLIST_USER_ID_REMOVED = new Integer(28);
    //provisioning
    public static final Integer PROVISIONING_UUID = new Integer(29);
    public static final Integer PROVISIONING_COMMAND=new Integer(30);
    public static final Integer PROVISIONING_STATUS_CHANGE=new Integer(31);

    // others
    public static final Integer ROW_CREATED = new Integer(25);
    public static final Integer ROW_DELETED = new Integer(7);
    public static final Integer ROW_UPDATED= new Integer(9); // field not specified
    public static final Integer USER_TRANSITIONS_LIST = new Integer(19);


    // event log modules in synch with db (event_log_module)
    public static final Integer MODULE_BILLING_PROCESS = new Integer(1);
    public static final Integer MODULE_USER_MAINTENANCE = new Integer(2);
    public static final Integer MODULE_ITEM_MAINTENANCE = new Integer(3);
    public static final Integer MODULE_ITEM_TYPE_MAINTENANCE = new Integer(4);
    public static final Integer MODULE_ITEM_USER_PRICE_MAINTENANCE = new Integer(5);
    public static final Integer MODULE_PROMOTION_MAINTENANCE = new Integer(6);
    public static final Integer MODULE_ORDER_MAINTENANCE = new Integer(7);
    public static final Integer MODULE_CREDIT_CARD_MAINTENANCE = new Integer(8);
    public static final Integer MODULE_INVOICE_MAINTENANCE = new Integer(9);
    public static final Integer MODULE_PAYMENT_MAINTENANCE = new Integer(10);
    public static final Integer MODULE_TASK_MAINTENANCE = new Integer(11);
    public static final Integer MODULE_WEBSERVICES = new Integer(12);
    public static final Integer MODULE_MEDIATION = new Integer(13);
    public static final Integer MODULE_BLACKLIST = new Integer(14);
    public static final Integer MODULE_PROVISIONING=new Integer(15);
    public static final Integer MODULE_PAYMENT_INFORMATION_MAINTENANCE =16;

    // levels of logging
    public static final Integer LEVEL_DEBUG = new Integer(1);
    public static final Integer LEVEL_INFO = new Integer(2);
    public static final Integer LEVEL_WARNING = new Integer(3);
    public static final Integer LEVEL_ERROR = new Integer(4);
    public static final Integer LEVEL_FATAL = new Integer(5);

    // log reissue
    public static final Integer REISSUE_LIMIT_EXCEEDED = 44;
    public static final Integer ASSET_UPDATED = 45;

    // Message ids for audit log report
    public static final Integer SIM_REISSUED = 46;
    public static final Integer CUSTOMER_TYPE_UPDATED = 47;
    public static final Integer GOVERNORATE_UPDATED = 48;
    public static final Integer IDENTIFICATION_TYPE_UPDATED = 49;
    public static final Integer IDENTIFICATION_TEXT_UPDATED = 50;
    public static final Integer IDENTIFICATION_IMAGE_UPDATED = 51;
    public static final Integer FIRST_NAME_UPDATED = 52;
    public static final Integer LAST_NAME_UPDATED = 53;
    public static final Integer ADDRESS_LINE_1_UPDATED = 54;
    public static final Integer ADDRESS_LINE_2_UPDATED = 55;
    public static final Integer CITY_UPDATED = 56;
    public static final Integer STATE_UPDATED = 57;
    public static final Integer POSTAL_CODE_UPDATED = 58;
    public static final Integer COUNTRY_UPDATED = 59;
    public static final Integer CONTACT_NO_UPDATED = 60;
    public static final Integer EMAIL_ID_UPDATED = 61;
    public static final Integer SIM_REISSUE_REFUND = 62;
    public static final Integer USER_DELETED = 63;
    public static final Integer CUSTOMER_CSV = 64;

    private EventLogDAS eventLogDAS = null;
    private EventLogMessageDAS eventLogMessageDAS = null;
    private EventLogModuleDAS eventLogModuleDAS = null;
    private JbillingTableDAS jbDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EventLogger.class));

    public EventLogger() {
        eventLogDAS = new EventLogDAS();
        eventLogMessageDAS = new EventLogMessageDAS();
        eventLogModuleDAS = new EventLogModuleDAS();
        jbDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
    }

    public static synchronized EventLogger getInstance() {
        return new EventLogger();
    }

    public void log(Integer level, Integer entity, Integer userAffectedId,
            Integer rowId, Integer module, Integer message, String table)  {
        CompanyDAS company = new CompanyDAS();
        UserDAS user= new UserDAS();
        EventLogDTO dto = new EventLogDTO(null, jbDAS.findByName(table), null,
                user.find(userAffectedId), eventLogMessageDAS.find(message),
                eventLogModuleDAS.find(module), company.find(entity), rowId,
                level, null, null, null);
        storeEventLog(dto);
    }

    public void debug(Integer entity, Integer userAffectedId, Integer rowId,
            Integer module, Integer message, String table)   {
        log(LEVEL_DEBUG, entity, userAffectedId, rowId, module, message, table);
    }

    public void info(Integer entity, Integer userAffectedId, Integer rowId,
            Integer module, Integer message, String table) {
        log(LEVEL_INFO, entity, userAffectedId, rowId, module, message, table);
    }

    public void warning(Integer entity, Integer userAffectedId, Integer rowId,
            Integer module, Integer message, String table)   {
        log(LEVEL_WARNING, entity, userAffectedId, rowId, module, message,
                table);
    }

    public void error(Integer entity, Integer userAffectedId, Integer rowId,
            Integer module, Integer message, String table)   {
        log(LEVEL_ERROR, entity, userAffectedId, rowId, module, message, table);
    }

    public void fatal(Integer entity, Integer userAffectedId, Integer rowId,
            Integer module, Integer message, String table)   {
        log(LEVEL_FATAL, entity, userAffectedId, rowId, module, message, table);
    }

    /*
     * This is intended for loggin a change in the database, where we want to
     * keep track of what changed
     */
    public void audit(Integer userExecutingId, Integer userAffectedId,
            String table, Integer rowId, Integer module, Integer message,
            Integer oldInt, String oldStr, Date oldDate) {

        UserDAS user= new UserDAS();

        EventLogDTO dto = new EventLogDTO(null, jbDAS.findByName(table),
                user.find(userExecutingId), (userAffectedId == null) ? null : user.find(userAffectedId),
                eventLogMessageDAS.find(message), eventLogModuleDAS.find(module),
                userExecutingId == null ? null : user.find(userExecutingId).getCompany(), rowId, LEVEL_INFO, oldInt, oldStr, oldDate);
        storeEventLog(dto);
    }


    /*
     * Same as previous but the change its not being done by any given user
     * (no executor) but by a batch process.
     */
    public void auditBySystem(Integer entityId, Integer userAffectedId,
            String table, Integer rowId, Integer module, Integer message,
            Integer oldInt, String oldStr, Date oldDate) {
        CompanyDAS company = new CompanyDAS();
        UserDAS user= new UserDAS();
        EventLogDTO dto = new EventLogDTO(null, jbDAS.findByName(table), null,
                user.find(userAffectedId), eventLogMessageDAS.find(message),
                eventLogModuleDAS.find(module), company.find(entityId), rowId,
                LEVEL_INFO, oldInt, oldStr, oldDate);
        storeEventLog(dto);

    }

    /**
     * Queries the event_log table to determine the position where the last query
     * of the user transitions ended. This is called if the user passes
     * <code>null</code> as the <code>from</code> parameter to the getUserTransitions
     * webservice call.
     * @return the id of the last queried transitions list.
     */
    public Integer getLastTransitionEvent(Integer entityId)  {
        return eventLogDAS.getLastTransitionEvent(entityId);
    }

    private void storeEventLog(EventLogDTO eventDto) {
        String storeInDB = Util.getSysProp("save.eventLogs.in.database");
        boolean saveInDB = Boolean.parseBoolean(storeInDB);
        if (saveInDB) {
            eventLogDAS.save(eventDto);
        }
        EventLogRepresentation eventLog = new EventLogRepresentation(eventDto);
        String logMsg = eventLog.toString();
        LOG.info(logMsg);
    }

    public void auditLog(Integer userExecutingId, Integer userAffectedId,
                         String table, Integer rowId, Integer module, Integer message,
                         Integer oldInt, String oldStr, Date oldDate) {

        UserDAS user = new UserDAS();
        AssetDAS asset = new AssetDAS();

        EventLogDTO dto = new EventLogDTO(null, jbDAS.findByName(table),
                user.find(userExecutingId), (userAffectedId == null) ? null : user.find(userAffectedId),
                eventLogMessageDAS.find(message), eventLogModuleDAS.find(module),
                userExecutingId == null ? null : user.find(userExecutingId).getCompany(),
                rowId, LEVEL_INFO, oldInt, oldStr, oldDate, (userAffectedId == null) ? null : asset.findSubscriberNumberByUserId(userAffectedId));
        storeEventLog(dto);
    }

}
