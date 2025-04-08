package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The persistent class for the sls_tbl_so_detail database table.
 * 
 */
@Entity
@Table(name = "sls_tbl_so_vehicle_details")
@NamedQuery(name = "SlsTblSoVehicleDetail.findAll", query = "SELECT s FROM SlsTblSoVehicleDetail s")
public class SlsTblSoVehicleDetail implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO)
	 * 
	 * @Column(name="ser_so_detail_id") private Integer serSoDetailId;
	 */

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "slsTblSoVehicleDetail_ser_so_payment_id_seq")
	@SequenceGenerator(name = "slsTblSoVehicleDetail_ser_so_payment_id_seq", sequenceName = "slsTblSoVehicleDetail_ser_so_payment_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name = "ser_so_vehicle_Detail_id")
	private Integer serSoVehicleDetailId;

	@Column(name = "txt_saleOrder_no")
	private String txtSaleOrderNo;

	@Column(name = "ser_group_id")
	private Integer serGroupId;

	@Column(name = "txt_vehicle_type")
	private String txtVehicleType;

	@Column(name = "txt_category")
	private String txtCategory;

	@Column(name = "txt_chassis_no")
	private String txtChassisNo;

	@Column(name = "txt_engine_no")
	private String txtEngineNo;

	@Column(name = "txt_registration_no")
	private String txtRegistrationNo;

	@Column(name = "txt_transmission")
	private String txtTransmission;

	@Column(name = "txt_invoice_no")
	private String txtInvoiceNo;

	@Temporal(TemporalType.DATE)
	@Column(name = "dte_invoice_date")
	private Date dteInvoiceDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "dte_delivery_date")
	private Date dteDeliveryDate;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "dte_registration_date")
	private Date dteRegistrationDate;

	@Column(name = "num_millage")
	private BigDecimal numMillage;

	@Column(name = "num_model")
	private BigDecimal numModel;

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

	// bi-directional many-to-one association to SlsTblSaleOrder
	@ManyToOne
	@JoinColumn(name = "ser_sale_order_id")
	private SlsTblSaleOrder slsTblSaleOrder;

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
	
	@ManyToOne
	@JoinColumn(name = "ser_dealer_id")
	private CfgTblCustomer cfgTblDealer;
	
	
	// bi-directional many-to-one association to SlsTblSaleOrder
		@ManyToOne
		@JoinColumn(name = "ser_brand_id")
		private CfgTblBrand cfgTblBrand;
		
		
		@Column(name = "bl_is_FFS_follow_up")
		private Boolean blIsFFSFollowUp;
		
		@Column(name = "bl_is_post_sales_follow_up")
		private Boolean blIsPostSalesFollowUp;
		
		
		
	public Integer getSerSoVehicleDetailId() {
		return serSoVehicleDetailId;
	}

	public void setSerSoVehicleDetailId(Integer serSoVehicleDetailId) {
		this.serSoVehicleDetailId = serSoVehicleDetailId;
	}

	public String getTxtSaleOrderNo() {
		return txtSaleOrderNo;
	}

	public void setTxtSaleOrderNo(String txtSaleOrderNo) {
		this.txtSaleOrderNo = txtSaleOrderNo;
	}

	public Integer getSerGroupId() {
		return serGroupId;
	}

	public void setSerGroupId(Integer serGroupId) {
		this.serGroupId = serGroupId;
	}

	public String getTxtCategory() {
		return txtCategory;
	}

	public void setTxtCategory(String txtCategory) {
		this.txtCategory = txtCategory;
	}

	public String getTxtChassisNo() {
		return txtChassisNo;
	}

	public void setTxtChassisNo(String txtChassisNo) {
		this.txtChassisNo = txtChassisNo;
	}

	public String getTxtEngineNo() {
		return txtEngineNo;
	}

	public void setTxtEngineNo(String txtEngineNo) {
		this.txtEngineNo = txtEngineNo;
	}

	public String getTxtRegistrationNo() {
		return txtRegistrationNo;
	}

	public void setTxtRegistrationNo(String txtRegistrationNo) {
		this.txtRegistrationNo = txtRegistrationNo;
	}

	public String getTxtTransmission() {
		return txtTransmission;
	}

	public void setTxtTransmission(String txtTransmission) {
		this.txtTransmission = txtTransmission;
	}

	public String getTxtInvoiceNo() {
		return txtInvoiceNo;
	}

	public void setTxtInvoiceNo(String txtInvoiceNo) {
		this.txtInvoiceNo = txtInvoiceNo;
	}

	public Date getDteInvoiceDate() {
		return dteInvoiceDate;
	}

	public void setDteInvoiceDate(Date dteInvoiceDate) {
		this.dteInvoiceDate = dteInvoiceDate;
	}

	public BigDecimal getNumMillage() {
		return numMillage;
	}

	public void setNumMillage(BigDecimal numMillage) {
		this.numMillage = numMillage;
	}

	public BigDecimal getNumModel() {
		return numModel;
	}

	public void setNumModel(BigDecimal numModel) {
		this.numModel = numModel;
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

	public SlsTblSaleOrder getSlsTblSaleOrder() {
		return slsTblSaleOrder;
	}

	public void setSlsTblSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		this.slsTblSaleOrder = slsTblSaleOrder;
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

	public CfgTblProduct getCfgTblProduct() {
		return cfgTblProduct;
	}

	public void setCfgTblProduct(CfgTblProduct cfgTblProduct) {
		this.cfgTblProduct = cfgTblProduct;
	}

	public String getTxtVehicleType() {
		return txtVehicleType;
	}

	public void setTxtVehicleType(String txtVehicleType) {
		this.txtVehicleType = txtVehicleType;
	}

	public CfgTblBrand getCfgTblBrand() {
		return cfgTblBrand;
	}

	public void setCfgTblBrand(CfgTblBrand cfgTblBrand) {
		this.cfgTblBrand = cfgTblBrand;
	}

	public Date getDteDeliveryDate() {
		return dteDeliveryDate;
	}

	public void setDteDeliveryDate(Date dteDeliveryDate) {
		this.dteDeliveryDate = dteDeliveryDate;
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

	public Date getDteRegistrationDate() {
		return dteRegistrationDate;
	}

	public void setDteRegistrationDate(Date dteRegistrationDate) {
		this.dteRegistrationDate = dteRegistrationDate;
	}

	public CfgTblCustomer getCfgTblDealer() {
		return cfgTblDealer;
	}

	public void setCfgTblDealer(CfgTblCustomer cfgTblDealer) {
		this.cfgTblDealer = cfgTblDealer;
	}
	
	

}