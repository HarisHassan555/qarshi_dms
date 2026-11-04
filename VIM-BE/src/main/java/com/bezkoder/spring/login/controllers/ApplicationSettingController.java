package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.utility.common.ApplicationSettingImageStorageUtil;
import com.bezkoder.spring.login.sa.bll.servicesimpl.ApplicationSettingService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblApplicationSetting;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class ApplicationSettingController {

    private static final Logger logger = LogManager.getLogger(ApplicationSettingController.class);

    @Autowired
    private ApplicationSettingService applicationSettingService;

    @RequestMapping(value = "/getAllApplicationSettings", method = RequestMethod.GET)
    public List<CfgTblApplicationSetting> getAllApplicationSettings() {
        return applicationSettingService.getAll();
    }

    @RequestMapping(value = "/getApplicationSetting", method = RequestMethod.GET)
    public ResponseEntity<CfgTblApplicationSetting> getApplicationSetting(
            @RequestParam(value = "serApplicationSettingId", required = false) Integer id) {
        try {
            CfgTblApplicationSetting row = id == null
                    ? applicationSettingService.getCurrent()
                    : applicationSettingService.getById(id);
            if (row == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(row);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/addApplicationSetting", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addApplicationSetting(
            @RequestParam("txtApplicationName") String txtApplicationName,
            @RequestParam(value = "txtAddress", required = false) String txtAddress,
            @RequestParam(value = "txtEmail", required = false) String txtEmail,
            @RequestParam(value = "txtSmtpHost", required = false) String txtSmtpHost,
            @RequestParam(value = "txtSmtpPort", required = false) String txtSmtpPort,
            @RequestParam(value = "txtSmtpUsername", required = false) String txtSmtpUsername,
            @RequestParam(value = "txtSmtpPassword", required = false) String txtSmtpPassword,
            @RequestParam(value = "blSmtpAuth", required = false) String blSmtpAuth,
            @RequestParam(value = "blSmtpSslEnable", required = false) String blSmtpSslEnable,
            @RequestParam(value = "blSmtpStarttlsEnable", required = false) String blSmtpStarttlsEnable,
            @RequestParam(value = "blIsActive", required = false) String blIsActive,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam(value = "hero", required = false) MultipartFile hero) {
        try {
            CfgTblApplicationSetting saved = applicationSettingService.create(
                    input(txtApplicationName, txtAddress, txtEmail, txtSmtpHost, txtSmtpPort, txtSmtpUsername,
                            txtSmtpPassword, blSmtpAuth, blSmtpSslEnable, blSmtpStarttlsEnable, blIsActive, false, false),
                    logo, hero);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("addApplicationSetting failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Could not save application settings"));
        }
    }

    @RequestMapping(value = "/updateApplicationSetting", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateApplicationSetting(
            @RequestParam("serApplicationSettingId") int serApplicationSettingId,
            @RequestParam("txtApplicationName") String txtApplicationName,
            @RequestParam(value = "txtAddress", required = false) String txtAddress,
            @RequestParam(value = "txtEmail", required = false) String txtEmail,
            @RequestParam(value = "txtSmtpHost", required = false) String txtSmtpHost,
            @RequestParam(value = "txtSmtpPort", required = false) String txtSmtpPort,
            @RequestParam(value = "txtSmtpUsername", required = false) String txtSmtpUsername,
            @RequestParam(value = "txtSmtpPassword", required = false) String txtSmtpPassword,
            @RequestParam(value = "blSmtpAuth", required = false) String blSmtpAuth,
            @RequestParam(value = "blSmtpSslEnable", required = false) String blSmtpSslEnable,
            @RequestParam(value = "blSmtpStarttlsEnable", required = false) String blSmtpStarttlsEnable,
            @RequestParam(value = "blIsActive", required = false) String blIsActive,
            @RequestParam(value = "clearLogo", required = false) String clearLogo,
            @RequestParam(value = "clearHero", required = false) String clearHero,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam(value = "hero", required = false) MultipartFile hero) {
        try {
            CfgTblApplicationSetting saved = applicationSettingService.update(
                    serApplicationSettingId,
                    input(txtApplicationName, txtAddress, txtEmail, txtSmtpHost, txtSmtpPort, txtSmtpUsername,
                            txtSmtpPassword, blSmtpAuth, blSmtpSslEnable, blSmtpStarttlsEnable, blIsActive,
                            truthy(clearLogo), truthy(clearHero)),
                    logo, hero);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("updateApplicationSetting failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Could not update application settings"));
        }
    }

    @RequestMapping(value = "/deleteApplicationSetting", method = RequestMethod.POST)
    public ResponseEntity<?> deleteApplicationSetting(@RequestParam("serApplicationSettingId") int serApplicationSettingId) {
        try {
            applicationSettingService.delete(serApplicationSettingId);
            return ResponseEntity.ok(Collections.singletonMap("status", "Success"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("deleteApplicationSetting failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Could not delete application settings"));
        }
    }

    @RequestMapping(value = "/getPublicApplicationBranding", method = RequestMethod.GET)
    public java.util.Map<String, Object> getPublicApplicationBranding() {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        CfgTblApplicationSetting row = applicationSettingService.getCurrent();
        if (row == null) {
            body.put("applicationName", "");
            body.put("hasLogo", false);
            return body;
        }
        body.put("applicationName", row.getTxtApplicationName() == null ? "" : row.getTxtApplicationName());
        body.put("hasLogo", ApplicationSettingImageStorageUtil.resolve(row.getTxtLogoPath()) != null);
        return body;
    }

    @RequestMapping(value = "/getPublicApplicationLogo", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getPublicApplicationLogo() {
        try {
            CfgTblApplicationSetting row = applicationSettingService.getCurrent();
            if (row == null) {
                return ResponseEntity.notFound().build();
            }
            File file = ApplicationSettingImageStorageUtil.resolve(row.getTxtLogoPath());
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ApplicationSettingImageStorageUtil.contentTypeFor(file))
                    .body(bytes);
        } catch (Exception ex) {
            logger.error("getPublicApplicationLogo failed: " + ex.getMessage(), ex);
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/getApplicationSettingImage", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getApplicationSettingImage(
            @RequestParam("serApplicationSettingId") int serApplicationSettingId,
            @RequestParam("kind") String kind) {
        try {
            CfgTblApplicationSetting row = applicationSettingService.getById(serApplicationSettingId);
            String path = "hero".equalsIgnoreCase(kind) ? row.getTxtDashboardHeroImagePath() : row.getTxtLogoPath();
            File file = ApplicationSettingImageStorageUtil.resolve(path);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, ApplicationSettingImageStorageUtil.contentTypeFor(file))
                    .body(bytes);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("getApplicationSettingImage failed: " + ex.getMessage(), ex);
            return ResponseEntity.status(500).build();
        }
    }

    private ApplicationSettingService.SettingInput input(String name, String address, String email, String host,
                                                         String port, String username, String password,
                                                         String auth, String ssl, String starttls, String active,
                                                         boolean clearLogo, boolean clearHero) {
        ApplicationSettingService.SettingInput input = new ApplicationSettingService.SettingInput();
        input.applicationName = name;
        input.address = address;
        input.email = email;
        input.smtpHost = host;
        input.smtpPort = port;
        input.smtpUsername = username;
        input.smtpPassword = password;
        input.smtpAuth = flag(auth);
        input.smtpSsl = flag(ssl);
        input.smtpStarttls = flag(starttls);
        input.active = flag(active);
        input.clearLogo = clearLogo;
        input.clearHero = clearHero;
        return input;
    }

    private Boolean flag(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return truthy(value);
    }

    private boolean truthy(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
