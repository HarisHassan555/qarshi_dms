package com.bezkoder.spring.login.admin.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.admin.bll.services.IBaaService;
import com.bezkoder.spring.login.admin.dal.dao.IBaaDAO;
import com.bezkoder.spring.login.admin.dal.entities.Baa;


@Service
public class BaaService implements IBaaService {
	
	@Autowired
	private IBaaDAO citTableBaaDAO;

	private Logger logger = LogManager.getLogger(BaaService.class);

	public BaaService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<Baa> getAllBaa() {
		logger.debug("getAllBaas()");
		List<Baa> baas = citTableBaaDAO.getAllBaa();
		return baas;
	}
	
	@Override
	public List<Baa> getActiveBaa() {
		logger.debug("getActiveBaas()");
		List<Baa> baas = citTableBaaDAO.getActiveBaa();
		return baas;
	}
	
	@Override
	public String generateBaaNo(String type) {
		
		return citTableBaaDAO.generateBaaNo(type);
		
	}
	
	@Override
	public boolean getBaaByProperty(String property, String value,String mode, String oldValue) {
		return !citTableBaaDAO.getBaaByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewBaa(Baa baa) {
				
		return citTableBaaDAO.addNewBaa(baa);
	}

	@Override
	public String updateBaa(Baa baa) {
		
		return citTableBaaDAO.updateBaa(baa);
	}

	@Override
	public String deleteBaa(List<String> baasId) {
		// TODO Auto-generated method stub
		return citTableBaaDAO.deleteBaa(baasId);
	}
	
	@Override
	public List<Baa> searchBaa(Baa baa) {
		// TODO Auto-generated method stub
		return citTableBaaDAO.searchBaa(baa);
	}

}
