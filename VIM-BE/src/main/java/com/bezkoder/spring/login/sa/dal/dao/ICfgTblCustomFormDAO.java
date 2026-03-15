package com.bezkoder.spring.login.sa.dal.dao;

import java.util.List;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;

public interface ICfgTblCustomFormDAO {

    List<CfgTblCustomForm> getAllCustomForms();

    CfgTblCustomForm getCustomFormById(Integer formId);

    String addNewCustomForm(CfgTblCustomForm customForm);

    String updateCustomForm(CfgTblCustomForm customForm);

    String deleteCustomForm(Integer formId);

    List<CfgTblCustomForm> getActiveCustomForms();
}











