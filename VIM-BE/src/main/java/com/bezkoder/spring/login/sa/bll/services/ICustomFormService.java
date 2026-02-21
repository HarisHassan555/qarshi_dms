package com.bezkoder.spring.login.sa.bll.services;

import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;

public interface ICustomFormService {

    List<CfgTblCustomForm> getAllCustomForms();

    CfgTblCustomForm getCustomFormById(Integer formId);

    String addNewCustomForm(CfgTblCustomForm customForm);

    String updateCustomForm(CfgTblCustomForm customForm);

    String deleteCustomForm(Integer formId);

    List<CfgTblCustomForm> getActiveCustomForms();
}









