package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;
import java.sql.Timestamp;


/**
 * The persistent class for the sls_tbl_customer_feedback database table.
 * 
 */
@Entity
@Table(name="sls_tbl_customer_feedback")
@NamedQuery(name="SlsTblCustomerFeedback.findAll", query="SELECT s FROM SlsTblCustomerFeedback s")
public class SlsTblCustomerFeedback implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="SLS_TBL_CUSTOMER_FEEDBACK_SERCUSTOMMERFEEDBACKID_GENERATOR", sequenceName="sls_tbl_customer_feedback_ser_customer_feedback_id_seq", initialValue = 11, allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SLS_TBL_CUSTOMER_FEEDBACK_SERCUSTOMMERFEEDBACKID_GENERATOR")
	@Column(name="ser_custommer_feedback_id")
	private Integer serCustommerFeedbackId;

	@Column(name="bl_is_complaint")
	private Boolean blIsComplaint;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;

	

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	private Integer priority;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;



	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="ser_preparedby_id")
	private Integer serPreparedbyId;

	@Column(name="txt_code")
	private String txtCode;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_remarks")
	private String txtRemarks;

	@Column(name="txt_status")
	private String txtStatus;

	public SlsTblCustomerFeedback() {
	}

	public Integer getSerCustommerFeedbackId() {
		return this.serCustommerFeedbackId;
	}

	public void setSerCustommerFeedbackId(Integer serCustommerFeedbackId) {
		this.serCustommerFeedbackId = serCustommerFeedbackId;
	}

	public Boolean getBlIsComplaint() {
		return this.blIsComplaint;
	}

	public void setBlIsComplaint(Boolean blIsComplaint) {
		this.blIsComplaint = blIsComplaint;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Timestamp getDteCreateddate() {
		return this.dteCreateddate;
	}

	public void setDteCreateddate(Timestamp dteCreateddate) {
		this.dteCreateddate = dteCreateddate;
	}

	public Date getDteDate() {
		return this.dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public Timestamp getDteModifieddate() {
		return this.dteModifieddate;
	}

	public void setDteModifieddate(Timestamp dteModifieddate) {
		this.dteModifieddate = dteModifieddate;
	}

	public Integer getPriority() {
		return this.priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
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

	public Integer getSerPreparedbyId() {
		return this.serPreparedbyId;
	}

	public void setSerPreparedbyId(Integer serPreparedbyId) {
		this.serPreparedbyId = serPreparedbyId;
	}

	public String getTxtCode() {
		return this.txtCode;
	}

	public void setTxtCode(String txtCode) {
		this.txtCode = txtCode;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtRemarks() {
		return this.txtRemarks;
	}

	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}

	public String getTxtStatus() {
		return this.txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}
	
	
	//bi-directional many-to-one association to CfgTblCustomer
		@ManyToOne
		@JoinColumn(name="ser_customer_id")
		private CfgTblCustomer cfgTblCustomer;

		public CfgTblCustomer getCfgTblCustomer() {
			return cfgTblCustomer;
		}

		public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
			this.cfgTblCustomer = cfgTblCustomer;
		}
		
		
		public String getDte_date_from() {
			return dte_date_from;
		}

		public void setDte_date_from(String dte_date_from) {
			this.dte_date_from = dte_date_from;
		}

		public String getDte_date_to() {
			return dte_date_to;
		}

		public void setDte_date_to(String dte_date_to) {
			this.dte_date_to = dte_date_to;
		}


		private String dte_date_from;
		
		
		private String dte_date_to;
		
		
		//bi-directional many-to-one association to CfgTblCustomer
				@ManyToOne
				@JoinColumn(name="ser_dealer_id")
				private CfgTblCustomer cfgTblDealer;

				public CfgTblCustomer getCfgTblDealer() {
					return cfgTblDealer;
				}

				public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
					this.cfgTblDealer = cfgTblDealer;
				}

}