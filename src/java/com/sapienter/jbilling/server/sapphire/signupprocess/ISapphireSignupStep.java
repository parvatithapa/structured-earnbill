package com.sapienter.jbilling.server.sapphire.signupprocess;

import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;

public interface ISapphireSignupStep {
    void executeStep(SignupPlaceHolder holder);
}
