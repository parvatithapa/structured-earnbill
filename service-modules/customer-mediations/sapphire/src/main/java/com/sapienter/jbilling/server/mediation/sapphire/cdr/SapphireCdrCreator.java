package com.sapienter.jbilling.server.mediation.sapphire.cdr;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil;
import com.sapienter.jbilling.server.mediation.sapphire.model.CallType;

public class SapphireCdrCreator {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer entityId;
    private Integer mediationConfigId;
    private List<String> recordKeyFields;

    public SapphireCdrCreator(Integer entityId, Integer mediationConfigId, List<String> recordKeyFields) {
        Assert.notNull(entityId, "Provide entityId!");
        Assert.notNull(mediationConfigId, "Provide mediationConfigId!");
        Assert.notNull(recordKeyFields, "Provide recordKeyFields!");
        this.entityId = entityId;
        this.mediationConfigId = mediationConfigId;
        this.recordKeyFields = recordKeyFields;
    }

    public ICallDataRecord createJbillingCdr(CallType sapphireCdr) {
        try {
            CallDataRecord cdr = new CallDataRecord();
            cdr.setEntityId(entityId);
            cdr.setMediationCfgId(mediationConfigId);
            for(CdrSkipPolicy skipPolicy : CdrSkipPolicy.values()) {
                if(skipPolicy.shouldSkip(sapphireCdr)) {
                    cdr.setKey(StringUtils.EMPTY);
                    return cdr;
                }
            }

            List<PricingField> pricingFields = SapphireUtil.collectPricingFieldsFromCdr(sapphireCdr);
            for(PricingField pricingField : pricingFields) {
                cdr.addField(pricingField, isKeyField(pricingField));
            }

            String cdrType = SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE;

            if(sapphireCdr.isLongcall()) {
                cdrType = SapphireMediationConstants.LONG_CALL_CDR_TYPE;
            }
            if(SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE.equals(cdrType)) {
                for(CdrTypeIdentifier identifier : CdrTypeIdentifier.values()) {
                    cdrType = identifier.identifyCdrType(cdr);
                    if(!cdrType.equals(SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE)) {
                        break;
                    }
                }
                if(cdrType.equals(SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE)) {
                    throw new InvalidCdrFormatException("cdr type is unknown!");
                }
            }
            cdr.addField(new PricingField(SapphireMediationConstants.CDR_TYPE, cdrType), false);
            logger.debug("Resolved Cdr type is {}", cdrType);
            logger.debug("cdr {}", cdr);
            return cdr;
        } catch (InvalidCdrFormatException cdrException) {
            throw cdrException;
        } catch (Exception ex) {
            logger.error("Failed during creation of cdr ", ex);
            throw new InvalidCdrFormatException(ex.getMessage(), ex);
        }
    }

    private boolean isKeyField(PricingField pricingField) {
        for(String key : recordKeyFields) {
            if(pricingField.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
