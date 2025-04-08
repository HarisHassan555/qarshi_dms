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
@Table(name = "sls_tbl_work_order")
@NamedQuery(name = "SlsTblWorkOrder.findAll", query = "SELECT s FROM SlsTblWorkOrder s")
public class SlsTblWorkOrder implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_so_detail_id") private Integer serSoDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slsTblWorkOrder_ser_work_order_id_seq")
	@SequenceGenerator(name = "slsTblWorkOrder_ser_work_order_id_seq", sequenceName = "slsTblWorkOrder_ser_work_order_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_work_order_id")
	private Integer serWorkOrderId;

	@Column(name = "txt_work_order_no")
	private String txtWorkOrderNo;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Column(name = "txt_driver")
	private String txtDriver;

	@Column(name = "txt_driver_phone_no")
	private String txtDriverPhoneNo;

	@Column(name = "txt_warranty_type")
	private String txtWarrantyType;

	@Column(name = "txt_estimated_ref")
	private String txtEstimatedRef;

	@Column(name = "dte_timein")
	private Timestamp dteTimeIn;

	@Column(name = "dte_timeout")
	private Timestamp dteTimeOut;
	
	 @Column(name="tim_time_in")
	  Timestamp timTimeIn;
	  
	  @Column(name="tim_time_out")
	  Timestamp timTimeOut;
	  
	  @Column(name = "txt_time_in")
		private String txtTimeIn;
	  
	  @Column(name = "txt_time_out")
		private String txtTimeOut;
	  

	@Column(name = "dte_promisetime")
	private Timestamp dtePromiseTime;

	@Column(name = "num_previous_millage")
	private BigDecimal numPreviousMillage;

	@Column(name = "num_current_millage")
	private BigDecimal numCurrentMillage;
	
	@Column(name = "num_discount")
	private BigDecimal numDiscount;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_invoice_date")
	private Date dteInvoiceDate;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_date")
	private Date dteDate;

	@Column(name = "txt_remarks")
	private String txtRemarks;
	
	@Column(name = "txt_rectification")
	private String txtRectification;
	
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

	@Column(name="tim_delivery_time")
	private Time timDeliveryTime;
	
	@Column(name = "txt_sublet")
	private String txtSublet;
	
	@Column(name = "num_sublet_amount")
	private BigDecimal numSubletAmount;
	
	// bi-directional many-to-one association to SlsTblSaleOrder
//	@OneToMany
//	@JoinColumn(name = "ser_work_order_detail_id")
//	@Column(name = "ser_work_order_detail_id")
//	private List<SlsTblWorkOrderDetail> slsTblWorkOrderDetail;

	@OneToMany(mappedBy = "slsTblWorkOrder")
	private List<SlsTblWorkOrderDetail> slsTblWorkOrderDetail;

	@OneToMany(mappedBy = "slsTblWorkOrder")
	private List<SlsTblWorkOrderDetail> slsTblWorkOrderDetailservices;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_product_id")
	private CfgTblProduct cfgTblProduct;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_model_id")
	private CfgTblModel cfgTblModel;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_color_id")
	private CfgTblColor cfgTblColor;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_customer_id")
	private CfgTblCustomer cfgTblCustomer;
	
	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_dealer_id")
	private CfgTblCustomer cfgTblDealer;


	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_jobCategory_id")
	private CfgTblJobCategory cfgTblJobCategory;

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_jobType_id")
	private CfgTblJobType cfgTblJobType;

	// bi-directional many-to-one association to CfgTblProduct
	@ManyToOne
	@JoinColumn(name = "ser_so_vehicle_Detail_id")
	private SlsTblSoVehicleDetail slsTblSoVehicleDetail;
	
	
	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_tir_id")
	private SlsTblTIR slsTblTIR;
	
	@Column(name = "txt_claim_no")
	private String txtClaimNo;
	
	@Column(name = "ser_claim_id")
	private Integer serClaimId;
	
	@Column(name = "txt_claim_no_service")
	private String txtClaimNoService;
	
	@Column(name = "ser_claim_id_service")
	private Integer serClaimIdService;
	
    private String dte_date_from;
	
	
	private String dte_date_to;
	
	@Column(name = "bl_is_post_service_follow_up")
	private Boolean blIsPostServiceFollowUp;
	
	
	@Column(name = "bl_is_maintinance_follow_up")
	private Boolean blIsMaintinanceFollowUp;
	
	@Column(name = "bl_is_FFS_follow_up")
	private Boolean blIsFFSFollowUp;
	
	@Column(name = "bl_is_post_sales_follow_up")
	private Boolean blIsPostSalesFollowUp;
	
	@Column(name = "bl_is_sp_created")
	private Boolean blIsSPCreated;
	
	@Column(name = "bl_is_service_created")
	private Boolean blIsServiceCreated;
	
	@Column(name = "txt_contact_person")
	private String txtContactPerson;

	@Column(name = "txt_cp_phone_no")
	private String txtCPPhoneNo;
	
	@Column(name = "txt_complaint")
	private String txtComplaint;
	
	@Column(name="txtType")
	private String txtType;
	
	@Column(name="bl_is_gal")
	private Boolean blIsGAL;
	
	public Integer getSerWorkOrderId() {
		return serWorkOrderId;
	}

	public void setSerWorkOrderId(Integer serWorkOrderId) {
		this.serWorkOrderId = serWorkOrderId;
	}

	public String getTxtWorkOrderNo() {
		return txtWorkOrderNo;
	}

	public void setTxtWorkOrderNo(String txtWorkOrderNo) {
		this.txtWorkOrderNo = txtWorkOrderNo;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public String getTxtDriver() {
		return txtDriver;
	}

	public void setTxtDriver(String txtDriver) {
		this.txtDriver = txtDriver;
	}

	public String getTxtDriverPhoneNo() {
		return txtDriverPhoneNo;
	}

	public void setTxtDriverPhoneNo(String txtDriverPhoneNo) {
		this.txtDriverPhoneNo = txtDriverPhoneNo;
	}

	public String getTxtWarrantyType() {
		return txtWarrantyType;
	}

	public void setTxtWarrantyType(String txtWarrantyType) {
		this.txtWarrantyType = txtWarrantyType;
	}

	public String getTxtEstimatedRef() {
		return txtEstimatedRef;
	}

	public void setTxtEstimatedRef(String txtEstimatedRef) {
		this.txtEstimatedRef = txtEstimatedRef;
	}

	public Timestamp getDteTimeIn() {
		return dteTimeIn;
	}

	public void setDteTimeIn(Timestamp dteTimeIn) {
		this.dteTimeIn = dteTimeIn;
	}

	public Timestamp getDteTimeOut() {
		return dteTimeOut;
	}

	public void setDteTimeOut(Timestamp dteTimeOut) {
		this.dteTimeOut = dteTimeOut;
	}

	public Timestamp getDtePromiseTime() {
		return dtePromiseTime;
	}

	public void setDtePromiseTime(Timestamp dtePromiseTime) {
		this.dtePromiseTime = dtePromiseTime;
	}

	public BigDecimal getNumPreviousMillage() {
		return numPreviousMillage;
	}

	public void setNumPreviousMillage(BigDecimal numPreviousMillage) {
		this.numPreviousMillage = numPreviousMillage;
	}

	public BigDecimal getNumCurrentMillage() {
		return numCurrentMillage;
	}

	public void setNumCurrentMillage(BigDecimal numCurrentMillage) {
		this.numCurrentMillage = numCurrentMillage;
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

	public List<SlsTblWorkOrderDetail> getSlsTblWorkOrderDetail() {
		return slsTblWorkOrderDetail;
	}

	public void setSlsTblWorkOrderDetail(List<SlsTblWorkOrderDetail> slsTblWorkOrderDetail) {
		this.slsTblWorkOrderDetail = slsTblWorkOrderDetail;
	}

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public CfgTblModel getCfgTblModel() {
		return cfgTblModel;
	}

	public void setCfgTblModel(CfgTblModel cfgTblModel) {
		this.cfgTblModel = cfgTblModel;
	}

	public CfgTblColor getCfgTblColor() {
		return cfgTblColor;
	}

	public void setCfgTblColor(CfgTblColor cfgTblColor) {
		this.cfgTblColor = cfgTblColor;
	}

	public CfgTblCustomer getCfgTblCustomer() {
		return cfgTblCustomer;
	}

	public void setCfgTblCustomer(CfgTblCustomer cfgTblCustomer) {
		this.cfgTblCustomer = cfgTblCustomer;
	}

	public CfgTblJobCategory getCfgTblJobCategory() {
		return cfgTblJobCategory;
	}

	public void setCfgTblJobCategory(CfgTblJobCategory cfgTblJobCategory) {
		this.cfgTblJobCategory = cfgTblJobCategory;
	}

	public CfgTblJobType getCfgTblJobType() {
		return cfgTblJobType;
	}

	public void setCfgTblJobType(CfgTblJobType cfgTblJobType) {
		this.cfgTblJobType = cfgTblJobType;
	}

	public SlsTblSoVehicleDetail getSlsTblSoVehicleDetail() {
		return slsTblSoVehicleDetail;
	}

	public void setSlsTblSoVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail) {
		this.slsTblSoVehicleDetail = slsTblSoVehicleDetail;
	}

	public List<SlsTblWorkOrderDetail> getSlsTblWorkOrderDetailservices() {
		return slsTblWorkOrderDetailservices;
	}

	public void setSlsTblWorkOrderDetailservices(List<SlsTblWorkOrderDetail> slsTblWorkOrderDetailservices) {
		this.slsTblWorkOrderDetailservices = slsTblWorkOrderDetailservices;
	}

	public Time getTimDeliveryTime() {
		return timDeliveryTime;
	}

	public void setTimDeliveryTime(Time timDeliveryTime) {
		this.timDeliveryTime = timDeliveryTime;
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

	public CfgTblCustomer getCfgTblDealer() {
		return cfgTblDealer;
	}

	public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
		this.cfgTblDealer = cfgTblDealer;
	}

	public BigDecimal getNumDiscount() {
		return numDiscount;
	}

	public void setNumDiscount(BigDecimal numDiscount) {
		this.numDiscount = numDiscount;
	}

	public Timestamp getTimTimeIn() {
		return timTimeIn;
	}

	public void setTimTimeIn(Timestamp timTimeIn) {
		this.timTimeIn = timTimeIn;
	}

	public Timestamp getTimTimeOut() {
		return timTimeOut;
	}

	public void setTimTimeOut(Timestamp timTimeOut) {
		this.timTimeOut = timTimeOut;
	}

	public String getTxtTimeIn() {
		return txtTimeIn;
	}

	public void setTxtTimeIn(String txtTimeIn) {
		this.txtTimeIn = txtTimeIn;
	}

	public String getTxtTimeOut() {
		return txtTimeOut;
	}

	public void setTxtTimeOut(String txtTimeOut) {
		this.txtTimeOut = txtTimeOut;
	}

	public Boolean getBlIsPostServiceFollowUp() {
		return blIsPostServiceFollowUp;
	}

	public void setBlIsPostServiceFollowUp(Boolean blIsPostServiceFollowUp) {
		this.blIsPostServiceFollowUp = blIsPostServiceFollowUp;
	}

	public Boolean getBlIsMaintinanceFollowUp() {
		return blIsMaintinanceFollowUp;
	}

	public void setBlIsMaintinanceFollowUp(Boolean blIsMaintinanceFollowUp) {
		this.blIsMaintinanceFollowUp = blIsMaintinanceFollowUp;
	}

	public Boolean getBlIsFFSFollowUp() {
		return blIsFFSFollowUp;
	}

	public void setBlIsFFSFollowUp(Boolean blIsFFSFollowUp) {
		this.blIsFFSFollowUp = blIsFFSFollowUp;
	}

	public Boolean getBlIsPostSalesFollowUp() {
		return blIsPostSalesFollowUp;
	}

	public void setBlIsPostSalesFollowUp(Boolean blIsPostSalesFollowUp) {
		this.blIsPostSalesFollowUp = blIsPostSalesFollowUp;
	}

	public SlsTblTIR getSlsTblTIR() {
		return slsTblTIR;
	}

	public void setSlsTblTIR(SlsTblTIR slsTblTIR) {
		this.slsTblTIR = slsTblTIR;
	}

	public Boolean getBlIsSPCreated() {
		return blIsSPCreated;
	}

	public void setBlIsSPCreated(Boolean blIsSPCreated) {
		this.blIsSPCreated = blIsSPCreated;
	}

	public Boolean getBlIsServiceCreated() {
		return blIsServiceCreated;
	}

	public void setBlIsServiceCreated(Boolean blIsServiceCreated) {
		this.blIsServiceCreated = blIsServiceCreated;
	}


	public String getTxtClaimNo() {
		return txtClaimNo;
	}

	public void setTxtClaimNo(String txtClaimNo) {
		this.txtClaimNo = txtClaimNo;
	}

	public Integer getSerClaimId() {
		return serClaimId;
	}

	public void setSerClaimId(Integer serClaimId) {
		this.serClaimId = serClaimId;
	}

	public String getTxtClaimNoService() {
		return txtClaimNoService;
	}

	public void setTxtClaimNoService(String txtClaimNoService) {
		this.txtClaimNoService = txtClaimNoService;
	}

	public Integer getSerClaimIdService() {
		return serClaimIdService;
	}

	public void setSerClaimIdService(Integer serClaimIdService) {
		this.serClaimIdService = serClaimIdService;
	}

	public String getTxtContactPerson() {
		return txtContactPerson;
	}

	public void setTxtContactPerson(String txtContactPerson) {
		this.txtContactPerson = txtContactPerson;
	}

	public String getTxtCPPhoneNo() {
		return txtCPPhoneNo;
	}

	public void setTxtCPPhoneNo(String txtCPPhoneNo) {
		this.txtCPPhoneNo = txtCPPhoneNo;
	}

	public String getTxtComplaint() {
		return txtComplaint;
	}

	public void setTxtComplaint(String txtComplaint) {
		this.txtComplaint = txtComplaint;
	}

	public String getTxtType() {
		return txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public Boolean getBlIsGAL() {
		return blIsGAL;
	}

	public void setBlIsGAL(Boolean blIsGAL) {
		this.blIsGAL = blIsGAL;
	}

	public String getTxtRectification() {
		return txtRectification;
	}

	public void setTxtRectification(String txtRectification) {
		this.txtRectification = txtRectification;
	}

	public String getTxtSublet() {
		return txtSublet;
	}

	public void setTxtSublet(String txtSublet) {
		this.txtSublet = txtSublet;
	}

	public BigDecimal getNumSubletAmount() {
		return numSubletAmount;
	}

	public void setNumSubletAmount(BigDecimal numSubletAmount) {
		this.numSubletAmount = numSubletAmount;
	}
	
	

}