package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_process database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_process")
@NamedQuery(name="CfgTblProcess.findAll", query="SELECT c FROM CfgTblProcess c")
public class CfgTblProcess implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_process_id")
	private Integer serProcessId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_is_issuable")
	private Boolean blnIsIssuable;

	@Column(name="bln_is_product_change")
	private Boolean blnIsProductChange;

	@Column(name="bln_is_qa")
	private Boolean blnIsQa;

	@Column(name="bln_is_receivable")
	private Boolean blnIsReceivable;

	@Column(name="bln_is_waste")
	private Boolean blnIsWaste;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_squence")
	private BigDecimal numSquence;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_process_parent_id")
	private Integer serProcessParentId;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_process_code")
	private String txtProcessCode;

	@Column(name="txt_process_name")
	private String txtProcessName;
	
	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	@Column(name="ser_group_id")
	private Integer serGroupId;

	

	public CfgTblProcess() {
	}

	public Integer getSerProcessId() {
		return this.serProcessId;
	}

	public void setSerProcessId(Integer serProcessId) {
		this.serProcessId = serProcessId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsIssuable() {
		return this.blnIsIssuable;
	}

	public void setBlnIsIssuable(Boolean blnIsIssuable) {
		this.blnIsIssuable = blnIsIssuable;
	}

	public Boolean getBlnIsProductChange() {
		return this.blnIsProductChange;
	}

	public void setBlnIsProductChange(Boolean blnIsProductChange) {
		this.blnIsProductChange = blnIsProductChange;
	}

	public Boolean getBlnIsQa() {
		return this.blnIsQa;
	}

	public void setBlnIsQa(Boolean blnIsQa) {
		this.blnIsQa = blnIsQa;
	}

	public Boolean getBlnIsReceivable() {
		return this.blnIsReceivable;
	}

	public void setBlnIsReceivable(Boolean blnIsReceivable) {
		this.blnIsReceivable = blnIsReceivable;
	}

	public Boolean getBlnIsWaste() {
		return this.blnIsWaste;
	}

	public void setBlnIsWaste(Boolean blnIsWaste) {
		this.blnIsWaste = blnIsWaste;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public Timestamp getDteCreateddate() {
		return this.dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public BigDecimal getNumSquence() {
		return this.numSquence;
	}

	public void setNumSquence(BigDecimal numSquence) {
		this.numSquence = numSquence;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public Integer getSerProcessParentId() {
		return this.serProcessParentId;
	}

	public void setSerProcessParentId(Integer serProcessParentId) {
		this.serProcessParentId = serProcessParentId;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtProcessCode() {
		return this.txtProcessCode;
	}

	public void setTxtProcessCode(String txtProcessCode) {
		this.txtProcessCode = txtProcessCode;
	}

	public String getTxtProcessName() {
		return this.txtProcessName;
	}

	public void setTxtProcessName(String txtProcessName) {
		this.txtProcessName = txtProcessName;
	}


}