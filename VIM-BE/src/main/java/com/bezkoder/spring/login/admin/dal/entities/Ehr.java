package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the ehr database table.
 * 
 */
@Entity
@Table(name="ehr")
@NamedQuery(name="Ehr.findAll", query="SELECT e FROM Ehr e")
public class Ehr implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long ehrid;

	@Column(name="auth_url")
	private String authUrl;

	@Column(name="auth_url_params")
	private String authUrlParams;



	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="callback_url")
	private String callbackUrl;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private int createdBy;

	@Column(name="ehr_description")
	private String ehrDescription;

	@Column(name="ehr_name")
	private String ehrName;

	@Column(name="external_ehr_id")
	private String externalEhrId;

	@Column(name="is_active")
	private Boolean isActive;

	@Column(name="phone_no")
	private String phoneNo;

	

	@Column(name="token_scope")
	private String tokenScope;

	@Column(name="token_url")
	private String tokenUrl;

	@Column(name="token_url_params")
	private String tokenUrlParams;

	@Column(name="updated_at")
	private Timestamp updatedAt;

	@Column(name="updated_by")
	private Long updatedBy;

	//bi-directional many-to-one association to EhrType
	@ManyToOne
	@JoinColumn(name="ehr_typeid")
	private EhrType ehrType;
	
	
	public Baa getBaa() {
		return baa;
	}

	public void setBaa(Baa baa) {
		this.baa = baa;
	}

	@ManyToOne
	@JoinColumn(name="baaid")
	private Baa baa;
	
	

	//bi-directional many-to-one association to Patient
	@OneToMany(mappedBy="ehr")
	private List<Patient> patients;

	//bi-directional many-to-one association to Provider
	@OneToMany(mappedBy="ehr")
	private List<Provider> providers;
	
	
	
	

	public ServiceProvider getServiceProvider() {
		return serviceProvider;
	}

	public void setServiceProvider(ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	@ManyToOne
	@JoinColumn(name="service_providerid")
	private ServiceProvider serviceProvider;

	public Ehr() {
	}

	public Long getEhrid() {
		return this.ehrid;
	}

	public void setEhrid(Long ehrid) {
		this.ehrid = ehrid;
	}

	public String getAuthUrl() {
		return this.authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getAuthUrlParams() {
		return this.authUrlParams;
	}

	public void setAuthUrlParams(String authUrlParams) {
		this.authUrlParams = authUrlParams;
	}



	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public String getCallbackUrl() {
		return this.callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public int getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public String getEhrDescription() {
		return this.ehrDescription;
	}

	public void setEhrDescription(String ehrDescription) {
		this.ehrDescription = ehrDescription;
	}

	public String getEhrName() {
		return this.ehrName;
	}

	public void setEhrName(String ehrName) {
		this.ehrName = ehrName;
	}

	public String getExternalEhrId() {
		return this.externalEhrId;
	}

	public void setExternalEhrId(String externalEhrId) {
		this.externalEhrId = externalEhrId;
	}

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public String getPhoneNo() {
		return this.phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}



	public String getTokenScope() {
		return this.tokenScope;
	}

	public void setTokenScope(String tokenScope) {
		this.tokenScope = tokenScope;
	}

	public String getTokenUrl() {
		return this.tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public String getTokenUrlParams() {
		return this.tokenUrlParams;
	}

	public void setTokenUrlParams(String tokenUrlParams) {
		this.tokenUrlParams = tokenUrlParams;
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

	public EhrType getEhrType() {
		return this.ehrType;
	}

	public void setEhrType(EhrType ehrType) {
		this.ehrType = ehrType;
	}

	public List<Patient> getPatients() {
		return this.patients;
	}

	public void setPatients(List<Patient> patients) {
		this.patients = patients;
	}

	public Patient addPatient(Patient patient) {
		getPatients().add(patient);
		patient.setEhr(this);

		return patient;
	}

	public Patient removePatient(Patient patient) {
		getPatients().remove(patient);
		patient.setEhr(null);

		return patient;
	}

	public List<Provider> getProviders() {
		return this.providers;
	}

	public void setProviders(List<Provider> providers) {
		this.providers = providers;
	}

	public Provider addProvider(Provider provider) {
		getProviders().add(provider);
		provider.setEhr(this);

		return provider;
	}

	public Provider removeProvider(Provider provider) {
		getProviders().remove(provider);
		provider.setEhr(null);

		return provider;
	}

}