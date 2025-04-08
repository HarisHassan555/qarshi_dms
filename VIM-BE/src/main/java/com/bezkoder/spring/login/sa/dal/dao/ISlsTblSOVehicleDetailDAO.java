package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoVehicleDetail;

public interface ISlsTblSOVehicleDetailDAO {

	List<SlsTblSoVehicleDetail> getAllVehicleDetail();

	List<SlsTblSoVehicleDetail> getActiveVehicleDetail();

	List<SlsTblSoVehicleDetail> getVehicleDetailByProperty(String property, String value, String mode, String oldValue);

	String addNewVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail);

	String deleteVehicleDetail(List<String> customerId);

	String updateVehicleDetail(SlsTblSoVehicleDetail slsTblSoVehicleDetail);

	String generateVehicleDetailNo(String type);

	String getVehicleDetailById(String ChassisNo);
	
	SlsTblSoVehicleDetail getVehicleDetailByChassisno(String ChassisNo);
	
	List<SlsTblSoVehicleDetail> searchVehicleDetail(SlsTblSoVehicleDetail vehicleDetail);
	
    List<SlsTblSoVehicleDetail> getWorkOrderForFFSFU();
	
	List<SlsTblSoVehicleDetail> getWorkOrderForPostSalesFU();
	
	SlsTblSoVehicleDetail addNewVehicleDetailRVC(SlsTblSoVehicleDetail VehicleDetail);
}
