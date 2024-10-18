package jbilling

import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.twofactorauth.TwoFactorAuthenticationHelperService
import com.sapienter.jbilling.twofactorauth.TwoFactorMethod
import com.sapienter.jbilling.twofactorauth.TwoFactorRequestWS
import com.sapienter.jbilling.twofactorauth.TwoFactorVerificationRequestWS
import com.sapienter.jbilling.twofactorauth.TwoFactorVerificationResponseWS
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.twofactorauth.db.User2FALogDTO
import java.util.Date
import org.apache.commons.lang.StringUtils
import org.springframework.security.core.context.SecurityContextHolder

@Secured('isAuthenticated()')
class TwoFactorVerifyController {
    TwoFactorAuthenticationHelperService twoFactorAuthenticationHelperService
    IWebServicesSessionBean webServicesSession
    def twoMinInMillis = 120000

    def index() {
        redirect action: 'generate', params: params
    }

    def generate() {
        def userId = session['user_id']
        def phoneNumber
        def currentTime = new Date()
        def userWS = webServicesSession.getUserWS(userId)
        def metaFields = userWS.getMetaFields()
        for (MetaFieldValueWS metaFieldValue in metaFields) {
            MetaFieldWS metaFieldWS = metaFieldValue.getMetaField()
            if (EntityType.CUSTOMER == metaFieldWS.getEntityType() && MetaFieldType.PHONE_NUMBER == metaFieldWS.getFieldUsage()) {
                phoneNumber = metaFieldValue.getStringValue()
            }
            if (EntityType.USER == metaFieldWS.getEntityType() && MetaFieldType.PHONE_NUMBER == metaFieldWS.getFieldUsage()) {
                phoneNumber = metaFieldValue.getStringValue()
            }
        }
        if (StringUtils.isBlank(phoneNumber)) {
            flash.twoFAError = message(code: 'two.FA.message')
            // Remove user credentials as unable to perform 2fa
            SecurityContextHolder.clearContext()
            redirect controller: 'login', action: 'auth'
        } else {
            session.setAttribute("phoneNumber", phoneNumber)
            TwoFactorRequestWS twoFactorRequestWS = new TwoFactorRequestWS()
            twoFactorRequestWS.setId(phoneNumber)
            twoFactorRequestWS.setTwoFactorMethod(TwoFactorMethod.SMS)
            twoFactorAuthenticationHelperService.generateOtp(session.getId(), twoFactorRequestWS)
            def userLogDto = User2FALogDTO.findBySessionId(session.getId())
            def remainingMinutesInMillis = currentTime.getTime() - userLogDto.getTimestamp().getTime()
            def timer = twoMinInMillis - remainingMinutesInMillis
            flash.twoFAMessage = 'otp.sent.on'
            def lastTwoDigits = phoneNumber[-2..-1]
            flash.args = [lastTwoDigits as String]
            render view: 'verify', model: [phoneNumber: phoneNumber, timer: timer]
        }
    }

    def verify() {
        String phoneNumber = session.getAttribute("phoneNumber")
        def currentTime = new Date()
        def timer
        TwoFactorVerificationRequestWS verifyRequest = new TwoFactorVerificationRequestWS()
        verifyRequest.setId(phoneNumber)
        verifyRequest.setTwoFactorMethod(TwoFactorMethod.SMS)
        verifyRequest.setOtp(params.otp)
        TwoFactorVerificationResponseWS verifyOtpResponse = twoFactorAuthenticationHelperService.verifyOtp(verifyRequest)
        def userLogDto = User2FALogDTO.findBySessionId(session.getId())
        def remainingMinutesInMillis = currentTime.getTime() - userLogDto.getTimestamp().getTime()
        timer = (remainingMinutesInMillis <= twoMinInMillis) ? (twoMinInMillis - remainingMinutesInMillis) : twoMinInMillis
        if (!params.otp) {
            log.error("OTP Cannot Be Null. Please make sure you've entered the code.")
            flash.twoFAError = message(code: 'otp.error.blank')
            params.clear()
            render view: 'verify', model: [timer: timer]
        } else if (!verifyOtpResponse.isOtpMatched()) {
            log.error("Invalid OTP. Please make sure you've entered the correct code. If the issue persists, request a new OTP.")
            flash.twoFAError = message(code: 'invalid.otp')
            params.clear()
            render view: 'verify', model: [timer: timer]
        } else {
            if (verifyOtpResponse.isOtpMatched() && twoMinInMillis - remainingMinutesInMillis <= 0) {
                log.error("Your OTP has expired. A new OTP has been sent to your registered mobile number.")
                flash.twoFAError = message(code: 'expired.otp')
                params.clear()
                render view: 'verify', model: [timer: timer]
            } else {
                // 2FA done.
                session.setAttribute("2fa_done", true)
                redirect uri: SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
            }
        }
    }
}
