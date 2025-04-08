package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssue;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssueDetail;


public interface IIssueService {

	List<InvTblIssue> getAllIssue();
	
	List<InvTblIssue> getActiveIssue();
	
	String addNewIssue(InvTblIssue invTblIssue);

	boolean getIssueByProperty(String property, String value, String mode, String oldValue);
	
	String deleteIssue(List<String> issueId);

	String updateIssue(InvTblIssue invTblIssue);
	
	String generateIssueNo(String type);
	
	List<InvTblIssue> searchIssue(InvTblIssue issue);
	
	List<InvTblIssueDetail> searchIssueDetail(int IssueId);
	
	public String AssignGatePassNumber(InvTblIssue invTblIssue);
	
	String addNewIssue(DCDTO invTblIssue);
	
	String updateIssue(DCDTO invTblIssue);

}
