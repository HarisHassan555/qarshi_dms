package com.bezkoder.spring.login.sa.dal.dao;

import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;

public interface ICfgTblCustomFormApplicationDAO {

    List<CfgTblCustomFormApplication> getAllApplications();

    List<CfgTblCustomFormApplication> getApplicationsByFormId(Integer formId);

    List<CfgTblCustomFormApplication> getApplicationsByUserId(Integer userId);

    CfgTblCustomFormApplication getApplicationById(Integer applicationId);

    String submitApplication(CfgTblCustomFormApplication application);

    String updateApplication(CfgTblCustomFormApplication application);

    String updateApplicationPdf(Integer applicationId, byte[] pdfData, String pdfName, String pdfMime,
            boolean refreshCapfSignatures);

    String deleteApplication(Integer applicationId);

    List<CfgTblCustomFormApplication> getApplicationsByStatus(String status);

    List<CfgTblCustomFormApplication> getApplicationsApprovedByUser(String status, Integer userId);

    List<CfgTblCustomFormApplication> getApplicationsByStatusAndUserId(String status, Integer userId);

    String getNextApplicationCode(Integer formId);

    List<CfgTblCustomFormApplication> getApplicationsPendingApprovalForDepartmentHead(Integer departmentHeadUserId);

    List<CfgTblCustomFormApplication> getAllApplicationsPendingApproval();

    String approveApplication(Integer applicationId, String remarks);

    String approveApplication(Integer applicationId, String remarks, Integer approverUserId, String approvedVia,
            String approvedIp);

    String rejectApplication(Integer applicationId, String remarks);

    String sendBackApplication(Integer applicationId, String remarks);
    String sendBackApplicationToInitiator(Integer applicationId, String remarks);


    String sendSubmissionEmailsForApplication(Integer applicationId);

    String assignAssetCode(Integer applicationId, String assetCode, Integer userId, String approvedIp);

    String assignPrCode(Integer applicationId, String prCode, Integer userId, String approvedIp);
}
