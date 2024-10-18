package com.sapienter.jbilling.server.timezone;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * Created by pablo_galera on 28/09/16.
 */
//Used anonymous class because TimezoneHelper called from many jasper reports and
//which are not able to compile lambda expression.
//Added Transaction behavior to avoid connection leak. Class was being called from many scheduled task
//so to avoid connection leak modified class.
public class TimezoneHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TimezoneHelper(){

    }

    public static Map<String, String> getAvailableTimezones() {
        Set<String> allZones = ZoneId.getAvailableZoneIds();
        LocalDateTime dt = LocalDateTime.now();

        Map<String, String> timezoneMap = new TreeMap<>();

        for (String zoneString : allZones) {
            ZoneId zone = ZoneId.of(zoneString);
            ZonedDateTime zdt = dt.atZone(zone);
            ZoneOffset offset = zdt.getOffset();
            timezoneMap.put(zoneString, "(GMT" + (offset.getTotalSeconds() == 0 ? "" : offset) + ") " + zoneString);
        }
        return timezoneMap;
    }

    public static Date currentDateForTimezone(String timezone) {
        return convertToTimezone(new Date(), timezone);
    }

    public static Date companyCurrentDate(Integer entityId) {
        if (entityId == null || Context.getApplicationContext() == null) {
            return new Date();
        }
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        Callable<Date> action = new Callable<Date>() {
            @Override
            public Date call() throws Exception {
                return currentDateForTimezone(new CompanyDAS().find(entityId).getTimezone());
            }
        };
        return txAction.execute(action);
    }

    public static Date convertToTimezone(Date date, String timezone) {
        if (date == null) {
            return null;
        }
        return Date.from(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of(timezone)).toLocalDateTime().atZone(ZoneId.of(Constants.DEFAULT_TIMEZONE)).toInstant());
    }

    public static Date companyCurrentDate(CompanyDTO company) {
        if (company == null) {
            return new Date();
        }
        return currentDateForTimezone(company.getTimezone());
    }

    public static LocalDateTime companyCurrentLDT(Integer entityId) {
        if (entityId == null || Context.getApplicationContext() == null) {
            return LocalDateTime.now();
        }
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        Callable<LocalDateTime> action = new Callable<LocalDateTime>() {
            @Override
            public LocalDateTime call() throws Exception {
                return convertToTimezone(LocalDateTime.now(), new CompanyDAS().find(entityId).getTimezone());
            }
        };
        return txAction.execute(action);
    }

    public static Date companyCurrentDatePlusTemporalUnit(Integer entityId, long amountToAdd, TemporalUnit unit) {
        if (entityId == null) {
            return DateConvertUtils.asUtilDate(LocalDateTime.now().plus(amountToAdd, unit).toLocalDate());
        }
        return DateConvertUtils.asUtilDate(companyCurrentLDT(entityId).plus(amountToAdd, unit).toLocalDate());
    }

    public static Date serverCurrentDate() {
        return new Date();
    }

    public static LocalDateTime companyCurrentLDTByUserId(Integer userId) {
        if (userId == null || Context.getApplicationContext() == null) {
            return LocalDateTime.now();
        }
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        Callable<LocalDateTime> action = new Callable<LocalDateTime>() {
            @Override
            public LocalDateTime call() throws Exception {
                return companyCurrentLDT(new UserDAS().getUserCompanyId(userId));
            }
        };
        return txAction.execute(action);
    }

    public static Date companyCurrentDateByUserId(Integer userId) {
        if (userId == null || Context.getApplicationContext() == null) {
            return new Date();
        }
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        Callable<Date> action = new Callable<Date>() {
            @Override
            public Date call() throws Exception {
                return companyCurrentDate(new UserDAS().getUserCompanyId(userId));
            }
        };
        return txAction.execute(action);
    }

    public static LocalDateTime convertToTimezone(LocalDateTime date, String timezone) {
        return date.atZone(ZoneId.of(Constants.DEFAULT_TIMEZONE)).toInstant().atZone(ZoneId.of(timezone)).toLocalDateTime();
    }

    public static Date convertToTimezoneAsUtilDate(LocalDateTime date, String timezone) {
        return Date.from(convertToTimezone(date, timezone).atZone(ZoneId.of(Constants.DEFAULT_TIMEZONE)).toInstant());
    }

    public static Date convertToTimezoneByEntityId(Date date, Integer entityId) {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        Callable<Date> action = new Callable<Date>() {
            @Override
            public Date call() throws Exception {
                if (date == null || entityId == null) {
                    return null;
                }
                return convertToTimezone(date, new CompanyDAS().find(entityId).getTimezone());
            }
        };
        return txAction.execute(action);
    }

    public static String getCompanyLevelTimeZone(Integer entityId) {
        Assert.notNull(entityId, "Please Provide entity id!");
        CompanyDTO entity = new CompanyDAS().find(entityId);
        String timeZone = entity.getTimezone();
        logger.debug("Reterived timezone {} for entity {}", timeZone, entityId);
        return timeZone;
    }

    public static String getOffsetFromTimeZone(String zoneString) {
        String defaultOffSet = "+0000";
        String colon = ":";
        try {
            ZoneId zone = ZoneId.of(zoneString);
            LocalDateTime dt = LocalDateTime.now();
            ZonedDateTime zdt = dt.atZone(zone);
            ZoneOffset offset = zdt.getOffset();
            String offsetString = offset.toString();
            logger.debug("timezone found {} and offset is {}", zoneString, offset);
            if (offsetString.contains(colon)) {
                    logger.debug("returning offset value {}", offsetString);
                    return offsetString.replace(colon, "");
            }
        } catch (Exception e) {
            logger.error("unknown timezone found {} setting default offset {}", zoneString, defaultOffSet, e);
        }
        logger.debug("returning default offset value {}", defaultOffSet);
        return defaultOffSet;
    }
}