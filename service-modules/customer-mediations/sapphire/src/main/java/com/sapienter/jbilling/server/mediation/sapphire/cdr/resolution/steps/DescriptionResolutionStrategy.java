package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireUtil.getNationalNumber;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;
import com.sapienter.jbilling.server.util.Context;

public enum DescriptionResolutionStrategy {

    INCOMING(SapphireMediationConstants.INCOMING_CALL_CDR_TYPE) {

        @Override
        public String generateDescription(MediationStepContext context) {
            PricingField callingPartyField = context.getPricingField(SapphireMediationConstants.CALLING_PARTY_ADDR);
            PricingField destField = context.getPricingField(SapphireMediationConstants.DEST_ADDR);
            String callingpartyAddr = getNationalNumber(callingPartyField.getStrValue());
            String destAddr = getNationalNumber(context.getPricingField(SapphireMediationConstants.DEST_ADDR).getStrValue());
            MediationStepResult result = context.getResult();
            result.setSource(callingPartyField.getStrValue());
            result.setDestination(destField.getStrValue());
            return String.format(SapphireMediationConstants.DESCRIPTION_FORMAT, callingpartyAddr, destAddr);
        }

    }, OUTGOING(SapphireMediationConstants.OUT_GOING_CALL_CDR_TYPE) {

        @Override
        public String generateDescription(MediationStepContext context) {
            return getDescription(context);

        }

    }, ONNET(SapphireMediationConstants.ON_NET_CALL_CDR_TYPE) {

        @Override
        public String generateDescription(MediationStepContext context) {
            return getDescription(context);
        }

    }, FORWARDED(SapphireMediationConstants.FORWARDED_CALL_CDR_TYPE) {

        @Override
        public String generateDescription(MediationStepContext context) {
            PricingField lastRedirectingAddrField = context.getPricingField(SapphireMediationConstants.LAST_REDIRECTING_ADDR);
            String lastRedirectingAddr = getNationalNumber(lastRedirectingAddrField.getStrValue());
            PricingField chargeAddrField = context.getPricingField(SapphireMediationConstants.CHARGE_ADDR);
            String chargeAddr = getNationalNumber(chargeAddrField.getStrValue());
            SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
            boolean isLastRedirectingAddrFound = service.isIdentifierPresent(lastRedirectingAddr);
            PricingField destField = context.getPricingField(SapphireMediationConstants.DEST_ADDR);
            String destAddr = getNationalNumber(destField.getStrValue());
            MediationStepResult result = context.getResult();
            result.setSource(isLastRedirectingAddrFound ? lastRedirectingAddrField.getStrValue() : chargeAddrField.getStrValue());
            result.setDestination(destField.getStrValue());
            return String.format(SapphireMediationConstants.DESCRIPTION_FORMAT,
                    isLastRedirectingAddrFound ? lastRedirectingAddr : chargeAddr, destAddr);
        }

    };

    private String cdrType;

    private DescriptionResolutionStrategy(String cdrType) {
        this.cdrType = cdrType;
    }

    public abstract String generateDescription(MediationStepContext context);

    public static DescriptionResolutionStrategy getStrategy(String cdrType) {
        for(DescriptionResolutionStrategy descriptionResolutionStrategy : values()) {
            if(descriptionResolutionStrategy.getCdrType().equals(cdrType)) {
                return descriptionResolutionStrategy;
            }
        }
        throw new IllegalArgumentException("Invalid cdr type passed!");
    }

    public String getCdrType() {
        return cdrType;
    }

    private static String getDescription(MediationStepContext context) {
        PricingField callingPartyField = context.getPricingField(SapphireMediationConstants.CALLING_PARTY_ADDR);
        PricingField destField = context.getPricingField(SapphireMediationConstants.DEST_ADDR);
        PricingField chargeAddrField = context.getPricingField(SapphireMediationConstants.CHARGE_ADDR);
        String callingpartyAddr = getNationalNumber(callingPartyField.getStrValue());
        String chargeAddr = getNationalNumber(chargeAddrField.getStrValue());
        SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
        boolean isCallingpartyAddrFound = service.isIdentifierPresent(callingpartyAddr);
        String destAddr = getNationalNumber(destField.getStrValue());
        MediationStepResult result = context.getResult();
        result.setSource(isCallingpartyAddrFound ? callingPartyField.getStrValue() : chargeAddrField.getStrValue());
        result.setDestination(destField.getStrValue());
        return String.format(SapphireMediationConstants.DESCRIPTION_FORMAT,
                isCallingpartyAddrFound ? callingpartyAddr : chargeAddr, destAddr);
    }

}
