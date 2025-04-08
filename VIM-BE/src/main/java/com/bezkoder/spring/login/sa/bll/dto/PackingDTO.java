package com.bezkoder.spring.login.sa.bll.dto;

/*public class PackingDTO {

}
*/

/*,cfg_tbl_product_component.ser_child_product_id,
cfg_tbl_product_component.num_quantity,cfg_tbl_product_component.bl_is_packing
,child_product.txt_product_code as txt_child_product_code,child_product.txt_product_name as txt_child_product_name
,parent_product.txt_product_code as txt_parent_product_code,parent_product.txt_product_name as txt_parent_product_name

,child_production_summary.ser_product_design_id
,cfg_Tbl_product_design.txt_product_design_code,cfg_Tbl_product_design.txt_product_design_name
,child_production_summary.ser_product_quality_id
,cfg_Tbl_product_quality.txt_product_quality_code,cfg_Tbl_product_quality.txt_product_quality_name
,Coalesce((select num_balance from pro_Tbl_production_Summary where ser_product_id=child_product.ser_product_id and ser_process_id=4 and ser_product_quality_id=39
and (ser_product_design_id=19 or ser_product_design_id is null) ),0) as num_balance
*/
import java.math.BigDecimal;
public class PackingDTO {
	
	  
	
	public int getSer_parent_product_id() {
		return ser_parent_product_id;
	}
	public void setSer_parent_product_id(int ser_parent_product_id) {
		this.ser_parent_product_id = ser_parent_product_id;
	}
	public int getSer_child_product_id() {
		return ser_child_product_id;
	}
	public void setSer_child_product_id(int ser_child_product_id) {
		this.ser_child_product_id = ser_child_product_id;
	}
	public int getSer_product_design_id() {
		return ser_product_design_id;
	}
	public void setSer_product_design_id(int ser_product_design_id) {
		this.ser_product_design_id = ser_product_design_id;
	}
	public int getSer_product_quality_id() {
		return ser_product_quality_id;
	}
	public void setSer_product_quality_id(int ser_product_quality_id) {
		this.ser_product_quality_id = ser_product_quality_id;
	}
	public String getTxt_child_product_code() {
		return txt_child_product_code;
	}
	public void setTxt_child_product_code(String txt_child_product_code) {
		this.txt_child_product_code = txt_child_product_code;
	}
	public String getTxt_child_product_name() {
		return txt_child_product_name;
	}
	public void setTxt_child_product_name(String txt_child_product_name) {
		this.txt_child_product_name = txt_child_product_name;
	}
	public String getTxt_parent_product_code() {
		return txt_parent_product_code;
	}
	public void setTxt_parent_product_code(String txt_parent_product_code) {
		this.txt_parent_product_code = txt_parent_product_code;
	}
	public String getTxt_parent_product_name() {
		return txt_parent_product_name;
	}
	public void setTxt_parent_product_name(String txt_parent_product_name) {
		this.txt_parent_product_name = txt_parent_product_name;
	}
	public String getTxt_product_design_code() {
		return txt_product_design_code;
	}
	public void setTxt_product_design_code(String txt_product_design_code) {
		this.txt_product_design_code = txt_product_design_code;
	}
	public String getTxt_product_design_name() {
		return txt_product_design_name;
	}
	public void setTxt_product_design_name(String txt_product_design_name) {
		this.txt_product_design_name = txt_product_design_name;
	}
	public String getTxt_product_quality_code() {
		return txt_product_quality_code;
	}
	public void setTxt_product_quality_code(String txt_product_quality_code) {
		this.txt_product_quality_code = txt_product_quality_code;
	}
	public String getTxt_product_quality_name() {
		return txt_product_quality_name;
	}
	public void setTxt_product_quality_name(String txt_product_quality_name) {
		this.txt_product_quality_name = txt_product_quality_name;
	}
	public BigDecimal getNum_quantity() {
		return num_quantity;
	}
	public void setNum_quantity(BigDecimal num_quantity) {
		this.num_quantity = num_quantity;
	}
	public BigDecimal getNum_balance() {
		return num_balance;
	}
	public void setNum_balance(BigDecimal num_balance) {
		this.num_balance = num_balance;
	}
	public BigDecimal getNum_new_balance() {
		return num_new_balance;
	}
	public void setNum_new_balance(BigDecimal num_new_balance) {
		this.num_new_balance = num_new_balance;
	}
	public BigDecimal getNum_breakage() {
		return num_breakage;
	}
	public void setNum_breakage(BigDecimal num_breakage) {
		this.num_breakage = num_breakage;
	}
	public BigDecimal getNum_unit_wt() {
		return num_unit_wt;
	}
	public void setNum_unit_wt(BigDecimal num_unit_wt) {
		this.num_unit_wt = num_unit_wt;
	}
	public boolean isBl_is_packing() {
		return bl_is_packing;
	}
	public void setBl_is_packing(boolean bl_is_packing) {
		this.bl_is_packing = bl_is_packing;
	}
	
	
	public int getSer_poduction_summary_id() {
		return ser_poduction_summary_id;
	}
	public void setSer_poduction_summary_id(int ser_poduction_summary_id) {
		this.ser_poduction_summary_id = ser_poduction_summary_id;
	}
	
	public BigDecimal getNum_quantity_req() {
		return num_quantity_req;
	}
	public void setNum_quantity_req(BigDecimal num_quantity_req) {
		this.num_quantity_req = num_quantity_req;
	}

	public BigDecimal getNum_mc_qty() {
		return num_mc_qty;
	}
	public void setNum_mc_qty(BigDecimal num_mc_qty) {
		this.num_mc_qty = num_mc_qty;
	}
	public BigDecimal getNum_set_qty() {
		return num_set_qty;
	}
	public void setNum_set_qty(BigDecimal num_set_qty) {
		this.num_set_qty = num_set_qty;
	}
	public int getSer_parent_design_id() {
		return ser_parent_design_id;
	}
	public void setSer_parent_design_id(int ser_parent_design_id) {
		this.ser_parent_design_id = ser_parent_design_id;
	}
	public int getSer_parent_quality_id() {
		return ser_parent_quality_id;
	}
	public void setSer_parent_quality_id(int ser_parent_quality_id) {
		this.ser_parent_quality_id = ser_parent_quality_id;
	}
	public int getSer_parent_process_id() {
		return ser_parent_process_id;
	}
	public void setSer_parent_process_id(int ser_parent_process_id) {
		this.ser_parent_process_id = ser_parent_process_id;
	}
	
	
	private int ser_poduction_summary_id;
	private int ser_parent_product_id;
	private int ser_child_product_id;
	private int ser_product_design_id;
	private int ser_product_quality_id;
	private int ser_parent_design_id;
	private int ser_parent_quality_id;
	private int ser_parent_process_id;
	
	private String txt_child_product_code;
	private String txt_child_product_name;
	private String txt_parent_product_code;
	private String txt_parent_product_name;
	private String txt_product_design_code;
	private String txt_product_design_name;
	private String txt_product_quality_code;
	private String txt_product_quality_name;
	
	private BigDecimal num_quantity;
	private BigDecimal num_balance;
	private BigDecimal num_new_balance;
	private BigDecimal num_breakage;
	private BigDecimal num_unit_wt;
	private BigDecimal num_quantity_req;
	private BigDecimal num_mc_qty;
	private BigDecimal num_set_qty;
	private boolean bl_is_packing;
	
	
}
