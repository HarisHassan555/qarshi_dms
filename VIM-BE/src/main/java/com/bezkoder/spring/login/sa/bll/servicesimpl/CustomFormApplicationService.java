package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormApplicationService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormApplicationDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;

@Service
public class CustomFormApplicationService implements ICustomFormApplicationService {

    @Autowired
    private ICfgTblCustomFormApplicationDAO customFormApplicationDAO;

    private Logger logger = LogManager.getLogger(CustomFormApplicationService.class);

    public CustomFormApplicationService() {
    }

    @Override
    public List<CfgTblCustomFormApplication> getAllApplications() {
        logger.debug("getAllApplications()");
        return customFormApplicationDAO.getAllApplications();
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsByFormId(Integer formId) {
        logger.debug("getApplicationsByFormId() - formId: " + formId);
        return customFormApplicationDAO.getApplicationsByFormId(formId);
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsByUserId(Integer userId) {
        logger.debug("getApplicationsByUserId() - userId: " + userId);
        return customFormApplicationDAO.getApplicationsByUserId(userId);
    }

    @Override
    public CfgTblCustomFormApplication getApplicationById(Integer applicationId) {
        logger.debug("getApplicationById() - applicationId: " + applicationId);
        return customFormApplicationDAO.getApplicationById(applicationId);
    }

    @Override
    public String submitApplication(CfgTblCustomFormApplication application) {
        logger.debug("submitApplication()");
        return customFormApplicationDAO.submitApplication(application);
    }

    @Override
    public String updateApplication(CfgTblCustomFormApplication application) {
        logger.debug("updateApplication()");
        return customFormApplicationDAO.updateApplication(application);
    }

    @Override
    public String updateApplicationPdf(Integer applicationId, byte[] pdfData, String pdfName, String pdfMime,
            boolean refreshCapfSignatures) {
        logger.debug("updateApplicationPdf() - applicationId: " + applicationId + ", refreshCapfSignatures: "
                + refreshCapfSignatures);
        return customFormApplicationDAO.updateApplicationPdf(applicationId, pdfData, pdfName, pdfMime,
                refreshCapfSignatures);
    }

    @Override
    public String deleteApplication(Integer applicationId) {
        logger.debug("deleteApplication() - applicationId: " + applicationId);
        return customFormApplicationDAO.deleteApplication(applicationId);
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsByStatus(String status) {
        logger.debug("getApplicationsByStatus() - status: " + status);
        return customFormApplicationDAO.getApplicationsByStatus(status);
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsApprovedByUser(String status, Integer userId) {
        logger.debug("getApplicationsApprovedByUser() - status: " + status + ", userId: " + userId);
        return customFormApplicationDAO.getApplicationsApprovedByUser(status, userId);
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsByStatusAndUserId(String status, Integer userId) {
        logger.debug("getApplicationsByStatusAndUserId() - status: " + status + ", userId: " + userId);
        return customFormApplicationDAO.getApplicationsByStatusAndUserId(status, userId);
    }

    @Override
    public String getNextApplicationCode(Integer formId) {
        logger.debug("getNextApplicationCode() - formId: " + formId);
        return customFormApplicationDAO.getNextApplicationCode(formId);
    }

    @Override
    public List<CfgTblCustomFormApplication> getApplicationsPendingApprovalForDepartmentHead(
            Integer departmentHeadUserId) {
        logger.debug(
                "getApplicationsPendingApprovalForDepartmentHead() - departmentHeadUserId: " + departmentHeadUserId);
        return customFormApplicationDAO.getApplicationsPendingApprovalForDepartmentHead(departmentHeadUserId);
    }

    @Override
    public List<CfgTblCustomFormApplication> getAllApplicationsPendingApproval() {
        logger.debug("getAllApplicationsPendingApproval()");
        return customFormApplicationDAO.getAllApplicationsPendingApproval();
    }

    @Override
    public String approveApplication(Integer applicationId, String remarks) {
        logger.debug("approveApplication() - applicationId: " + applicationId);
        return approveApplication(applicationId, remarks, null, "SYSTEM", null);
    }

    @Override
    public String approveApplication(Integer applicationId, String remarks, Integer approverUserId, String approvedVia,
            String approvedIp) {
        logger.debug("approveApplication() - applicationId: " + applicationId + ", approverUserId: " + approverUserId);
        return customFormApplicationDAO.approveApplication(applicationId, remarks, approverUserId, approvedVia,
                approvedIp);
    }

    @Override
    public String rejectApplication(Integer applicationId, String remarks) {
        logger.debug("rejectApplication() - applicationId: " + applicationId);
        return customFormApplicationDAO.rejectApplication(applicationId, remarks);
    }

    @Override
    public String sendBackApplication(Integer applicationId, String remarks) {
        logger.debug("sendBackApplication() - applicationId: " + applicationId);
        return customFormApplicationDAO.sendBackApplication(applicationId, remarks);
    }

    @Override
    public String sendBackApplicationToInitiator(Integer applicationId, String remarks) {
        logger.debug("sendBackApplicationToInitiator() - applicationId: " + applicationId);
        return customFormApplicationDAO.sendBackApplicationToInitiator(applicationId, remarks);
    }


    @Override
    public String sendSubmissionEmailsForApplication(Integer applicationId) {
        logger.debug("sendSubmissionEmailsForApplication() - applicationId: " + applicationId);
        return customFormApplicationDAO.sendSubmissionEmailsForApplication(applicationId);
    }

    @Override
    public String assignAssetCode(Integer applicationId, String assetCode, Integer userId, String approvedIp) {
        logger.debug("assignAssetCode() - applicationId: " + applicationId + ", userId: " + userId);
        return customFormApplicationDAO.assignAssetCode(applicationId, assetCode, userId, approvedIp);
    }

    @Override
    public String assignPrCode(Integer applicationId, String prCode, Integer userId, String approvedIp) {
        logger.debug("assignPrCode() - applicationId: " + applicationId + ", userId: " + userId);
        return customFormApplicationDAO.assignPrCode(applicationId, prCode, userId, approvedIp);
    }
}
