package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.utility.common.OrganizationLogoStorageUtil;
import com.bezkoder.spring.login.sa.bll.dto.HrDivisionDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationBrandingUpdateDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationDto;
import com.bezkoder.spring.login.sa.bll.dto.HrSubareaDto;
import com.bezkoder.spring.login.sa.bll.services.IHrOrgStructureService;
import com.bezkoder.spring.login.sa.dal.entities.HrTblOrganization;
import com.bezkoder.spring.login.sa.dal.entities.HrTblOrganizationRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
public class HrOrgStructureController {

    private static final Logger logger = LogManager.getLogger(HrOrgStructureController.class);

    @Autowired
    private IHrOrgStructureService hrOrgStructureService;

    @Autowired
    private HrTblOrganizationRepository organizationRepository;

    @RequestMapping(value = "/getAllOrganizations", method = RequestMethod.GET)
    public List<HrOrganizationDto> getAllOrganizations(HttpServletResponse response) {
        try {
            return hrOrgStructureService.getAllOrganizations();
        } catch (Exception ex) {
            logger.error("getAllOrganizations failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/getAllSubareas", method = RequestMethod.GET)
    public List<HrSubareaDto> getAllSubareas(HttpServletResponse response) {
        try {
            return hrOrgStructureService.getAllSubareas();
        } catch (Exception ex) {
            logger.error("getAllSubareas failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/getAllDivisions", method = RequestMethod.GET)
    public List<HrDivisionDto> getAllDivisions(HttpServletResponse response) {
        try {
            return hrOrgStructureService.getAllDivisions();
        } catch (Exception ex) {
            logger.error("getAllDivisions failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return Collections.emptyList();
        }
    }

    @RequestMapping(value = "/updateOrganizationBranding", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public HrOrganizationDto updateOrganizationBranding(@RequestBody HrOrganizationBrandingUpdateDto body, HttpServletResponse response) {
        try {
            return hrOrgStructureService.updateOrganizationBranding(body);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        } catch (Exception ex) {
            logger.error("updateOrganizationBranding failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getOrganizationLogo", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getOrganizationLogo(@RequestParam int serOrganizationId) {
        try {
            if (serOrganizationId <= 0) {
                return ResponseEntity.notFound().build();
            }
            Optional<HrTblOrganization> optional = organizationRepository.findById(serOrganizationId);
            if (!optional.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            HrTblOrganization org = optional.get();
            if (org.getLogo() == null || org.getLogo().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            String logo = org.getLogo().trim();
            if (!logo.startsWith(OrganizationLogoStorageUtil.LOGOS_DIR + "/")) {
                return ResponseEntity.notFound().build();
            }
            File file = OrganizationLogoStorageUtil.resolveLogoFile(logo);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(bytes);
        } catch (Exception ex) {
            logger.error("getOrganizationLogo failed for id=" + serOrganizationId + ": " + ex.getMessage(), ex);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
    }
}
