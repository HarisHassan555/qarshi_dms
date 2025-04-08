package com.bezkoder.spring.login.sa.dal.dao;

import java.util.Date;
import java.util.List;

import com.bezkoder.spring.login.admin.bll.dto.DashBoardDto;
import com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto;

public interface IDashBoardDAO {

	public List<DashBoardDto> getTopMostUsedServices(Integer howManyRecordRequired);

	public List<DashBoardDto> getTopMostUsedSpareParts(Integer howManyRecordRequired);

	public List<DashBoardRevenueDto> getRevenueGenerationFromPartsMonthNDealerWise(Date dateFrom, Date dateTo);

	public List<DashBoardRevenueDto> getRevenueGenerationFromServiceMonthlyNDealerWise(Date dateFrom, Date dateTo);

	public List<DashBoardDto> getTopDefectWisePhenomenCount(Integer howManyRecordRequired);

	public List<DashBoardRevenueDto> getWarrantyCostMonthWise(Date dateFrom, Date dateTo);
	
	public List<DashBoardRevenueDto> getCustomerRetension(Date dateFrom, Date dateTo);
	
	
}
