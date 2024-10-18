package com.sapienter.jbilling.server.pricing;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.pricing.PricingBeanMessage.Action;
import com.sapienter.jbilling.server.util.Context;


public enum PricingBeanRegisterType {


    RATE_CARD_BEAN {

        @Override
        public void registerBean(Integer rateCardId, Action action) {
            IMethodTransactionalWrapper txEnabledAction = getTxEnabledActionWrapper();
            if(txEnabledAction == null) {
                return;
            }

            logger.debug("Registering Rate Card {} in ApplicationContext", rateCardId);
            txEnabledAction.execute(() -> {
                RateCardBL rateCardBL = new RateCardBL(rateCardId);
                if(action.equals(Action.UPDATE) ||
                        action.equals(Action.REMOVE)) {
                    rateCardBL.removeSpringBeans();
                }

                if(action.equals(Action.CREATE) ||
                        action.equals(Action.UPDATE)) {
                    rateCardBL.registerSpringBeans();
                }
            });
        }
    }, ROUTE_BEAN {

        @Override
        public void registerBean(Integer routeId, Action action) {
            IMethodTransactionalWrapper txEnabledAction = getTxEnabledActionWrapper();
            if(txEnabledAction == null) {
                return;
            }
            logger.debug("Registering Route {} in ApplicationContext", routeId);
            txEnabledAction.execute(()-> {
                RouteBL routeBL = new RouteBL(routeId);
                if(action.equals(Action.UPDATE) ||
                        action.equals(Action.REMOVE)) {
                    routeBL.removeSpringBeans();
                }

                if(action.equals(Action.CREATE) ||
                        action.equals(Action.UPDATE)) {
                    routeBL.registerSpringBeans();
                }
            });
        }
    }, ROUTE_BAESD_RATE_CARD_BEAN {

        @Override
        public void registerBean(Integer routeId, Action action) {
            IMethodTransactionalWrapper txEnabledAction = getTxEnabledActionWrapper();
            if(txEnabledAction == null) {
                return;
            }
            logger.debug("Registering Route Based Rate Card in ApplicationContext", routeId);
            txEnabledAction.execute(()-> {
                RouteBasedRateCardBL routeBasedRateCardBL = new RouteBasedRateCardBL(routeId);
                if(action.equals(Action.UPDATE) ||
                        action.equals(Action.REMOVE)) {
                    routeBasedRateCardBL.removeSpringBeans();
                }

                if(action.equals(Action.CREATE) ||
                        action.equals(Action.UPDATE)) {
                    routeBasedRateCardBL.registerSpringBeans();
                }

            });
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static IMethodTransactionalWrapper getTxEnabledActionWrapper() {
        if( Context.getApplicationContext()!=null ) {
            return Context.getBean(IMethodTransactionalWrapper.class);
        }
        return null;
    }

    public abstract void registerBean(Integer id, Action action);
}
