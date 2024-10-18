package com.sapienter.jbilling.server.order;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.resources.OrderMetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
public class OrderResourceHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;

    /**
     * Updates Order level meta fields
     * @param orderMetaFieldValueWS
     * @return
     */
    public OrderMetaFieldValueWS updateOrderMetaFields(OrderMetaFieldValueWS orderMetaFieldValueWS) {
        MetaFieldValueWS[] metaFieldValues = MetaFieldHelper.convertAndValidateMFNameAndValueMapToMetaFieldValueWS(api.getCallerCompanyId(),
                EntityType.ORDER, orderMetaFieldValueWS.getMetaFieldValues());
        logger.debug("request meta fields {} converted to {}", orderMetaFieldValueWS.getMetaFieldValues(), metaFieldValues);
        Integer orderId = orderMetaFieldValueWS.getOrderId();
        api.updateOrderMetaFields(orderId, metaFieldValues);
        logger.debug("{} updated on order {}", metaFieldValues, orderId);
        return api.getOrderMetaFieldValueWS(orderId);
    }
}
