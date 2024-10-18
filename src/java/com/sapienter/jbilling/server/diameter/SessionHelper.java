package com.sapienter.jbilling.server.diameter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.sapienter.jbilling.server.item.PricingField;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Request;
import org.jdiameter.api.ro.ServerRoSession;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.common.impl.app.ro.RoCreditControlAnswerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.util.PreferenceBL;

@Transactional(propagation = Propagation.REQUIRED)
public class SessionHelper {

    private DiameterUserLocator userLocator;
    private DiameterItemLocator itemLocator;

    private Integer entityId;

    public interface AvpName {
        public static final String DESTINATION_REALM = "Destination-Realm";
        public static final String SUBSCRIPTION_ID_DATA = "Subscription-Id-Data";
        public static final String CALLED_PARTY_ADDRESS = "Called-Party-Address";
        public static final String RATING_GROUP = "Rating-Group";
    }

    private final static Logger logger = LoggerFactory.getLogger(SessionHelper.class);

    /**
     * Create a template answer with any mandatory AVPs included.
     *
     * @param request    The request being answered.
     * @param resultCode The result code for the answer.
     * @return The answer object, ready for any additional AVPs.
     * @throws InternalException
     * @throws AvpDataException
     */
    public RoCreditControlAnswer createCCA(RoCreditControlRequest request, int resultCode)
            throws InternalException {
        RoCreditControlAnswerImpl answer = new RoCreditControlAnswerImpl(
                (Request) request.getMessage(), resultCode);

        AvpSet ccrAvps = request.getMessage().getAvps();
        AvpSet ccaAvps = answer.getMessage().getAvps();

        // <Credit-Control-Answer> ::= < Diameter Header: 272, PXY >
        //  < Session-Id >
        //  { Result-Code }
        //  { Origin-Host }
        //  { Origin-Realm }
        //  { Auth-Application-Id }
        // The above should be added by the lower layers of the stack

        //  { CC-Request-Type }
        // Copy from the request
        ccaAvps.addAvp(ccrAvps.getAvp(Avp.CC_REQUEST_TYPE));

        //  { CC-Request-Number }
        // Copy from the request
        ccaAvps.addAvp(ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER));

        //  [ User-Name ] NOT USED BY 3GPP
        //  [ CC-Session-Failover ]
        //  [ CC-Sub-Session-Id ] NOT USED BY 3GPP
        //  [ Acct-Multi-Session-Id ] NOT USED BY 3GPP
        //  [ Origin-State-Id ] NOT USED BY 3GPP
        //  [ Event-Timestamp ] NOT USED BY 3GPP
        //  [ Granted-Service-Unit ] NOT USED BY 3GPP
        // *[ Multiple-Services-Credit-Control ]
        //  [ Cost-Information]
        //  [ Final-Unit-Indication ] NOT USED BY 3GPP
        //  [ Check-Balance-Result ] NOT USED BY 3GPP
        //  [ Credit-Control-Failure-Handling ]
        //  [ Direct-Debiting-Failure-Handling ]
        //  [ Validity-Time] NOT USED BY 3GPP
        // *[ Redirect-Host]
        //  [ Redirect-Host-Usage ]
        //  [ Redirect-Max-Cache-Time ]
        // *[ Proxy-Info ]
        // *[ Route-Record ]
        // *[ Failed-AVP ]
        // *[ AVP ]
        return answer;
    }

    /**
     * Adds a Multiple-Services-Credit-Control AVP to a Credit-Control-Answer message.
     * Applies the unit multiplier to the output units, if requested.
     *
     * @param ccaAvps
     * @param ratingGroup
     * @param result
     * @param includeResult
     * @param session
     * @throws AvpDataException
     */
    private void addMSCC(AvpSet ccaAvps, Long ratingGroup, DiameterResultWS result, boolean includeResult, boolean session,
                         boolean applyMultiplier) throws AvpDataException {
        AvpSet mscc = ccaAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL, true, false);

        final BigDecimal unitDivisor = getUnitDivisor(entityId);

        AvpSet gsu = mscc.addGroupedAvp(Avp.GRANTED_SERVICE_UNIT, true, false);
        if (session) {
            // 32-bit time value
            gsu.addAvp(Avp.CC_TIME, result.getGrantedUnits().multiply(
                    applyMultiplier ? unitDivisor : BigDecimal.ONE).longValue(),
                    true, false, true);
        } else {
            // 64-bit service-specific-units value
            gsu.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, result.getGrantedUnits().multiply(
                    applyMultiplier ? unitDivisor : BigDecimal.ONE).longValue(),
                    true, false, false);
        }

        if (ratingGroup != null) {
            mscc.addAvp(Avp.RATING_GROUP, ratingGroup, true, false, true);
        }

        if (includeResult) {
            mscc.addAvp(Avp.RESULT_CODE, result.getResultCode(), true, false);
        }

        if (session) {
            if (result.isTerminateWhenConsumed()) {
                AvpSet fui = mscc.addGroupedAvp(Avp.FINAL_UNIT_INDICATION, true, false);
                fui.addAvp(Avp.FINAL_UNIT_ACTION, 0, true, false);
            }

            mscc.addAvp(Avp.TIME_QUOTA_THRESHOLD, result.getQuotaThreshold(),
                    ChargingServer.VENDOR_ID_3GPP, true, false, true);
        }
    }

    /**
     * Process an incoming initial request.
     *
     * @param session
     * @param request
     * @return
     * @throws InternalException
     * @throws AvpDataException
     */
    public RoCreditControlAnswer processInitialRequest(
            ServerRoSession session, RoCreditControlRequest request)
            throws InternalException, AvpDataException {
        RoCreditControlAnswer ans;
        List<PricingField> apiData = new ArrayList<PricingField>();
        DiameterResultWS res;

        AvpSet ccrAvps = request.getMessage().getAvps();
        String sessionId = session.getSessionId();
        Date timestamp = ccrAvps.getAvp(Avp.EVENT_TIMESTAMP).getTime();
        apiData.add(new PricingField(AvpName.DESTINATION_REALM, request.getDestinationRealm()));
        apiData.add(new PricingField(AvpName.SUBSCRIPTION_ID_DATA,
                ccrAvps.getAvp(Avp.SUBSCRIPTION_ID).getGrouped()
                        .getAvp(Avp.SUBSCRIPTION_ID_DATA).getUTF8String()
        ));
        AvpSet imsInfo = ccrAvps
                .getAvp(Avp.SERVICE_INFORMATION, ChargingServer.VENDOR_ID_3GPP).getGrouped()
                .getAvp(Avp.IMS_INFORMATION, ChargingServer.VENDOR_ID_3GPP).getGrouped();
        apiData.add(new PricingField(AvpName.CALLED_PARTY_ADDRESS,
                imsInfo.getAvp(Avp.CALLED_PARTY_ADDRESS, ChargingServer.VENDOR_ID_3GPP).getUTF8String()
        ));
        AvpSet msccGroup = ccrAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL).getGrouped();
        long ratingGroup = msccGroup.getAvp(Avp.RATING_GROUP).getUnsigned32();
        apiData.add(new PricingField(AvpName.RATING_GROUP, Long.valueOf(ratingGroup)));
        Avp rsu = msccGroup.getAvp(Avp.REQUESTED_SERVICE_UNIT).getGrouped().getAvpByIndex(0);

        DiameterBL api = new DiameterBL(userLocator, itemLocator, entityId);

        switch (rsu.getCode()) {
            case Avp.CC_TIME:
                Long requestedUnits = Long.valueOf(rsu.getUnsigned32());
                res = api.createSession(sessionId, timestamp,
                        BigDecimal.valueOf(requestedUnits).divide(getUnitDivisor(entityId)),
                        apiData);
                ans = createCCA(request, res.getResultCode());
                if (res.getResultCode() == DiameterResultWS.DIAMETER_SUCCESS) {
                    addMSCC(ans.getMessage().getAvps(), ratingGroup, res, false, true, true);
                }
                break;
            case Avp.CC_SERVICE_SPECIFIC_UNITS:
                requestedUnits = Long.valueOf(rsu.getUnsigned64());
                res = api.reserveUnits(sessionId, timestamp, requestedUnits.intValue(),
                        apiData);
                ans = createCCA(request, res.getResultCode());
                if (res.getResultCode() == DiameterResultWS.DIAMETER_SUCCESS) {
                    addMSCC(ans.getMessage().getAvps(), ratingGroup, res, false, false, false);
                }
                break;
            default:
                logger.error("Unexpected Requested-Service-Unit type: {}", rsu.getCode());
                ans = createCCA(request, DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
                break;
        }
        return ans;
    }

    public RoCreditControlAnswer processUpdateRequest(ServerRoSession session,
                                                      RoCreditControlRequest request) throws InternalException, AvpDataException {
        RoCreditControlAnswer ans = null;
        AvpSet ccaAvps = null;
        List<PricingField> apiData = new ArrayList<PricingField>();

        // Extract the common variables from the request
        AvpSet ccrAvps = request.getMessage().getAvps();
        String sessionId = session.getSessionId();
        Date timestamp = ccrAvps.getAvp(Avp.EVENT_TIMESTAMP).getTime();

        // Iterate through each MSCC AVP, calling into jBilling for each rating group
        AvpSet msccSet = ccrAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        Iterator<Avp> msccIter = msccSet.iterator();
        while (msccIter.hasNext()) {
            AvpSet msccGroup = msccIter.next().getGrouped();

            // If rating group is not present, or there is only one MULTIPLE_SERVICES_CREDIT_CONTROL AVP
            // treat as an extension of the previous reservation request.
            Avp ratingGroupAvp = msccGroup.getAvp(Avp.RATING_GROUP);
            Long ratingGroup = null;
            if (ratingGroupAvp != null) {
                ratingGroup = Long.valueOf(ratingGroupAvp.getUnsigned32());
                apiData.add(new PricingField(AvpName.RATING_GROUP, ratingGroup));
            }

            Long requestedTime = Long.valueOf(
                    msccGroup.getAvp(Avp.REQUESTED_SERVICE_UNIT)
                            .getGrouped().getAvp(Avp.CC_TIME).getUnsigned32());
            Long usedTime = Long.valueOf(
                    msccGroup.getAvp(Avp.USED_SERVICE_UNIT)
                            .getGrouped().getAvp(Avp.CC_TIME).getUnsigned32());
            DiameterBL api = new DiameterBL(userLocator, itemLocator, getEntityId());

            DiameterResultWS res;
            if ((ratingGroup == null) || (msccSet.size() == 1)) {
                res = api.extendSession(sessionId, timestamp,
                        BigDecimal.valueOf(usedTime).divide(getUnitDivisor(entityId), 10, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(requestedTime).divide(getUnitDivisor(entityId)));
            } else {
                res = api.updateSession(sessionId, timestamp,
                        BigDecimal.valueOf(usedTime).divide(getUnitDivisor(entityId), 10, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(requestedTime).divide(getUnitDivisor(entityId)),
                        apiData);
            }

            if (ans == null) {
                // First rating group - create the CCA
                ans = createCCA(request, res.getResultCode());
                ccaAvps = ans.getMessage().getAvps();

                if (res.getResultCode() == DiameterResultWS.DIAMETER_SUCCESS) {
                    addMSCC(ccaAvps, ratingGroup, res, true, true, true);
                } else {
                    // Don't bother adding any MSCC AVPs
                    break;
                }
            } else {
                // Additional rating group - add another MSCC AVP
                addMSCC(ccaAvps, ratingGroup, res, true, true, true);
            }
        }
        return ans;
    }

    public RoCreditControlAnswer processTerminationRequest(
            ServerRoSession session, RoCreditControlRequest request)
            throws InternalException, AvpDataException {
        RoCreditControlAnswer ans;

        AvpSet ccrAvps = request.getMessage().getAvps();
        String sessionId = session.getSessionId();
        Date timestamp = ccrAvps.getAvp(Avp.EVENT_TIMESTAMP).getTime();
        AvpSet imsInfo = ccrAvps.getAvp(Avp.SERVICE_INFORMATION, ChargingServer.VENDOR_ID_3GPP).getGrouped()
                .getAvp(Avp.IMS_INFORMATION, ChargingServer.VENDOR_ID_3GPP).getGrouped();
        int causeCode = imsInfo.getAvp(Avp.CAUSE_CODE, ChargingServer.VENDOR_ID_3GPP).getInteger32();
        AvpSet msccGroup = ccrAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL).getGrouped();
        Avp usu = msccGroup.getAvp(Avp.USED_SERVICE_UNIT).getGrouped().getAvpByIndex(0);
        Long usedUnits;
        DiameterBL api = new DiameterBL(userLocator, itemLocator, entityId);

        logger.debug("USED_SERVICE_UNIT Code: " + usu.getCode());

        switch (usu.getCode()) {
            case Avp.CC_TIME:
                usedUnits = Long.valueOf(usu.getUnsigned32());
                logger.debug("usedUnits: " + usedUnits);
                BigDecimal divisor = getUnitDivisor(entityId);
                logger.debug("divisor: " + divisor);
                BigDecimal convertedUsedUnits = BigDecimal.valueOf(usedUnits).divide(divisor, 10, RoundingMode.HALF_UP);
                logger.debug("usedUnits after divisor: " + convertedUsedUnits);

                api.endSession(sessionId, timestamp, convertedUsedUnits, causeCode);
                ans = createCCA(request, DiameterResultWS.DIAMETER_SUCCESS);
                break;
            case Avp.CC_SERVICE_SPECIFIC_UNITS:
                usedUnits = Long.valueOf(usu.getUnsigned64());
                api.consumeReservedUnits(sessionId, timestamp, usedUnits.intValue(), causeCode);
                ans = createCCA(request, DiameterResultWS.DIAMETER_SUCCESS);
                break;
            default:
                logger.error("Unexpected Used-Service-Unit type: {}", usu.getCode());
                ans = createCCA(request, DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
                break;
        }
        return ans;
    }

    private BigDecimal getUnitDivisor(Integer entityId) {
        PreferenceBL div = new PreferenceBL(entityId, CommonConstants.PREFERENCE_DIAMETER_UNIT_DIVISOR);
        return div.isNull() ? BigDecimal.ONE : div.getDecimal();
    }

    public DiameterUserLocator getUserLocator() {
        return userLocator;
    }

    public void setUserLocator(DiameterUserLocator userLocator) {
        this.userLocator = userLocator;
    }

    public DiameterItemLocator getItemLocator() {
        return itemLocator;
    }

    public void setItemLocator(DiameterItemLocator itemLocator) {
        this.itemLocator = itemLocator;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }
}
