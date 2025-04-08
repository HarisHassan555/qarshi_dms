package com.bezkoder.spring.login.sa.dal.dao;


import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssue;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssueDetail;

public interface IInvTblIssueDAO {

	List<InvTblIssue> getAllIssue();

	List<InvTblIssue> getActiveIssue();

	List<InvTblIssue> getIssueByProperty(String property, String value, String mode, String oldValue);

	String addNewIssue(InvTblIssue invTblIssue);

	String deleteIssue(List<String> customerId);

	String updateIssue(InvTblIssue invTblIssue);

	String generateIssueNo(String type);

	String getIssueById(String customerId);
	
	List<InvTblIssue> searchIssue(InvTblIssue Issue);
	
	List<InvTblIssueDetail> searchIssueDetail(int IssueId);
	
	public String AssignGatePassNumber(InvTblIssue invTblIssue);
	
	String addNewIssue(DCDTO invTblIssue);
	
	String updateIssue(DCDTO invTblIssue);
	
	String updateIssueWithDCDelete(DCDTO dto);
	
	String updateIssueWithInvoiceCancel(DCDTO dto);
}
