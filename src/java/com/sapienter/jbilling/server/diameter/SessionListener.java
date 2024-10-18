package com.sapienter.jbilling.server.diameter;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.ro.ServerRoSession;
import org.jdiameter.api.ro.ServerRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.util.Context;

/**
 * @author gavin.llewellyn
 *
 */
public class SessionListener implements ServerRoSessionListener {

	private final static Logger logger = LoggerFactory.getLogger(SessionListener.class);

	/**
	 * @param api
	 */
	public SessionListener() {
	}
	
    /* (non-Javadoc)
	 * @see org.jdiameter.api.ro.ServerRoSessionListener#doCreditControlRequest(org.jdiameter.api.ro.ServerRoSession, org.jdiameter.api.ro.events.RoCreditControlRequest)
	 */
	@Override
	public void doCreditControlRequest(ServerRoSession session,
			RoCreditControlRequest request) throws InternalException,
			IllegalDiameterStateException, RouteException, OverloadException {
		logger.info("doCreditControlRequest");
		RoCreditControlAnswer cca = null;
		
		SessionHelper helper = Context.getBean(Context.Name.DIAMETER_HELPER);

		try {
			switch (request.getRequestTypeAVPValue()) {
			case 1:
				// INITIAL_REQUEST
				cca = helper.processInitialRequest(session, request);
				break;
			case 2:
				// UPDATE_REQUEST
				cca = helper.processUpdateRequest(session, request);
				break;
			case 3:
				// TERMINATION_REQUEST
				cca = helper.processTerminationRequest(session, request);
				break;
			case 4:
				// EVENT_REQUEST
				// Not currently supported
			default:
				logger.error("Unexpected CC-Request-Type type: {}", request.getRequestTypeAVPValue());
				cca = helper.createCCA(request, DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
				break;
			}
		} catch (AvpDataException e) {
			logger.error("Error processing AVP {}", e.getAvp().getCode(), e);
			cca = helper.createCCA(request, 5004);
			Message msg = cca.getMessage();
			msg.getAvps().addGroupedAvp(Avp.FAILED_AVP, true, false).addAvp(e.getAvp());
		} catch (Exception e) {
			logger.error("Error processing AVP", e);
			cca = helper.createCCA(request, DiameterResultWS.DIAMETER_UNABLE_TO_COMPLY);
		}
		
		if (cca != null) {
			session.sendCreditControlAnswer(cca);
		}
	}

	/* (non-Javadoc)
	 * @see org.jdiameter.api.ro.ServerRoSessionListener#doReAuthAnswer(org.jdiameter.api.ro.ServerRoSession, org.jdiameter.api.auth.events.ReAuthRequest, org.jdiameter.api.auth.events.ReAuthAnswer)
	 */
	@Override
	public void doReAuthAnswer(ServerRoSession session, ReAuthRequest request,
			ReAuthAnswer answer) throws InternalException,
			IllegalDiameterStateException, RouteException, OverloadException {
		logger.info("doReAuthAnswer");
		// We don't send RARs, so this should never be called
	}

	/* (non-Javadoc)
	 * @see org.jdiameter.api.ro.ServerRoSessionListener#doOtherEvent(org.jdiameter.api.app.AppSession, org.jdiameter.api.app.AppRequestEvent, org.jdiameter.api.app.AppAnswerEvent)
	 */
	@Override
	public void doOtherEvent(AppSession session, AppRequestEvent request,
			AppAnswerEvent answer) throws InternalException,
			IllegalDiameterStateException, RouteException, OverloadException {
		logger.info("doOtherEvent");
		// We should only be receiving CCRs
	}
}
