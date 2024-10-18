package com.sapienter.jbilling.server.timezone;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;
import grails.plugin.springsecurity.SpringSecurityService;
import org.apache.log4j.Logger;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;

/**
 * Created by pablo_galera on 01/09/16.
 */
public class WSTimezoneAdvice implements AfterReturningAdvice {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(WSTimezoneAdvice.class));

    private SpringSecurityService springSecurityService;
    private UserDAS userDAS = new UserDAS();

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

    public Integer getCallerCompanyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService().getPrincipal();
        return details.getCompanyId();
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        if (getSpringSecurityService().getPrincipal() instanceof CompanyUserDetails) {
            if (returnValue != null) {
                if (returnValue instanceof Collection) {
                    for (Object element : (Collection) returnValue) {
                        convertToTimezone(element);
                    }
                } else if (returnValue.getClass().isArray() && !isPrimitiveArray(returnValue)) {
                    for (Object element : (Object[]) returnValue) {
                        convertToTimezone(element);
                    }
                } else {
                    convertToTimezone(returnValue);
                }                
            }    
        }
    }

    private boolean isPrimitiveArray(Object obj) {
        return  obj instanceof boolean[] ||
                obj instanceof byte[] || obj instanceof short[] ||
                obj instanceof char[] || obj instanceof int[] ||
                obj instanceof long[] || obj instanceof float[] ||
                obj instanceof double[];
    }

    private void convertToTimezone(Object object) {

        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field : fields) {
            try {
                if (field.getAnnotation(ConvertToTimezone.class) instanceof ConvertToTimezone) {
                    field.setAccessible(true);
                    if (field.get(object) != null) {                        
                        field.set(object, TimezoneHelper.convertToTimezone((Date) field.get(object), new CompanyDAS().find(getCallerCompanyId()).getTimezone()));                        
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}

