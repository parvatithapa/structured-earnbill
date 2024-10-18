package com.sapienter.jbilling.auth;

import com.sapienter.jbilling.server.adennet.AdennetConstants;
import com.sapienter.jbilling.server.adennet.ws.UserAndAssetAssociationResponseWS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sapienter.jbilling.client.authentication.model.User;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleType;

@Component
class JwtUtils {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String ALGORITHM = "RSA";
	private static final String PRIVATE_KEY_FILE_PATH = "jwt.private.key.file.path";
	private static final String PUBLIC_KEY_FILE_PATH = "jwt.public.key.file.path";
	private static final String PRIVATE_KEY_FILE_NAME = "private_key.der";
	private static final String PUBLIC_KEY_FILE_NAME = "public_key.der";

	//TODO get from company level meta field.
	private static final Integer TOKEN_VALIDITY_IN_HOURS = 2;
	private static final Integer TOKEN_VALIDITY_IN_MINUTES = 15;

	private PrivateKey privateKey;
	private PublicKey publicKey;

	@Resource
	private Environment environment;
	@Resource
	private UserDAS userDAS;
	@Resource
	private RefreshTokenService refreshTokenService;

	@PostConstruct
	void init() {
		// loading private key
		loadPrivateKey();
		//loading public key
		loadPublicKey();
		Assert.notNull(privateKey, "privateKey is required");
		Assert.notNull(publicKey, "publicKey is required");
	}

	public String jwtToken(User authenticatedUser) {
		Instant now = Instant.now();
		List<String> scopeList = collectAuthorities(userDAS.find(authenticatedUser.getId()));
		String jwtToken = Jwts.builder()
				.claim("scope", scopeList)
				.claim("name", authenticatedUser.getUsername())
				.claim("userId", authenticatedUser.getId())
				.claim("currencyId", authenticatedUser.getCurrencyId())
				.claim("entityId", authenticatedUser.getCompanyId())
				.claim("languageId", authenticatedUser.getLanguageId())
				.claim("roleId", authenticatedUser.getMainRoleId())
				.claim("role", RoleType.getRoleTypeById(authenticatedUser.getMainRoleId()).getAuthorityTitle())
				.claim("locale", authenticatedUser.getLocale().toString())
				.subject(authenticatedUser.getUsername() + ";" + authenticatedUser.getCompanyId())
				.issuer("BillingHub")
				.id(UUID.randomUUID().toString())
				.issuedAt(Date.from(now))
				.audience().add("BillingHub").and()
				.header().add("type", "jwt").and()
				.expiration(Date.from(now.plus(TOKEN_VALIDITY_IN_HOURS, ChronoUnit.HOURS)))
				.signWith(privateKey)
				.compact();
		logger.debug("jwtToken={} created for user={} for entityId={}", jwtToken,
				authenticatedUser.getId(), authenticatedUser.getCompanyId());
		return jwtToken;
	}

	public JwtTokenResponseWS createJwtToken(User authenticatedUser) {
		Assert.notNull(authenticatedUser, "user required to create a token");
		try {
			// generate refresh token.
			String refreshToken = refreshTokenService.createOrUpdateRefreshToken(authenticatedUser.getId());
			String jwt = jwtToken(authenticatedUser);
			return new JwtTokenResponseWS(jwt, refreshToken, System.currentTimeMillis());
		} catch(Exception ex) {
			logger.error("token generation failed for user={}, entity={} because of:",
					authenticatedUser.getUsername(), authenticatedUser.getCompanyId(), ex);
			throw new SessionInternalError("Token generation failed", HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public List<String> collectAuthorities(UserDTO user) {
		List<String> authorities = new ArrayList<>();
		UserBL userBL = new UserBL(user);
		for (PermissionDTO permission : userBL.getPermissions()) {
			permission.initializeAuthority();
			authorities.add(permission.getAuthority());
		}
		for (RoleDTO role : user.getRoles()) {
			role.initializeAuthority();
			authorities.add(role.getAuthority());
		}
		return authorities;
	}

	@SuppressWarnings("unchecked")
	public JwtDecodedTokenInfoWS verifyToken(String token) {
		JwtDecodedTokenInfoWS tokenInfo = new JwtDecodedTokenInfoWS(token);
		try {
			Jws<Claims> claims = Jwts.parser()
					.verifyWith(publicKey)
					.build()
					.parseSignedClaims(token);
			logger.debug("token={} valid", token);
			tokenInfo.setClaims(claims.getPayload());
			Integer userId = (Integer) claims.getPayload().get("userId");
			Integer entityId = (Integer) claims.getPayload().get("entityId");
			UserDTO user = userDAS.findNow(userId);
			// check user belongs to same entity and not deleted.
			if(null == user || 1 == user.getDeleted() || entityId != user.getCompany().getId()) {
				tokenInfo.setErrorMessage("user not found in system");
				tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
			}
			// check for password change.
            assert user != null;
            Date passwordChangeDate = user.getChangePasswordDate();
			Date jwtCreateDate = claims.getPayload().getIssuedAt();
			if(null!= passwordChangeDate && passwordChangeDate.after(jwtCreateDate)) {
				tokenInfo.setErrorMessage("user password is changed, re authenticate user");
				tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
			}
			// check for permission.
			List<String> scopeList = (List<String>) claims.getPayload().get("scope");
			if(!collectAuthorities(user).equals(scopeList)) {
				tokenInfo.setHttpStatusCode(HttpStatus.SC_FORBIDDEN);
				tokenInfo.setErrorMessage("User permission has been changed, (re-authenticate user)");
			}
			UserBL userBL = new UserBL(user);
			if(userBL.isAccountLocked()) {
				tokenInfo.setErrorMessage("user account is locked");
				tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
			}

			if(userBL.validateAccountExpired(user.getAccountDisabledDate())) {
				tokenInfo.setErrorMessage("user account is expired");
				tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
			}

			if(userBL.isPasswordExpired()) {
				tokenInfo.setErrorMessage("credentials expired");
				tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
			}
		} catch(JwtException jwtException) {
			logger.error("token verification failed because of", jwtException);
			tokenInfo.setErrorMessage(jwtException.getLocalizedMessage());
			tokenInfo.setHttpStatusCode(HttpStatus.SC_UNAUTHORIZED);
		} catch (Exception e) {
			logger.error("token verification failed because of, ", e);
			throw new SessionInternalError("Token Verification failed",
					"Can not verify token at a moment (contact support)",
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
		return tokenInfo;
	}

	private void loadPrivateKey() {
		try {
			String privateKeyFilePath = environment.getProperty(PRIVATE_KEY_FILE_PATH);
			if(StringUtils.isEmpty(privateKeyFilePath)) {
				String defaultPath = defaultPath(PRIVATE_KEY_FILE_NAME);
				logger.debug("environment variable={} not set for private Key, so using default={}", PRIVATE_KEY_FILE_PATH, defaultPath);
				privateKeyFilePath = defaultPath;
			}
			logger.debug("loading private key from={}", privateKeyFilePath);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(privateKeyFilePath)));
			KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
			privateKey = kf.generatePrivate(keySpec);
			logger.debug("private key loaded");
		} catch(IOException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
			logger.error("private key loading failed", exception);
			throw new SessionInternalError("private key file loading failed", exception);
		}
	}

	private String defaultPath(String fileName) {
		return Objects.requireNonNull(JwtUtils.class.getResource("/" + fileName)).getFile();
	}

	private void loadPublicKey() {
		try {
			String publicKeyFilePath = environment.getProperty(PUBLIC_KEY_FILE_PATH);
			if(StringUtils.isEmpty(publicKeyFilePath)) {
				String defaultPath = defaultPath(PUBLIC_KEY_FILE_NAME);
				logger.debug("environment variable={} not set for public Key, so using default={}", PUBLIC_KEY_FILE_PATH, defaultPath);
				publicKeyFilePath = defaultPath;
			}
			logger.debug("loading public key from={}", publicKeyFilePath);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Files.readAllBytes(Paths.get(publicKeyFilePath)));
			KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
			publicKey = kf.generatePublic(keySpec);
			logger.debug("public key loaded");
		} catch(IOException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
			logger.error("public key loading failed", exception);
			throw new SessionInternalError("public key file loading failed", exception);
		}
	}

	public String adennetJwtToken(String subscriberNumber, Integer userId, Integer entityId) {
		Instant now = Instant.now();
		String jwtToken = Jwts.builder()
				.claim("userId", userId)
				.claim("subscriberNumber", subscriberNumber)
				.claim("entityId", entityId)
				.subject(subscriberNumber + ";" + userId)
				.issuer("BillingHub")
				.id(UUID.randomUUID().toString())
				.issuedAt(Date.from(now))
				.audience().add("BillingHub").and()
				.header().add("type", "jwt").and()
				.expiration(Date.from(now.plus(TOKEN_VALIDITY_IN_MINUTES, ChronoUnit.MINUTES)))
				.signWith(privateKey)
				.compact();
		logger.debug("jwtToken={} created for subscriber={} and userId={} ", jwtToken,
				subscriberNumber, userId);
		return jwtToken;
	}

	public AdennetJwtTokenResponseWS createAdennetJwtToken(UserAndAssetAssociationResponseWS subscriber){
		Assert.notNull(subscriber.getSubscriberNumber(), "Subscriber number is required to create a token");
		try {
			// If subscriber is suspended or released, then it will not create jwt token and refresh token
			if (subscriber.getAssetStatus().equalsIgnoreCase(AdennetConstants.ASSET_STATUS_IN_USE) && subscriber.getUserId() != null
					&& Boolean.FALSE.equals(subscriber.getIsAssetDeleted()) && Boolean.FALSE.equals(subscriber.getIsSuspended()) && Boolean.FALSE.equals(subscriber.getIsUserDeleted())){
				// Generate refresh token
				String refreshToken = refreshTokenService.createOrUpdateRefreshToken(subscriber.getUserId());
				// Generate jwt token
				String jwtToken = adennetJwtToken(subscriber.getSubscriberNumber(), subscriber.getUserId(), subscriber.getEntityId());
				// Get subscriber language description
				String languageDescription = new LanguageDAS().getLanguageDescriptionByUserId(subscriber.getUserId());
				return new AdennetJwtTokenResponseWS(jwtToken,refreshToken, System.currentTimeMillis(), AdennetConstants.TOKEN_TYPE_BEARER, subscriber.getSubscriberNumber(), subscriber.getIsSuspended(), subscriber.getIsAssetDeleted(), subscriber.getIsUserDeleted(), subscriber.getAssetStatus(), languageDescription);
			}
			return new AdennetJwtTokenResponseWS(null, null, System.currentTimeMillis(), null, subscriber.getSubscriberNumber(),subscriber.getIsSuspended(), subscriber.getIsAssetDeleted(), subscriber.getIsUserDeleted(), subscriber.getAssetStatus(), null);

		}catch (Exception ex){
			logger.error("Token generation failed for subscriber={} because of:",
					subscriber.getSubscriberNumber(), ex);
			throw new SessionInternalError("Token generation failed", HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
