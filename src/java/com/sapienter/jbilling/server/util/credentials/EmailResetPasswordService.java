package com.sapienter.jbilling.server.util.credentials;

import grails.util.Holders;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDAS;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.PreferenceBL;

/**
 * Service Class for reset users passwords
 *
 * @author Javier Rivero
 * @since 14/04/15.
 */
public class EmailResetPasswordService implements PasswordService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Here we will have the initial credentials for a user be created
     *
     * @param user
     */
    @Override
    public void createPassword(UserDTO user) {
        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS();

        ResetPasswordCodeDTO resetCode = new ResetPasswordCodeDTO();
        resetCode.setUser(user);
        resetCode.setNewPassword("123qwe");
        user.setPassword("$2a$10$gNwEehQuLk2dw5Sao.uIfeJ06HJSsG0aDvr63fpwKGEm4Di5neE1y");
        resetCode.setDateCreated(TimezoneHelper.serverCurrentDate());
        resetCode.setToken(RandomStringUtils.random(32, true, true));
        resetCodeDAS.save(resetCode);

        try {
            new UserBL().sendCredentials(user.getCompany().getId(), user.getId(),user.getLanguage().getId(),generateLink(resetCode.getToken()));
        } catch (SessionInternalError e) {
            logger.error(e.getMessage(), e);
            throw new SessionInternalError("Exception while sending notification : " + e.getMessage());
        } catch (NotificationNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new SessionInternalError("createCredentials.notification.not.found");
        }

    }

    /**
     * This method sends an email to the given user with the link to reset his password
     *
     * @param user the user
     */
    @Override
    public void resetPassword(UserDTO user) {
        try {
            ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS();
            //find previous passwordCode
            ResetPasswordCodeDTO resetCode = resetCodeDAS.findByUser(user);
            if (resetCode == null) {
                resetCode = new ResetPasswordCodeDTO();
                resetCode.setUser(user);
                resetCode.setDateCreated(TimezoneHelper.serverCurrentDate());
                resetCode.setToken(RandomStringUtils.random(32, true, true));
                resetCodeDAS.save(resetCode);
                resetCodeDAS.flush();
            } else  {
                DateTime dateResetCode = new DateTime(resetCode.getDateCreated());
                DateTime today = DateTime.now();
                Duration duration = new Duration(dateResetCode, today);
                Long minutesDifference = duration.getStandardMinutes();
                Long expirationMinutes = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                        CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION).longValue() * 60;
                if (minutesDifference > expirationMinutes) {
                    resetCode.setDateCreated(TimezoneHelper.serverCurrentDate());
                    resetCode.setToken(RandomStringUtils.random(32, true, true));
                    resetCodeDAS.save(resetCode);
                    resetCodeDAS.flush();
                }
            }
            new UserBL().sendLostPassword(user.getCompany().getId(), user.getId(), 1, generateLink( resetCode.getToken()));
        } catch (NotificationNotFoundException e) {
            logger.error("Exception while sending notification ", e);
            throw new SessionInternalError("forgotPassword.notification.not.found", e);
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in resetPassword", e);
            throw new SessionInternalError("Error in resetPassword", e);
        }
    }

    /**
     * Helper method to create the link
     * @param token the token for the link
     * @return the link for the email
     */
    private String generateLink(String token) {

        String baseLink = (String) Holders.getFlatConfig().get("grails.serverURL");
        UriComponents uriComponents = UriComponentsBuilder
                .fromHttpUrl(baseLink + "/resetPassword/changePassword")
                .queryParam("token", token)
                .build()
                .encode();

        return uriComponents.toUri().toString();
    }
}

