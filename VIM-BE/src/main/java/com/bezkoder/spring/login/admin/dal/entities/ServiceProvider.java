package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the service_provider database table.
 * 
 */
@Entity
@Table(name="service_provider")
@NamedQuery(name="ServiceProvider.findAll", query="SELECT s FROM ServiceProvider s")
public class ServiceProvider implements Serializable {
	private static final long serialVersionUID = 1L;

//	@Id
//	@Column(name="service_providerid")
//	private Long serviceProviderid;
	
	@Id
	@SequenceGenerator(name="SERVICE_PROVIDER_SERPASSWORDHISTORYID_GENERATOR", sequenceName="cfg_tbl_service_provider_service_providerid_seq", initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SERVICE_PROVIDER_SERPASSWORDHISTORYID_GENERATOR")
	@Column(name="service_providerid")
	private Long serviceProviderid;

	@Column(name="access_token")
	private String accessToken;

	private String apikey;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private Long createdBy;

	@Column(name="is_active")
	private Boolean isActive;

	@Column(name="refresh_token")
	private String refreshToken;

	private String secret;

	@Column(name="service_provider_name")
	private String serviceProviderName;

	@Column(name="token_expiration")
	private Timestamp tokenExpiration;

	@Column(name="updated_at")
	private Timestamp updatedAt;

	@Column(name="updated_by")
	private Long updatedBy;

	public ServiceProvider() {
	}

	public Long getServiceProviderid() {
		return this.serviceProviderid;
	}

	public void setServiceProviderid(Long serviceProviderid) {
		this.serviceProviderid = serviceProviderid;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getApikey() {
		return this.apikey;
	}

	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Long getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getRefreshToken() {
		return this.refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getSecret() {
		return this.secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getServiceProviderName() {
		return this.serviceProviderName;
	}

	public void setServiceProviderName(String serviceProviderName) {
		this.serviceProviderName = serviceProviderName;
	}

	public Timestamp getTokenExpiration() {
		return this.tokenExpiration;
	}

	public void setTokenExpiration(Timestamp tokenExpiration) {
		this.tokenExpiration = tokenExpiration;
	}

	public Timestamp getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getUpdatedBy() {
		return this.updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	@Column(name="bln_status")
	private Boolean blnStatus;

	public Boolean getBlnStatus() {
		return blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}
}