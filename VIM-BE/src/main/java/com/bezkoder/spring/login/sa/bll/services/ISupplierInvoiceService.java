package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblSupplierInvoice;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblInvoiceDetail;


public interface ISupplierInvoiceService {

	List<SlsTblSupplierInvoice> getAllSupplierInvoice();
	
	List<SlsTblSupplierInvoice> getActiveSupplierInvoice();
	
	String addNewSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice);

	boolean getSupplierInvoiceByProperty(String property, String value, String mode, String oldValue);
	
	String deleteSupplierInvoice(List<String> supplierInvoiceId);

	String updateSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice);
	
	String generateSupplierInvoiceNo(String type);
	
	List<SlsTblSupplierInvoice> searchSupplierInvoice(SlsTblSupplierInvoice supplierInvoice);
	
	List<SlsTblInvoiceDetail> searchSupplierInvoiceDetail(int SupplierInvoiceId);

}
