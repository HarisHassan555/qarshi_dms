package com.bezkoder.spring.login.sa.bll.servicesimpl;

import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bezkoder.spring.login.sa.bll.services.ICustomFormService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomFormDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;

@Service
public class CustomFormService implements ICustomFormService {

    @Autowired
    private ICfgTblCustomFormDAO customFormDAO;

    private Logger logger = LogManager.getLogger(CustomFormService.class);

    public CustomFormService() {
    }

    @Override
    public List<CfgTblCustomForm> getAllCustomForms() {
        logger.debug("getAllCustomForms()");
        return customFormDAO.getAllCustomForms();
    }

    @Override
    public CfgTblCustomForm getCustomFormById(Integer formId) {
        logger.debug("getCustomFormById() - formId: " + formId);
        return customFormDAO.getCustomFormById(formId);
    }

    @Override
    public String addNewCustomForm(CfgTblCustomForm customForm) {
        logger.debug("addNewCustomForm()");
        return customFormDAO.addNewCustomForm(customForm);
    }

    @Override
    public String updateCustomForm(CfgTblCustomForm customForm) {
        logger.debug("updateCustomForm()");
        return customFormDAO.updateCustomForm(customForm);
    }

    @Override
    public String deleteCustomForm(Integer formId) {
        logger.debug("deleteCustomForm() - formId: " + formId);
        return customFormDAO.deleteCustomForm(formId);
    }

    @Override
    public List<CfgTblCustomForm> getActiveCustomForms() {
        logger.debug("getActiveCustomForms()");
        return customFormDAO.getActiveCustomForms();
    }
}





