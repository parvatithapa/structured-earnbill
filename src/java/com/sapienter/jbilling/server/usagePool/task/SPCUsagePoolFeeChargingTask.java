package com.sapienter.jbilling.server.usagePool.task;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineUsagePoolBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class SPCUsagePoolFeeChargingTask extends UsagePoolConsumptionFeeChargingTask {

    private static final String SERVICE_ID = "ServiceId";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected void setOrderLineServiceId(OrderLineWS line, CustomerUsagePoolDTO customerUsagePool) {
        logger.debug("setting order line service id in SPCUsagePoolFeeChargingTask for customer usage pool = {}, entity id = {}, line id = {}", customerUsagePool, getEntityId(), line.getId());
        String serviceId = "";
        String callIdentifier = "";
        OrderLineUsagePoolBL orderLineUsagePoolBL = new OrderLineUsagePoolBL();
        List<OrderLineUsagePoolDTO> orderLineUsagePools = orderLineUsagePoolBL.findByCustomerUsagePoolId(customerUsagePool.getId());
        Set<Integer> items = customerUsagePool.getUsagePool().getItems().stream().map(item -> item.getId()).collect(Collectors.toSet());
        IWebServicesSessionBean api = Context.getBean("webServicesSession");
        for (OrderLineUsagePoolDTO olUsagePoolDTO : orderLineUsagePools) {
            if (items.contains(olUsagePoolDTO.getOrderLine().getItemId())) {
                callIdentifier = olUsagePoolDTO.getOrderLine().getCallIdentifier();
                AssetWS asset = api.getAssetByIdentifier(callIdentifier);
                for(MetaFieldValueWS mf : asset.getMetaFields()) {
                    if(mf.getFieldName().equalsIgnoreCase(SERVICE_ID) && StringUtils.isNotBlank(mf.getStringValue())) {
                        serviceId = mf.getStringValue();
                        break;
                    }
                }
            }
        }
        //if service id meta field is not found on asset then call identifier from the orderline will be set
        serviceId = StringUtils.isNotBlank(serviceId) ? serviceId : callIdentifier;
        if(StringUtils.isNotBlank(serviceId)) {
            MetaFieldValueWS serviceIdMetaField = new MetaFieldValueWS();
            serviceIdMetaField.setFieldName(SERVICE_ID);
            serviceIdMetaField.setDataType(DataType.STRING);
            serviceIdMetaField.setValue(serviceId);
            serviceIdMetaField.setEntityId(getEntityId());
            line.setMetaFields(new MetaFieldValueWS[] { serviceIdMetaField });
            logger.debug("service id : {} for the order line : {} has been set", serviceId, line.getId());
        }
    }
    
    
    @Override
    protected Date getActiveSince(Date activeSince, Date nextInvoiceDate) {
    	
    	if(null != activeSince && null != nextInvoiceDate){
    		Calendar calActiveSinceDate = Calendar.getInstance();
        	Calendar calNextInvoiceDate = Calendar.getInstance();
        	calActiveSinceDate.setTime(activeSince);
        	calNextInvoiceDate.setTime(nextInvoiceDate);
        	if(calActiveSinceDate.before(calNextInvoiceDate)){
        		return activeSince;
        	}
    	}
    	return companyCurrentDate();
    }
    
}
