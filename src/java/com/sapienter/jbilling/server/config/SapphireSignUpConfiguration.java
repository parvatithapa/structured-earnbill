package com.sapienter.jbilling.server.config;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.sapphire.signupprocess.ISapphireSignupStep;
import com.sapienter.jbilling.server.sapphire.signupprocess.InvoiceGenerationSignupStep;
import com.sapienter.jbilling.server.sapphire.signupprocess.PaymentCreationSignupStep;
import com.sapienter.jbilling.server.sapphire.signupprocess.UserAndOrderCreationSignupStep;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Configuration
public class SapphireSignUpConfiguration {

    @Bean(name = "sapphireSignupSteps")
    public List<ISapphireSignupStep> sapphireSignupSteps(@Qualifier("webServicesSession") IWebServicesSessionBean service,
            IMethodTransactionalWrapper txAction) {
        List<ISapphireSignupStep> steps = new LinkedList<>();
        steps.add(new UserAndOrderCreationSignupStep(service, txAction, true, false));
        steps.add(new PaymentCreationSignupStep(service, txAction, true, false));
        steps.add(new InvoiceGenerationSignupStep(service, txAction, true, true));
        return steps;
    }
}
