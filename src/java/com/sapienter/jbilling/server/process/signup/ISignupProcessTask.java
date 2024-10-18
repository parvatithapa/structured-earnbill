package com.sapienter.jbilling.server.process.signup;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public interface ISignupProcessTask {
    /**
     * method performs various action based on plugin class implements this interface
     * @param signupPlaceHolder
     */
    void processSignupRequest(SignupPlaceHolder signupPlaceHolder);

    /**
     * validates {@link SignupRequestWS}
     * @param signupPlaceHolder
     */
    void validateSignupRequest(SignupPlaceHolder signupPlaceHolder);
}
