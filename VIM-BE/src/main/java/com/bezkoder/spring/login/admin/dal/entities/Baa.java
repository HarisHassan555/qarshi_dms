package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;


/**
 * The persistent class for the baa database table.
 * 
 */
@Entity
@Table(name="baa")
@NamedQuery(name="Baa.findAll", query="SELECT b FROM Baa b")
public class Baa implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="BAA_BAAID_GENERATOR", sequenceName="BAA_BAA_ID_SEQ", initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="BAA_BAAID_GENERATOR")
	@Column(name="baaid")
	private Long baaId;

	@Column(name="baa_description")
	private String baaDescription;

	@Column(name="baa_name")
	private String baaName;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private int createdBy;

	@Column(name="is_active")
	private Boolean isActive;

	@Column(name="updated_at")
	private Timestamp updatedAt;

	@Column(name="updated_by")
	private int updatedBy;

	public Baa() {
	}

	public Long getBaaId() {
		return this.baaId;
	}

	public void setBaaId(Long baaId) {
		this.baaId = baaId;
	}

	public String getBaaDescription() {
		return this.baaDescription;
	}

	public void setBaaDescription(String baaDescription) {
		this.baaDescription = baaDescription;
	}

	public String getBaaName() {
		return this.baaName;
	}

	public void setBaaName(String baaName) {
		this.baaName = baaName;
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

	public Boolean getIsActive() {
		return this.isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Timestamp getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public int getUpdatedBy() {
		return this.updatedBy;
	}

	public void setUpdatedBy(int updatedBy) {
		this.updatedBy = updatedBy;
	}
	

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}


	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	

	@Column(name="bln_Status")
	private Boolean blnStatus;

	public Boolean getBlnStatus() {
		return blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}
	

}