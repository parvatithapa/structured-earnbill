package com.sapienter.jbilling.auth;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import jbilling.UserService;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.client.authentication.model.User;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Service
@Transactional
public class JwtAuthenticationService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private JwtUtils jwtUtils;
	@Resource
	private UserDAS userDAS;
	@Resource
	private UserService userService;
	@Resource(name = "webServicesSession")
	private IWebServicesSessionBean api;

	private List<String> authenticateUser(UserDTO dbUser, User user, String password) {
		List<String> errors = new ArrayList<>();
		UserBL userBL = new UserBL(dbUser);
		if(!userBL.matchPasswordForUser(dbUser, password)) {
			errors.add("incorrect password");
		}
		if(user.isAccountExpired()) {
			errors.add("user account is expired");
		}
		if(user.isAccountLocked()) {
			errors.add("user account is locked");
		}
		if(user.isCredentialsExpired()) {
			errors.add("credentials expired");
		}
		return errors;
	}

	public JwtTokenResponseWS authenticateUser(AuthRequestWS authRequest) {
		try {
			logger.debug("authenticating user={} for entityId={}", authRequest.getUsername(), authRequest.getEntityId());
			UserDTO dbUser = userDAS.findByUserName(authRequest.getUsername(), authRequest.getEntityId());
			if(null == dbUser || 1 == dbUser.getDeleted()) {
				logger.error("user={} not found for entityId={}", authRequest.getUsername(), authRequest.getEntityId());
				throw new SessionInternalError("user not found", "user "+ authRequest.getUsername() +
						" not found under entity"+ authRequest.getEntityId(), HttpStatus.SC_NOT_FOUND);
			}
			//authenticate user.
			User user = userService.getUser(authRequest.getUsername(), authRequest.getEntityId(), null);
			List<String> errors = authenticateUser(dbUser, user, authRequest.getPassword());
			if(CollectionUtils.isNotEmpty(errors)) {
				logger.error("user={} for entityId={} authentication failed because of errors={}",
						authRequest.getUsername(), authRequest.getEntityId(), errors);
				throw new SessionInternalError("Autentication failed",
						errors.toArray(new String[errors.size()]),
						HttpStatus.SC_BAD_REQUEST);
			}
			return jwtUtils.createJwtToken(user);
		} catch (SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception exception) {
			throw new SessionInternalError("Authentication failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public JwtDecodedTokenInfoWS verifyToken(TokenVerificationRequestWS tokenVerificationRequestWS) {
		return jwtUtils.verifyToken(tokenVerificationRequestWS.getToken());
	}

	public LoggedInUserInfoWS loggedInUserInfo() {
		try {
			UserDTO userDTO = userDAS.find(api.getCallerId());
			User loggedUser = userService.getUser(userDTO.getUserName(), api.getCallerCompanyId(), null);
			return new LoggedInUserInfoWS(loggedUser, jwtUtils.collectAuthorities(userDTO));
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception exception) {
			throw new SessionInternalError("loggedInUserInfo failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
