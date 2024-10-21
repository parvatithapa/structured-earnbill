package com.sapienter.jbilling.auth;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Service
@Transactional
public class RefreshTokenService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	//TODO get from company level meta field.
	private static final Integer REFRESH_TOKEN_VALIDITY_IN_DAYS = 10;

	@Resource
	private RefreshTokenDAS refreshTokenDAS;
	@Resource
	private UserDAS userDAS;

	public String createOrUpdateRefreshToken(Integer userId) {
		Assert.notNull(userId, "userId is required property");
		// lock user.
		UserDTO user = userDAS.findForUpdate(userId);
		Integer entityId = user.getCompany().getId();
		RefreshTokenDTO refreshToken = refreshTokenDAS.findByUserId(userId);
		if(null == refreshToken) {
			refreshToken = new RefreshTokenDTO();
			refreshToken.setExpiryDate(expiryDate(entityId));
			refreshToken.setToken(UUID.randomUUID().toString());
			refreshToken.setUserId(userId);
			refreshToken = refreshTokenDAS.save(refreshToken);
			logger.debug("refreshToken={} created for user={}", refreshToken.getToken(), refreshToken.getUserId());
		} else {
			Date now = TimezoneHelper.companyCurrentDate(entityId);
			if(refreshToken.getExpiryDate().before(now)) {
				refreshToken.setExpiryDate(expiryDate(entityId));
				refreshToken.setToken(UUID.randomUUID().toString());
				logger.debug("refreshToken={} updated for user={}", refreshToken.getToken(), refreshToken.getUserId());
			}
		}
		return refreshToken.getToken();
	}

	private Date expiryDate(Integer entityId) {
		Calendar createDateTime = Calendar.getInstance();
		createDateTime.setTime(TimezoneHelper.companyCurrentDate(entityId));
		createDateTime.add(Calendar.DATE, REFRESH_TOKEN_VALIDITY_IN_DAYS);
		return createDateTime.getTime();
	}

	public RefreshTokenDTO findRefreshToken(String token) {
		Assert.hasLength(token, "refreshToken is required property");
		return refreshTokenDAS.findByToken(token);
	}

	public boolean isExpired(String token) {
		Assert.hasLength(token, "refreshToken is required property");
		RefreshTokenDTO refreshToken = refreshTokenDAS.findByToken(token);
		if(null == refreshToken) {
			throw new SessionInternalError("refreshToken validation failed", "refreshToken not found", HttpStatus.SC_NOT_FOUND);
		}
		Integer entityId = userDAS.find(refreshToken.getUserId()).getCompany().getId();
		Date now = TimezoneHelper.companyCurrentDate(entityId);
		if(refreshToken.getExpiryDate().before(now)) {
			refreshTokenDAS.delete(refreshToken);
			logger.debug("refreshToken={} expried, so deleting it", refreshToken);
			return true;
		}
		return false;
	}

}