package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name = "sls_tbl_claim")
@NamedQuery(name = "SlsTblClaim.findAll", query = "SELECT s FROM SlsTblClaim s")
public class SlsTblClaim implements Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SlsTblClaim_ser_claim_id_seq")
	@SequenceGenerator(name = "SlsTblClaim_ser_claim_id_seq", sequenceName = "SlsTblClaim_ser_claim_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_claim_id")
	private Integer serClaimId;

	@Column(name = "txt_claim_no")
	private String txtClaimNo;
	
	@Column(name = "txt_description")
	private String txtDescription;
	
	@Column(name="bln_is_Service")
	private Boolean blnIsService;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_invoice_date")
	private Date dteInvoiceDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_date")
	private Date dteDate;

	@Column(name = "txt_remarks")
	private String txtRemarks;

	@Column(name = "txt_status")
	private String txtStatus;

	@Column(name = "bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name = "dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name = "dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name = "ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name = "ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name = "txt_machine_ip")
	private String txtMachineIp;

	private String dte_date_from;

	private String dte_date_to;

	@Column(name = "num_level")
	private Integer numLevel;

	@Column(name = "ser_approvedby_id1")
	private Integer serApprovedbyId1;

	@Column(name = "ser_approvedby_id2")
	private Integer serApprovedbyId2;

	@Column(name = "ser_approvedby_id3")
	private Integer serApprovedbyId3;

	@Column(name = "txt_status1")
	private String txtStatus1;

	@Column(name = "txt_status2")
	private String txtStatus2;

	@Column(name = "txt_status3")
	private String txtStatus3;

	@Column(name = "txt_status4")
	private String txtStatus4;
	
	@Column(name = "txt_status5")
	private String txtStatus5;

	@Column(name = "txt_Level")
	private String txtLevel;

	@Column(name = "dte_approveddate1")
	private Timestamp dteApproveddate1;

	@Column(name = "dte_approveddate2")
	private Timestamp dteApproveddate2;

	@Column(name = "dte_approveddate3")
	private Timestamp dteApproveddate3;

	@Column(name = "dte_approveddate4")
	private Timestamp dteApproveddate4;
	
	@Column(name = "dte_approveddate5")
	private Timestamp dteApproveddate5;
	
	
	 @Column(name="num_amount_before_tax")
	 private BigDecimal numAmountBeforeTax;
	 
	 @Column(name="num_sales_Tax")
	 private BigDecimal numSalesTax;
	 
	 
	 @Column(name="num_service_amount_before_tax")
	 private BigDecimal numServiceAmountBeforeTax;
	 
	 @Column(name="num_service_Tax")
	 private BigDecimal numServiceTax;
	 
	 
	 @Column(name="num_amount_after_tax")
	 private BigDecimal numAmountAfterTax;
	 
	@OneToMany(mappedBy = "slsTblClaim")
	private List<SlsTblClaimDetail> SlsTblClaimDetail;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_dealer_id")
	private CfgTblCustomer cfgTblDealer;


	public Integer getSerClaimId() {
		return serClaimId;
	}

	public void setSerClaimId(Integer serClaimId) {
		this.serClaimId = serClaimId;
	}

	public String getTxtClaimNo() {
		return txtClaimNo;
	}

	public void setTxtClaimNo(String txtClaimNo) {
		this.txtClaimNo = txtClaimNo;
	}

	public String getTxtDescription() {
		return txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public Date getDteInvoiceDate() {
		return dteInvoiceDate;
	}

	public void setDteInvoiceDate(Date dteInvoiceDate) {
		this.dteInvoiceDate = dteInvoiceDate;
	}

	public Date getDteDate() {
		return dteDate;
	}

	public void setDteDate(Date dteDate) {
		this.dteDate = dteDate;
	}

	public String getTxtRemarks() {
		return txtRemarks;
	}

	public void setTxtRemarks(String txtRemarks) {
		this.txtRemarks = txtRemarks;
	}

	public String getTxtStatus() {
		return txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	public Boolean getBlIsDeleted() {
		return blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
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

	public Integer getSerModifiedUserId() {
		return serModifiedUserId;
	}

	public void setSerModifiedUserId(Integer serModifiedUserId) {
		this.serModifiedUserId = serModifiedUserId;
	}

	public String getTxtMachineIp() {
		return txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
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

	public Integer getNumLevel() {
		return numLevel;
	}

	public void setNumLevel(Integer numLevel) {
		this.numLevel = numLevel;
	}

	public Integer getSerApprovedbyId1() {
		return serApprovedbyId1;
	}

	public void setSerApprovedbyId1(Integer serApprovedbyId1) {
		this.serApprovedbyId1 = serApprovedbyId1;
	}

	public Integer getSerApprovedbyId2() {
		return serApprovedbyId2;
	}

	public void setSerApprovedbyId2(Integer serApprovedbyId2) {
		this.serApprovedbyId2 = serApprovedbyId2;
	}

	public Integer getSerApprovedbyId3() {
		return serApprovedbyId3;
	}

	public void setSerApprovedbyId3(Integer serApprovedbyId3) {
		this.serApprovedbyId3 = serApprovedbyId3;
	}

	public String getTxtStatus1() {
		return txtStatus1;
	}

	public void setTxtStatus1(String txtStatus1) {
		this.txtStatus1 = txtStatus1;
	}

	public String getTxtStatus2() {
		return txtStatus2;
	}

	public void setTxtStatus2(String txtStatus2) {
		this.txtStatus2 = txtStatus2;
	}

	public String getTxtStatus3() {
		return txtStatus3;
	}

	public void setTxtStatus3(String txtStatus3) {
		this.txtStatus3 = txtStatus3;
	}

	public String getTxtStatus4() {
		return txtStatus4;
	}

	public void setTxtStatus4(String txtStatus4) {
		this.txtStatus4 = txtStatus4;
	}

	public String getTxtLevel() {
		return txtLevel;
	}

	public void setTxtLevel(String txtLevel) {
		this.txtLevel = txtLevel;
	}

	public Timestamp getDteApproveddate1() {
		return dteApproveddate1;
	}

	public void setDteApproveddate1(Timestamp dteApproveddate1) {
		this.dteApproveddate1 = dteApproveddate1;
	}

	public Timestamp getDteApproveddate2() {
		return dteApproveddate2;
	}

	public void setDteApproveddate2(Timestamp dteApproveddate2) {
		this.dteApproveddate2 = dteApproveddate2;
	}

	public Timestamp getDteApproveddate3() {
		return dteApproveddate3;
	}

	public void setDteApproveddate3(Timestamp dteApproveddate3) {
		this.dteApproveddate3 = dteApproveddate3;
	}

	public Timestamp getDteApproveddate4() {
		return dteApproveddate4;
	}

	public void setDteApproveddate4(Timestamp dteApproveddate4) {
		this.dteApproveddate4 = dteApproveddate4;
	}

	public BigDecimal getNumAmountBeforeTax() {
		return numAmountBeforeTax;
	}

	public void setNumAmountBeforeTax(BigDecimal numAmountBeforeTax) {
		this.numAmountBeforeTax = numAmountBeforeTax;
	}

	public BigDecimal getNumSalesTax() {
		return numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public BigDecimal getNumAmountAfterTax() {
		return numAmountAfterTax;
	}

	public void setNumAmountAfterTax(BigDecimal numAmountAfterTax) {
		this.numAmountAfterTax = numAmountAfterTax;
	}

	public List<SlsTblClaimDetail> getSlsTblClaimDetail() {
		return SlsTblClaimDetail;
	}

	public void setSlsTblClaimDetail(List<SlsTblClaimDetail> slsTblClaimDetail) {
		SlsTblClaimDetail = slsTblClaimDetail;
	}

	public CfgTblCustomer getCfgTblDealer() {
		return cfgTblDealer;
	}

	public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
		this.cfgTblDealer = cfgTblDealer;
	}

	public Boolean getBlnIsService() {
		return blnIsService;
	}

	public void setBlnIsService(Boolean blnIsService) {
		this.blnIsService = blnIsService;
	}

	public BigDecimal getNumServiceAmountBeforeTax() {
		return numServiceAmountBeforeTax;
	}

	public void setNumServiceAmountBeforeTax(BigDecimal numServiceAmountBeforeTax) {
		this.numServiceAmountBeforeTax = numServiceAmountBeforeTax;
	}

	public BigDecimal getNumServiceTax() {
		return numServiceTax;
	}

	public void setNumServiceTax(BigDecimal numServiceTax) {
		this.numServiceTax = numServiceTax;
	}

	public String getTxtStatus5() {
		return txtStatus5;
	}

	public void setTxtStatus5(String txtStatus5) {
		this.txtStatus5 = txtStatus5;
	}

	public Timestamp getDteApproveddate5() {
		return dteApproveddate5;
	}

	public void setDteApproveddate5(Timestamp dteApproveddate5) {
		this.dteApproveddate5 = dteApproveddate5;
	}
	
	

}