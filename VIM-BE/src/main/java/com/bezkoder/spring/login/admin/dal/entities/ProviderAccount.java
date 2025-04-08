package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the provider_account database table.
 * 
 */
@Entity
@Table(name="provider_account")
@NamedQuery(name="ProviderAccount.findAll", query="SELECT p FROM ProviderAccount p")
public class ProviderAccount implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="provider_accountid")
	private Long providerAccountid;

	@Column(name="email_id")
	private String emailId;

	@Column(name="first_name")
	private String firstName;

	private String gender;

	@Column(name="last_name")
	private String lastName;

	@Column(name="middle_name")
	private String middleName;

	@Column(name="mobile_phone_no")
	private String mobilePhoneNo;

	

	public ProviderAccount() {
	}

	public Long getProviderAccountid() {
		return this.providerAccountid;
	}

	public void setProviderAccountid(Long providerAccountid) {
		this.providerAccountid = providerAccountid;
	}

	public String getEmailId() {
		return this.emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getGender() {
		return this.gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return this.middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getMobilePhoneNo() {
		return this.mobilePhoneNo;
	}

	public void setMobilePhoneNo(String mobilePhoneNo) {
		this.mobilePhoneNo = mobilePhoneNo;
	}

	

	
	
	
	@Column(name="updated_at")
	private Timestamp updatedAt;

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}


	@Column(name="updated_by")
	private Long updatedBy;
	
	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private int createdBy;
	
	

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnStatus() {
		return blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	

	@Column(name="bln_Status")
	private Boolean blnStatus;
	
	
	public String getTxtContact() {
		return txtContact;
	}

	public void setTxtContact(String txtContact) {
		this.txtContact = txtContact;
	}




	@Column(name="txt_contact")
	private String txtContact;
	
	
/*	 public ProviderAuthentication getProviderAuthentication() {
		return providerAuthentication;
	}

	public void setProviderAuthentication(ProviderAuthentication providerAuthentication) {
		this.providerAuthentication = providerAuthentication;
	}


	@OneToOne(mappedBy = "providerAccount")
	    private ProviderAuthentication providerAuthentication;*/

}