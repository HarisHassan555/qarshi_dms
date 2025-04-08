package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the account_authentication database table.
 * 
 */
@Entity
@Table(name="account_authentication", schema="mmp")
@NamedQuery(name="AccountAuthentication.findAll", query="SELECT a FROM AccountAuthentication a")
public class AccountAuthentication implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="account_authid")
	private Long accountAuthid;



	@Column(name="is_forced_change_password")
	private Boolean isForcedChangePassword;

	@Column(name="last_login")
	private Timestamp lastLogin;

	private String password;

	private Long roleid;

	private String username;

	public AccountAuthentication() {
	}

	public Long getAccountAuthid() {
		return this.accountAuthid;
	}

	public void setAccountAuthid(Long accountAuthid) {
		this.accountAuthid = accountAuthid;
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
	
	
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}


	@OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "accountid", referencedColumnName = "accountid")
    private Account account;

}