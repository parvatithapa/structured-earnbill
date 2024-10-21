package com.sapienter.jbilling.server.mediation.processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;

/**
 * This will aggregate JMRs in hourly buckets. The aggregated JMR will contain the earliest event date
 * and the sum of all quantities.
 *
 * To determine if a JMR can be aggregated it will get the value of the pricing field {@code pricingFieldName}.
 * If it contains any of the values in {@code pricingFieldValues} it will be aggregated.
 */
public class HourlyAggregator implements JmrProcessorAggregator{

    @Autowired
    private JMRRepository jmrRepository;

    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MMddHH");

    private String pricingFieldName;

    private Set<String> pricingFieldValues;

    private Map<String, JbillingMediationRecord> bucketsByDate = new HashMap<>();

    @Override
    public boolean aggregate(JbillingMediationRecord jmr) {
        String value = jmr.getPricingFieldValueByName(pricingFieldName);
        if(pricingFieldValues.contains(value)) {
            String bucketKey = value + dateTimeFormatter.print(jmr.getEventDate().getTime());
            JbillingMediationRecord aggregate = bucketsByDate.get(bucketKey);
            if(aggregate == null) {
                aggregate = new JbillingMediationRecord(jmr.getId(),
                        jmr.getStatus(),
                        jmr.getType(),
                        jmr.getjBillingCompanyId(),
                        jmr.getMediationCfgId(),
                        jmr.getRecordKey() + 'A',
                        jmr.getUserId(),
                        jmr.getEventDate(),
                        jmr.getQuantity(),
                        jmr.getDescription(),
                        jmr.getCurrencyId(),
                        jmr.getItemId(),
                        null, null, jmr.getPricingFields(),
                        null, null,
                        jmr.getProcessId(),
                        jmr.getSource(),
                        jmr.getDestination(),
                        jmr.getCdrType(),
                        jmr.getOriginalQuantity(),
                        jmr.getRatedPriceWithTax(),
                        jmr.getTaxAmount());

                bucketsByDate.put(bucketKey, aggregate);
            } else {
                if(jmr.getEventDate().before(aggregate.getEventDate())) {
                    aggregate.setEventDate(jmr.getEventDate());
                }
                aggregate.setQuantity(aggregate.getQuantity().add(jmr.getQuantity()));
                aggregate.setOriginalQuantity(aggregate.getOriginalQuantity().add(jmr.getOriginalQuantity()));
            }
            return true;
        } else {
            return false;
        }
    }

    public void setPricingFieldName(String pricingFieldName) {
        this.pricingFieldName = pricingFieldName;
    }

    public void setPricingFieldValues(Set<String> pricingFieldValues) {
        this.pricingFieldValues = pricingFieldValues;
    }

    @Override
    public Collection<JbillingMediationRecord> getAggregates() {
        return bucketsByDate.values();
    }

    @Override
    public void clear() {
        bucketsByDate.clear();
    }

    public JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }
}
