package com.bezkoder.spring.login.admin.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/***
 * 
 * @author Usman Ali
 *
 */
public class CustomUsernamePasswordAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private int voId;
	private Object firstName;
	private Object lastName;
	private int fkRegionId;
	private Boolean regionHead;
	public void setPasswordChange(boolean isPasswordChange) {
		this.isPasswordChange = isPasswordChange;
	}

	public boolean isPasswordChange() {
		return isPasswordChange;
	}

	private boolean isPasswordChange;
	

	public CustomUsernamePasswordAuthenticationToken(Object principal,
			Object credentials,
			Collection<? extends GrantedAuthority> authorities) {
		super(principal, credentials, authorities);
	}
	
	public CustomUsernamePasswordAuthenticationToken(int voId,Object principal,Object credentials,Object firstName,
			Object lastName,boolean isPasswordChange,Collection<? extends GrantedAuthority> authorities )
	{
		super(principal, credentials, authorities);
		this.voId = voId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.fkRegionId= fkRegionId;
		this.regionHead = regionHead;
		this.isPasswordChange=isPasswordChange;
	}

	public Object getFirstName() {
		return firstName;
	}

	public void setFirstName(Object firstName) {
		this.firstName = firstName;
	}

	public Object getLastName() {
		return lastName;
	}

	public void setLastName(Object lastName) {
		this.lastName = lastName;
	}

	public int getFkRegionId() {
		return fkRegionId;
	}

	public void setFkRegionId(int fkRegionId) {
		this.fkRegionId = fkRegionId;
	}

	public Boolean getRegionHead() {
		return regionHead;
	}

	public void setRegionHead(Boolean regionHead) {
		this.regionHead = regionHead;
	}

	public int getVoId() {
		return voId;
	}

	public void setVoId(int voId) {
		this.voId = voId;
	}
}
