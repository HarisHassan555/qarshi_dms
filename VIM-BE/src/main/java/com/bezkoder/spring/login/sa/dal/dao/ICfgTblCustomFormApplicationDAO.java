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

    String deleteApplication(Integer applicationId);

    List<CfgTblCustomFormApplication> getApplicationsByStatus(String status);

    String getNextApplicationCode(Integer formId);

    List<CfgTblCustomFormApplication> getApplicationsPendingApprovalForDepartmentHead(Integer departmentHeadUserId);

    String approveApplication(Integer applicationId, String remarks);

    String approveApplication(Integer applicationId, String remarks, Integer approverUserId, String approvedVia);

    String rejectApplication(Integer applicationId, String remarks);

    String sendBackApplication(Integer applicationId, String remarks);
}
