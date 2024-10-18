package com.sapienter.jbilling.log;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Created by nenad on 10/10/16.
 */
public class EnhancedLoggerFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide (ILoggingEvent loggingEvent) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CompanyUserDetails) {
                CompanyUserDetails userDetails = (CompanyUserDetails) principal;
                MDC.put("user", userDetails.getUserId().toString());
                MDC.put("company", userDetails.getCompanyId().toString());
            } else if (principal instanceof User) {
                MDC.put("user", ((User) principal).getUsername());
            }
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            MDC.put("userIp", ((ServletRequestAttributes) requestAttributes).getRequest().getRemoteAddr());
        }

        if (loggingEvent.getMessage().contains("message=\"")) {
            MDC.remove("msgKey");
            MDC.remove("quote");
        } else {
            MDC.put("msgKey", "message=");
            MDC.put("quote", "\"");
        }

        return FilterReply.NEUTRAL;
    }
}
