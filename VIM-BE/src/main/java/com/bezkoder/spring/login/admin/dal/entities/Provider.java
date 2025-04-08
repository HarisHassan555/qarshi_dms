package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.*;
import java.util.Date;


/**
 * The persistent class for the provider database table.
 * 
 */
@Entity
@Table(name="provider")
@NamedQuery(name="Provider.findAll", query="SELECT p FROM Provider p")
public class Provider implements Serializable {
	private static final long serialVersionUID = 1L;

//	@Id
//	@GeneratedValue(strategy=GenerationType.AUTO)
//	private Long providerid;
	
	@Id
	@SequenceGenerator(name="PROVIDER_PROVIDERID_GENERATOR", sequenceName="provider_provider_id_seq", initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="PROVIDER_PROVIDERID_GENERATOR")
	@Column(name="providerid")
	private Long providerid;

	@Temporal(TemporalType.DATE)
	@Column(name="date_of_birth")
	private Date dateOfBirth;

	@Column(name="first_name")
	private String firstName;

	private String gender;

	@Column(name="home_phone_no")
	private String homePhoneNo;

	@Column(name="is_active")
	private Boolean isActive;

	@Column(name="last_name")
	private String lastName;

	@Column(name="middle_name")
	private String middleName;

	@Column(name="mobile_phone_no")
	private String mobilePhoneNo;

	private String npi;

	@Column(name="office_phone_no")
	private String officePhoneNo;

	//bi-directional many-to-one association to Ehr
	@ManyToOne
	@JoinColumn(name="ehrid")
	private Ehr ehr;

	//bi-directional many-to-one association to ProviderAccount
	@ManyToOne
	@JoinColumn(name="provideraccountid")
	private ProviderAccount providerAccount;

	public ProviderAccount getProviderAccount() {
		return providerAccount;
	}

	public void setProviderAccount(ProviderAccount providerAccount) {
		this.providerAccount = providerAccount;
	}

	public Provider() {
	}

	public Long getProviderid() {
		return this.providerid;
	}

	public void setProviderid(Long providerid) {
		this.providerid = providerid;
	}

	public Date getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
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

	public String getHomePhoneNo() {
		return this.homePhoneNo;
	}

	public void setHomePhoneNo(String homePhoneNo) {
		this.homePhoneNo = homePhoneNo;
	}

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
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

	public String getNpi() {
		return this.npi;
	}

	public void setNpi(String npi) {
		this.npi = npi;
	}

	public String getOfficePhoneNo() {
		return this.officePhoneNo;
	}

	public void setOfficePhoneNo(String officePhoneNo) {
		this.officePhoneNo = officePhoneNo;
	}

	public Ehr getEhr() {
		return this.ehr;
	}

	public void setEhr(Ehr ehr) {
		this.ehr = ehr;
	}

	
	
	
	

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

	@Column(name="updated_at")
	private Timestamp updatedAt;

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
	
	
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	@Column(name="emailid")
	private String emailId;

	
	
	

}