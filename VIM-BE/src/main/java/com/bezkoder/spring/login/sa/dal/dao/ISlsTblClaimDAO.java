package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaim;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblClaimDetail;


public interface ISlsTblClaimDAO {

	List<SlsTblClaim> getAllClaim();

	List<SlsTblClaim> getActiveClaim();

	List<SlsTblClaim> getClaimByProperty(String property, String value, String mode, String oldValue);

	 String addNewClaim(SlsTblClaim slsTblClaim);

	String deleteClaim(List<String> customerId);

	String updateClaim(SlsTblClaim slsTblClaim);

	String generateClaimNo(String type);

	String getClaimById(String customerId);
	
	List<SlsTblClaim> searchClaim(SlsTblClaim Claim);
	
	List<SlsTblClaimDetail> searchClaimDetail(int ClaimId);
	
	public String AssignGatePassNumber(SlsTblClaim slsTblClaim);
	
	String addNewClaim(DCDTO slsTblClaim);
	
	String updateClaim(DCDTO slsTblClaim);
	
	SlsTblClaim getPrevious(String vehicle) ;
	
	List<SlsTblClaim> getClaimForPostServiceFU();
	
	List<SlsTblClaim> getClaimForMaintinanceFU();
	
	String createClaimfromClaim(List<String> ClaimsId);
	
	
}
