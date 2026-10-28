package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationAttachmentDownload;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegatableDocumentDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationSaveRequest;
import com.bezkoder.spring.login.sa.bll.services.IUserDelegationService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class UserDelegationController {

    private static final Logger logger = LogManager.getLogger(UserDelegationController.class);
    private static final String DELEGATION_SUBMENU_URL = "delegation-management";

    @Autowired
    private IUserDelegationService userDelegationService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private IMenuService menuService;

    @RequestMapping(value = "/getAllUserDelegations", method = RequestMethod.GET)
    public List<UserDelegationDto> getAllUserDelegations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer delegateUserId,
            @RequestParam(required = false) Integer onBehalfUserId,
            HttpServletResponse response) {
        try {
            int currentUserId = commonService.getCurrentLoggedInUser();
            boolean queryingOwnDelegateAssignments = delegateUserId != null
                    && delegateUserId > 0
                    && currentUserId > 0
                    && delegateUserId.equals(currentUserId);
            if (!hasViewPermission()) {
                if (!queryingOwnDelegateAssignments) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return Collections.emptyList();
                }
                return userDelegationService.getAll(status, currentUserId, null);
            }
            return userDelegationService.getAll(status, delegateUserId, onBehalfUserId);
        } catch (Exception ex) {
            logger.error("getAllUserDelegations failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/getDelegatableDocumentsForDelegation", method = RequestMethod.GET)
    public List<UserDelegatableDocumentDto> getDelegatableDocumentsForDelegation(
            @RequestParam Integer onBehalfUserId,
            HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Collections.emptyList();
            }
            if (onBehalfUserId == null || onBehalfUserId <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return Collections.emptyList();
            }
            return userDelegationService.getDelegatableDocumentsForOnBehalfUser(onBehalfUserId);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Collections.emptyList();
        } catch (Exception ex) {
            logger.error("getDelegatableDocumentsForDelegation failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/getUserDelegationAttachment", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getUserDelegationAttachment(@RequestParam Integer id, HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).build();
            }
            UserDelegationAttachmentDownload file = userDelegationService.loadAttachmentDownload(id);
            String fileName = file.getFileName() != null ? file.getFileName() : "delegation-attachment";
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                    .body(file.getContent());
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("getUserDelegationAttachment failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/getUserDelegationById", method = RequestMethod.GET)
    public UserDelegationDto getUserDelegationById(@RequestParam Integer id, HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            return userDelegationService.getById(id);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception ex) {
            logger.error("getUserDelegationById failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/addNewUserDelegation", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> addNewUserDelegation(@RequestBody UserDelegationSaveRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasCreatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to create delegations");
                return result;
            }
            int actorUserId = commonService.getCurrentLoggedInUser();
            UserDelegationDto saved = userDelegationService.create(request, actorUserId);
            result.put("status", "Success");
            result.put("data", saved);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("addNewUserDelegation failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/updateUserDelegation", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateUserDelegation(@RequestBody UserDelegationSaveRequest request,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasUpdatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to update delegations");
                return result;
            }
            int actorUserId = commonService.getCurrentLoggedInUser();
            UserDelegationDto saved = userDelegationService.update(request, actorUserId);
            result.put("status", "Success");
            result.put("data", saved);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("updateUserDelegation failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/revokeUserDelegation", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> revokeUserDelegation(@RequestBody Map<String, Object> body,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasUpdatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to revoke delegations");
                return result;
            }
            Integer id = toInteger(body.get("serDelegationId"));
            if (id == null || id <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("status", "Failure");
                result.put("message", "serDelegationId is required");
                return result;
            }
            int actorUserId = commonService.getCurrentLoggedInUser();
            UserDelegationDto saved = userDelegationService.revoke(id, actorUserId);
            result.put("status", "Success");
            result.put("data", saved);
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("revokeUserDelegation failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/deleteUserDelegation", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> deleteUserDelegation(@RequestBody Map<String, Object> body,
            HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasDeletePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to delete delegations");
                return result;
            }
            Integer id = toInteger(body.get("serDelegationId"));
            if (id == null || id <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("status", "Failure");
                result.put("message", "serDelegationId is required");
                return result;
            }
            int actorUserId = commonService.getCurrentLoggedInUser();
            userDelegationService.delete(id, actorUserId);
            result.put("status", "Success");
            return result;
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            logger.error("deleteUserDelegation failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasViewPermission() {
        return hasDelegationPermission(false, false, false);
    }

    private boolean hasCreatePermission() {
        return hasDelegationPermission(true, false, false);
    }

    private boolean hasUpdatePermission() {
        return hasDelegationPermission(false, true, false);
    }

    private boolean hasDeletePermission() {
        return hasDelegationPermission(false, false, true);
    }

    private boolean hasDelegationPermission(boolean requireCreate, boolean requireUpdate, boolean requireDelete) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            return false;
        }
        CfgTblUser user = commonService.getCurrentUser(userId);
        if (user == null || user.getCfgTblRole() == null) {
            return false;
        }
        if (isAdminRole(user.getCfgTblRole().getTxtRoleName())) {
            return true;
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
            if (subMenu == null || !matchesDelegationUrl(subMenu.getTxtSubMenuUrl())) {
                continue;
            }
            if (requireDelete) {
                if (isTrue(permission.getBlIsDelete())) {
                    return true;
                }
            } else if (requireUpdate) {
                if (isTrue(permission.getBlIsUpdate()) || isTrue(permission.getBlIsNewUpdate())) {
                    return true;
                }
            } else if (requireCreate) {
                if (isTrue(permission.getBlIsAdd()) || isTrue(permission.getBlIsNewCreate())) {
                    return true;
                }
            } else if (isTrue(permission.getBlIsview()) || isTrue(permission.getBlIsNewView())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase();
        return normalized.contains("ADMIN");
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

    private boolean matchesDelegationUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.trim().replaceAll("^/+", "").toLowerCase();
        return DELEGATION_SUBMENU_URL.equals(normalized);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
