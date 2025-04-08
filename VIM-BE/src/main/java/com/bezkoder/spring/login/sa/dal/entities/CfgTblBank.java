package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_bank database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_bank")
@NamedQuery(name="CfgTblBank.findAll", query="SELECT c FROM CfgTblBank c")
public class CfgTblBank implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_bank_id")
	private Integer serBankId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cfg_tbl_bank_ser_bank_id_seq")
	@SequenceGenerator(name = "cfg_tbl_bank_ser_bank_id_seq", sequenceName = "cfg_tbl_bank_ser_bank_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_bank_id")
	private Integer serBankId;

	@Column(name="txt_bank_code")
	private String txtBankCode;

	@Column(name="txt_bank_name")
	private String txtBankName;
	
	@Column(name="iban")
	private String txtIBAN;
	
	@Column(name="txt_account_no")
	private String txtAccountNo;
	
	@Column(name="txt_address")
	private String txtAddress;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;
	
	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_status")
	private Boolean blnStatus;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

		public CfgTblBank() {
	}

		public Integer getSerBankId() {
			return serBankId;
		}

		public void setSerBankId(Integer serBankId) {
			this.serBankId = serBankId;
		}

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

		public Timestamp getDteCreateddate() {
			return dteCreateddate;
		}

		public void setDteCreateddate(Timestamp dteCreateddate) {
			this.dteCreateddate = dteCreateddate;
		}

		public Timestamp getDteModifieddate() {
			return dteModifieddate;
		}

		public void setDteModifieddate(Timestamp dteModifieddate) {
			this.dteModifieddate = dteModifieddate;
		}

		public Integer getSerCreatedUserId() {
			return serCreatedUserId;
		}

		public void setSerCreatedUserId(Integer serCreatedUserId) {
			this.serCreatedUserId = serCreatedUserId;
		}

		public Integer getSerGroupId() {
			return serGroupId;
		}

		public void setSerGroupId(Integer serGroupId) {
			this.serGroupId = serGroupId;
		}

		public Integer getSerModifiedUserId() {
			return serModifiedUserId;
		}

		public void setSerModifiedUserId(Integer serModifiedUserId) {
			this.serModifiedUserId = serModifiedUserId;
		}

		public String getTxtBankCode() {
			return txtBankCode;
		}

		public void setTxtBankCode(String txtBankCode) {
			this.txtBankCode = txtBankCode;
		}

		public String getTxtBankName() {
			return txtBankName;
		}

		public void setTxtBankName(String txtBankName) {
			this.txtBankName = txtBankName;
		}

		public String getTxtAccountNo() {
			return txtAccountNo;
		}

		public void setTxtAccountNo(String txtAccountNo) {
			this.txtAccountNo = txtAccountNo;
		}

		public String getTxtAddress() {
			return txtAddress;
		}

		public void setTxtAddress(String txtAddress) {
			this.txtAddress = txtAddress;
		}

		public String getTxtDescription() {
			return txtDescription;
		}

		public void setTxtDescription(String txtDescription) {
			this.txtDescription = txtDescription;
		}

		public String getTxtMachineIp() {
			return txtMachineIp;
		}

		public void setTxtMachineIp(String txtMachineIp) {
			this.txtMachineIp = txtMachineIp;
		}

		public String getTxtIBAN() {
			return txtIBAN;
		}

		public void setTxtIBAN(String txtIBAN) {
			this.txtIBAN = txtIBAN;
		}

	

}