package com.sapienter.jbilling.twofactorauth;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.twofactorauth.providers.ITwoFactorAuthenticationProvider;
import com.sapienter.jbilling.twofactorauth.providers.TwoFactorRequest;
import com.sapienter.jbilling.twofactorauth.providers.TwoFactorResponse;
import com.sapienter.jbilling.twofactorauth.providers.TwoFactorVerificationRequest;
import com.sapienter.jbilling.twofactorauth.providers.TwoFactorVerificationResponse;

@Transactional
public class TwoFactorAuthenticationHelperService {

	private static final String TWOFACTOR_PLUGIN_INTERFACE_NAME = "com.sapienter.jbilling.twofactorauth.providers.ITwoFactorAuthenticationProvider";
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final TwoFactorResponseWS NO_OTP_RESPONSE = new TwoFactorResponseWS("N/A",
			"Otp is alreday sent to mobile, retry otp generation after 2 minutes");

	@Resource(name = "webServicesSession")
	private IWebServicesSessionBean api;
	@Resource
	private PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS;
	@Resource
	private User2FALogService user2faLogService;

	public TwoFactorResponseWS generateOtp(String sessionId, TwoFactorRequestWS twoFactorRequest) {
		try {

			if(!user2faLogService.logOtpRequestAndCheckOtpCanSendToUser(sessionId, twoFactorRequest.getId())) {
				return NO_OTP_RESPONSE;
			}
			ITwoFactorAuthenticationProvider twoFactorAuthenticationProvider = load2FAPluginForEntity(api.getCallerCompanyId());
			TwoFactorResponse twoFactorResponse = twoFactorAuthenticationProvider.generateOTP(new TwoFactorRequest(
					twoFactorRequest.getTwoFactorMethod(), twoFactorRequest.getId()));
			logger.debug("otp={} generated for id={}", twoFactorResponse.getOtp(), twoFactorRequest.getId());
			return new TwoFactorResponseWS(twoFactorResponse.getOtp(), twoFactorResponse.getJsonResponseBody());
		} catch(PluggableTaskException pluggableTaskException) {
			throw new SessionInternalError("generateOtp failed", new String[] { pluggableTaskException
					.getLocalizedMessage() }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		} catch (SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch(Exception exception) {
			logger.error("generateOtp faild for twoFactorRequest={}, error- ", twoFactorRequest, exception);
			throw new SessionInternalError("generateOtp failed for "+ twoFactorRequest.getId(),
					new String[] { exception.getLocalizedMessage() }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	public TwoFactorVerificationResponseWS verifyOtp(TwoFactorVerificationRequestWS twoFactorVerificationRequest) {
		try {
			ITwoFactorAuthenticationProvider twoFactorAuthenticationProvider = load2FAPluginForEntity(api.getCallerCompanyId());
			TwoFactorVerificationRequest verificationRequest = new TwoFactorVerificationRequest(twoFactorVerificationRequest.getId(),
					twoFactorVerificationRequest.getOtp(), twoFactorVerificationRequest.getTwoFactorMethod());
			TwoFactorVerificationResponse otpResponse = twoFactorAuthenticationProvider.verifyOTP(verificationRequest);
			return new TwoFactorVerificationResponseWS(otpResponse.isOtpMatched(), otpResponse.getResponseJson());
		} catch(PluggableTaskException pluggableTaskException) {
			throw new SessionInternalError("verifyOtp failed", new String[] { pluggableTaskException
					.getLocalizedMessage() }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		} catch (SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch(Exception exception) {
			logger.error("verifyOtp faild for twoFactorVerificationRequest={}, error- ", twoFactorVerificationRequest, exception);
			throw new SessionInternalError("verifyOtp failed for "+ twoFactorVerificationRequest.getId(),
					new String[] { exception.getLocalizedMessage() }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	public boolean is2FAEnable(Integer entityId) {
		try {
			load2FAPluginForEntity(entityId);
			return true;
		} catch(SessionInternalError sessionInternalError) {
			if(sessionInternalError.getMessage().contains("2FA plugin not configured")) {
				return false;
			}
			throw sessionInternalError;
		}
	}

	private ITwoFactorAuthenticationProvider load2FAPluginForEntity(Integer entityId) {
		try {
			int twofactorPluginTypeId = pluggableTaskTypeCategoryDAS.findByInterfaceName(TWOFACTOR_PLUGIN_INTERFACE_NAME).getId();
			PluggableTaskManager<ITwoFactorAuthenticationProvider> taskManager = new PluggableTaskManager<>(entityId, twofactorPluginTypeId);
			ITwoFactorAuthenticationProvider task = taskManager.getNextClass();
			if(null == task) {
				throw new SessionInternalError("2FA plugin not configured",new String[] { "no TwoFactorAuth plugin "
						+ "configued for entityId "+ entityId }, HttpStatus.INTERNAL_SERVER_ERROR.value());
			}
			return task;
		} catch (SessionInternalError e) {
			throw e;
		} catch (Exception e) {
			logger.error("load2FAPluginForEntity failed!", e);
			throw new SessionInternalError("load2FAPluginForEntity failed", new String[]{ "load2FAPluginForEntity "
					+ "failed for entityId "+ entityId }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}
}
