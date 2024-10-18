package com.sapienter.jbilling.server.apiUserDetail.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.client.authentication.CompanyUserDetailsService;
import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS;
import com.sapienter.jbilling.server.apiUserDetail.db.ApiUserDetailDAS;
import com.sapienter.jbilling.server.apiUserDetail.db.ApiUserDetailDTO;

public class ApiUserDetailBL {

	private ApiUserDetailDAS  das;
	private CompanyUserDetailsService userDetailsService;
	private JBillingPasswordEncoder passwordEncoder;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public void setUserDetailsService(CompanyUserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public void setPasswordEncoder(JBillingPasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public ApiUserDetailBL(){
		this.das = new ApiUserDetailDAS();
	}

	public static ApiUserDetailDTO getDTO(ApiUserDetailWS ws){
		if(ws == null)
			return null;

		ApiUserDetailDTO apiUserDetail = new ApiUserDetailDTO();

		apiUserDetail.setAccessCode(ws.getAccessCode());
		apiUserDetail.setCompanyId(ws.getCompanyId());
		apiUserDetail.setUserName(ws.getUserName());

		return apiUserDetail;
	}

	public static ApiUserDetailWS getWS(ApiUserDetailDTO dto){
		if(dto == null)
			return null;

		ApiUserDetailWS apiUserDetail=new ApiUserDetailWS();
		apiUserDetail.setAccessCode(dto.getAccessCode());
		apiUserDetail.setCompanyId(dto.getCompanyId());
		apiUserDetail.setUserName(dto.getUserName());

		return apiUserDetail;
	}

	public ApiUserDetailDTO create(ApiUserDetailDTO dto) {
		try {
			dto = das.save(dto);
			logger.debug("API user details saved successfully");
		} catch(HibernateException he) {
			logger.error("ApiUserDetailBL.create Exception while api user details insertion", he);
			throw new SessionInternalError("Data access issue while creating ApiUserDetai: " + he.getMessage(),
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}

		return dto;
	}

	public Boolean authenticateUser(ApiUserDetailWS ws){
		String usernameWithComanyId = ws.getUserName() + ";" +ws.getCompanyId();
		CompanyUserDetails companyUserDetails = (CompanyUserDetails)userDetailsService.loadUserByUsername(usernameWithComanyId);

		if(companyUserDetails == null){
			return false;
		}

		return passwordEncoder.isPasswordValid(companyUserDetails.getPassword(), ws.getPassword(), companyUserDetails);
	}

	public String generateAccessCode(String tokenString) {
		try {
			tokenString = tokenString.concat(Integer.toHexString(new Random().nextInt(100)));
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(tokenString.getBytes());
			StringBuffer sb = new StringBuffer();
			for (byte element : array) {
				sb.append(Integer.toHexString((element & 0xFF) | 0x100).substring(1,3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new SessionInternalError("access code generation issue: " + e.getMessage(),
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public List<ApiUserDetailWS> findAll(Integer max, Integer offset, Integer companyId) {
		try {

			List<ApiUserDetailDTO> userDetailDTOList = das.findAll(max, offset, companyId);
			if(CollectionUtils.isNotEmpty(userDetailDTOList)) {
				return das.findAll(max, offset, companyId).stream().map(x -> getWS(x)).collect(Collectors.toList());
			}

			return new ArrayList<>();

		} catch(HibernateException he) {
			logger.error("Exception in APIUserDetailBL.findAll ", he);
			throw new SessionInternalError("Data access issue: " + he.getMessage(),
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public Long countAllApiUserDetail() {
		try {
			return das.countAll();
		} catch(HibernateException he) {
			logger.error("Exception in APIUserDetailBL.countAllApiUserDetail ", he);
			throw new SessionInternalError("Data access issue: " + he.getMessage(),
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public ApiUserDetailWS getUserDetails(String accessCode){
		try {
			ApiUserDetailDTO apiUserDetailDTO = das.findByAccessCode(accessCode);
			return getWS(apiUserDetailDTO);
		} catch(HibernateException he) {
			logger.error("Exception in APIUserDetailBL.getUserDetails ", he);
			throw new SessionInternalError("Data access issue: " + he.getMessage(),
					HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
