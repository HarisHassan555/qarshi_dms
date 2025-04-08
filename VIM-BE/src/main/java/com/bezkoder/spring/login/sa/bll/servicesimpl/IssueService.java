package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.spring.login.sa.bll.dto.DCDTO;
import com.bezkoder.spring.login.sa.bll.services.IIssueService;
import com.bezkoder.spring.login.sa.dal.dao.IInvTblIssueDAO;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssue;
import com.bezkoder.spring.login.sa.dal.entities.InvTblIssueDetail;


@Service
public class IssueService implements IIssueService {
	
	@Autowired
	private IInvTblIssueDAO citTableIssueDAO;

	private Logger logger = LogManager.getLogger(IssueService.class);

	public IssueService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<InvTblIssue> getAllIssue() {
		logger.debug("getAllIssues()");
		List<InvTblIssue> citys = citTableIssueDAO.getAllIssue();
		return citys;
	}
	
	@Override
	public List<InvTblIssue> getActiveIssue() {
		logger.debug("getActiveIssues()");
		List<InvTblIssue> citys = citTableIssueDAO.getActiveIssue();
		return citys;
	}
	
	@Override
	public String generateIssueNo(String type) {
		
		return citTableIssueDAO.generateIssueNo(type);
		
	}
	
	@Override
	public boolean getIssueByProperty(String property, String value,String mode, String oldValue) {
		return !citTableIssueDAO.getIssueByProperty(property, value,mode,oldValue).isEmpty();
	}
	
	@Override
	public String addNewIssue(InvTblIssue invTblIssue) {
				
		return citTableIssueDAO.addNewIssue(invTblIssue);
	}

	@Override
	public String updateIssue(InvTblIssue invTblIssue) {
		
		return citTableIssueDAO.updateIssue(invTblIssue);
	}

	@Override
	public String deleteIssue(List<String> soId) {
		// TODO Auto-generated method stub
		return citTableIssueDAO.deleteIssue(soId);
	}
	
	@Override
	public List<InvTblIssue> searchIssue(InvTblIssue so) {
		// TODO Auto-generated method stub
		return citTableIssueDAO.searchIssue(so);
	}

	@Override
	public List<InvTblIssueDetail> searchIssueDetail(int IssueId){
		// TODO Auto-generated method stub
		return citTableIssueDAO.searchIssueDetail(IssueId);
	}
	
	@Override
	public String AssignGatePassNumber(InvTblIssue invTblIssue) {
		// TODO Auto-generated method stub
		return citTableIssueDAO.AssignGatePassNumber(invTblIssue);
	}


	@Override
	public String addNewIssue(DCDTO invTblIssue) {
				
		return citTableIssueDAO.addNewIssue(invTblIssue);
	}
	
	@Override
	public String updateIssue(DCDTO invTblIssue) {
				
		return citTableIssueDAO.updateIssue(invTblIssue);
	}
}
