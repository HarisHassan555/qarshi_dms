package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.ICustomFormService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomForm;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CustomFormController {

    private Logger logger = LogManager.getLogger(CustomFormController.class);

    @Autowired
    private ICustomFormService customFormService;

    @RequestMapping(value = "/getAllCustomForms", method = RequestMethod.GET)
    public List<CfgTblCustomForm> getAllCustomForms(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("getAllCustomForms()");
        try {
            List<CfgTblCustomForm> forms = customFormService.getAllCustomForms();
            return forms;
        } catch (Exception ex) {
            logger.error("Error fetching all custom forms: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getCustomFormById", method = RequestMethod.GET)
    public CfgTblCustomForm getCustomFormById(@RequestParam Integer formId, 
                                               HttpServletRequest request, 
                                               HttpServletResponse response) {
        logger.debug("getCustomFormById() - formId: " + formId);
        try {
            CfgTblCustomForm form = customFormService.getCustomFormById(formId);
            return form;
        } catch (Exception ex) {
            logger.error("Error fetching custom form by ID: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/addNewCustomForm", 
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> addNewCustomForm(@RequestBody CfgTblCustomForm customForm,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        logger.debug("addNewCustomForm()");
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormService.addNewCustomForm(customForm);
            if (status != null && status.startsWith("Success")) {
                result.put("status", "Success");
                result.put("message", "Form created successfully");
                result.put("formId", customForm.getSerFormId());
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : "Failed to create form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error creating custom form: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/updateCustomForm",
                    method = RequestMethod.POST,
                    headers = "Accept=application/json",
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateCustomForm(@RequestBody CfgTblCustomForm customForm,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("updateCustomForm()");
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormService.updateCustomForm(customForm);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Form updated successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", "Failed to update form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating custom form: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/deleteCustomForm", method = RequestMethod.POST)
    public Map<String, Object> deleteCustomForm(@RequestParam Integer formId,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        logger.debug("deleteCustomForm() - formId: " + formId);
        Map<String, Object> result = new HashMap<>();
        try {
            String status = customFormService.deleteCustomForm(formId);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Form deleted successfully");
            } else {
                result.put("status", "Failure");
                result.put("message", "Failed to delete form");
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error deleting custom form: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/getActiveCustomForms", method = RequestMethod.GET)
    public List<CfgTblCustomForm> getActiveCustomForms(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("getActiveCustomForms()");
        try {
            List<CfgTblCustomForm> forms = customFormService.getActiveCustomForms();
            return forms;
        } catch (Exception ex) {
            logger.error("Error fetching active custom forms: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }
}

