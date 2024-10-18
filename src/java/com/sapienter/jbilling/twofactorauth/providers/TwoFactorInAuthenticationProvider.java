package com.sapienter.jbilling.twofactorauth.providers;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.twofactorauth.TwoFactorAuthException;
import com.sapienter.jbilling.twofactorauth.TwoFactorMethod;

public class TwoFactorInAuthenticationProvider extends PluggableTask implements ITwoFactorAuthenticationProvider {

	private static final String REST_TEMPLATE_CLIENT_NAME = "twoFactorRestClient";

	private enum OtpAction {

		GENERATE("AUTOGEN2"),
		VERIFY("VERIFY3");

		private final String actionType;
		private OtpAction(String actionType) {
			this.actionType = actionType;
		}

		public String getActionType() {
			return actionType;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String DEFAULT_TEMPLATE = "OTP1";

	private static final ParameterDescription PARAM_BASE_URL =
			new ParameterDescription("base_url", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_API_KEY =
			new ParameterDescription("api_key", true, ParameterDescription.Type.STR);

	private static final ParameterDescription PARAM_OTP_TEMPLATE_NAME =
			new ParameterDescription("otp_template_name", false, ParameterDescription.Type.STR);


	public TwoFactorInAuthenticationProvider() {
		descriptions.add(PARAM_BASE_URL);
		descriptions.add(PARAM_API_KEY);
		descriptions.add(PARAM_OTP_TEMPLATE_NAME);
	}

	private static final List<TwoFactorMethod> SUPPORTED_METHODS = Arrays.asList(TwoFactorMethod.SMS);

	@Override
	public List<TwoFactorMethod> supportedMethods() {
		return SUPPORTED_METHODS;
	}

	@Override
	public TwoFactorResponse generateOTP(TwoFactorRequest twoFactorRequest) throws PluggableTaskException {
		try {
			Map<String, String> otpResponse = makeApiCall(twoFactorRequest.getTwoFactorMethod(),
					twoFactorRequest.getId(), null, OtpAction.GENERATE);
			String status = otpResponse.get("Status");
			if(status.equalsIgnoreCase("Error")) {
				throw new PluggableTaskException(otpResponse.get("Details"));
			}
			return new TwoFactorResponse(otpResponse.get("OTP"), otpResponse.toString());
		} catch(TwoFactorAuthException twoFactorAuthException) {
			logger.error("otp generation failed for phoneNumber={}, error-", twoFactorRequest.getId(), twoFactorAuthException);
			throw new PluggableTaskException(twoFactorAuthException.getLocalizedMessage());
		}
	}

	@Override
	public TwoFactorVerificationResponse verifyOTP(TwoFactorVerificationRequest twoVerificationRequest) throws PluggableTaskException {
		try {
			Map<String, String> verificationResponse = makeApiCall(twoVerificationRequest.getTwoFactorMethod(),
					twoVerificationRequest.getId(), twoVerificationRequest.getOtp(), OtpAction.VERIFY);
			String status = verificationResponse.get("Details");
			return new TwoFactorVerificationResponse(status.equalsIgnoreCase("OTP Matched"), verificationResponse.toString());
		} catch(TwoFactorAuthException twoFactorAuthException) {
			logger.error("verifyOTP failed for phoneNumber={}, error-", twoVerificationRequest.getId(), twoFactorAuthException);
			throw new PluggableTaskException(twoFactorAuthException.getLocalizedMessage());
		}
	}

	@Override
	public void afterSetup() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		HttpSession session = attributes.getRequest().getSession();
		session.setAttribute("is2FAConfiguredInCurrentSession", true);
	}

	private RestTemplate restTemplate() {
		return Context.getBean(REST_TEMPLATE_CLIENT_NAME);
	}

	private String generateOTPRequestUrl(String phoneNumber, TwoFactorMethod twoFactorMethod, OtpAction otpAction, String otp) throws PluggableTaskException {
		String baseUrl = getMandatoryStringParameter(PARAM_BASE_URL.getName());
		if(!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + '/';
		}
		StringBuilder urlBuilder = new StringBuilder().append(baseUrl)
				.append(getMandatoryStringParameter(PARAM_API_KEY.getName()) + "/")
				.append(twoFactorMethod.name() + "/");

		if(otpAction.equals(OtpAction.GENERATE)) {
			urlBuilder.append(phoneNumber + "/")
			.append(otpAction.getActionType() + "/")
			.append(getParameter(PARAM_OTP_TEMPLATE_NAME.getName(), DEFAULT_TEMPLATE));
		} else if(otpAction.equals(OtpAction.VERIFY)) {
			urlBuilder.append(otpAction.getActionType() + "/")
			.append(phoneNumber + "/").append(otp);
		}
		return urlBuilder.toString();
	}

	private Map<String, String> makeApiCall(TwoFactorMethod twoFactorMethod, String id, String otp, OtpAction otpAction) throws PluggableTaskException {
		if(!SUPPORTED_METHODS.contains(twoFactorMethod)) {
			throw new PluggableTaskException("TwoFactorInAuthenticationProvider done not support "
					+ "otp generation using "+ twoFactorMethod + "method");
		}
		String otpRequestUrl = generateOTPRequestUrl(id, twoFactorMethod, otpAction, otp);
		logger.debug("otpRequestUrl={}, with otp method={}, phoneNumber={}", otpRequestUrl, twoFactorMethod, id);
		return restTemplate().getForEntity(otpRequestUrl, HashMap.class).getBody();
	}
}
