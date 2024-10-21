package com.sapienter.jbilling.server.pluggableTask;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.lang.invoke.MethodHandles;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;

/**
 * This plugin is designed to configure the SPC specific email parameters.
 * 
 */
public class SpcEmailNotificationTask extends BasicEmailNotificationTask {

    public static final String PARAMETER_AGL_FROM_NAME ="agl_from_name";
    public static final String PARAMETER_AGL_FROM ="agl_from";
    public static final String PARAMETER_AGL_REPLYTO ="agl_reply_to";
    public static final String PARAMETER_AGL_BCCTO ="agl_bcc_to";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);

    @Override
    protected void setCustomEmailParameters(UserDTO user, MimeMessageHelper msg) throws TaskException {

        if (spcHelperService.isAGL(user.getId(), null)) {
            // the from address
            String from = parameters.get(PARAMETER_AGL_FROM);
            if (isBlank(from)){
                throw new TaskException("Plugin level attribute "+PARAMETER_AGL_FROM+" not configured");
            }

            String fromName = parameters.get(PARAMETER_AGL_FROM_NAME);
            if (isBlank(fromName)){
                throw new TaskException("Plugin level attribute "+PARAMETER_AGL_FROM_NAME+" not configured");
            }
            try {
                msg.setFrom(new InternetAddress(from, fromName));
            } catch (Exception e1) {
                throw new TaskException("Invalid from address:" + from + " from name"+ fromName +
                        "." + e1.getMessage());
            }
            String replyTo = parameters.get(PARAMETER_AGL_REPLYTO);
            if (isBlank(replyTo)){
                throw new TaskException("Plugin level attribute not configured");
            }
            // the reply to
            try {
                msg.setReplyTo(replyTo);
            } catch (Exception e5) {
                logger.error(String.format("Exception when setting the replyTo address: %s", replyTo), e5);
            }
            // the bcc if specified
            String bcc = parameters.get(PARAMETER_AGL_BCCTO);
            if (isNotBlank(bcc)){
                try {
                    msg.setBcc(new InternetAddress(bcc, false));
                } catch (AddressException e5) {
                    logger.warn("The bcc address {} is not valid. Sending without bcc {}",bcc, e5);
                } catch (MessagingException e5) {
                        throw new TaskException("Exception setting bcc " +
                            e5.getMessage());
                }
            }
        } else {
            super.setCustomEmailParameters(user, msg);
        }
    }

}
