package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the inv_tbl_issue database table.
 * 
 */
@Entity
@Table(name="inv_tbl_issue")
@NamedQuery(name="InvTblIssue.findAll", query="SELECT i FROM InvTblIssue i")
public class InvTblIssue implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_issue_id")
	private Integer serIssueId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inv_tbl_issue_ser_issue_id_seq")
	@SequenceGenerator(name = "inv_tbl_issue_ser_issue_id_seq", sequenceName = "inv_tbl_issue_ser_issue_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_issue_id")
	private Integer serIssueId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;

	@Column(name="bln_is_approved")
	private Boolean blnIsApproved;

	@Column(name="bln_is_billed")
	private Boolean blnIsBilled;
	
	public Boolean getBlnIsPaid() {
		return blnIsPaid;
	}

	public void setBlnIsPaid(Boolean blnIsPaid) {
		this.blnIsPaid = blnIsPaid;
	}

	@Column(name="bln_is_paid")
	private Boolean blnIsPaid;

	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Temporal(TemporalType.DATE)
	@Column(name="dte_date")
	private Date dteDate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_freight")
	private BigDecimal numFreight;

	@Column(name="num_total_freight")
	private Integer numTotalFreight;
	
	public Integer getNumTotalCartont() {
		return numTotalCartont;
	}

	public void setNumTotalCartont(Integer numTotalCartont) {
		this.numTotalCartont = numTotalCartont;
	}

	@Column(name="num_total_carton")
	private Integer numTotalCartont;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_created_user_id_voucher")
	private Integer serCreatedUserIdVoucher;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_bilty_no")
	private String txtBiltyNo;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_dispatch_address")
	private String txtDispatchAddress;

	@Column(name="txt_dispatch_address2")
	private String txtDispatchAddress2;

	@Column(name="txt_driver_id")
	private String txtDriverId;

	@Column(name="txt_driver_mobile")
	private String txtDriverMobile;

	@Column(name="txt_driver_name")
	private String txtDriverName;

	@Column(name="txt_gate_pass_no")
	private String txtGatePassNo;

	@Column(name="txt_invoice_status")
	private String txtInvoiceStatus;

	@Column(name="txt_issue_code")
	private String txtIssueCode;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_paid")
	private String txtPaid;

	@Column(name="txt_station")
	private String txtStation;

	@Column(name="txt_status")
	private String txtStatus;

	@Column(name="txt_transporter")
	private String txtTransporter;

	@Column(name="txt_vehicle_no")
	private String txtVehicleNo;
	
	@Column(name="txt_vehicle_type")
	private String txtVehicleType;
	
	@Column(name="num_quantity")
	private BigDecimal numQuantity;
	
	@Column(name="num_tare_weight")
	private BigDecimal numTareWeight;
	
	@Column(name="num_net_weight")
	private BigDecimal numNetWeight;
	
	@Column(name="num_gross_weight")
	private BigDecimal numGrossWeight;
	
	@Column(name="txt_inv_no")
	private String txtINVNo;
	
	@Column(name="txt_inv_status")
	private String txtInvStatus;
	
	@Column(name="num_invoice_quantity")
	private BigDecimal numInvoiceQuantity;
	
	@Temporal(TemporalType.DATE)
	@Column(name="dte_invoice_date")
	private Date dteInvoiceDate;
	
	@Column(name="dte_inv_createddate")
	private Timestamp dteInvcreateddate;

	@Column(name="dte_inv_modifieddate")
	private Timestamp dteInvModifieddate;

	public String getTxtVehicleType() {
		return txtVehicleType;
	}

	public void setTxtVehicleType(String txtVehicleType) {
		this.txtVehicleType = txtVehicleType;
	}

	//bi-directional many-to-one association to CfgTblSupplier
	@ManyToOne
	@JoinColumn(name="ser_supplier_id")
	private CfgTblSupplier cfgTblSupplier;

	//bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name="ser_sale_order_id")
	private SlsTblSaleOrder slsTblSaleOrder;

	//bi-directional many-to-one association to InvTblIssueDetail
	@OneToMany(mappedBy="invTblIssue")
	private List<InvTblIssueDetail> invTblIssueDetails;

	public InvTblIssue() {
	}

	public Integer getSerIssueId() {
		return this.serIssueId;
	}

	public void setSerIssueId(Integer serIssueId) {
		this.serIssueId = serIssueId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsApproved() {
		return this.blnIsApproved;
	}

	public void setBlnIsApproved(Boolean blnIsApproved) {
		this.blnIsApproved = blnIsApproved;
	}

	public Boolean getBlnIsBilled() {
		return this.blnIsBilled;
	}

	public void setBlnIsBilled(Boolean blnIsBilled) {
		this.blnIsBilled = blnIsBilled;
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

	public BigDecimal getNumFreight() {
		return this.numFreight;
	}

	public void setNumFreight(BigDecimal numFreight) {
		this.numFreight = numFreight;
	}

	public Integer getNumTotalFreight() {
		return this.numTotalFreight;
	}

	public void setNumTotalFreight(Integer numTotalFreight) {
		this.numTotalFreight = numTotalFreight;
	}

	public Integer getSerCreatedUserId() {
		return this.serCreatedUserId;
	}

	public void setSerCreatedUserId(Integer serCreatedUserId) {
		this.serCreatedUserId = serCreatedUserId;
	}

	public Integer getSerCreatedUserIdVoucher() {
		return this.serCreatedUserIdVoucher;
	}

	public void setSerCreatedUserIdVoucher(Integer serCreatedUserIdVoucher) {
		this.serCreatedUserIdVoucher = serCreatedUserIdVoucher;
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

	public String getTxtBiltyNo() {
		return this.txtBiltyNo;
	}

	public void setTxtBiltyNo(String txtBiltyNo) {
		this.txtBiltyNo = txtBiltyNo;
	}

	public String getTxtDescription() {
		return this.txtDescription;
	}

	public void setTxtDescription(String txtDescription) {
		this.txtDescription = txtDescription;
	}

	public String getTxtDispatchAddress() {
		return this.txtDispatchAddress;
	}

	public void setTxtDispatchAddress(String txtDispatchAddress) {
		this.txtDispatchAddress = txtDispatchAddress;
	}

	public String getTxtDispatchAddress2() {
		return this.txtDispatchAddress2;
	}

	public void setTxtDispatchAddress2(String txtDispatchAddress2) {
		this.txtDispatchAddress2 = txtDispatchAddress2;
	}

	public String getTxtDriverId() {
		return this.txtDriverId;
	}

	public void setTxtDriverId(String txtDriverId) {
		this.txtDriverId = txtDriverId;
	}

	public String getTxtDriverMobile() {
		return this.txtDriverMobile;
	}

	public void setTxtDriverMobile(String txtDriverMobile) {
		this.txtDriverMobile = txtDriverMobile;
	}

	public String getTxtDriverName() {
		return this.txtDriverName;
	}

	public void setTxtDriverName(String txtDriverName) {
		this.txtDriverName = txtDriverName;
	}

	public String getTxtGatePassNo() {
		return this.txtGatePassNo;
	}

	public void setTxtGatePassNo(String txtGatePassNo) {
		this.txtGatePassNo = txtGatePassNo;
	}

	public String getTxtInvoiceStatus() {
		return this.txtInvoiceStatus;
	}

	public void setTxtInvoiceStatus(String txtInvoiceStatus) {
		this.txtInvoiceStatus = txtInvoiceStatus;
	}

	public String getTxtIssueCode() {
		return this.txtIssueCode;
	}

	public void setTxtIssueCode(String txtIssueCode) {
		this.txtIssueCode = txtIssueCode;
	}

	public String getTxtMachineIp() {
		return this.txtMachineIp;
	}

	public void setTxtMachineIp(String txtMachineIp) {
		this.txtMachineIp = txtMachineIp;
	}

	public String getTxtPaid() {
		return this.txtPaid;
	}

	public void setTxtPaid(String txtPaid) {
		this.txtPaid = txtPaid;
	}

	public String getTxtStation() {
		return this.txtStation;
	}

	public void setTxtStation(String txtStation) {
		this.txtStation = txtStation;
	}

	public String getTxtStatus() {
		return this.txtStatus;
	}

	public void setTxtStatus(String txtStatus) {
		this.txtStatus = txtStatus;
	}

	public String getTxtTransporter() {
		return this.txtTransporter;
	}

	public void setTxtTransporter(String txtTransporter) {
		this.txtTransporter = txtTransporter;
	}

	public String getTxtVehicleNo() {
		return this.txtVehicleNo;
	}

	public void setTxtVehicleNo(String txtVehicleNo) {
		this.txtVehicleNo = txtVehicleNo;
	}

	public CfgTblSupplier getCfgTblSupplier() {
		return this.cfgTblSupplier;
	}

	public void setCfgTblSupplier(CfgTblSupplier cfgTblSupplier) {
		this.cfgTblSupplier = cfgTblSupplier;
	}

	public SlsTblSaleOrder getSlsTblSaleOrder() {
		return this.slsTblSaleOrder;
	}

	public void setSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		this.slsTblSaleOrder = slsTblSaleOrder;
	}

	public List<InvTblIssueDetail> getInvTblIssueDetails() {
		return this.invTblIssueDetails;
	}

	public void setInvTblIssueDetails(List<InvTblIssueDetail> invTblIssueDetails) {
		this.invTblIssueDetails = invTblIssueDetails;
	}

	public InvTblIssueDetail addInvTblIssueDetail(InvTblIssueDetail invTblIssueDetail) {
		getInvTblIssueDetails().add(invTblIssueDetail);
		invTblIssueDetail.setInvTblIssue(this);

		return invTblIssueDetail;
	}

	public InvTblIssueDetail removeInvTblIssueDetail(InvTblIssueDetail invTblIssueDetail) {
		getInvTblIssueDetails().remove(invTblIssueDetail);
		invTblIssueDetail.setInvTblIssue(null);

		return invTblIssueDetail;
	}

	
	
    public BigDecimal getNumQuantity() {
		return numQuantity;
	}

	public void setNumQuantity(BigDecimal numQuantity) {
		this.numQuantity = numQuantity;
	}

	public BigDecimal getNumTareWeight() {
		return numTareWeight;
	}

	public void setNumTareWeight(BigDecimal numTareWeight) {
		this.numTareWeight = numTareWeight;
	}

	public BigDecimal getNumNetWeight() {
		return numNetWeight;
	}

	public void setNumNetWeight(BigDecimal numNetWeight) {
		this.numNetWeight = numNetWeight;
	}

	public BigDecimal getNumGrossWeight() {
		return numGrossWeight;
	}

	public void setNumGrossWeight(BigDecimal numGrossWeight) {
		this.numGrossWeight = numGrossWeight;
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

	public String getTxtINVNo() {
		return txtINVNo;
	}

	public void setTxtINVNo(String txtINVNo) {
		this.txtINVNo = txtINVNo;
	}

	public String getTxtInvStatus() {
		return txtInvStatus;
	}

	public void setTxtInvStatus(String txtInvStatus) {
		this.txtInvStatus = txtInvStatus;
	}

	public BigDecimal getNumInvoiceQuantity() {
		return numInvoiceQuantity;
	}

	public void setNumInvoiceQuantity(BigDecimal numInvoiceQuantity) {
		this.numInvoiceQuantity = numInvoiceQuantity;
	}

	public Date getDteInvoiceDate() {
		return dteInvoiceDate;
	}

	public void setDteInvoiceDate(Date dteInvoiceDate) {
		this.dteInvoiceDate = dteInvoiceDate;
	}

	public Timestamp getDteInvcreateddate() {
		return dteInvcreateddate;
	}

	public void setDteInvcreateddate(Timestamp dteInvcreateddate) {
		this.dteInvcreateddate = dteInvcreateddate;
	}

	public Timestamp getDteInvModifieddate() {
		return dteInvModifieddate;
	}

	public void setDteInvModifieddate(Timestamp dteInvModifieddate) {
		this.dteInvModifieddate = dteInvModifieddate;
	}
	
	
}