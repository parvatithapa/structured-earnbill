package com.sapienter.jbilling.paymentUrl.db;

import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.Criteria;
import org.json.JSONObject;


import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class PaymentUrlLogDAS extends AbstractDAS<PaymentUrlLogDTO> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentUrlLogDAS.class));

    public PaymentUrlLogDTO findByInvoiceId(Integer invoiceId) {
        Criteria criteria = getSession().createCriteria(PaymentUrlLogDTO.class)
                .add(Restrictions.eq("invoiceId", invoiceId))
                .addOrder(Order.desc("createdAt"));
        List<PaymentUrlLogDTO> paymentUrlLogDTOS= criteria.list();
        return paymentUrlLogDTOS.isEmpty() ? null : paymentUrlLogDTOS.get(0);
    }

    public List<PaymentUrlLogDTO> findAllByInvoiceId(Integer invoiceId) {
        return getSession().createCriteria(PaymentUrlLogDTO.class)
                .add(Restrictions.eq("invoiceId", invoiceId))
                .addOrder(Order.asc("createdAt"))
                .list();
    }

    public PaymentUrlLogDTO findByGatewayId(String gatewayId) {
        return (PaymentUrlLogDTO) getSession().createCriteria(PaymentUrlLogDTO.class)
            .add(Restrictions.eq("gatewayId", gatewayId))
            .uniqueResult();
    }

    public String getRequestPayloadValueFromPaymentUrl(Integer invoiceId, String fieldName, boolean isMetaData) {
        PaymentUrlLogDTO paymentUrlLog = findByInvoiceId(invoiceId);
        if( paymentUrlLog != null ) {
            String jsonString = isMetaData ? paymentUrlLog.getPaymentUrlResponse() : paymentUrlLog.getPaymentUrlRequestPayload();
            if( jsonString != null ) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    if( !isMetaData ) {
                        return jsonObject.optString(fieldName);
                    } else {
                        JSONObject metaData = jsonObject.optJSONObject("metaData");
                        if( metaData != null ) {
                            return metaData.optString(fieldName);
                        }
                    }
                } catch (JSONException e) {
                    LOG.error("Error parsing JSON payload");
                }
            }
        }
        return null;
    }

}
