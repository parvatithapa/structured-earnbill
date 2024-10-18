package com.sapienter.jbilling.server.mediation.sapphire.cdr;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.sapienter.jbilling.server.mediation.sapphire.model.CallType;
import com.sapienter.jbilling.server.mediation.sapphire.model.PartyType;

public enum CdrSkipPolicy {

    TEST_CALL {

        @Override
        boolean shouldSkip(CallType cdr) {
            return cdr.isTestcall();
        }

    }, SAME_BUSINESS_GROUP_NAME {

        @Override
        boolean shouldSkip(CallType cdr) {
            PartyType origPartyType = cdr.getOrigParty();
            PartyType termPartyType = cdr.getTermParty();
            return (null !=origPartyType && null!= termPartyType && isNotBlank(origPartyType.getBusinessGroupName())
                    && isNotBlank(termPartyType.getBusinessGroupName())
                    && origPartyType.getBusinessGroupName().equals(termPartyType.getBusinessGroupName()));
        }
    };

    abstract boolean shouldSkip(CallType cdr);
}
