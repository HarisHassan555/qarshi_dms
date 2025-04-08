package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.Date;
import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.SODTO;
import com.bezkoder.spring.login.sa.dal.entities.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.services.ISaleOrderService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;


@Service
public class SaleOrderService implements ISaleOrderService {
	
	@Autowired
	private ISlsTblSaleOrderDAO citTableSaleOrderDAO;

	private Logger logger = LogManager.getLogger(SaleOrderService.class);

	public SaleOrderService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<SlsTblSaleOrder> getAllSaleOrder() {
		logger.debug("getAllSaleOrders()");
		List<SlsTblSaleOrder> citys = citTableSaleOrderDAO.getAllSaleOrder();
		return citys;
	}
	
	@Override
	public List<SlsTblSaleOrder> getActiveSaleOrder() {
		logger.debug("getActiveSaleOrders()");
		List<SlsTblSaleOrder> citys = citTableSaleOrderDAO.getActiveSaleOrder();
		return citys;
	}
	
	@Override
	public String generateSaleOrderNo(String type) {
		
		return citTableSaleOrderDAO.generateSaleOrderNo(type);
		
	}
	
	@Override
	public boolean getSaleOrderByProperty(String property, String value,String mode, String oldValue) {
		return !citTableSaleOrderDAO.getSaleOrderByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
				
		return citTableSaleOrderDAO.addNewSaleOrder(slsTblSaleOrder);
	}

	@Override
	public String updateSaleOrder(SlsTblSaleOrder slsTblSaleOrder) {
		
		return citTableSaleOrderDAO.updateSaleOrder(slsTblSaleOrder);
	}

	@Override
	public String deleteSaleOrder(List<String> soId) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.deleteSaleOrder(soId);
	}
	
	@Override
	public List<SlsTblSaleOrder> searchSaleOrder(SlsTblSaleOrder so) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrder(so);
	}

	@Override
	public List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId){
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderDetail(SaleOrderId);
	}
	
	@Override
	public List<SlsTblSaleItemSchedule> searchSaleOrderDetailSchedule(int sodetailid,boolean isforSO){
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderDetailSchedule(sodetailid, isforSO);
	}
	
	

	@Override
	public	byte[] getSOPicture(String id)
	{
		return citTableSaleOrderDAO.getSOPicture(id);
	}
	
	@Override
	public	String updateSaleOrderfromSAP(SlsTblSaleOrder slsTblSaleOrder)
	{
		return citTableSaleOrderDAO.updateSaleOrderfromSAP(slsTblSaleOrder);
	}
	
	@Override
	public	String updateSaleOrder(List<String> lstOrders,Date delivery)
	{
		return citTableSaleOrderDAO.updateSaleOrder(lstOrders, delivery);
	}

	
	@Override
	public	String addNewSaleOrderPayment(SlsTblSoPayments slsTblSoPayments)
	{
		return citTableSaleOrderDAO.addNewSaleOrderPayment(slsTblSoPayments);
	}
	
	@Override
	public List<SlsTblSoPayments> getAllSaleOrderPayments() {
		logger.debug("getAllSaleOrderPayments()");
		List<SlsTblSoPayments> citys = citTableSaleOrderDAO.getAllSaleOrderPayments();
		return citys;
	}
	
	
	@Override
	public List<SlsTblSoPayments> searchSaleOrderPayments(SlsTblSoPayments slsTblSoPayments) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderPayments(slsTblSoPayments);
	}
	
	@Override
	public String uploadPaymentDocument(SOPaymentDocument paymentDocument) {
		return citTableSaleOrderDAO.uploadPaymentDocument(paymentDocument);
	}


	@Override
	public byte[] downloadDocument(int documentId) {
		return citTableSaleOrderDAO.downloadDocument(documentId);
	}


	@Override
	public String removeCandidateDocument(int documentId) {
		return citTableSaleOrderDAO.removeCandidateDocument(documentId);
	}
	

	@Override
	public List<SOPaymentDocument> getSOPaymentDocumentList(int id) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.getSOPaymentDocumentList(id);
	}
	
	@Override
	public String updateSaleOrder(List<String> lstOrders, Date delivery, int Level, SODTO dto) throws Exception {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.updateSaleOrder(lstOrders,delivery,Level, dto);
	}
	

	@Override
	public String addNewSaleOrderWithPaymentDocument(SlsTblSaleOrder slsTblSaleOrder,SOPaymentDocument PD)  {
				
		return citTableSaleOrderDAO.addNewSaleOrderWithPaymentDocument(slsTblSaleOrder,PD);
	}
	
	
	@Override
	public String  deletePayment(List<String> paymentId){
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.deletePayment(paymentId);
	}
	
	
	@Override
	public String addToolsDetail(List<SlsTblToolDetail> lstToolDetail) {
		
		return citTableSaleOrderDAO.addToolsDetail(lstToolDetail);
	}
	
	@Override
	public List<SlsTblToolDetail> searchSaleOrderToolDetail(int SaleOrderId) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderToolDetail(SaleOrderId);
	}
	
	@Override
	public String updateToolsDetail(List<SlsTblToolDetail> lstToolDetail) {
		
		return citTableSaleOrderDAO.updateToolsDetail(lstToolDetail);
	}

	@Override
	public String addNewSaleOrderWithPaymentDocuments(SlsTblSaleOrder slsTblSaleOrder,List<SOPaymentDocument> PD)  {

		return citTableSaleOrderDAO.addNewSaleOrderWithPaymentDocuments(slsTblSaleOrder,PD);
	}


	@Override
	public List<AuditSlsTblSaleOrder> searchSaleOrderAudit(int SaleOrderId){
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderAudit(SaleOrderId);
	}


	@Override
	public List<SlsTblSaleOrder> searchSaleOrderBtDepartment(SlsTblSaleOrder so) {
		// TODO Auto-generated method stub
		return citTableSaleOrderDAO.searchSaleOrderByDepartment(so);
	}

}
