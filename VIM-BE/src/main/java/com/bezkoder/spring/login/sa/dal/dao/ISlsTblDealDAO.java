package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleItemSchedule;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDeal;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDealDetails;

public interface ISlsTblDealDAO {

	List<SlsTblDeal> getAllDeal();

	List<SlsTblDeal> getActiveDeal();

	List<SlsTblDeal> getDealByProperty(String property, String value, String mode, String oldValue);

	String addNewDeal(SlsTblDeal slsTblDeal);

	String deleteDeal(List<String> customerId);

	String updateDeal(SlsTblDeal slsTblDeal);

	String generateDealNo(String type);

	String getDealById(String customerId);
	
	List<SlsTblDeal> searchDeal(SlsTblDeal Deal);
	
	List<SlsTblDealDetails> searchDealDetail(int DealId);
	
	List<SlsTblSaleItemSchedule> searchDealDetailSchedule(int sodetailid,boolean isforSO);
	
	 byte[] getSOPicture(String id);
	 
	 String updateDealfromSAP(SlsTblDeal slsTblDeal);
	 
	 List<SlsTblDeal> getDealBySapId(String DealId);

	 String updateDeal(List<String> lstOrders);
	 
	 SlsTblDealDetails getDealDetailByPK(int detailId);
	 
	 void sendSaleOrderinMail(SlsTblSaleOrder slsTblSaleOrder, SlsTblSoDetail soDetail);
	 
	 SlsTblDeal getSOByPK(int SOId);

	String addNewDeals(List<SlsTblDeal> slsTblDeal);

	String updateDealDetailsSAP(SlsTblDealDetails slsTblDealDetails);
}
