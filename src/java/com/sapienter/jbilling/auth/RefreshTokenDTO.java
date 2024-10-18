package com.sapienter.jbilling.auth;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@TableGenerator(
		name            = "refresh_token_GEN",
		table           = "jbilling_seqs",
		pkColumnName    = "name",
		valueColumnName = "next_id",
		pkColumnValue   = "refresh_token"
		)
@Table(name = "refresh_token")
public class RefreshTokenDTO {

	private Integer id;
	private String token;
	private Date expiryDate;
	private Integer userId;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "refresh_token_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "token", nullable = false)
	public String getToken() {
		return token;
	}

	@Column(name = "expiry_date", nullable = false)
	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Column(name = "user_id", nullable = false)
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}
}