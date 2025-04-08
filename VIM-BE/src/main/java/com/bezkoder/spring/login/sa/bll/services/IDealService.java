package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleItemSchedule;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDeal;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDealDetails;


public interface IDealService {

	List<SlsTblDeal> getAllDeal();
	
	List<SlsTblDeal> getActiveDeal();
	
	String addNewDeal(SlsTblDeal slsTblDeal);

	boolean getDealByProperty(String property, String value, String mode, String oldValue);
	
	String deleteDeal(List<String> dealId);

	String updateDeal(SlsTblDeal slsTblDeal);
	
	String generateDealNo(String type);
	
	List<SlsTblDeal> searchDeal(SlsTblDeal deal);
	
	List<SlsTblDealDetails> searchDealDetail(int DealId);
	
	List<SlsTblSaleItemSchedule> searchDealDetailSchedule(int sodetailid,boolean isforSO);
	
	 byte[] getSOPicture(String id);
	 
	 String updateDealfromSAP(SlsTblDeal slsTblDeal);
	 
	 String updateDeal(List<String> lstOrders);


}
