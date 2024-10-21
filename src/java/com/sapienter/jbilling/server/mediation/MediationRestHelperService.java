package com.sapienter.jbilling.server.mediation;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecordRestWS;

@Transactional(value="jbillingMediationTransactionManager")
public class MediationRestHelperService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * 
     * @param records: Mediation records
     * @return
     */
    public JbillingMediationRecordRestWS[] convertPricingFields(JbillingMediationRecord[] records) {
        List<JbillingMediationRecordRestWS> mediationRecords = new ArrayList<>();
        if (null != records && records.length > 0) {
            Arrays.stream(records)
            .filter(Objects::nonNull)
            .forEach(record -> {
                logger.info("Pricing fields: {}", record.getPricingFields());
                JbillingMediationRecordRestWS mediationRecord = new JbillingMediationRecordRestWS(record);
                Map<String,String> taxFields = new HashMap<>();
                taxFields.put("ratedPriceWithTax", record.getRatedPriceWithTax() != null ? record.getRatedPriceWithTax().toPlainString() : "");
                taxFields.put("taxAmount", record.getTaxAmount() != null ? record.getTaxAmount().toPlainString() : "");
                mediationRecord.setAdditionalFields(taxFields);
                if (StringUtils.isNotEmpty(record.getPricingFields())) {
                    mediationRecord.setPricingFields(createPricingFieldMap(record.getPricingFields()));
                }
                mediationRecords.add(mediationRecord);
            }); 
        }
        return mediationRecords.toArray(new JbillingMediationRecordRestWS[0]);
    }

    /**
     * Convert pricingFields in the Map as a key and value pair.
     * @param pricingFields
     * @return
     */
    private Map<String,String> createPricingFieldMap(String pricingFields) {
        Assert.hasLength(pricingFields, "Please provide pricing fields");
        return Arrays.stream(PricingField.getPricingFieldsValue(pricingFields))
                .peek(pricingField -> {
                    Object value = pricingField.getValue();
                    pricingField.setStrValue(value!=null ? value.toString() : "");
                })
                .collect(Collectors.toMap(PricingField::getName, PricingField::getStrValue));
    }
}
