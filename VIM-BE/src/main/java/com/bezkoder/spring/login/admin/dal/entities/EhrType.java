package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the ehr_type database table.
 * 
 */
@Entity
@Table(name="ehr_type")
@NamedQuery(name="EhrType.findAll", query="SELECT e FROM EhrType e")
public class EhrType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="ehr_typeid")
	private Long ehrTypeid;

	@Column(name="created_at")
	private Timestamp createdAt;

	@Column(name="created_by")
	private Long createdBy;

	@Column(name="ehr_type_name")
	private String ehrTypeName;

	@Column(name="updated_at")
	private Timestamp updatedAt;

	@Column(name="updated_by")
	private Long updatedBy;

	//bi-directional many-to-one association to Ehr
	@OneToMany(mappedBy="ehrType")
	private List<Ehr> ehrs;

	public EhrType() {
	}

	public Long getEhrTypeid() {
		return this.ehrTypeid;
	}

	public void setEhrTypeid(Long ehrTypeid) {
		this.ehrTypeid = ehrTypeid;
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

	public String getEhrTypeName() {
		return this.ehrTypeName;
	}

	public void setEhrTypeName(String ehrTypeName) {
		this.ehrTypeName = ehrTypeName;
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

	public List<Ehr> getEhrs() {
		return this.ehrs;
	}

	public void setEhrs(List<Ehr> ehrs) {
		this.ehrs = ehrs;
	}

	public Ehr addEhr(Ehr ehr) {
		getEhrs().add(ehr);
		ehr.setEhrType(this);

		return ehr;
	}

	public Ehr removeEhr(Ehr ehr) {
		getEhrs().remove(ehr);
		ehr.setEhrType(null);

		return ehr;
	}

}