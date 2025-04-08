package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_product_design database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_product_design")
@NamedQuery(name="CfgTblProductDesign.findAll", query="SELECT c FROM CfgTblProductDesign c")
public class CfgTblProductDesign implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_product_design_id")
	private Integer serProductDesignId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_is_print")
	private Boolean blnIsPrint;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_no_of_colors")
	private BigDecimal numNoOfColors;

	@Column(name="ser_color_type")
	private Integer serColorType;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_parent_design_id")
	private Integer serParentDesignId;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_product_design_code")
	private String txtProductDesignCode;

	@Column(name="txt_product_design_name")
	private String txtProductDesignName;

	@Column(name="txt_remarks")
	private String txtRemarks;

	@Column(name="txt_type_name")
	private String txtTypeName;

	


	public CfgTblProductDesign() {
	}

	public Integer getSerProductDesignId() {
		return this.serProductDesignId;
	}

	public void setSerProductDesignId(Integer serProductDesignId) {
		this.serProductDesignId = serProductDesignId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsPrint() {
		return this.blnIsPrint;
	}

	public void setBlnIsPrint(Boolean blnIsPrint) {
		this.blnIsPrint = blnIsPrint;
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

	public BigDecimal getNumNoOfColors() {
		return this.numNoOfColors;
	}

	public void setNumNoOfColors(BigDecimal numNoOfColors) {
		this.numNoOfColors = numNoOfColors;
	}

	public Integer getSerColorType() {
		return this.serColorType;
	}

	public void setSerColorType(Integer serColorType) {
		this.serColorType = serColorType;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerGroupId() {
		return this.serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public Integer getSerModifiedUserId() {
		return this.serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public Integer getSerParentDesignId() {
		return this.serParentDesignId;
	}

	public void setSerParentDesignId(Integer serParentDesignId) {
		this.serParentDesignId = serParentDesignId;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtProductDesignCode() {
		return this.txtProductDesignCode;
	}

	public void setTxtProductDesignCode(String txtProductDesignCode) {
		this.txtProductDesignCode = txtProductDesignCode;
	}

	public String getTxtProductDesignName() {
		return this.txtProductDesignName;
	}

	public void setTxtProductDesignName(String txtProductDesignName) {
		this.txtProductDesignName = txtProductDesignName;
	}

	public String getTxtRemarks() {
		return this.txtRemarks;
	}

	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}

	public String getTxtTypeName() {
		return this.txtTypeName;
	}

	public void setTxtTypeName(String txtTypeName) {
		this.txtTypeName = txtTypeName;
	}

	

}