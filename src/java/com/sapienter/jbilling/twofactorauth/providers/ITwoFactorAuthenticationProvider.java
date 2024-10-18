package com.sapienter.jbilling.twofactorauth.providers;

import java.util.List;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.twofactorauth.TwoFactorMethod;

public interface ITwoFactorAuthenticationProvider {
	TwoFactorResponse generateOTP(TwoFactorRequest twoFactorRequest) throws PluggableTaskException;
	TwoFactorVerificationResponse verifyOTP(TwoFactorVerificationRequest twoVerificationRequest) throws PluggableTaskException;
	List<TwoFactorMethod> supportedMethods();
}
