package com.sapienter.jbilling.twofactorauth.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;


@Entity
@Table(name = "user_2fa_log")
public class User2FALogDTO {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_2fa_log_generator")
	@SequenceGenerator(name = "user_2fa_log_generator", sequenceName = "user_2fa_log_seq", allocationSize = 1)
	private long id;
	@Column(name = "session_id", unique = true)
	private String sessionId;
	@Column(name = "two_af_id")
	private String twoFAId;
	@Column(name = "otp")
	private String otp;
	@Column(name = "user_id", nullable = false)
	private Integer userId;
	@Column(name = "entity_id", nullable = false)
	private Integer entityId;
	@Column(name="timestamp")
	private Date timestamp = new Date();

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getTwoFAId() {
		return twoFAId;
	}

	public void setTwoFAId(String twoFAId) {
		this.twoFAId = twoFAId;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
