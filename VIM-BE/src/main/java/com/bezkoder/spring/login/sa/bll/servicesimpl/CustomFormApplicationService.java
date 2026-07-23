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
    public List<CfgTblCustomFormApplication> getDepartmentApplications(Integer userId) {
        logger.debug("getDepartmentApplications() - userId: " + userId);
        return customFormApplicationDAO.getDepartmentApplications(userId);
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
            boolean refreshCapfSignatures, boolean suppressEditNotification) {
        logger.debug("updateApplicationPdf() - applicationId: " + applicationId + ", refreshCapfSignatures: "
                + refreshCapfSignatures + ", suppressEditNotification: " + suppressEditNotification);
        return customFormApplicationDAO.updateApplicationPdf(applicationId, pdfData, pdfName, pdfMime,
                refreshCapfSignatures, suppressEditNotification);
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
    public String resubmitApplicationFromInitiator(Integer applicationId, String remarks, Integer userId) {
        logger.debug("resubmitApplicationFromInitiator() - applicationId: " + applicationId + ", userId: " + userId);
        return customFormApplicationDAO.resubmitApplicationFromInitiator(applicationId, remarks, userId);
    }

    @Override
    public String requestApplicationOpinion(Integer applicationId, Integer opinionUserId, String remarks) {
        logger.debug("requestApplicationOpinion() - applicationId: " + applicationId + ", opinionUserId: " + opinionUserId);
        return customFormApplicationDAO.requestApplicationOpinion(applicationId, opinionUserId, remarks);
    }

    @Override
    public String submitApplicationOpinion(Integer applicationId, String action, String remarks) {
        logger.debug("submitApplicationOpinion() - applicationId: " + applicationId + ", action: " + action);
        return customFormApplicationDAO.submitApplicationOpinion(applicationId, action, remarks);
    }

    @Override
    public String sendSubmissionEmailsForApplication(Integer applicationId) {
        logger.debug("sendSubmissionEmailsForApplication() - applicationId: " + applicationId);
        return customFormApplicationDAO.sendSubmissionEmailsForApplication(applicationId);
    }

    @Override
    public String sendTemplatePostApprovalEmails(Integer applicationId) {
        logger.debug("sendTemplatePostApprovalEmails() - applicationId: " + applicationId);
        return customFormApplicationDAO.sendTemplatePostApprovalEmails(applicationId);
    }

    @Override
    public String sendTemplatePostApprovalEmails(Integer applicationId, byte[] initiatorPdf, byte[] approverPdf,
            String pdfName, String pdfMime) {
        logger.debug("sendTemplatePostApprovalEmails() - applicationId: " + applicationId + ", custom PDFs");
        return customFormApplicationDAO.sendTemplatePostApprovalEmails(applicationId, initiatorPdf, approverPdf,
                pdfName, pdfMime);
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

    @Override
    public String assignPoCode(Integer applicationId, String poCode, Integer userId, String approvedIp) {
        logger.debug("assignPoCode() - applicationId: " + applicationId + ", userId: " + userId);
        return customFormApplicationDAO.assignPoCode(applicationId, poCode, userId, approvedIp);
    }
}
