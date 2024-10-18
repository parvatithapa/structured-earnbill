package com.sapienter.jbilling.auth;

import com.sapienter.jbilling.client.authentication.model.User;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.adennet.AdennetHelperService;
import com.sapienter.jbilling.server.adennet.ws.UserAndAssetAssociationResponseWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import jbilling.UserService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

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
    @Resource
    private RefreshTokenService refreshTokenService;
    @Resource(name= "adennetHelperService")
    private AdennetHelperService adennetHelperService;
    @Resource
    private AssetDAS assetDAS;

    private List<String> authenticateUser(UserDTO dbUser, User user, String password) {
        List<String> errors = new ArrayList<>();
        UserBL userBL = new UserBL(dbUser);
        if (!userBL.matchPasswordForUser(dbUser, password)) {
            errors.add("incorrect password");
        }
        if (user.isAccountExpired()) {
            errors.add("user account is expired");
        }
        if (user.isAccountLocked()) {
            errors.add("user account is locked");
        }
        if (user.isCredentialsExpired()) {
            errors.add("credentials expired");
        }
        return errors;
    }

    public JwtTokenResponseWS authenticateUser(AuthRequestWS authRequest) {
        try {
            logger.debug("authenticating user={} for entityId={}", authRequest.getUsername(), authRequest.getEntityId());
            UserDTO dbUser = userDAS.findByUserName(authRequest.getUsername(), authRequest.getEntityId());
            if (null == dbUser || 1 == dbUser.getDeleted()) {
                logger.error("user={} not found for entityId={}", authRequest.getUsername(), authRequest.getEntityId());
                throw new SessionInternalError("user not found", "user " + authRequest.getUsername() +
                    " not found under entity" + authRequest.getEntityId(), HttpStatus.SC_NOT_FOUND);
            }
            //authenticate user.
            User user = userService.getUser(authRequest.getUsername(), authRequest.getEntityId(), null);
            List<String> errors = authenticateUser(dbUser, user, authRequest.getPassword());
            if (CollectionUtils.isNotEmpty(errors)) {
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

    public RefreshTokenResponseWS refreshToken(String refreshToken) {
        try {
            RefreshTokenResponseWS refreshTokenResponse = new RefreshTokenResponseWS(refreshToken);
            if(refreshTokenService.isExpired(refreshToken)) {
                refreshTokenResponse.setStatus(StatusWS.EXPIRED);
            } else {
                RefreshTokenDTO refreshTokenDTO = refreshTokenService.findRefreshToken(refreshToken);
                User authenticatedUser;
                try (UserDTO user = userDAS.find(refreshTokenDTO.getUserId())) {
                    authenticatedUser = userService.getUser(user.getUserName(), user.getCompany().getId(), null);
                }
                refreshTokenResponse.setAccessToken( jwtUtils.jwtToken(authenticatedUser));
                refreshTokenResponse.setStatus(StatusWS.VALID);
            }
            return refreshTokenResponse;
        } catch (SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError("refreshToken failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
        } catch (SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError("loggedInUserInfo failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public GetSecurityUserResponseWS getSecurityUser(GetSecurityUserRequestWS getSecurityUserRequestWS) {
        try {
            UserDTO userDTO = userDAS.findByUserName(getSecurityUserRequestWS.getUsername(), getSecurityUserRequestWS.getEntityId());
            if(null == userDTO) {
                throw new SessionInternalError("getSecurityUser failed", getSecurityUserRequestWS.getUsername() + " user not found for "
                    + "entityId "+ getSecurityUserRequestWS.getEntityId(), HttpStatus.SC_NOT_FOUND);
            }
            return new GetSecurityUserResponseWS(userService.getUser(getSecurityUserRequestWS.getUsername()
                , getSecurityUserRequestWS.getEntityId(), null), jwtUtils.collectAuthorities(userDTO));
        } catch(SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError("getSecurityUser failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public AdennetJwtTokenResponseWS authenticateAdennetSubscriber(String subscriberNumber) {
        try {
            logger.info("Generating JWT token for subscriber = {}", subscriberNumber);
            UserAndAssetAssociationResponseWS subscriber = adennetHelperService.getUserAssetAssociationsBySubscriberNumber(subscriberNumber);
            return jwtUtils.createAdennetJwtToken(subscriber);
        } catch (SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError("Jwt token creation failed for ", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public RefreshTokenResponseWS adennetRefreshToken(String refreshToken) {
        try {
            RefreshTokenResponseWS refreshTokenResponse = new RefreshTokenResponseWS(refreshToken);
            if(refreshTokenService.isExpired(refreshToken)) {
                refreshTokenResponse.setStatus(StatusWS.EXPIRED);
            } else {
                RefreshTokenDTO refreshTokenDTO = refreshTokenService.findRefreshToken(refreshToken);
                try (UserDTO user = userDAS.find(refreshTokenDTO.getUserId())) {
                    String subscriberNumber = assetDAS.findSubscriberNumberByIdentifier(user.getUserName());
                    refreshTokenResponse.setAccessToken(jwtUtils.adennetJwtToken(subscriberNumber, refreshTokenDTO.getUserId(), user.getEntity().getId()));
                    refreshTokenResponse.setStatus(StatusWS.VALID);
                }
            }
            return refreshTokenResponse;
        } catch (SessionInternalError sessionInternalError) {
            throw sessionInternalError;
        } catch (Exception exception) {
            throw new SessionInternalError("refreshToken failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
