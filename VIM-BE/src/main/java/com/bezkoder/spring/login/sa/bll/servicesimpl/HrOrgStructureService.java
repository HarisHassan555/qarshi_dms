package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.utility.common.OrganizationLogoStorageUtil;
import com.bezkoder.spring.login.sa.bll.dto.HrDivisionDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationBrandingUpdateDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationDto;
import com.bezkoder.spring.login.sa.bll.dto.HrSubareaDto;
import com.bezkoder.spring.login.sa.bll.services.IHrOrgStructureService;
import com.bezkoder.spring.login.sa.dal.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HrOrgStructureService implements IHrOrgStructureService {

    @Autowired
    private HrTblOrganizationRepository organizationRepository;

    @Autowired
    private HrTblSubareaRepository subareaRepository;

    @Autowired
    private HrTblDivisionRepository divisionRepository;

    @Override
    public List<HrOrganizationDto> getAllOrganizations() {
        List<HrOrganizationDto> result = new ArrayList<>();
        for (HrTblOrganization row : organizationRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc()) {
            result.add(toOrganizationDto(row));
        }
        return result;
    }

    @Override
    public HrOrganizationDto updateOrganizationBranding(HrOrganizationBrandingUpdateDto request) {
        if (request == null || request.getSerOrganizationId() == null || request.getSerOrganizationId() <= 0) {
            throw new IllegalArgumentException("Organization id is required");
        }
        Optional<HrTblOrganization> optional = organizationRepository.findById(request.getSerOrganizationId());
        if (!optional.isPresent()) {
            throw new IllegalArgumentException("Organization not found");
        }
        HrTblOrganization row = optional.get();
        if (Boolean.TRUE.equals(row.getBlIsDeleted())) {
            throw new IllegalArgumentException("Organization is deleted");
        }

        if (request.getTxtOrganizationName() != null) {
            String newName = normalizeRequired(request.getTxtOrganizationName(), "Organization name is required");
            if (!newName.equals(row.getTxtOrganizationName())) {
                Optional<HrTblOrganization> duplicate = organizationRepository.findActiveByNameIgnoreCase(newName);
                if (duplicate.isPresent()
                        && !duplicate.get().getSerOrganizationId().equals(row.getSerOrganizationId())) {
                    throw new IllegalArgumentException("Organization name already exists");
                }
                row.setTxtOrganizationName(newName);
            }
        }

        if (request.getTxtAddress() != null) {
            row.setTxtAddress(normalizeNullable(request.getTxtAddress()));
        }

        String logoBase64 = request.getLogoBase64();
        if (logoBase64 != null && !logoBase64.trim().isEmpty()) {
            try {
                String storedPath = OrganizationLogoStorageUtil.saveOrganizationLogo(row.getSerOrganizationId(), logoBase64);
                if (storedPath != null) {
                    row.setLogo(storedPath);
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not save logo image: " + ex.getMessage());
            }
        } else if (request.getLogo() != null) {
            row.setLogo(normalizeNullable(request.getLogo()));
        }

        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        HrTblOrganization saved = organizationRepository.save(row);
        return toOrganizationDto(saved);
    }

    private static HrOrganizationDto toOrganizationDto(HrTblOrganization row) {
        HrOrganizationDto dto = new HrOrganizationDto();
        dto.setSerOrganizationId(row.getSerOrganizationId());
        dto.setLogo(row.getLogo());
        dto.setTxtOrganizationName(row.getTxtOrganizationName());
        dto.setTxtOrganizationCode(row.getTxtOrganizationCode());
        dto.setTxtAddress(row.getTxtAddress());
        dto.setBlnStatus(row.getBlnStatus());
        return dto;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    @Override
    public List<HrSubareaDto> getAllSubareas() {
        Map<Integer, String> orgNames = new HashMap<>();
        for (HrTblOrganization org : organizationRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc()) {
            orgNames.put(org.getSerOrganizationId(), org.getTxtOrganizationName());
        }

        List<HrSubareaDto> result = new ArrayList<>();
        for (HrTblSubarea row : subareaRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtSubareaNameAsc()) {
            HrSubareaDto dto = new HrSubareaDto();
            dto.setSerSubareaId(row.getSerSubareaId());
            dto.setSerOrganizationId(row.getSerOrganizationId());
            dto.setOrganizationName(orgNames.get(row.getSerOrganizationId()));
            dto.setTxtSubareaName(row.getTxtSubareaName());
            dto.setTxtSubareaCode(row.getTxtSubareaCode());
            dto.setBlnStatus(row.getBlnStatus());
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<HrDivisionDto> getAllDivisions() {
        Map<Integer, HrTblSubarea> subareaById = new HashMap<>();
        Map<Integer, String> orgNames = new HashMap<>();
        for (HrTblOrganization org : organizationRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc()) {
            orgNames.put(org.getSerOrganizationId(), org.getTxtOrganizationName());
        }
        for (HrTblSubarea sub : subareaRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtSubareaNameAsc()) {
            subareaById.put(sub.getSerSubareaId(), sub);
        }

        List<HrDivisionDto> result = new ArrayList<>();
        for (HrTblDivision row : divisionRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtDivisionNameAsc()) {
            HrDivisionDto dto = new HrDivisionDto();
            dto.setSerDivisionId(row.getSerDivisionId());
            dto.setSerSubareaId(row.getSerSubareaId());
            dto.setTxtDivisionName(row.getTxtDivisionName());
            dto.setTxtDivisionCode(row.getTxtDivisionCode());
            dto.setBlnStatus(row.getBlnStatus());

            HrTblSubarea sub = subareaById.get(row.getSerSubareaId());
            if (sub != null) {
                dto.setSubareaName(sub.getTxtSubareaName());
                dto.setOrganizationName(orgNames.get(sub.getSerOrganizationId()));
            }
            result.add(dto);
        }
        return result;
    }
}
