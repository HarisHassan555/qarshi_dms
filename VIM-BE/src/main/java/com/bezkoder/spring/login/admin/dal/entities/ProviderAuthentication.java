package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the provider_authentication database table.
 * 
 */
@Entity
@Table(name="provider_authentication")
@NamedQuery(name="ProviderAuthentication.findAll", query="SELECT p FROM ProviderAuthentication p")
public class ProviderAuthentication implements Serializable {
	private static final long serialVersionUID = 1L;
	
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="provider_authid")
	private Long providerAuthid;

	@Column(name="is_forced_change_password")
	private Boolean isForcedChangePassword;

	@Column(name="last_login")
	private Timestamp lastLogin;

	private String password;

//	private Long provideraccountid;

	private Long providerid;

	private Long roleid;

	private String username;

	public ProviderAuthentication() {
	}

	public Long getProviderAuthid() {
		return this.providerAuthid;
	}

	public void setProviderAuthid(Long providerAuthid) {
		this.providerAuthid = providerAuthid;
	}

	public Boolean getIsForcedChangePassword() {
		return this.isForcedChangePassword;
	}

	public void setIsForcedChangePassword(Boolean isForcedChangePassword) {
		this.isForcedChangePassword = isForcedChangePassword;
	}

	public Timestamp getLastLogin() {
		return this.lastLogin;
	}

	public void setLastLogin(Timestamp lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getProviderid() {
		return this.providerid;
	}

	public void setProviderid(Long providerid) {
		this.providerid = providerid;
	}

	public Long getRoleid() {
		return this.roleid;
	}

	public void setRoleid(Long roleid) {
		this.roleid = roleid;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	
	@OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "provideraccountid", referencedColumnName = "provider_accountid")
    private ProviderAccount providerAccount;

	public ProviderAccount getProviderAccount() {
		return providerAccount;
	}

	public void setProviderAccount(ProviderAccount providerAccount) {
		this.providerAccount = providerAccount;
	}

}