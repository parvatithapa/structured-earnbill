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
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil.getNationalNumber;

import java.util.Optional;

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
            PricingField callingPartyAddrField = PricingField.find(cdr.getFields(), CALLING_PARTY_ADDR);
            PricingField chargeAddrField = PricingField.find(cdr.getFields(), CHARGE_ADDR);
            if(null!= callingPartyAddrField &&
                    null!= chargeAddrField) {
                SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
                String callingpartyAddr = getNationalNumber(callingPartyAddrField.getStrValue());
                String chargeAddr = getNationalNumber(chargeAddrField.getStrValue());

                boolean isCallingpartyAddrFound = service.isIdentifierPresent(callingpartyAddr);
                boolean isChargeAddrFound = service.isIdentifierPresent(chargeAddr);

                Optional<Integer> orgProductId = Optional.empty();
                if(isCallingpartyAddrFound) {
                    orgProductId = service.getProductIdByIdentifier(callingpartyAddr);
                } else if(isChargeAddrFound) {
                    orgProductId = service.getProductIdByIdentifier(chargeAddr);
                }

                PricingField destinationAddr = PricingField.find(cdr.getFields(), DEST_ADDR);
                String destAddr = getNationalNumber(destinationAddr.getStrValue());
                boolean isDestAddrFound = service.isIdentifierPresent(destAddr);
                Optional<Integer> destProductId = Optional.empty();

                if(isDestAddrFound) {
                    destProductId = service.getProductIdByIdentifier(destAddr);
                }

                if(isCallingpartyAddrFound || isChargeAddrFound) {
                    cdrType = OUT_GOING_CALL_CDR_TYPE;
                }

                if(orgProductId.isPresent() && destProductId.isPresent()
                        && destProductId.equals(orgProductId)) {
                    cdrType = ON_NET_CALL_CDR_TYPE;
                }

                PricingField forwardedTag = PricingField.find(cdr.getFields(), HAS_FORWARDED_TAG);
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
}
