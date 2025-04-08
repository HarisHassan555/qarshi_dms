package com.bezkoder.spring.login.sa.dal.dao;


import java.util.Date;
import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;
import com.bezkoder.spring.login.sa.bll.dto.SODTO;
import com.bezkoder.spring.login.sa.bll.dto.ServiceDTO;
import com.bezkoder.spring.login.sa.dal.entities.*;

public interface ISlsTblSaleOrderDAO {

	List<SlsTblSaleOrder> getAllSaleOrder();

	List<SlsTblSaleOrder> getActiveSaleOrder();

	List<SlsTblSaleOrder> getSaleOrderByProperty(String property, String value, String mode, String oldValue);

	String addNewSaleOrder(SlsTblSaleOrder slsTblSaleOrder);

	String deleteSaleOrder(List<String> customerId);

	String updateSaleOrder(SlsTblSaleOrder slsTblSaleOrder);

	String generateSaleOrderNo(String type);

	String getSaleOrderById(String customerId);
	
	List<SlsTblSaleOrder> searchSaleOrder(SlsTblSaleOrder SaleOrder);
	
	List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId);
	
	List<SlsTblSaleItemSchedule> searchSaleOrderDetailSchedule(int sodetailid,boolean isforSO);
	
	 byte[] getSOPicture(String id);
	 
	 String updateSaleOrderfromSAP(SlsTblSaleOrder slsTblSaleOrder);
	 
	 List<SlsTblSaleOrder> getSaleOrderBySapId(String SaleOrderId);

	 String updateSaleOrder(List<String> lstOrders, Date delivery);
	 
	 String addNewSaleOrderPayment(SlsTblSoPayments slsTblSoPayments);
	 
	 List<SlsTblSoPayments> getAllSaleOrderPayments();
	 
	 List<SlsTblSoPayments> searchSaleOrderPayments(SlsTblSoPayments slsTblSoPayments);
	 
	 SlsTblSaleOrder getSaleOrderByPK(int id);
	 
	 String uploadPaymentDocument(SOPaymentDocument paymentDocument);
	 
	 byte[] downloadDocument(int documentId);
	 
	 String removeCandidateDocument(int documentId);
	 
	 List<SOPaymentDocument> getSOPaymentDocumentList(int id);
	 
	 String updateSaleOrder(List<String> lstOrders, Date delivery, int Level, SODTO dto) throws Exception;
	 
	 String addNewSaleOrderWithPaymentDocument(SlsTblSaleOrder SlsTblSaleOrder,SOPaymentDocument PD);
	 
	 
	 String deletePayment(List<String> paymentId);
	 
	 String addToolsDetail(List<SlsTblToolDetail> lstToolDetail);
	 
	 List<SlsTblToolDetail> searchSaleOrderToolDetail(int SaleOrderId);
	 
	 String updateToolsDetail(List<SlsTblToolDetail> lstToolDetail);
	 
	 String deleteSaleOrder(List<String> SaleOrdersId,String Remarks);
	 
	 String generateSaleOrderNoFORGNL(SlsTblSaleOrder SlsTblSaleOrder);
	 
	 String updateSaleOrderSAP(SlsTblSaleOrder slsTblSaleOrder);

	String updateSaleOrderSAPPending(SlsTblSaleOrder slsTblSaleOrder);

	List<SlsTblSoPayments> CheckPaymentDuplicationChequeNo(SlsTblSoPayments payment);
	 
	 List<SlsTblSoPayments> CheckPaymentDuplicationtxtSlipNo(SlsTblSoPayments payment);
	 
	 List<SlsTblSoPayments> searchSaleOrderPayments(ReportDTO dto);
	 
	 String updateSaleOrderPayment(List<String> lstOrderPayments,  int Level);

	String addNewSaleOrderWithPaymentDocuments(SlsTblSaleOrder SlsTblSaleOrder,List<SOPaymentDocument> PD);

	String uploadPaymentDocumentWithSale(SOPaymentDocument paymentDocument);

	List<AuditSlsTblSaleOrder> searchSaleOrderAudit(int SaleOrderId);

	String updateSaleOrderPayment(ServiceDTO serviceDTO) throws Exception;

	List<SlsTblSaleOrder> searchSaleOrderByDepartment(SlsTblSaleOrder SaleOrder);


	String updateSaleOrderRemarks(List<SlsTblSaleOrder> saleOrders);


	String updateSaleOrderSAPRepost(SlsTblSaleOrder slsTblSaleOrder);

}
