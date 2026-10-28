package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.dal.entities.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.Optional;

@Service
public class SapHcmOrgStructureSyncService {

    private static final Logger logger = LogManager.getLogger(SapHcmOrgStructureSyncService.class);

    @Autowired
    private HrTblOrganizationRepository organizationRepository;

    @Autowired
    private HrTblSubareaRepository subareaRepository;

    @Autowired
    private HrTblDivisionRepository divisionRepository;

    @Autowired
    private HrTblDepartmentJpaRepository departmentJpaRepository;

    /**
     * Upsert organization → subarea → division → department from one SAP employee row.
     */
    public void upsertFromSapFields(String companyName, String subareaName, String divisionName, String departmentName) {
        String orgName = normalize(companyName);
        String subName = normalize(subareaName);
        String divName = normalize(divisionName);
        String deptName = normalize(departmentName);

        if (orgName == null) {
            return;
        }

        try {
            HrTblOrganization org = upsertOrganization(orgName);
            if (org == null || org.getSerOrganizationId() == null) {
                return;
            }

            if (subName == null) {
                return;
            }
            HrTblSubarea subarea = upsertSubarea(org, subName);
            if (subarea == null || subarea.getSerSubareaId() == null) {
                return;
            }

            if (divName == null) {
                return;
            }
            HrTblDivision division = upsertDivision(subarea, divName);
            if (division == null || division.getSerDivisionId() == null) {
                return;
            }

            if (deptName != null) {
                upsertDepartment(division, deptName);
            }
        } catch (Exception ex) {
            logger.warn("Org structure upsert skipped for row: " + ex.getMessage());
        }
    }

    private HrTblOrganization upsertOrganization(String name) {
        Timestamp now = now();
        Optional<HrTblOrganization> existing = organizationRepository.findActiveByNameIgnoreCase(name);
        if (existing.isPresent()) {
            HrTblOrganization org = existing.get();
            boolean changed = false;
            if (!name.equals(org.getTxtOrganizationName())) {
                org.setTxtOrganizationName(name);
                changed = true;
            }
            if (org.getTxtOrganizationCode() == null || org.getTxtOrganizationCode().isEmpty()) {
                org.setTxtOrganizationCode(toCode(name));
                changed = true;
            }
            if (changed) {
                org.setDteModifiedDate(now);
                return organizationRepository.save(org);
            }
            return org;
        }

        HrTblOrganization org = new HrTblOrganization();
        org.setTxtOrganizationName(name);
        org.setTxtOrganizationCode(toCode(name));
        org.setBlnStatus(true);
        org.setBlIsDeleted(false);
        org.setDteCreatedDate(now);
        org.setDteModifiedDate(now);
        return organizationRepository.save(org);
    }

    private HrTblSubarea upsertSubarea(HrTblOrganization org, String name) {
        Timestamp now = now();
        Integer orgId = org.getSerOrganizationId();
        Optional<HrTblSubarea> existing = subareaRepository.findActiveByOrganizationAndNameIgnoreCase(orgId, name);
        if (existing.isPresent()) {
            HrTblSubarea sub = existing.get();
            boolean changed = false;
            if (!orgId.equals(sub.getSerOrganizationId())) {
                sub.setSerOrganizationId(orgId);
                changed = true;
            }
            if (!name.equals(sub.getTxtSubareaName())) {
                sub.setTxtSubareaName(name);
                changed = true;
            }
            if (sub.getTxtSubareaCode() == null || sub.getTxtSubareaCode().isEmpty()) {
                sub.setTxtSubareaCode(toCode(name));
                changed = true;
            }
            if (changed) {
                sub.setDteModifiedDate(now);
                return subareaRepository.save(sub);
            }
            return sub;
        }

        HrTblSubarea sub = new HrTblSubarea();
        sub.setSerOrganizationId(orgId);
        sub.setTxtSubareaName(name);
        sub.setTxtSubareaCode(toCode(name));
        sub.setBlnStatus(true);
        sub.setBlIsDeleted(false);
        sub.setDteCreatedDate(now);
        sub.setDteModifiedDate(now);
        return subareaRepository.save(sub);
    }

    private HrTblDivision upsertDivision(HrTblSubarea subarea, String name) {
        Timestamp now = now();
        Integer subareaId = subarea.getSerSubareaId();
        Optional<HrTblDivision> existing = divisionRepository.findActiveBySubareaAndNameIgnoreCase(subareaId, name);
        if (existing.isPresent()) {
            HrTblDivision div = existing.get();
            boolean changed = false;
            if (!subareaId.equals(div.getSerSubareaId())) {
                div.setSerSubareaId(subareaId);
                changed = true;
            }
            if (!name.equals(div.getTxtDivisionName())) {
                div.setTxtDivisionName(name);
                changed = true;
            }
            if (div.getTxtDivisionCode() == null || div.getTxtDivisionCode().isEmpty()) {
                div.setTxtDivisionCode(toCode(name));
                changed = true;
            }
            if (changed) {
                div.setDteModifiedDate(now);
                return divisionRepository.save(div);
            }
            return div;
        }

        HrTblDivision div = new HrTblDivision();
        div.setSerSubareaId(subareaId);
        div.setTxtDivisionName(name);
        div.setTxtDivisionCode(toCode(name));
        div.setBlnStatus(true);
        div.setBlIsDeleted(false);
        div.setDteCreatedDate(now);
        div.setDteModifiedDate(now);
        return divisionRepository.save(div);
    }

    private void upsertDepartment(HrTblDivision division, String name) {
        Timestamp now = now();
        Integer divisionId = division.getSerDivisionId();
        Optional<HrTblDepartment> existing = departmentJpaRepository.findActiveByDivisionAndNameIgnoreCase(divisionId, name);
        if (existing.isPresent()) {
            HrTblDepartment dept = existing.get();
            boolean changed = false;
            if (!divisionId.equals(dept.getSerDivisionId())) {
                dept.setSerDivisionId(divisionId);
                changed = true;
            }
            if (!name.equals(dept.getTxtDepartmentName())) {
                dept.setTxtDepartmentName(name);
                changed = true;
            }
            if (changed) {
                dept.setDteModifiedDate(now);
                departmentJpaRepository.save(dept);
            }
            return;
        }

        HrTblDepartment dept = new HrTblDepartment();
        dept.setSerDivisionId(divisionId);
        dept.setTxtDepartmentName(name);
        dept.setTxtDepartmentCode(toCode(name) + "-" + divisionId);
        dept.setBlnStatus(true);
        dept.setBlIsActive(true);
        dept.setBlIsDeleted(false);
        dept.setDteCreatedDate(now);
        dept.setDteModifiedDate(now);
        departmentJpaRepository.save(dept);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String toCode(String name) {
        String base = name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        return base.isEmpty() ? "SAP" : base;
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
