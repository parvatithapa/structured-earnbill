package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.*;

public class ConcurrentCall {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentCall.class);
    //private final int THREADS = Runtime.getRuntime().availableProcessors();
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    public void invokeApi (Runner runner) throws  Exception{
        executor.execute(runner);
    }

    public void waitToFinish(){
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

    static class Runner implements Runnable {
        private JbillingAPI API;
        private UserWS user;
        private ItemDTOEx item;

        public Runner(JbillingAPI API, UserWS user, ItemDTOEx item){
            this.API = API;
            this.user = user;
            this.item = item;
        }

        public void run(){

            BaseDiameterTest baseDiameterTest = new BaseDiameterTest();
            String sessionId = baseDiameterTest.getUniqueSessionId();

            logger.debug("START {} : {}", sessionId, new Date().getTime());

            PricingField[] data = baseDiameterTest.createData("realm", user.getUserName(), "sip:jeanie.jen@crocodiletalk.com", item.getNumber());


            API.createSession(sessionId, new Date(), new BigDecimal("10"), data);


            API.extendSession(sessionId, new Date(), new BigDecimal("10"), new BigDecimal("10"));

            API.extendSession(sessionId, new Date(), new BigDecimal("10"), new BigDecimal("10"));


            API.endSession(sessionId, new Date(), new BigDecimal("10"), 0);

            logger.debug("END {} : {}", sessionId, new Date().getTime());

        }

    }
}
