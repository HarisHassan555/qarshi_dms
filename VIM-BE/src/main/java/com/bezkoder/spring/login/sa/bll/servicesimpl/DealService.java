package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.IDealService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblDealDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleItemSchedule;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDeal;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblDealDetails;


@Service
public class DealService implements IDealService {
	
	@Autowired
	private ISlsTblDealDAO citTableDealDAO;

	private Logger logger = LogManager.getLogger(DealService.class);

	public DealService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<SlsTblDeal> getAllDeal() {
		logger.debug("getAllDeals()");
		List<SlsTblDeal> citys = citTableDealDAO.getAllDeal();
		return citys;
	}
	
	@Override
	public List<SlsTblDeal> getActiveDeal() {
		logger.debug("getActiveDeals()");
		List<SlsTblDeal> citys = citTableDealDAO.getActiveDeal();
		return citys;
	}
	
	@Override
	public String generateDealNo(String type) {
		
		return citTableDealDAO.generateDealNo(type);
		
	}
	
	@Override
	public boolean getDealByProperty(String property, String value,String mode, String oldValue) {
		return !citTableDealDAO.getDealByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewDeal(SlsTblDeal slsTblDeal) {
				
		return citTableDealDAO.addNewDeal(slsTblDeal);
	}

	@Override
	public String updateDeal(SlsTblDeal slsTblDeal) {
		
		return citTableDealDAO.updateDeal(slsTblDeal);
	}

	@Override
	public String deleteDeal(List<String> soId) {
		// TODO Auto-generated method stub
		return citTableDealDAO.deleteDeal(soId);
	}
	
	@Override
	public List<SlsTblDeal> searchDeal(SlsTblDeal so) {
		// TODO Auto-generated method stub
		return citTableDealDAO.searchDeal(so);
	}

	@Override
	public List<SlsTblDealDetails> searchDealDetail(int DealId){
		// TODO Auto-generated method stub
		return citTableDealDAO.searchDealDetail(DealId);
	}
	
	@Override
	public List<SlsTblSaleItemSchedule> searchDealDetailSchedule(int sodetailid,boolean isforSO){
		// TODO Auto-generated method stub
		return citTableDealDAO.searchDealDetailSchedule(sodetailid, isforSO);
	}
	
	

	@Override
	public	byte[] getSOPicture(String id)
	{
		return citTableDealDAO.getSOPicture(id);
	}
	
	@Override
	public	String updateDealfromSAP(SlsTblDeal slsTblDeal)
	{
		return citTableDealDAO.updateDealfromSAP(slsTblDeal);
	}
	
	@Override
	public	String updateDeal(List<String> lstOrders)
	{
		return citTableDealDAO.updateDeal(lstOrders);
	}

}
