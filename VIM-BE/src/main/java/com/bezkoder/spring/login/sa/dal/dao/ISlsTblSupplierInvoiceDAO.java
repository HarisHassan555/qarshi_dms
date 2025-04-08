package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSupplierInvoice;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblInvoiceDetail;

public interface ISlsTblSupplierInvoiceDAO {

	List<SlsTblSupplierInvoice> getAllSupplierInvoice();

	List<SlsTblSupplierInvoice> getActiveSupplierInvoice();

	List<SlsTblSupplierInvoice> getSupplierInvoiceByProperty(String property, String value, String mode, String oldValue);

	String addNewSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice);

	String deleteSupplierInvoice(List<String> customerId);

	String updateSupplierInvoice(SlsTblSupplierInvoice slsTblSupplierInvoice);

	String generateSupplierInvoiceNo(String type);

	String getSupplierInvoiceById(String customerId);
	
	List<SlsTblSupplierInvoice> searchSupplierInvoice(SlsTblSupplierInvoice SupplierInvoice);
	
	List<SlsTblInvoiceDetail> searchSupplierInvoiceDetail(int SupplierInvoiceId);
}
