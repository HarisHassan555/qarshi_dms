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
import java.util.Date;
public class PriceListDTO {
	
	  

	
	

	public int getSer_product_id() {
		return ser_product_id;
	}
	public void setSer_product_id(int ser_product_id) {
		this.ser_product_id = ser_product_id;
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
	public int getSer_product_category_id() {
		return ser_product_category_id;
	}
	public void setSer_product_category_id(int ser_product_category_id) {
		this.ser_product_category_id = ser_product_category_id;
	}
	public String getTxt_product_code() {
		return txt_product_code;
	}
	public void setTxt_product_code(String txt_product_code) {
		this.txt_product_code = txt_product_code;
	}
	public String getTxt_product_name() {
		return txt_product_name;
	}
	public void setTxt_product_name(String txt_product_name) {
		this.txt_product_name = txt_product_name;
	}
	public String getTxt_master_pack() {
		return txt_master_pack;
	}
	public void setTxt_master_pack(String txt_master_pack) {
		this.txt_master_pack = txt_master_pack;
	}
	public String getTxt_price_unit() {
		return txt_price_unit;
	}
	public void setTxt_price_unit(String txt_price_unit) {
		this.txt_price_unit = txt_price_unit;
	}
	public String getTxt_product_quality_name() {
		return txt_product_quality_name;
	}
	public void setTxt_product_quality_name(String txt_product_quality_name) {
		this.txt_product_quality_name = txt_product_quality_name;
	}
	public String getTxt_product_design_name() {
		return txt_product_design_name;
	}
	public void setTxt_product_design_name(String txt_product_design_name) {
		this.txt_product_design_name = txt_product_design_name;
	}
	public String getTxt_product_category_name() {
		return txt_product_category_name;
	}
	public void setTxt_product_category_name(String txt_product_category_name) {
		this.txt_product_category_name = txt_product_category_name;
	}
	public Date getDte_modifieddate() {
		return dte_modifieddate;
	}
	public void setDte_modifieddate(Date dte_modifieddate) {
		this.dte_modifieddate = dte_modifieddate;
	}
	public BigDecimal getNum_rate() {
		return num_rate;
	}
	public void setNum_rate(BigDecimal num_rate) {
		this.num_rate = num_rate;
	}
	public BigDecimal getNum_discounted_rate() {
		return num_discounted_rate;
	}
	public void setNum_discounted_rate(BigDecimal num_discounted_rate) {
		this.num_discounted_rate = num_discounted_rate;
	}
	
	public int getSer_price_list_id() {
		return ser_price_list_id;
	}
	public void setSer_price_list_id(int ser_price_list_id) {
		this.ser_price_list_id = ser_price_list_id;
	}

	public int getReq_all() {
		return req_all;
	}
	public void setReq_all(int req_all) {
		this.req_all = req_all;
	}
	

	

	private int ser_product_id;
	private int ser_product_design_id;
	private int ser_product_quality_id;
	private int ser_product_category_id;
	private int ser_price_list_id;
	private int req_all;
	
	private int ser_customer_id;

	public int getSer_customer_id() {
		return ser_customer_id;
	}
	public void setSer_customer_id(int ser_customer_id) {
		this.ser_customer_id = ser_customer_id;
	}




	private boolean is_for_all_design;
	
	public boolean isIs_for_all_design() {
		return is_for_all_design;
	}
	public void setIs_for_all_design(boolean is_for_all_design) {
		this.is_for_all_design = is_for_all_design;
	}



	
	public String getTxt_customer_code() {
		return txt_customer_code;
	}
	public void setTxt_customer_code(String txt_customer_code) {
		this.txt_customer_code = txt_customer_code;
	}
	public String getTxt_customer_name() {
		return txt_customer_name;
	}
	public void setTxt_customer_name(String txt_customer_name) {
		this.txt_customer_name = txt_customer_name;
	}



	private String txt_customer_code;
	private String txt_customer_name;
	
	private String txt_product_code;
	private String txt_product_name;
	private String txt_master_pack;
	private String txt_price_unit;
	private String txt_product_quality_name;
	private String txt_product_design_name;
	private String txt_product_category_name;
	private Date dte_modifieddate;
	
	private BigDecimal num_rate;
	private BigDecimal num_discounted_rate;
	
	
	
}
