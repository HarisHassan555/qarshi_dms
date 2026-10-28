package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDetailDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineDto;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowPipelineSaveRequest;
import com.bezkoder.spring.login.sa.bll.services.IWorkflowPipelineService;
import com.bezkoder.spring.login.sa.bll.servicesimpl.WorkflowPipelineService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class WorkflowPipelineController {

    private static final Logger logger = LogManager.getLogger(WorkflowPipelineController.class);
    private static final String SUBMENU_URL = "workflow-pipelines";

    @Autowired
    private IWorkflowPipelineService workflowPipelineService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private IMenuService menuService;

    @RequestMapping(value = "/getAllWorkflowPipelines", method = RequestMethod.GET)
    public List<WorkflowPipelineDto> getAllWorkflowPipelines(HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Collections.emptyList();
            }
            return workflowPipelineService.getAll();
        } catch (Exception ex) {
            logger.error("getAllWorkflowPipelines failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/getWorkflowPipeline", method = RequestMethod.GET)
    public WorkflowPipelineDetailDto getWorkflowPipeline(@RequestParam Integer id, HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            if (id == null || id <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
            return workflowPipelineService.getById(id);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception ex) {
            logger.error("getWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getNewWorkflowPipeline", method = RequestMethod.GET)
    public WorkflowPipelineDetailDto getNewWorkflowPipeline(HttpServletResponse response) {
        try {
            if (!hasCreatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            WorkflowPipelineDetailDto dto = new WorkflowPipelineDetailDto();
            dto.setName("");
            dto.setCode("");
            dto.setDescription("");
            dto.setPipelineType("STANDARD");
            dto.setActive(true);
            dto.setStepCount(0);
            dto.setPipeline(WorkflowPipelineService.defaultPipelineSteps());
            return dto;
        } catch (Exception ex) {
            logger.error("getNewWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/addWorkflowPipeline", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkflowPipelineDetailDto addWorkflowPipeline(@RequestBody WorkflowPipelineSaveRequest body, HttpServletResponse response) {
        try {
            if (!hasCreatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            int userId = commonService.getCurrentLoggedInUser();
            return workflowPipelineService.create(body, userId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        } catch (Exception ex) {
            logger.error("addWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/updateWorkflowPipeline", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkflowPipelineDetailDto updateWorkflowPipeline(@RequestBody WorkflowPipelineSaveRequest body, HttpServletResponse response) {
        try {
            if (!hasUpdatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            int userId = commonService.getCurrentLoggedInUser();
            return workflowPipelineService.update(body, userId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        } catch (Exception ex) {
            logger.error("updateWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/deleteWorkflowPipeline", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public String deleteWorkflowPipeline(@RequestBody String idsCsv, HttpServletResponse response) {
        try {
            if (!hasDeletePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return "{\"status\":\"Failure\"}";
            }
            List<Integer> ids = parseIds(idsCsv);
            int userId = commonService.getCurrentLoggedInUser();
            return workflowPipelineService.deleteByIds(ids, userId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "{\"status\":\"Failure\",\"message\":\"" + escapeJson(ex.getMessage()) + "\"}";
        } catch (Exception ex) {
            logger.error("deleteWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return "{\"status\":\"Failure\"}";
        }
    }

    @RequestMapping(value = "/duplicateWorkflowPipeline", method = RequestMethod.POST)
    public WorkflowPipelineDetailDto duplicateWorkflowPipeline(@RequestParam Integer id, HttpServletResponse response) {
        try {
            if (!hasCreatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            if (id == null || id <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return null;
            }
            int userId = commonService.getCurrentLoggedInUser();
            return workflowPipelineService.duplicate(id, userId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        } catch (Exception ex) {
            logger.error("duplicateWorkflowPipeline failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/workflowPipelineExistByCode", method = RequestMethod.GET)
    public Map<String, Object> workflowPipelineExistByCode(
            @RequestParam String code,
            @RequestParam(required = false) Integer excludeId,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("exists", false);
                return result;
            }
            String trimmed = code != null ? code.trim() : "";
            if (trimmed.isEmpty()) {
                result.put("exists", false);
                return result;
            }
            boolean exists = workflowPipelineService.existsByCode(trimmed, excludeId);
            result.put("exists", exists);
            return result;
        } catch (Exception ex) {
            logger.error("workflowPipelineExistByCode failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("exists", false);
            return result;
        }
    }

    private List<Integer> parseIds(String idsCsv) {
        List<Integer> ids = new ArrayList<>();
        if (idsCsv == null) {
            return ids;
        }
        String raw = idsCsv.trim();
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1);
        }
        for (String part : raw.split(",")) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip invalid tokens
            }
        }
        return ids;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean hasViewPermission() {
        return hasSubMenuPermission(PermissionMode.VIEW);
    }

    private boolean hasCreatePermission() {
        return hasSubMenuPermission(PermissionMode.CREATE);
    }

    private boolean hasUpdatePermission() {
        return hasSubMenuPermission(PermissionMode.UPDATE);
    }

    private boolean hasDeletePermission() {
        return hasSubMenuPermission(PermissionMode.DELETE);
    }

    private enum PermissionMode {
        VIEW, CREATE, UPDATE, DELETE
    }

    private boolean hasSubMenuPermission(PermissionMode mode) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            return false;
        }
        CfgTblUser user = commonService.getCurrentUser(userId);
        if (user == null || user.getCfgTblRole() == null || user.getCfgTblRole().getSerRoleId() == null) {
            return false;
        }
        Long roleId = Long.valueOf(user.getCfgTblRole().getSerRoleId());
        List<CfgTblSubMenuRole> permissions = menuService.getAllSubMenuRole(roleId, Long.valueOf(userId));
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        for (CfgTblSubMenuRole permission : permissions) {
            if (!isPermissionEnabled(permission)) {
                continue;
            }
            CfgTblSubMenu subMenu = permission.getCfgTblSubMenu();
            if (subMenu == null || !matchesSubMenuUrl(subMenu.getTxtSubMenuUrl())) {
                continue;
            }
            switch (mode) {
                case VIEW:
                    if (isTrue(permission.getBlIsview()) || isTrue(permission.getBlIsNewView())) {
                        return true;
                    }
                    break;
                case CREATE:
                    if (isTrue(permission.getBlIsAdd()) || isTrue(permission.getBlIsNewCreate())) {
                        return true;
                    }
                    break;
                case UPDATE:
                    if (isTrue(permission.getBlIsUpdate()) || isTrue(permission.getBlIsNewUpdate())) {
                        return true;
                    }
                    break;
                case DELETE:
                    if (isTrue(permission.getBlIsDelete())) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    private boolean isPermissionEnabled(CfgTblSubMenuRole permission) {
        if (permission == null || isTrue(permission.getBlIsDeleted())) {
            return false;
        }
        if (!isTrue(permission.getBlIsEnabled())) {
            return false;
        }
        return isTrue(permission.getBlIsActive()) || isTrue(permission.getBlnStatus());
    }

    private boolean matchesSubMenuUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.trim().replaceAll("^/+", "").toLowerCase();
        if (SUBMENU_URL.equals(normalized)) {
            return true;
        }
        return normalized.endsWith("/" + SUBMENU_URL);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
