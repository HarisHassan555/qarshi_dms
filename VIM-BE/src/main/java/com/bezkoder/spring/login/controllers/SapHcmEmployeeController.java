package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.admin.utility.common.SapHcmEmployeeStorageUtil;
import com.bezkoder.spring.login.sa.bll.dto.SapHcmEmployeeDto;
import com.bezkoder.spring.login.sa.bll.dto.SapHcmXmlSyncResult;
import com.bezkoder.spring.login.sa.bll.services.ISapHcmEmployeeService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSapHcmEmployee;
import com.bezkoder.spring.login.sa.dal.entities.SapHcmEmployeeRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
public class SapHcmEmployeeController {

    private static final Logger logger = LogManager.getLogger(SapHcmEmployeeController.class);
    private static final String SUBMENU_URL = "sap-hcm-employees";

    @Autowired
    private ISapHcmEmployeeService sapHcmEmployeeService;

    @Autowired
    private SapHcmEmployeeRepository employeeRepository;

    @Autowired
    private IMenuService menuService;

    @Autowired
    private ICommonService commonService;

    @RequestMapping(value = "/getAllSapHcmEmployees", method = RequestMethod.GET)
    public List<SapHcmEmployeeDto> getAllSapHcmEmployees(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Collections.emptyList();
            }
            return sapHcmEmployeeService.getAllEmployees();
        } catch (Exception ex) {
            logger.error("getAllSapHcmEmployees failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/syncSapHcmEmployeesFromSap", method = RequestMethod.POST)
    public SapHcmXmlSyncResult syncSapHcmEmployeesFromSap(HttpServletResponse response) {
        SapHcmXmlSyncResult result = new SapHcmXmlSyncResult();
        try {
            if (!hasSyncPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.setStatus("Failure");
                result.setMessage("You do not have permission to sync SAP HCM data");
                return result;
            }
            return sapHcmEmployeeService.importFromSap();
        } catch (Exception ex) {
            logger.error("syncSapHcmEmployeesFromSap failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.setStatus("Failure");
            result.setMessage(ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/syncSapHcmEmployeesFromXml", method = RequestMethod.POST)
    public SapHcmXmlSyncResult syncSapHcmEmployeesFromXml(
            @RequestParam("file") MultipartFile file,
            HttpServletResponse response) {
        SapHcmXmlSyncResult result = new SapHcmXmlSyncResult();
        try {
            if (!hasSyncPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.setStatus("Failure");
                result.setMessage("You do not have permission to sync SAP HCM data");
                return result;
            }
            if (file == null || file.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.setStatus("Failure");
                result.setMessage("XML file is required");
                return result;
            }
            return sapHcmEmployeeService.importFromXml(file.getInputStream());
        } catch (Exception ex) {
            logger.error("syncSapHcmEmployeesFromXml failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.setStatus("Failure");
            result.setMessage(ex.getMessage());
            return result;
        }
    }

    @RequestMapping(value = "/getSapHcmEmployeePhoto", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getSapHcmEmployeePhoto(
            @RequestParam String employeeId,
            HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).build();
            }
            Optional<CfgTblSapHcmEmployee> rowOpt = employeeRepository
                    .findByTxtEmployeeIdAndBlIsDeletedFalse(employeeId.trim());
            if (!rowOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            CfgTblSapHcmEmployee row = rowOpt.get();
            File photo = null;
            if (row.getTxtPicturePath() != null && !row.getTxtPicturePath().trim().isEmpty()) {
                photo = SapHcmEmployeeStorageUtil.resolvePhotoFile(row.getTxtPicturePath());
            }
            if (photo == null && row.getTxtEmployeeId() != null) {
                photo = SapHcmEmployeeStorageUtil.resolvePhotoFile(
                        SapHcmEmployeeStorageUtil.relativePhotoPath(row.getTxtEmployeeId(), "jpg"));
            }
            if (photo == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(photo.toPath());
            String contentType = Files.probeContentType(photo.toPath());
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(bytes);
        } catch (Exception ex) {
            logger.error("getSapHcmEmployeePhoto failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean hasViewPermission() {
        return hasSubMenuPermission(false);
    }

    private boolean hasSyncPermission() {
        return hasSubMenuPermission(true);
    }

    private boolean hasSubMenuPermission(boolean requireUpdate) {
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
            if (requireUpdate) {
                if (isTrue(permission.getBlIsUpdate()) || isTrue(permission.getBlIsNewUpdate())
                        || isTrue(permission.getBlIsAdd()) || isTrue(permission.getBlIsNewCreate())) {
                    return true;
                }
            } else if (isTrue(permission.getBlIsview()) || isTrue(permission.getBlIsNewView())) {
                return true;
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
        return SUBMENU_URL.equals(normalized);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
