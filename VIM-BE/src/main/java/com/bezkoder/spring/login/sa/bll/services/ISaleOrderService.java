package com.bezkoder.spring.login.sa.bll.services;

import java.util.Date;
import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.SODTO;
import com.bezkoder.spring.login.sa.dal.entities.*;


public interface ISaleOrderService {

	List<SlsTblSaleOrder> getAllSaleOrder();
	
	List<SlsTblSaleOrder> getActiveSaleOrder();
	
	String addNewSaleOrder(SlsTblSaleOrder slsTblSaleOrder);

	boolean getSaleOrderByProperty(String property, String value, String mode, String oldValue);
	
	String deleteSaleOrder(List<String> saleOrderId);

	String updateSaleOrder(SlsTblSaleOrder slsTblSaleOrder);
	
	String generateSaleOrderNo(String type);
	
	List<SlsTblSaleOrder> searchSaleOrder(SlsTblSaleOrder saleOrder);
	
	List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId);
	
	List<SlsTblSaleItemSchedule> searchSaleOrderDetailSchedule(int sodetailid,boolean isforSO);
	
	 byte[] getSOPicture(String id);
	 
	 String updateSaleOrderfromSAP(SlsTblSaleOrder slsTblSaleOrder);
	 
	 String updateSaleOrder(List<String> lstOrders,Date delivery);

	 String addNewSaleOrderPayment(SlsTblSoPayments slsTblSoPayments);
	 
	 List<SlsTblSoPayments> getAllSaleOrderPayments();
	 
	 List<SlsTblSoPayments> searchSaleOrderPayments(SlsTblSoPayments slsTblSoPayments);
	 
		String uploadPaymentDocument(SOPaymentDocument paymentDocument);
		
		byte[] downloadDocument(int parseInt);
		
		String removeCandidateDocument(int parseInt);
		
		List<SOPaymentDocument> getSOPaymentDocumentList(int id);
		
		String updateSaleOrder(List<String> lstOrders, Date delivery, int Level, SODTO dto) throws Exception;
		
		String addNewSaleOrderWithPaymentDocument(SlsTblSaleOrder slsTblSaleOrder,SOPaymentDocument PD);
		
		String deletePayment(List<String> paymentId);
		
		String addToolsDetail(List<SlsTblToolDetail> lstToolDetail);
		
		 List<SlsTblToolDetail> searchSaleOrderToolDetail(int SaleOrderId);
		 
		 String updateToolsDetail(List<SlsTblToolDetail> lstToolDetail);

		String addNewSaleOrderWithPaymentDocuments(SlsTblSaleOrder slsTblSaleOrder,List<SOPaymentDocument> PD);



	List<AuditSlsTblSaleOrder> searchSaleOrderAudit(int SaleOrderId);

	List<SlsTblSaleOrder> searchSaleOrderBtDepartment(SlsTblSaleOrder saleOrder);
}
