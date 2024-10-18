package com.sapienter.jbilling.server.mediation.sapphire;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CALLING_PARTY_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CHARGE_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CONNECT_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.DEST_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.HAS_FORWARDED_TAG;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.LAST_REDIRECTING_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.PLUS_PREFIX;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.RELAESE_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.REQUESTED_ADDR;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.SEQUENCE_NUM;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.TRUNK_GROUP_ID;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.model.AddressType;
import com.sapienter.jbilling.server.mediation.sapphire.model.CallForwardInfoType;
import com.sapienter.jbilling.server.mediation.sapphire.model.CallType;
import com.sapienter.jbilling.server.mediation.sapphire.model.PartyType;
import com.sapienter.jbilling.server.mediation.sapphire.model.TrunkGroupType;
import com.sapienter.jbilling.server.util.Context;

public abstract class SapphireUtil {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SapphireUtil() {}


    public static String getNationalNumber(String number) {
        try {
            if(!number.startsWith(PLUS_PREFIX)) {
                number = PLUS_PREFIX + number;
            }
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            PhoneNumber phoneNumber = phoneUtil.parse(number, StringUtils.EMPTY);
            if(!phoneUtil.isValidNumber(phoneNumber)) {
                throw new IllegalArgumentException("Given Number " + number + " is not valid!");
            }
            return String.valueOf(phoneNumber.getNationalNumber());
        } catch (NumberParseException ex) {
            throw new IllegalArgumentException("Number parsing failed! ", ex);
        }
    }

    public static List<PricingField> collectPricingFieldsFromCdr(CallType sapphireCdr) {
        List<PricingField> fields = new ArrayList<>();
        PartyType origPartyType = sapphireCdr.getOrigParty();
        TrunkGroupType trunkGroupType = origPartyType.getTrunkGroup();
        if(null != trunkGroupType) {
            fields.add(new PricingField(TRUNK_GROUP_ID, sapphireCdr.getOrigParty().getTrunkGroup().getTrunkGroupId()));
        }
        fields.add(new PricingField(CONNECT_TIME, sapphireCdr.getConnectTime()));
        fields.add(new PricingField(RELAESE_TIME, sapphireCdr.getReleaseTime()));
        fields.add(new PricingField(REQUESTED_ADDR, sapphireCdr.getRoutingInfo().getRequestedAddr().getValue()));
        fields.add(new PricingField(DEST_ADDR, sapphireCdr.getRoutingInfo().getDestAddr().getValue()));
        fields.add(new PricingField(SEQUENCE_NUM, sapphireCdr.getSeqnum()));

        AddressType callingPartyAddr = origPartyType.getCallingPartyAddr();
        if(null!= callingPartyAddr) {
            fields.add(new PricingField(CALLING_PARTY_ADDR, callingPartyAddr.getValue()));
        }

        AddressType chargePartyAddr = origPartyType.getChargeAddr();

        if(null!= chargePartyAddr) {
            fields.add(new PricingField(CHARGE_ADDR, chargePartyAddr.getValue()));
        }

        CallForwardInfoType callForwardInfoType = sapphireCdr.getCallForwardInfo();
        if(null!= callForwardInfoType) {
            fields.add(new PricingField(LAST_REDIRECTING_ADDR, callForwardInfoType.getLastRedirectingAddr().getValue()));
            fields.add(new PricingField(HAS_FORWARDED_TAG, true));
        }
        return fields;
    }

    public static void setUserOnMediationStepResult(MediationStepResult result, String assetIdentifier) {
        SapphireMediationHelperService service = Context.getBean(SapphireMediationHelperService.class);
        if(!service.isIdentifierPresent(assetIdentifier)) {
            result.setDone(true);
            result.addError("ERR-ASSET-NOT-FOUND");
            return;
        }
        Map<String, Integer> userCurrencyMap = service.getUserIdForAssetIdentifier(assetIdentifier);
        if(userCurrencyMap.isEmpty()) {
            result.addError("USER-NOT-FOUND");
            result.setDone(true);
            result.addError("ERR-ASSET-NOT-ASSIGNED-TO-ANY-CUSTOMER");
            logger.error("Asset {} found but is not assigned to any customer", assetIdentifier);
        } else {
            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            logger.debug("Resolved user {} and currency {} for number{}", result.getUserId(), result.getCurrencyId(), assetIdentifier);
        }
    }


}
