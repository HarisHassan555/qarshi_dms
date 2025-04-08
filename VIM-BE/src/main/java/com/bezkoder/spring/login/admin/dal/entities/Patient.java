package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.sql.Timestamp;


/**
 * The persistent class for the patient database table.
 * 
 */
@Entity
@Table(name="patient")
@NamedQuery(name="Patient.findAll", query="SELECT p FROM Patient p")
public class Patient implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long patientid;

	@Column(name="connection_status")
	private String connectionStatus;

	@Temporal(TemporalType.DATE)
	@Column(name="date_of_birth")
	private Date dateOfBirth;

	@Column(name="email_ids")
	private String emailIds;

	@Column(name="first_name")
	private String firstName;

	private String gender;

	@Column(name="home_phone_no")
	private String homePhoneNo;

	@Column(name="last_name")
	private String lastName;

	@Column(name="last_sync_message")
	private String lastSyncMessage;

	@Column(name="last_sync_time")
	private Timestamp lastSyncTime;

	@Column(name="merge_raw_json")
	private String mergeRawJson;

	@Column(name="middle_name")
	private String middleName;

	@Column(name="mobile_phone_no")
	private String mobilePhoneNo;

	private String mrn;

	@Column(name="new_raw_json")
	private String newRawJson;

	private String nist;

	@Column(name="office_phone_no")
	private String officePhoneNo;

	private String ssn;

	@Column(name="sync_statusid")
	private Long syncStatusid;

	@Column(name="update_raw_json")
	private String updateRawJson;

	//bi-directional many-to-one association to Account
	@ManyToOne
	@JoinColumn(name="accountid")
	private Account account;

	//bi-directional many-to-one association to Ehr
	@ManyToOne
	@JoinColumn(name="ehrid")
	private Ehr ehr;

	public Patient() {
	}

	public Long getPatientid() {
		return this.patientid;
	}

	public void setPatientid(Long patientid) {
		this.patientid = patientid;
	}

	public String getConnectionStatus() {
		return this.connectionStatus;
	}

	public void setConnectionStatus(String connectionStatus) {
		this.connectionStatus = connectionStatus;
	}

	public Date getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getEmailIds() {
		return this.emailIds;
	}

	public void setEmailIds(String emailIds) {
		this.emailIds = emailIds;
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

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getLastSyncMessage() {
		return this.lastSyncMessage;
	}

	public void setLastSyncMessage(String lastSyncMessage) {
		this.lastSyncMessage = lastSyncMessage;
	}

	public Timestamp getLastSyncTime() {
		return this.lastSyncTime;
	}

	public void setLastSyncTime(Timestamp lastSyncTime) {
		this.lastSyncTime = lastSyncTime;
	}

	public String getMergeRawJson() {
		return this.mergeRawJson;
	}

	public void setMergeRawJson(String mergeRawJson) {
		this.mergeRawJson = mergeRawJson;
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

	public String getMrn() {
		return this.mrn;
	}

	public void setMrn(String mrn) {
		this.mrn = mrn;
	}

	public String getNewRawJson() {
		return this.newRawJson;
	}

	public void setNewRawJson(String newRawJson) {
		this.newRawJson = newRawJson;
	}

	public String getNist() {
		return this.nist;
	}

	public void setNist(String nist) {
		this.nist = nist;
	}

	public String getOfficePhoneNo() {
		return this.officePhoneNo;
	}

	public void setOfficePhoneNo(String officePhoneNo) {
		this.officePhoneNo = officePhoneNo;
	}

	public String getSsn() {
		return this.ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public Long getSyncStatusid() {
		return this.syncStatusid;
	}

	public void setSyncStatusid(Long syncStatusid) {
		this.syncStatusid = syncStatusid;
	}

	public String getUpdateRawJson() {
		return this.updateRawJson;
	}

	public void setUpdateRawJson(String updateRawJson) {
		this.updateRawJson = updateRawJson;
	}

	public Account getAccount() {
		return this.account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public Ehr getEhr() {
		return this.ehr;
	}

	public void setEhr(Ehr ehr) {
		this.ehr = ehr;
	}

}