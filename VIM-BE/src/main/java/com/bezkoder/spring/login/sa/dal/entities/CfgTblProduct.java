package com.bezkoder.spring.login.sa.dal.entities;

import java.io.Serializable;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


/**
 * The persistent class for the cfg_tbl_product database table.
 * 
 */
@Entity
@Table(name="cfg_tbl_product")
@NamedQuery(name="CfgTblProduct.findAll", query="SELECT c FROM CfgTblProduct c")
public class CfgTblProduct implements Serializable {
	private static final long serialVersionUID = 1L;

	/*@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ser_product_id")
	private Integer serProductId;*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	//@SequenceGenerator(name = "cfg_tbl_product_ser_product_id_seq", sequenceName = "cfg_tbl_product_ser_product_id_seq", initialValue = 01, allocationSize = 1)
	@Column(name="ser_product_id")
	private Integer serProductId;

	@Column(name="bl_is_deleted")
	private Boolean blIsDeleted;
	
	@Column(name="bl_is_production")
	private Boolean blIsProduction;

	public Boolean getBlIsProduction() {
		return blIsProduction;
	}

	public void setBlIsProduction(Boolean blIsProduction) {
		this.blIsProduction = blIsProduction;
	}

	@Column(name="bln_is_inventory_item")
	private Boolean blnIsInventoryItem;

	@Column(name="bln_is_purchase_item")
	private Boolean blnIsPurchaseItem;

	@Column(name="bln_is_sale_item")
	private Boolean blnIsSaleItem;


	@Column(name="bln_is_kichen_item")
	private Boolean blnIsKichenItem;
	
	@Column(name="bln_is_shop_item")
	private Boolean blnIsShopItem;

	@Column(name="bln_is_tangible")
	private Boolean blnIsTangible;

	@Column(name="bln_status")
	private Boolean blnStatus;
	
	@Column(name="bl_is_packing")
	private Boolean blIspacking;

	public Boolean getBlIspacking() {
		return blIspacking;
	}

	public void setBlIspacking(Boolean blIspacking) {
		this.blIspacking = blIspacking;
	}
	
	
	@Column(name="bl_is_set")
	private Boolean blIsSet;
	
	@Column(name="bl_is_component")
	private Boolean blIsComponent;

	public Boolean getBlIsComponent() {
		return blIsComponent;
	}

	public void setBlIsComponent(Boolean blIsComponent) {
		this.blIsComponent = blIsComponent;
	}



	public Boolean getBlIsSet() {
		return blIsSet;
	}

	public void setBlIsSet(Boolean blIsSet) {
		this.blIsSet = blIsSet;
	}

	public Boolean getBlIsImport() {
		return blIsImport;
	}

	public void setBlIsImport(Boolean blIsImport) {
		this.blIsImport = blIsImport;
	}

	@Column(name="bl_is_import")
	private Boolean blIsImport;
	
	@Column(name="dte_createddate")
	private Timestamp dteCreateddate;

	@Column(name="dte_modifieddate")
	private Timestamp dteModifieddate;

	@Column(name="num_discount")
	private BigDecimal numDiscount;

	@Column(name="num_market_retail_price")
	private BigDecimal numMarketRetailPrice;

	@Column(name="num_pieces_in_master_pack")
	private BigDecimal numPiecesInMasterPack;

	@Column(name="num_product_weight")
	private BigDecimal numProductWeight;

	@Column(name="num_sale_price")
	private BigDecimal numSalePrice;

	@Column(name="num_trade_price")
	private BigDecimal numTradePrice;

	@Column(name="num_unit_price")
	private BigDecimal numUnitPrice;

	@Column(name="num_units_in_master_pack")
	private BigDecimal numUnitsInMasterPack;
	
	@Column(name="num_inner_in_master_pack")
	private BigDecimal numInnersInMasterPack;

	@Column(name="pic_product_image")
	private byte[] picProductImage;

	@Column(name="ser_created_user_id")
	private Integer serCreatedUserId;

	@Column(name="ser_group_id")
	private Integer serGroupId;

	@Column(name="ser_modified_user_id")
	private Integer serModifiedUserId;

	@Column(name="txt_description")
	private String txtDescription;

	@Column(name="txt_machine_ip")
	private String txtMachineIp;

	@Column(name="txt_master_pack")
	private String txtMasterPack;

	@Column(name="txt_price_unit")
	private String txtPriceUnit;

	@Column(name="txt_product_code")
	private String txtProductCode;

	@Column(name="txt_product_name")
	private String txtProductName;

	@Column(name="txt_quality")
	private String txtQuality;
	
	@Column(name="bl_is_truck")
	private Boolean blIsTruck;
	
	@Column(name="bl_is_bus")
	private Boolean blIsBus;
	
	@Column(name="bl_is_pickup")
	private Boolean blIsPickup;
	
	

	@Column(name="txt_type")
	private String txtType;
	
	@Column(name="txt_sap_code")
	private String txtSapCode;
	
	
	@Column(name="num_engine_cc")
	private BigDecimal numEngineCC;

	//bi-directional many-to-one association to CfgTblBrand
	@ManyToOne
	@JoinColumn(name="ser_brand_id")
	private CfgTblBrand cfgTblBrand;

	//bi-directional many-to-one association to CfgTblProductCategory
	@ManyToOne
	@JoinColumn(name="ser_product_category_id")
	private CfgTblProductCategory cfgTblProductCategory;

	//bi-directional many-to-one association to CfgTblUom
	@ManyToOne
	@JoinColumn(name="ser_uom_id")
	private CfgTblUom cfgTblUom;
	
	@Column(name="txt_variant")
	private String txtVariant;
	
	@Column(name="txt_transmission")
	private String txtTransmission;
	
	@Column(name="txt_color")
	private String txtColor;
	
	@Column(name="txt_interior_color")
	private String txtInteriorColor;
	

	@Column(name="num_old_price")
	private BigDecimal numOldPrice;
	
	@Column(name="num_sales_tax")
	private BigDecimal numSalesTax;
	
	@Column(name="num_fed")
	private BigDecimal numFED;
	
	@Column(name="num_cvt")
	private BigDecimal numCVT;
	
	@Column(name="num_non_filer_amount")
	private BigDecimal numNonfilerAmount;
	
	@Column(name="num_filer_amount")
	private BigDecimal numfilerAmount;
	
	
	@Column(name="num_amount")
	private BigDecimal numAmount;

	@Column(name="txt_sku")
	private String txtSKU;
	
	//bi-directional many-to-one association to SlsTblSoDetail
	@OneToMany(mappedBy="cfgTblProduct", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<SlsTblSoDetail> slsTblSoDetails;
	
	public BigDecimal getNumInnersInMasterPack() {
		return numInnersInMasterPack;
	}

	public void setNumInnersInMasterPack(BigDecimal numInnersInMasterPack) {
		this.numInnersInMasterPack = numInnersInMasterPack;
	}



	public CfgTblProduct() {
	}

	public Integer getSerProductId() {
		return this.serProductId;
	}

	public void setSerProductId(Integer serProductId) {
		this.serProductId = serProductId;
	}

	public Boolean getBlIsDeleted() {
		return this.blIsDeleted;
	}

	public void setBlIsDeleted(Boolean blIsDeleted) {
		this.blIsDeleted = blIsDeleted;
	}

	public Boolean getBlnIsInventoryItem() {
		return this.blnIsInventoryItem;
	}

	public void setBlnIsInventoryItem(Boolean blnIsInventoryItem) {
		this.blnIsInventoryItem = blnIsInventoryItem;
	}

	public Boolean getBlnIsPurchaseItem() {
		return this.blnIsPurchaseItem;
	}

	public void setBlnIsPurchaseItem(Boolean blnIsPurchaseItem) {
		this.blnIsPurchaseItem = blnIsPurchaseItem;
	}

	public Boolean getBlnIsSaleItem() {
		return this.blnIsSaleItem;
	}

	public void setBlnIsSaleItem(Boolean blnIsSaleItem) {
		this.blnIsSaleItem = blnIsSaleItem;
	}

	public Boolean getBlnIsTangible() {
		return this.blnIsTangible;
	}

	public void setBlnIsTangible(Boolean blnIsTangible) {
		this.blnIsTangible = blnIsTangible;
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

	public BigDecimal getNumDiscount() {
		return this.numDiscount;
	}

	public void setNumDiscount(BigDecimal numDiscount) {
		this.numDiscount = numDiscount;
	}

	public BigDecimal getNumMarketRetailPrice() {
		return this.numMarketRetailPrice;
	}

	public void setNumMarketRetailPrice(BigDecimal numMarketRetailPrice) {
		this.numMarketRetailPrice = numMarketRetailPrice;
	}

	public BigDecimal getNumPiecesInMasterPack() {
		return this.numPiecesInMasterPack;
	}

	public void setNumPiecesInMasterPack(BigDecimal numPiecesInMasterPack) {
		this.numPiecesInMasterPack = numPiecesInMasterPack;
	}

	public BigDecimal getNumProductWeight() {
		return this.numProductWeight;
	}

	public void setNumProductWeight(BigDecimal numProductWeight) {
		this.numProductWeight = numProductWeight;
	}

	public BigDecimal getNumSalePrice() {
		return this.numSalePrice;
	}

	public void setNumSalePrice(BigDecimal numSalePrice) {
		this.numSalePrice = numSalePrice;
	}

	public BigDecimal getNumTradePrice() {
		return this.numTradePrice;
	}

	public void setNumTradePrice(BigDecimal numTradePrice) {
		this.numTradePrice = numTradePrice;
	}

	public BigDecimal getNumUnitPrice() {
		return this.numUnitPrice;
	}

	public void setNumUnitPrice(BigDecimal numUnitPrice) {
		this.numUnitPrice = numUnitPrice;
	}

	public BigDecimal getNumUnitsInMasterPack() {
		return this.numUnitsInMasterPack;
	}

	public void setNumUnitsInMasterPack(BigDecimal numUnitsInMasterPack) {
		this.numUnitsInMasterPack = numUnitsInMasterPack;
	}

	public byte[] getPicProductImage() {
		return this.picProductImage;
	}

	public void setPicProductImage(byte[] picProductImage) {
		this.picProductImage = picProductImage;
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

	public String getTxtMasterPack() {
		return this.txtMasterPack;
	}

	public void setTxtMasterPack(String txtMasterPack) {
		this.txtMasterPack = txtMasterPack;
	}

	public String getTxtPriceUnit() {
		return this.txtPriceUnit;
	}

	public void setTxtPriceUnit(String txtPriceUnit) {
		this.txtPriceUnit = txtPriceUnit;
	}

	public String getTxtProductCode() {
		return this.txtProductCode;
	}

	public void setTxtProductCode(String txtProductCode) {
		this.txtProductCode = txtProductCode;
	}

	public String getTxtProductName() {
		return this.txtProductName;
	}

	public void setTxtProductName(String txtProductName) {
		this.txtProductName = txtProductName;
	}

	public String getTxtQuality() {
		return this.txtQuality;
	}

	public void setTxtQuality(String txtQuality) {
		this.txtQuality = txtQuality;
	}

	public CfgTblBrand getCfgTblBrand() {
		return this.cfgTblBrand;
	}

	public void setCfgTblBrand(CfgTblBrand cfgTblBrand) {
		this.cfgTblBrand = cfgTblBrand;
	}

	public CfgTblProductCategory getCfgTblProductCategory() {
		return this.cfgTblProductCategory;
	}

	public void setCfgTblProductCategory(CfgTblProductCategory cfgTblProductCategory) {
		this.cfgTblProductCategory = cfgTblProductCategory;
	}

	public CfgTblUom getCfgTblUom() {
		return this.cfgTblUom;
	}

	public void setCfgTblUom(CfgTblUom cfgTblUom) {
		this.cfgTblUom = cfgTblUom;
	}

	

	public List<SlsTblSoDetail> getSlsTblSoDetails() {
		return this.slsTblSoDetails;
	}

	public void setSlsTblSoDetails(List<SlsTblSoDetail> slsTblSoDetails) {
		this.slsTblSoDetails = slsTblSoDetails;
	}

	public SlsTblSoDetail addSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		getSlsTblSoDetails().add(slsTblSoDetail);
		slsTblSoDetail.setCfgTblProduct(this);

		return slsTblSoDetail;
	}

	public SlsTblSoDetail removeSlsTblSoDetail(SlsTblSoDetail slsTblSoDetail) {
		getSlsTblSoDetails().remove(slsTblSoDetail);
		slsTblSoDetail.setCfgTblProduct(null);

		return slsTblSoDetail;
	}
	
	
	public Boolean getBlnIsKichenItem() {
		return blnIsKichenItem;
	}

	public void setBlnIsKichenItem(Boolean blnIsKichenItem) {
		this.blnIsKichenItem = blnIsKichenItem;
	}

	public Boolean getBlnIsShopItem() {
		return blnIsShopItem;
	}

	public void setBlnIsShopItem(Boolean blnIsShopItem) {
		this.blnIsShopItem = blnIsShopItem;
	}

	public String getTxtVariant() {
		return txtVariant;
	}

	public void setTxtVariant(String txtVariant) {
		this.txtVariant = txtVariant;
	}

	public String getTxtTransmission() {
		return txtTransmission;
	}

	public void setTxtTransmission(String txtTransmission) {
		this.txtTransmission = txtTransmission;
	}

	public String getTxtColor() {
		return txtColor;
	}

	public void setTxtColor(String txtColor) {
		this.txtColor = txtColor;
	}

	public String getTxtInteriorColor() {
		return txtInteriorColor;
	}

	public void setTxtInteriorColor(String txtInteriorColor) {
		this.txtInteriorColor = txtInteriorColor;
	}

	public BigDecimal getNumOldPrice() {
		return numOldPrice;
	}

	public void setNumOldPrice(BigDecimal numOldPrice) {
		this.numOldPrice = numOldPrice;
	}

	public BigDecimal getNumSalesTax() {
		return numSalesTax;
	}

	public void setNumSalesTax(BigDecimal numSalesTax) {
		this.numSalesTax = numSalesTax;
	}

	public BigDecimal getNumFED() {
		return numFED;
	}

	public void setNumFED(BigDecimal numFED) {
		this.numFED = numFED;
	}

	public BigDecimal getNumNonfilerAmount() {
		return numNonfilerAmount;
	}

	public void setNumNonfilerAmount(BigDecimal numNonfilerAmount) {
		this.numNonfilerAmount = numNonfilerAmount;
	}

	public BigDecimal getNumAmount() {
		return numAmount;
	}

	public void setNumAmount(BigDecimal numAmount) {
		this.numAmount = numAmount;
	}

	public String getTxtSKU() {
		return txtSKU;
	}

	public void setTxtSKU(String txtSKU) {
		this.txtSKU = txtSKU;
	}

	public BigDecimal getNumfilerAmount() {
		return numfilerAmount;
	}

	public void setNumfilerAmount(BigDecimal numfilerAmount) {
		this.numfilerAmount = numfilerAmount;
	}

	public BigDecimal getNumCVT() {
		return numCVT;
	}

	public void setNumCVT(BigDecimal numCVT) {
		this.numCVT = numCVT;
	}

	public BigDecimal getNumEngineCC() {
		return numEngineCC;
	}

	public void setNumEngineCC(BigDecimal numEngineCC) {
		this.numEngineCC = numEngineCC;
	}

	public Boolean getBlIsTruck() {
		return blIsTruck;
	}

	public void setBlIsTruck(Boolean blIsTruck) {
		this.blIsTruck = blIsTruck;
	}

	public Boolean getBlIsBus() {
		return blIsBus;
	}

	public void setBlIsBus(Boolean blIsBus) {
		this.blIsBus = blIsBus;
	}

	public Boolean getBlIsPickup() {
		return blIsPickup;
	}

	public void setBlIsPickup(Boolean blIsPickup) {
		this.blIsPickup = blIsPickup;
	}

	public String getTxtType() {
		return txtType;
	}

	public void setTxtType(String txtType) {
		this.txtType = txtType;
	}

	public String getTxtSapCode() {
		return txtSapCode;
	}

	public void setTxtSapCode(String txtSapCode) {
		this.txtSapCode = txtSapCode;
	}
	
	
	
	
	

}