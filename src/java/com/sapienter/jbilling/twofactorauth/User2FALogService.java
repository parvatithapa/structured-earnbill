package com.sapienter.jbilling.twofactorauth;

import grails.plugin.springsecurity.SpringSecurityService;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.twofactorauth.db.User2FALogDAS;
import com.sapienter.jbilling.twofactorauth.db.User2FALogDTO;
import org.apache.http.HttpStatus;
import com.sapienter.jbilling.common.SessionInternalError;

public class User2FALogService {

    @Resource
    private User2FALogDAS user2faLogDAS;
    @Resource
    private SpringSecurityService springSecurityService;
    @Resource(name = "readWriteTx")
    private TransactionTemplate readWriteTransaction;

    private boolean isValidUUID(String sessionId) {
        try {
            UUID.fromString(sessionId);
            return true;
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    public boolean logOtpRequestAndCheckOtpCanSendToUser(String sessionId, String twoFAId) {
        try {
            CompanyUserDetails details = (CompanyUserDetails) springSecurityService.getPrincipal();
            Date currentTime = TimezoneHelper.companyCurrentDate(details.getCompanyId());
            return readWriteTransaction.execute(status -> {
                User2FALogDTO user2faLogDTO = user2faLogDAS.findBySessionIdAndUserId(sessionId, details.getUserId());
                if (null == user2faLogDTO && isValidUUID(sessionId)) {
                    user2faLogDTO = user2faLogDAS.findByUserId(details.getUserId());
                }
                if (null != user2faLogDTO) {
                    long remainingMinutesInMillis = currentTime.getTime() - user2faLogDTO.getTimestamp().getTime();
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMinutesInMillis);
                    if (minutes < 2) {
                        return false;
                    } else {
                        user2faLogDTO.setTimestamp(currentTime);
                        return true;
                    }
                } else {
                    user2faLogDTO = new User2FALogDTO();
                    user2faLogDTO.setSessionId(sessionId);
                    user2faLogDTO.setTwoFAId(twoFAId);
                    user2faLogDTO.setEntityId(details.getCompanyId());
                    user2faLogDTO.setTimestamp(currentTime);
                    user2faLogDTO.setUserId(details.getUserId());
                    user2faLogDAS.save(user2faLogDTO);
                    return true;
                }
            });
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (dataIntegrityViolationException.getMessage().contains("user_2fa_log_session_id_user_id_un")) {
                return false;
            }
            throw dataIntegrityViolationException;
        }
    }
}