package jbilling

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.LastPasswordOverrideError
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.saml.SamlUtil
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.security.JBCrypto
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.*
import com.sapienter.jbilling.server.user.db.*
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.joda.time.Duration
import org.springframework.security.saml.SAMLEntryPoint
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.apache.commons.lang.StringUtils;
import javax.validation.ConstraintViolationException

import com.megatome.grails.RecaptchaService
class ResetPasswordController {


    RecaptchaService recaptchaService
    IWebServicesSessionBean webServicesSession
    def userService

    def index (){
        //set default company
        /*if(!params.company){
            def list = CompanyDTO.list();
            params.company = null != list && list.size() > 0 ? list.get(0).getId() : null;
        }*/
        def errorMsg = chainModel?.errorMsg
        def companyId = params.companyId

            // Check if the companyId is integer when fetched from params
            if(companyId) {
                companyId = params.int('companyId')
                if(!companyId) {
                    flash.error = message(code: 'login.company.id.type.error')
                }
            }
        if(errorMsg){
            flash.error = errorMsg
        }
        [captchaEnabled: Boolean.parseBoolean(Util.getSysProp('forgot.password.captcha')),
         useEmail: doUseEmail(companyId) , params: params, companyId: companyId
        ]
    }

    @RequiresValidFormToken
    def captcha () {
        def companyId
        if (recaptchaService.verifyAnswer(session, request.remoteAddr, params)) {
            //find user
            UserDTO user
			UserBL userBl = new UserBL();
            def enteredUsername = params.userName
            if (grailsApplication.config.useUniqueLoginName) {
                companyId = ControllerUtil.fetchCompanyIdFromUserName(enteredUsername)
            } else {
                if (!params.companyId?.isInteger()) {
                    flash.error = message(code: 'forgotpassword.user.companyid.should.integer')
                    render view: 'index', model: [email: params.email, userName: params.userName, companyId: companyId]
                    companyId = null
                    return
                }else {
                    companyId = params.companyId as Integer
                }
            }
            if (companyId == null) {
                flash.error = message(code: 'forgotpassword.user.companyid.null')
                render view: 'index', model: [email: params.email, userName: params.userName, companyId: companyId]
                return
            }
            if (StringUtils.isBlank(companyId.toString())) {
				flash.error = message(code: 'forgotpassword.user.companyid.not.blank')
                render view: 'index', model: [email: params.email, userName: params.userName, companyId: companyId]
                return
			}
			if(!params.email?.trim() && !params.userName?.trim()){
				flash.error = message(code: 'forgotpassword.user.not.blank')
                render view: 'index', model: [email: params.email, userName: params.userName, companyId: companyId]
                companyId = null
				return
			}
			if(params.email?.trim() && params.userName?.trim()){
                user = userBl.findUsersByEmailAndUserName(params.email.trim(), params.userName.trim(), companyId)
                if(!user){
                    //check if the user has email as metaField into the account Type
                    user = userBl.findUsersByUserName(params.userName.trim(), companyId)
                    boolean hasEmail = user?.getCustomer()?.getCustomerAccountInfoTypeMetaFields()?.any {
                                            it.metaFieldValue.field.fieldUsage.equals(MetaFieldType.EMAIL) &&
                                            it.metaFieldValue.value.equals(params.email)
                                       }

                    if (!hasEmail) {
                        flash.error = message(code: 'forgotpassword.user.not.found')
                        render view: 'index', model: [email: params.email, userName: params.userName, companyId: companyId]
                        companyId = null
                        return
                    }
				}
			}else if(params.userName?.trim()) {
                user = userBl.findUsersByUserName(params.userName.trim(), companyId)
                if(null == user){
					flash.error = message(code: 'forgotpassword.user.not.found')
                    render view: 'index', model: [userName: params.userName.trim(), companyId: companyId]
                    companyId = null
					return
				}
			}else if(params.email?.trim()) {
                List<UserDTO> users = userBl.findUsersByEmail(params.email.trim(), companyId);
                if(users?.size() > 1){
					flash.error = message(code: 'forgotpassword.many.user.found', args:[params.email] )
                    render view: 'index', model: [email: params.email.trim(), companyId: companyId]
                    companyId = null
					return

				}else if (!users){
					flash.error = message(code: 'forgotpassword.user.not.found')
                    render view: 'index', model: [email: params.email.trim(), companyId: companyId]
                    companyId = null
					return
				}else{
					user = users.get(0);
				}
			}
            if (!user?.id) {
                flash.message = message(code: 'forgotPassword.email.sent')
                companyId = null
                forward controller:'login', action: 'auth'
                return
            }

            if (null != user) {
                boolean isUserSSOEnabled = SamlUtil.getUserSSOEnabledStatus(user.getId())
                def ssoActive = PreferenceBL.getPreferenceValue(companyId as int, CommonConstants.PREFERENCE_SSO) as int
                if (isUserSSOEnabled && ssoActive) {
                    def url = SamlUtil.getDefaultResetPasswordUrl(companyId)
                    if(null != url && !url.isEmpty()) {
                        def defaultIdp = SamlUtil.getDefaultIdpUrl(companyId)
                        if(null != defaultIdp && !defaultIdp.isEmpty()) {
                            url += "?" + SAMLEntryPoint.IDP_PARAMETER + "=" + defaultIdp
                            log.debug("Final redirect url : " + url)
                            redirect(url: url)
                        } else {
                            log.error("No default Idp is configured for company : " + companyId.toString())
                            flash.error = message(code: 'default.idp.error')
                        }
                    } else {
                        log.error("No default reset password url of Idp is configured for company : " + companyId.toString())
                        flash.error = message(code: 'default.reset.password.url.idp.error')
                    }
                } else {
                    try{
                        //send email to reset password
                        webServicesSession.resetPassword(user.userId)
                        recaptchaService.cleanUp(session)
                    } catch (SessionInternalError e) {
                        flash.error = message(code: 'forgotPassword.notification.not.found')
                        companyId = null
                        forward action: 'index'
                        return
                    }
                }
            }
        } else {
            flash.error = message(code: 'forgotPassword.captcha.wrong')
            companyId = null
            forward action: 'index'
            return
        }
        flash.message = message(code: 'forgotPassword.email.sent')
        forward controller:'login', action: 'auth'
    }

    def changePassword (){
        ResetPasswordCodeDTO resetCode = new ResetPasswordCodeDAS().findByToken(params.token)

        if (resetCode && resetCode?.user?.deleted == 0) {
            DateTime dateResetCode = new DateTime(resetCode.getDateCreated())
            DateTime today = DateTime.now()
            Duration duration = new Duration(dateResetCode, today)
            Long minutesDifference = duration.getStandardMinutes()
            Long expirationMinutes = PreferenceBL.getPreferenceValueAsIntegerOrZero(resetCode.user.company.id,
                    CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION).longValue() * 60
            if (minutesDifference > expirationMinutes) {
                flash.error = message(code: 'forgotPassword.expired.token')
                forward controller: 'login', action: 'auth'
            } else {
                render view: 'changePassword', model: [token: params.token]
            }
        } else {
            flash.error = message(code: 'forgotPassword.expired.token')
            forward controller: 'login', action: 'auth'
        }
    }

    @RequiresValidFormToken
    def updatePassword () {
        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS()
        try{
            String newPassword = params.newPassword.trim()
            String confirmedNewPassword = params.confirmedNewPassword.trim()

            // password validation
            if (newPassword.length() == 0 || confirmedNewPassword.length() == 0 ) {
                flash.error = message(code: 'password.required')
                render view: 'changePassword', model: [ token: params.token ]
                return
            }

            if (newPassword.length() != confirmedNewPassword.length() || !newPassword.equals(confirmedNewPassword) ) {
                flash.error = message(code: 'passwords.dont.match')
                render view: 'changePassword', model: [ token: params.token ]
                return
            }

            if (newPassword.length() < 8 || newPassword.length() > 40 || !newPassword.matches(Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES) ) {
                flash.error = message(code: 'validation.error.password.size')
                flash.args = [ 8, 40]
                render view: 'changePassword', model: [ token: params.token ]
                return
            }
            ResetPasswordCodeDTO resetCode =  resetCodeDAS.findByToken(params.token)
            userService.updatePassword(resetCode, newPassword)
            flash.message = message(code: 'forgotPassword.success')
            forward controller:'login', action: 'auth'

        } catch(ObjectNotFoundException e){
            flash.error= message(code: 'validation.error.password.object.not.found')
            redirect controller:'login', action: 'auth'
        }
		  catch(LastPasswordOverrideError passEx){
			  flash.error = message(code: 'forgotPassword.user.password.last.six.unique')
			  forward controller:'resetPassword', action: 'changePassword'
		  }
		catch(Exception ex) {
			log.error("Exception occurred during reset password." + ex)
            flash.error= message(code: 'forgotPassword.failure')
            forward controller:'login', action: 'auth'
        }
    }

    private boolean doUseEmail(Integer companyId){
        def uniqueEmails = PreferenceBL.getPreferenceValueAsIntegerOrZero(
        					companyId, CommonConstants.PREFERENCE_FORCE_UNIQUE_EMAILS)
        (uniqueEmails == 1) ? true : false
    }

    private String generateLink(String action, linkParams) {
        createLink(base: "$request.scheme://$request.serverName:$request.serverPort$request.contextPath",
                controller: 'resetPassword', action: action,
                params: linkParams)

    }

    def resetExpiryPassword () {
        //set default company
		def request= RequestContextHolder?.currentRequestAttributes()
		def forwordEntityId = request.getAttribute("login_company",RequestAttributes.SCOPE_SESSION)
        [forwordEntityId:forwordEntityId as Integer
        ]
    }

    @RequiresValidFormToken
    def resetPassword () {
        boolean result = true
        if (!recaptchaService.verifyAnswer(session, request.remoteAddr, params)) {
            result = false
        }

        String newPassword = params.newPassword
        String oldPassword = params.oldPassword
        if (result) {
            //find user
            UserDTO user = new UserDAS().findByUserName(params.username, params.int('company'))

			if(!user) {
				flash.error = message(code: 'forgotPassword.user.username.not.exist', args: [params?.username] )
				forward action: 'resetExpiryPassword'
				return
			}
            UserWS userWS = new UserBL(user.id).getUserWS()
            Integer methodId = JBCrypto.getPasswordEncoderId(userWS.getMainRoleId());
            if (!user?.id || !JBCrypto.passwordsMatch(methodId, user.password, oldPassword)) {
                flash.error = message(code: 'forgotPassword.user.username.not.found')
                forward action: 'resetExpiryPassword'
                return
            }
            if(!newPassword.equals(params.confirmPassword)){
                flash.error = message(code: 'forgotPassword.user.password.not.match')
                forward action: 'resetExpiryPassword'
                return
            }
            if ( !newPassword.matches(com.sapienter.jbilling.server.util.Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES) ) {
                flash.error = message(code: 'validation.error.password.size', args: [8,40])
                forward action: 'resetExpiryPassword'
                return
            }

            UserPasswordDAS resetCodeDAS = new UserPasswordDAS()
            IUserSessionBean myRemoteSession = (IUserSessionBean) Context.getBean(
                    Context.Name.USER_SESSION)

            // do the actual password change
            UserDTOEx userDTOEx = new UserDTOEx(userWS, user?.entity?.id);
            RoleDTO role = new RoleBL().findByTypeOrId(userDTOEx.mainRoleId, userDTOEx.entityId)
            List<String> passwords = resetCodeDAS.findLastSixPasswords(user,newPassword)
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(role.roleTypeId);
            for(String password: passwords){
                if(JBCrypto.passwordsMatch(passwordEncoderId, user.getPassword(), newPassword)){
                    flash.error = message(code: 'forgotPassword.user.password.last.six.unique')
                    forward action: 'resetExpiryPassword'
                    return
                }
            }
            try {
                //create a new password code
                UserPasswordDTO resetCode = new UserPasswordDTO();
                resetCode.setUser(user);
                resetCode.setDateCreated(TimezoneHelper.serverCurrentDate());
                resetCode.setPassword(newPassword,role.roleTypeId);
                resetCode.setPassword(newPassword)
                resetCodeDAS.save(resetCode);

                userDTOEx.setPassword(newPassword);
                myRemoteSession.update(userDTOEx.id, userDTOEx)

                flash.message = message(code: 'forgotPassword.success')
                forward controller:'login', action: 'auth'
            }catch (ConstraintViolationException e){
                flash.error = message(code: 'validation.error.password.size')
                flash.args = [ 8,40]
                log.debug(e.getMessage())
                forward action: 'resetExpiryPassword'
            }


        }else{
            flash.error = message(code: 'forgotPassword.captcha.wrong')
            forward action: 'resetExpiryPassword'
        }
    }

}
