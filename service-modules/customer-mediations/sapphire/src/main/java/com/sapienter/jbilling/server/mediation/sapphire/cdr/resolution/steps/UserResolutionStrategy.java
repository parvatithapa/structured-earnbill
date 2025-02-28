package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.AMBIGUOUS_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CARRIER_TABLE_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.FORWARDED_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.INCOMING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ON_NET_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.OUT_GOING_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.UNKNOWN_CALL_CDR_TYPE;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil.setUserOnMediationStepResult;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil.validateAndCheckAssetNumberInSystem;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.CdrTypeIdentifier;
import com.sapienter.jbilling.server.util.Context;

public enum UserResolutionStrategy {


    INCOMING(INCOMING_CALL_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            MediationStepResult result = context.getResult();
            PricingField trunkGroupIdField = context.getPricingField(SapphireMediationConstants.TRUNK_GROUP_ID);
            Integer trunkGroupId = trunkGroupIdField.getIntValue();
            SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
            String tableName = service.getMetaFieldsForEntity(context.getEntityId()).get(CARRIER_TABLE_FIELD_NAME);
            Optional<Integer> userId = service.getUserIdForIncomingCall(tableName, trunkGroupId);
            if(userId.isPresent()) {
                result.setUserId(userId.get());
                result.setCurrencyId(service.getCurrencyIdForUser(userId.get()));
            } else {
                result.addError("USER-NOT-FOUND");
            }

        }

    },
    OUTGOING(OUT_GOING_CALL_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            MediationStepResult result = context.getResult();
            String callingPartyAddrFieldValue = context.getPricingField(SapphireMediationConstants.CALLING_PARTY_ADDR).getStrValue();
            String assetNumber = validateAndCheckAssetNumberInSystem(callingPartyAddrFieldValue);
            if(StringUtils.isEmpty(assetNumber)) {
                String chargeAddrFieldValue = context.getPricingField(SapphireMediationConstants.CHARGE_ADDR).getStrValue();
                assetNumber = validateAndCheckAssetNumberInSystem(chargeAddrFieldValue);
            }
            setUserOnMediationStepResult(result, assetNumber);
        }

    },
    ONNET(ON_NET_CALL_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            OUTGOING.resolveUser(context);
        }

    },
    FORWARDED(FORWARDED_CALL_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            MediationStepResult result = context.getResult();
            String lastRedirectingAddrFieldValue = context.getPricingField(SapphireMediationConstants.LAST_REDIRECTING_ADDR).getStrValue();
            String assetNumber = validateAndCheckAssetNumberInSystem(lastRedirectingAddrFieldValue);
            if(StringUtils.isEmpty(assetNumber)) {
                String chargeAddrFieldValue = context.getPricingField(SapphireMediationConstants.CHARGE_ADDR).getStrValue();
                assetNumber = validateAndCheckAssetNumberInSystem(chargeAddrFieldValue);
            }
            setUserOnMediationStepResult(result, assetNumber);
        }

    },
    UNKNOWN(UNKNOWN_CALL_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            context.getResult().addError("USER-NOT-RESOLVED");
        }

    },
    AMBIGUOUS(AMBIGUOUS_CDR_TYPE) {

        @Override
        public void resolveUser(MediationStepContext context) {
            String resolvedCdrType = getCdrType();
            for(CdrTypeIdentifier identifier : CdrTypeIdentifier.values()) {
                resolvedCdrType = identifier.identifyCdrType(context.getRecord());
                if(!resolvedCdrType.equals(UNKNOWN_CALL_CDR_TYPE)) {
                    break;
                }
            }
            MediationStepResult result = context.getResult();
            if(AMBIGUOUS_CDR_TYPE.equals(resolvedCdrType)) {
                result.addError("USER-NOT-RESOLVED");
                result.addError("AMBIGUOUS-CDR-TYPE");
            } else {
                List<PricingField> pricingFields = context.getPricingFields().stream()
                        .filter(field -> !(field.getName().equals(CDR_TYPE))).collect(Collectors.toList());
                context.getRecord().setFields(pricingFields);
                context.getRecord().addField(new PricingField(CDR_TYPE, resolvedCdrType), false);
                getStrategyByCdrType(resolvedCdrType).resolveUser(context);
            }
        }

    };
    private String cdrType;

    private UserResolutionStrategy(String cdrType) {
        this.cdrType = cdrType;
    }

    public String getCdrType() {
        return this.cdrType;
    }

    public static UserResolutionStrategy getStrategyByCdrType(String cdrType) {
        for(UserResolutionStrategy strategy : values()) {
            if(strategy.getCdrType().equals(cdrType)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Invalid cdr type passed!");
    }

    public abstract void resolveUser(MediationStepContext context);

}
