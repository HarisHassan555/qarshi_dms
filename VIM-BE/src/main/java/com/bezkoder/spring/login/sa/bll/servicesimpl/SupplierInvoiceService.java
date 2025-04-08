package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ISupplierInvoiceService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSupplierInvoiceDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSupplierInvoice;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblInvoiceDetail;


@Service
public class SupplierInvoiceService implements ISupplierInvoiceService {
	
	@Autowired
	private ISlsTblSupplierInvoiceDAO citTableSupplierInvoiceDAO;

	private Logger logger = LogManager.getLogger(SupplierInvoiceService.class);

	public SupplierInvoiceService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<SlsTblSupplierInvoice> getAllSupplierInvoice() {
		logger.debug("getAllSupplierInvoices()");
		List<SlsTblSupplierInvoice> citys = citTableSupplierInvoiceDAO.getAllSupplierInvoice();
		return citys;
	}
	
	@Override
	public List<SlsTblSupplierInvoice> getActiveSupplierInvoice() {
		logger.debug("getActiveSupplierInvoices()");
		List<SlsTblSupplierInvoice> citys = citTableSupplierInvoiceDAO.getActiveSupplierInvoice();
		return citys;
	}
	
	@Override
	public String generateSupplierInvoiceNo(String type) {
		
		return citTableSupplierInvoiceDAO.generateSupplierInvoiceNo(type);
		
	}
	
	@Override
	public boolean getSupplierInvoiceByProperty(String property, String value,String mode, String oldValue) {
		return !citTableSupplierInvoiceDAO.getSupplierInvoiceByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice) {
				
		return citTableSupplierInvoiceDAO.addNewSupplierInvoice(slsTblSupplierInvoice);
	}

	@Override
	public String updateSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice) {
		
		return citTableSupplierInvoiceDAO.updateSupplierInvoice(slsTblSupplierInvoice);
	}

	@Override
	public String deleteSupplierInvoice(List<String> soId) {
		// TODO Auto-generated method stub
		return citTableSupplierInvoiceDAO.deleteSupplierInvoice(soId);
	}
	
	@Override
	public List<SlsTblSupplierInvoice> searchSupplierInvoice(SlsTblSupplierInvoice so) {
		// TODO Auto-generated method stub
		return citTableSupplierInvoiceDAO.searchSupplierInvoice(so);
	}

	@Override
	public List<SlsTblInvoiceDetail> searchSupplierInvoiceDetail(int SupplierInvoiceId){
		// TODO Auto-generated method stub
		return citTableSupplierInvoiceDAO.searchSupplierInvoiceDetail(SupplierInvoiceId);
	}

}
