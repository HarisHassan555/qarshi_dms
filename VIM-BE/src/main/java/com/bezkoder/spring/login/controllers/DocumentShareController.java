package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.servicesimpl.DocumentShareLinkService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class DocumentShareController {

    private static final Logger logger = LogManager.getLogger(DocumentShareController.class);

    @Autowired
    private DocumentShareLinkService documentShareLinkService;

    @Autowired
    private ICommonService commonService;

    @PostMapping(value = "/createDocumentShareLink", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createDocumentShareLink(@RequestBody Map<String, Object> body,
                                                       HttpServletResponse response) {
        try {
            int actorUserId = commonService.getCurrentLoggedInUser();
            Integer applicationId = toInteger(body.get("applicationId"));
            Integer expiresInDays = toInteger(body.get("expiresInDays"));
            Map<String, Object> result = documentShareLinkService.createShareLink(actorUserId, applicationId, expiresInDays);
            if ("Failure".equals(result.get("status"))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return result;
        } catch (Exception ex) {
            logger.error("createDocumentShareLink failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return failure(ex.getMessage());
        }
    }

    @GetMapping(value = "/getDocumentShareLinks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getDocumentShareLinks(@RequestParam Integer applicationId,
                                                     HttpServletResponse response) {
        try {
            int actorUserId = commonService.getCurrentLoggedInUser();
            List<Map<String, Object>> items = documentShareLinkService.listShareLinks(actorUserId, applicationId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "Success");
            result.put("items", items);
            return result;
        } catch (Exception ex) {
            logger.error("getDocumentShareLinks failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return failure(ex.getMessage());
        }
    }

    @PostMapping(value = "/revokeDocumentShareLink", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> revokeDocumentShareLink(@RequestBody Map<String, Object> body,
                                                       HttpServletResponse response) {
        try {
            int actorUserId = commonService.getCurrentLoggedInUser();
            Integer shareId = toInteger(body.get("shareId"));
            Map<String, Object> result = documentShareLinkService.revokeShareLink(actorUserId, shareId);
            if ("Failure".equals(result.get("status"))) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            return result;
        } catch (Exception ex) {
            logger.error("revokeDocumentShareLink failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return failure(ex.getMessage());
        }
    }

    @GetMapping(value = "/public/documentShare", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> publicDocumentShare(@RequestParam String token,
                                                   HttpServletResponse response) {
        try {
            Map<String, Object> result = documentShareLinkService.resolvePublicShare(token);
            if ("Failure".equals(result.get("status"))) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return result;
        } catch (Exception ex) {
            logger.error("publicDocumentShare failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return failure(ex.getMessage());
        }
    }

    @GetMapping(value = "/public/documentSharePdf", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> publicDocumentSharePdf(@RequestParam String token,
                                                      HttpServletResponse response) {
        try {
            Map<String, Object> result = documentShareLinkService.resolvePublicSharePdf(token);
            if ("Failure".equals(result.get("status"))) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return result;
        } catch (Exception ex) {
            logger.error("publicDocumentSharePdf failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return failure(ex.getMessage());
        }
    }

    private static Map<String, Object> failure(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Failure");
        result.put("message", message);
        return result;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
