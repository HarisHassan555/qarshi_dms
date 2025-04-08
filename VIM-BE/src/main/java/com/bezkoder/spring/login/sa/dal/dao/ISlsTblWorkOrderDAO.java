package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrder;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblWorkOrderDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaim;

public interface ISlsTblWorkOrderDAO {

	List<SlsTblWorkOrder> getAllWorkOrder();

	List<SlsTblWorkOrder> getActiveWorkOrder();

	List<SlsTblWorkOrder> getWorkOrderByProperty(String property, String value, String mode, String oldValue);

	String addNewWorkOrder(SlsTblWorkOrder slsTblWorkOrder);

	String deleteWorkOrder(List<String> customerId);

	String updateWorkOrder(SlsTblWorkOrder slsTblWorkOrder);

	String generateWorkOrderNo(String type);

	String getWorkOrderById(String customerId);
	
	List<SlsTblWorkOrder> searchWorkOrder(SlsTblWorkOrder WorkOrder);
	
	List<SlsTblWorkOrderDetail> searchWorkOrderDetail(int WorkOrderId);
	
	public String AssignGatePassNumber(SlsTblWorkOrder slsTblWorkOrder);
	
	String addNewWorkOrder(DCDTO slsTblWorkOrder);
	
	String updateWorkOrder(DCDTO slsTblWorkOrder);
	
	SlsTblWorkOrder getPrevious(String vehicle) ;
	
	List<SlsTblWorkOrder> getWorkOrderForPostServiceFU();
	
	List<SlsTblWorkOrder> getWorkOrderForMaintinanceFU();
	
	String createClaimfromWorkOrder(List<String> WorkOrdersId,String Type);
	
	SlsTblClaim addNewClaim(SlsTblClaim slsTblClaim);
	
	List<SlsTblWorkOrder> getAllDealerJCS();
	
	SlsTblWorkOrder getWorkOrderByCode(String WorkOrderId);
	
}
