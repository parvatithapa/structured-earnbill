package com.sapienter.jbilling.auth;


import grails.plugin.springsecurity.SpringSecurityUtils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.SessionInternalError;

@Component("billingHubMicroServiceAuthenticationFilter")
public class BillingHubMicroServiceAuthenticationFilter extends AbstarctFilter {

	private static final String MICROSERVICE_AUTH_TABLE_NAME_MF_NAME = "microservice.authorization.table.name";
	private static final String AUTHORIZATION_HEADER = "X-MicroService-Authorization";
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.getAuthenticationEntryPoint(), "An AuthenticationEntryPoint is required");
	}

	private class CredContainer {
		private String entityId;
		private String serviceName;
	}

	/**
	 * Extracts entityId and serviceName.
	 * @param token
	 * @return
	 */
	private CredContainer extractUserAndEntityId(String token) {
		try {
			token = new String(Base64.getDecoder().decode(token));
			String[] creds = token.split(":");
			if(2 != creds.length) {
				throw new BadCredentialsException("invalid token passed");
			}
			CredContainer credContainer = new CredContainer();
			credContainer.entityId = creds[0];
			credContainer.serviceName = creds[1];
			return credContainer;
		} catch(IllegalArgumentException illegalArgumentException) {
			throw new BadCredentialsException("invalid token passed");
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String token = request.getHeader(AUTHORIZATION_HEADER);
		if(StringUtils.isNotEmpty(token)) {
			try {
				// extract token from authorization header.
				// load user from authorization repo by entityId and serviceName.
				authentication(extractUserAndEntityId(token));
			} catch(Exception exception) {
				SecurityContextHolder.clearContext();
				log.error("Authentication request failed", exception);
				if(exception instanceof AuthenticationException) {
					this.getAuthenticationEntryPoint().commence(request, response, (AuthenticationException) exception);
				} else {
					response.sendError(500, exception.getMessage());
				}
				return;
			}
		}
		filterChain.doFilter(req, res);
	}

	private static final String FIND_MICROSERVICE_AUTHORIZATION_TABLE_NAME_SQL =
			"SELECT string_value FROM meta_field_value "
					+ " WHERE dtype = 'string' AND meta_field_name_id = "
					+ "(SELECT id FROM meta_field_name WHERE entity_type = 'COMPANY' "
					+ "AND entity_id = ? AND name = ?)";


	private static final String FIND_USER_FOR_ENTITY_AND_MICROSERVICE_SQL =
			"SELECT user_id FROM %s WHERE entity_id = ? AND service_name = ?";

	/**
	 * find micro service authorization table name by entityId and company level metafield.
	 * @param entityId
	 * @param metaFieldName
	 * @return
	 */
	private String findMicroserviceAuthorizationTableName(Integer entityId) {
		SqlRowSet tableNameRow = getJdbcTemplate().queryForRowSet(FIND_MICROSERVICE_AUTHORIZATION_TABLE_NAME_SQL,
				entityId, MICROSERVICE_AUTH_TABLE_NAME_MF_NAME);
		if(!tableNameRow.next()) {
			log.error("Company Level Meta Field {} not found for entity {} ", MICROSERVICE_AUTH_TABLE_NAME_MF_NAME, entityId);
			throw new SessionInternalError("Company Level Meta Field " + MICROSERVICE_AUTH_TABLE_NAME_MF_NAME + " not found for entity "+ entityId);
		}

		String tableName = tableNameRow.getString("string_value");
		if(!isTablePresent(getJdbcTemplate(), tableName)) {
			log.error("Data Table: {} not found for entity {} ", tableName, entityId);
			throw new SessionInternalError("Data Table:" + tableName + " not found for entity "+ entityId);
		}
		return tableName;
	}

	private boolean isTablePresent(JdbcTemplate jdbcTemplate, String tableName) {
		try (Connection connection = jdbcTemplate.getDataSource().getConnection();
				ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null);) {
			return rs.next();
		} catch (SQLException sqlException) {
			throw new SessionInternalError(sqlException);
		}
	}

	private static final String CREDS_NOT_FOUND_ERROR_MESSAGE_FORMAT = "creds not found microservice=%s and entityId=%s";
	private static final String BAD_CREDS_ERROR_MESSAGE_FORMAT = "bad creds for microservice=%s and entityId=%s";

	private void authentication(CredContainer credContainer) {
		String authRepoTable = findMicroserviceAuthorizationTableName(Integer.parseInt(credContainer.entityId));
		SqlRowSet result = getJdbcTemplate().queryForRowSet(String.format(FIND_USER_FOR_ENTITY_AND_MICROSERVICE_SQL, authRepoTable),
				credContainer.entityId, credContainer.serviceName);
		if(!result.next()) {
			String errorMessage = String.format(CREDS_NOT_FOUND_ERROR_MESSAGE_FORMAT, credContainer.serviceName, credContainer.entityId);
			log.error(errorMessage);
			throw new AuthenticationCredentialsNotFoundException(errorMessage);
		}
		try {
			Integer userId = Integer.parseInt(result.getString("user_id"));
			String username = findUsernameByIdAndEntityId(userId, Integer.parseInt(credContainer.entityId));
			if(authenticationIsRequired(username)) {
				// re authenticating user.
				SpringSecurityUtils.reauthenticate(username + ";"+ credContainer.entityId, StringUtils.EMPTY);
				log.debug("userId={} authenticated for service={}, entityId={}",
						userId, credContainer.serviceName, credContainer.entityId);
			}
		} catch(NumberFormatException numberFormatException) {
			String errorMessage = String.format(BAD_CREDS_ERROR_MESSAGE_FORMAT, credContainer.serviceName, credContainer.entityId);
			log.error(errorMessage);
			throw new BadCredentialsException(errorMessage);
		}
	}
}
