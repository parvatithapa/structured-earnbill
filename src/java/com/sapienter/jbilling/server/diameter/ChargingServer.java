package com.sapienter.jbilling.server.diameter;

import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.ro.ClientRoSession;
import org.jdiameter.api.ro.ServerRoSession;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.impl.app.ro.RoSessionFactoryImpl;
import org.jdiameter.server.impl.app.ro.ServerRoSessionImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gavin.llewellyn
 */
public class ChargingServer implements NetworkReqListener {

	private final static Logger logger = LoggerFactory.getLogger(ChargingServer.class);
	
	final static long VENDOR_ID_3GPP = 10415;
	private final static ApplicationId roAppId = ApplicationId.createByAuthAppId(VENDOR_ID_3GPP, 4L);
	private Stack stack;
	private SessionFactory sessFactory;
	
	private String configurationFile = "/jdiameter-config.xml";

	public ChargingServer() throws Exception {
	}

	/**
	 * @throws Exception 
	 */
	@PostConstruct
	public void initialize() throws Exception {
		logger.info("Starting Diameter server");
		InputStream is = this.getClass().getResourceAsStream(configurationFile);
		Configuration config = new XMLConfiguration(is);

		/* Create and configure the diameter stack */
		stack = new org.jdiameter.server.impl.StackImpl();
		sessFactory = stack.init(config);

		/* Ask for notification of incoming Ro requests */
		Network network = stack.unwrap(Network.class);
		network.addNetworkReqListener(this, roAppId);

		stack.start(); 

		/* Set up the Ro application session factory */
		RoSessionFactoryImpl roSessFactory = new RoSessionFactoryImpl(sessFactory);
		roSessFactory.setServerSessionListener(new SessionListener());
		((ISessionFactory) sessFactory).registerAppFacory(ServerRoSession.class, roSessFactory);
		((ISessionFactory) sessFactory).registerAppFacory(ClientRoSession.class, roSessFactory);
	}
	
	@PreDestroy
	public void destroy() throws Exception {
		// TODO: is it necessary to perform any tear-off operations?.
	}

	@Override
	public Answer processRequest(Request request) {
		try {
			/* Find the existing session object or create a new one */
			ServerRoSessionImpl session = sessFactory.getNewAppSession(request.getSessionId(),
					roAppId, ServerRoSession.class);
			/* Get the session object to do the initial processing */
			return session.processRequest(request);
		} catch (InternalException e) {
			logger.error("Failure handling received request", e);
		}
		return null;
	}

	public String getConfigurationFile() {
		return configurationFile;
	}

	public void setConfigurationFile(String configurationFile) {
		this.configurationFile = configurationFile;
	}
}
