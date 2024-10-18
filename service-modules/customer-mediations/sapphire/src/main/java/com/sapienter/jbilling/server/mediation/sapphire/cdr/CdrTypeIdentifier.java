package com.sapienter.jbilling.server.mediation.sapphire.cdr;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.AMBIGUOUS_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CALLING_PARTY_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CHARGE_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.DEST_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.FORWARDED_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.HAS_FORWARDED_TAG;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.INCOMING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ON_NET_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OUT_GOING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.TRUNK_GROUP_ID;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil.validateAndCheckAssetNumberInSystem;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;
import com.sapienter.jbilling.server.util.Context;

public enum CdrTypeIdentifier {

    INCOMING {

        @Override
        public String identifyCdrType(ICallDataRecord cdr) {
            PricingField trunkGroupField = PricingField.find(cdr.getFields(), TRUNK_GROUP_ID);
            if(null!= trunkGroupField) {
                Integer trunkGroupId = trunkGroupField.getIntValue();
                SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
                String tableName = service.getMetaFieldsForEntity(cdr.getEntityId()).get(CARRIER_TABLE_FIELD_NAME);
                Optional<Integer> userId = service.getUserIdForIncomingCall(tableName, trunkGroupId);
                return userId.isPresent() ? INCOMING_CALL_CDR_TYPE : UNKNOWN_CALL_CDR_TYPE;
            }
            return UNKNOWN_CALL_CDR_TYPE;
        }

    }, OUT_GOING {

        @Override
        public String identifyCdrType(ICallDataRecord cdr) {
            String cdrType = UNKNOWN_CALL_CDR_TYPE;
            List<PricingField> pricingFields = cdr.getFields();
            PricingField callingPartyAddrField = PricingField.find(pricingFields, CALLING_PARTY_ADDR);
            PricingField chargeAddrField = PricingField.find(pricingFields, CHARGE_ADDR);
            if(null!= callingPartyAddrField && null!= chargeAddrField) {
                Optional<Integer> orgProductId = Optional.empty();
                String assetNumber = validateAndCheckAssetNumberInSystem(callingPartyAddrField.getStrValue());
                if(StringUtils.isEmpty(assetNumber)) {
                    assetNumber = validateAndCheckAssetNumberInSystem(chargeAddrField.getStrValue());
                    logger.debug("asset number {} found by chargeAddrFieldValue", assetNumber);
                } else {
                    logger.debug("asset number {} found by callingPartyAddrField", assetNumber);
                }

                SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
                if(StringUtils.isNotEmpty(assetNumber)) {
                    orgProductId = service.getProductIdByIdentifier(assetNumber);
                    cdrType = OUT_GOING_CALL_CDR_TYPE;
                    logger.debug("org product {} resolved for asset number {}", orgProductId, assetNumber);
                    logger.debug("cdr type {} resolved for asset number {}", cdrType, assetNumber);
                }

                String desitnationAddrFieldValue = PricingField.find(pricingFields, DEST_ADDR).getStrValue();
                assetNumber = validateAndCheckAssetNumberInSystem(desitnationAddrFieldValue);
                Optional<Integer> destProductId = Optional.empty();
                if(StringUtils.isNotEmpty(assetNumber)) {
                    destProductId = service.getProductIdByIdentifier(assetNumber);
                }

                if(orgProductId.isPresent() && destProductId.isPresent()
                        && destProductId.equals(orgProductId)) {
                    cdrType = ON_NET_CALL_CDR_TYPE;
                }

                PricingField forwardedTag = PricingField.find(pricingFields, HAS_FORWARDED_TAG);
                if(null!= forwardedTag) {
                    cdrType = FORWARDED_CALL_CDR_TYPE;
                }

                if(cdrType.equals(UNKNOWN_CALL_CDR_TYPE)) {
                    cdrType = AMBIGUOUS_CDR_TYPE;
                }
            }
            return cdrType;
        }

    };

    public abstract String identifyCdrType(ICallDataRecord cdr);
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
}
