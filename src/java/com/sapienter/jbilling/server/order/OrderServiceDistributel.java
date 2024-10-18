/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DetailTypeField;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DistributelMediationConstant;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by igutierrez on 1/26/17.
 */
public class OrderServiceDistributel extends OrderServiceImpl{
    private String detailFileNamesMF;

    @Override
    @Transactional(propagation = Propagation.REQUIRED,  value="transactionManager")
    public MediationEventResult addMediationEventDistributel(JbillingMediationRecord jmr) {

        MediationEventResult firstResult = addMediationEvent(jmr);
        OrderDAS orderDAS = new OrderDAS();
        OrderDTO order = orderDAS.find(firstResult.getCurrentOrderId());
        OrderBL orderBL = new OrderBL();
        OrderLineWS orderLine = orderBL.getOrderLineWS(order.getLines().get(0).getId());
        orderLine.setDescription(jmr.getDescription());
        orderBL.updateOrderLine(orderLine,jmr.getUserId());
        List<CallDataRecord> records = getCallDataRecords(jmr.getPricingFields());
        List<PricingField> fields = new ArrayList<>();
        for (CallDataRecord record : records) {
            PricingField.addAll(fields, record.getFields());
        }
        String fieldFileName = PricingField.find(fields, DistributelMediationConstant.DETAIL_FILES_NAME).getStrValue();
        if (!StringUtils.isEmpty(fieldFileName)) {
            MetaField metaField = MetaFieldBL.getFieldByName(order.getUser().getEntity().getId(), new EntityType[]{EntityType.ORDER}, detailFileNamesMF);
            order.setMetaField(metaField, addExtensionFile(fieldFileName,fields));
        }

        orderDAS.makePersistent(order);
        return firstResult;
    }

    public String getDetailFileNamesMF() {
        return detailFileNamesMF;
    }

    public void setDetailFileNamesMF(String detailFileNamesMF) {
        this.detailFileNamesMF = detailFileNamesMF;
    }

    private String addExtensionFile(String baseFileName, List<PricingField> fields){
        Integer detailType =  PricingField.find(fields, DistributelMediationConstant.DETAIL_TYPE).getIntValue();
        StringBuffer fileName = new StringBuffer();
        if(DetailTypeField.CSV.getValue().equals(detailType)){
            fileName.append(baseFileName).append(".").append(DetailTypeField.CSV.getExtension());
        }
        if(DetailTypeField.PDF.getValue().equals(detailType)){
            fileName.append(baseFileName).append(".").append(DetailTypeField.PDF.getExtension());
        }
        if(DetailTypeField.PDF_CSV.getValue().equals(detailType)){
            Arrays.stream(DetailTypeField.PDF_CSV.getExtension().split("_")).forEach(extension ->
                fileName.append(baseFileName).append(".").append(extension).append(";")
            );
        }
        return fileName.toString();
    }
}
