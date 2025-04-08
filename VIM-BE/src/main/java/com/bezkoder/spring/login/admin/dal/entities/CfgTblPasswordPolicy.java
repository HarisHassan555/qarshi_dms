package com.bezkoder.spring.login.admin.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;


/**
 * The persistent class for the cfg_tbl_password_policy database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_password_policy")
@NamedQuery(name="CfgTblPasswordPolicy.findAll", query="SELECT c FROM CfgTblPasswordPolicy c")
public class CfgTblPasswordPolicy implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="CFG_TBL_PASSWORD_POLICY_SERPASSWORDPOLICYID_GENERATOR", sequenceName="cfg_tbl_password_policy_ser_password_policy_id_seq",  initialValue = 01, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="CFG_TBL_PASSWORD_POLICY_SERPASSWORDPOLICYID_GENERATOR")
	@Column(name="ser_password_policy_id")
	private Integer serPasswordPolicyId;

	@Column(name="bl_is_active")
	private Boolean blIsActive;

	@Column(name="bl_is_alpha_numeric")
	private Boolean blIsAlphaNumeric;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bl_is_number_required")
	private Boolean blIsNumberRequired;

	@Column(name="bl_is_special_cha_req")
	private Boolean blIsSpecialChaReq;
	
	@Column(name="bl_is_lower_upper")
	private Boolean blIsLowerUpper;

	public Boolean getBlIsLowerUpper() {
		return blIsLowerUpper;
	}

	public void setBlIsLowerUpper(Boolean blIsLowerUpper) {
		this.blIsLowerUpper = blIsLowerUpper;
	}



	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_created_date")
	private Timestamp dteCreatedDate;

	@Column(name="dte_modified_date")
	private Timestamp dteModifiedDate;

	@Column(name="num_attempt")
	private BigDecimal numAttempt;

	@Column(name="num_expire_days")
	private BigDecimal numExpireDays;

	@Column(name="num_history_count")
	private BigDecimal numHistoryCount;

	@Column(name="num_pass_length")
	private BigDecimal numPassLength;

	@Column(name="ser_created_user")
	private Integer serCreatedUser;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user")
	private Integer serModifiedUser;

	public CfgTblPasswordPolicy() {
	}

	public Integer getSerPasswordPolicyId() {
		return this.serPasswordPolicyId;
	}

	public void setSerPasswordPolicyId(Integer serPasswordPolicyId) {
		this.serPasswordPolicyId = serPasswordPolicyId;
	}

	public Boolean getBlIsActive() {
		return this.blIsActive;
	}

	public void setBlIsActive(Boolean blIsActive) {
		this.blIsActive = blIsActive;
	}

	public Boolean getBlIsAlphaNumeric() {
		return this.blIsAlphaNumeric;
	}

	public void setBlIsAlphaNumeric(Boolean blIsAlphaNumeric) {
		this.blIsAlphaNumeric = blIsAlphaNumeric;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlIsNumberRequired() {
		return this.blIsNumberRequired;
	}

	public void setBlIsNumberRequired(Boolean blIsNumberRequired) {
		this.blIsNumberRequired = blIsNumberRequired;
	}

	public Boolean getBlIsSpecialChaReq() {
		return this.blIsSpecialChaReq;
	}

	public void setBlIsSpecialChaReq(Boolean blIsSpecialChaReq) {
		this.blIsSpecialChaReq = blIsSpecialChaReq;
	}

	public Boolean getBlnStatus() {
		return this.blnStatus;
	}

	public void setBlnStatus(Boolean blnStatus) {
		this.blnStatus = blnStatus;
	}

	public Timestamp getDteCreatedDate() {
		return this.dteCreatedDate;
	}

	public void setDteCreatedDate(Timestamp dteCreatedDate) {
		this.dteCreatedDate = dteCreatedDate;
	}

	public Timestamp getDteModifiedDate() {
		return this.dteModifiedDate;
	}

	public void setDteModifiedDate(Timestamp dteModifiedDate) {
		this.dteModifiedDate = dteModifiedDate;
	}

	public BigDecimal getNumAttempt() {
		return this.numAttempt;
	}

	public void setNumAttempt(BigDecimal numAttempt) {
		this.numAttempt = numAttempt;
	}

	public BigDecimal getNumExpireDays() {
		return this.numExpireDays;
	}

	public void setNumExpireDays(BigDecimal numExpireDays) {
		this.numExpireDays = numExpireDays;
	}

	public BigDecimal getNumHistoryCount() {
		return this.numHistoryCount;
	}

	public void setNumHistoryCount(BigDecimal numHistoryCount) {
		this.numHistoryCount = numHistoryCount;
	}

	public BigDecimal getNumPassLength() {
		return this.numPassLength;
	}

	public void setNumPassLength(BigDecimal numPassLength) {
		this.numPassLength = numPassLength;
	}

	public Integer getSerCreatedUser() {
		return this.serCreatedUser;
	}

	public void setSerCreatedUser(Integer serCreatedUser) {
		this.serCreatedUser = serCreatedUser;
	}

	public Integer getSerGroupId() {
		return this.serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public Integer getSerModifiedUser() {
		return this.serModifiedUser;
	}

	public void setSerModifiedUser(Integer serModifiedUser) {
		this.serModifiedUser = serModifiedUser;
	}
	
	

	public String getTxtCode() {
		return txtCode;
	}

	public void setTxtCode(String txtCode) {
		this.txtCode = txtCode;
	}



	@Column(name="txt_code")
	private String txtCode;

}