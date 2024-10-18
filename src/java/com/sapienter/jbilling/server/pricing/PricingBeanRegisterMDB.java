package com.sapienter.jbilling.server.pricing;

import java.lang.invoke.MethodHandles;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class PricingBeanRegisterMDB implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    @Transactional
    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            PricingBeanMessage pricingBeanMessage = (PricingBeanMessage) objectMessage.getObject();
            logger.debug("Registering Bean {}", pricingBeanMessage);
            pricingBeanMessage.registerBean();
        } catch(Exception ex) {
            logger.error("Error During Registering Bean!", ex);
        }
    }

}
