package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.dal.entities.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

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
        String code = toCode(name);
        List<HrTblOrganization> matches = organizationRepository.findActiveByNameOrCode(name, code);
        HrTblOrganization org = pickSapMatch(
                matches,
                name,
                code,
                HrTblOrganization::getTxtOrganizationName,
                HrTblOrganization::getTxtOrganizationCode,
                HrTblOrganization::getSerOrganizationId);
        if (org != null) {
            boolean changed = applySapName(
                    matches,
                    org.getSerOrganizationId(),
                    org.getTxtOrganizationName(),
                    name,
                    HrTblOrganization::getTxtOrganizationName,
                    HrTblOrganization::getSerOrganizationId,
                    org::setTxtOrganizationName);
            if (isBlank(org.getTxtOrganizationCode())) {
                org.setTxtOrganizationCode(code);
                changed = true;
            }
            if (changed) {
                org.setDteModifiedDate(now);
                return organizationRepository.save(org);
            }
            return org;
        }

        org = new HrTblOrganization();
        org.setTxtOrganizationName(name);
        org.setTxtOrganizationCode(code);
        org.setBlnStatus(true);
        org.setBlIsDeleted(false);
        org.setDteCreatedDate(now);
        org.setDteModifiedDate(now);
        return organizationRepository.save(org);
    }

    private HrTblSubarea upsertSubarea(HrTblOrganization org, String name) {
        Timestamp now = now();
        Integer orgId = org.getSerOrganizationId();
        String code = toCode(name);
        List<HrTblSubarea> matches = subareaRepository.findActiveByOrganizationAndNameOrCode(orgId, name, code);
        HrTblSubarea sub = pickSapMatch(
                matches,
                name,
                code,
                HrTblSubarea::getTxtSubareaName,
                HrTblSubarea::getTxtSubareaCode,
                HrTblSubarea::getSerSubareaId);
        if (sub != null) {
            boolean changed = false;
            if (!orgId.equals(sub.getSerOrganizationId())) {
                sub.setSerOrganizationId(orgId);
                changed = true;
            }
            changed = applySapName(
                    matches,
                    sub.getSerSubareaId(),
                    sub.getTxtSubareaName(),
                    name,
                    HrTblSubarea::getTxtSubareaName,
                    HrTblSubarea::getSerSubareaId,
                    sub::setTxtSubareaName) || changed;
            if (isBlank(sub.getTxtSubareaCode())) {
                sub.setTxtSubareaCode(code);
                changed = true;
            }
            if (changed) {
                sub.setDteModifiedDate(now);
                return subareaRepository.save(sub);
            }
            return sub;
        }

        sub = new HrTblSubarea();
        sub.setSerOrganizationId(orgId);
        sub.setTxtSubareaName(name);
        sub.setTxtSubareaCode(code);
        sub.setBlnStatus(true);
        sub.setBlIsDeleted(false);
        sub.setDteCreatedDate(now);
        sub.setDteModifiedDate(now);
        return subareaRepository.save(sub);
    }

    private HrTblDivision upsertDivision(HrTblSubarea subarea, String name) {
        Timestamp now = now();
        Integer subareaId = subarea.getSerSubareaId();
        String code = toCode(name);
        List<HrTblDivision> matches = divisionRepository.findActiveBySubareaAndNameOrCode(subareaId, name, code);
        HrTblDivision div = pickSapMatch(
                matches,
                name,
                code,
                HrTblDivision::getTxtDivisionName,
                HrTblDivision::getTxtDivisionCode,
                HrTblDivision::getSerDivisionId);
        if (div != null) {
            boolean changed = false;
            if (!subareaId.equals(div.getSerSubareaId())) {
                div.setSerSubareaId(subareaId);
                changed = true;
            }
            changed = applySapName(
                    matches,
                    div.getSerDivisionId(),
                    div.getTxtDivisionName(),
                    name,
                    HrTblDivision::getTxtDivisionName,
                    HrTblDivision::getSerDivisionId,
                    div::setTxtDivisionName) || changed;
            if (isBlank(div.getTxtDivisionCode())) {
                div.setTxtDivisionCode(code);
                changed = true;
            }
            if (changed) {
                div.setDteModifiedDate(now);
                return divisionRepository.save(div);
            }
            return div;
        }

        div = new HrTblDivision();
        div.setSerSubareaId(subareaId);
        div.setTxtDivisionName(name);
        div.setTxtDivisionCode(code);
        div.setBlnStatus(true);
        div.setBlIsDeleted(false);
        div.setDteCreatedDate(now);
        div.setDteModifiedDate(now);
        return divisionRepository.save(div);
    }

    private void upsertDepartment(HrTblDivision division, String name) {
        Timestamp now = now();
        Integer divisionId = division.getSerDivisionId();
        String code = toCode(name) + "-" + divisionId;
        List<HrTblDepartment> matches = departmentJpaRepository.findActiveByDivisionAndNameOrCode(divisionId, name, code);
        HrTblDepartment dept = pickSapMatch(
                matches,
                name,
                code,
                HrTblDepartment::getTxtDepartmentName,
                HrTblDepartment::getTxtDepartmentCode,
                HrTblDepartment::getSerDepartmentId);
        if (dept != null) {
            boolean changed = false;
            if (!divisionId.equals(dept.getSerDivisionId())) {
                dept.setSerDivisionId(divisionId);
                changed = true;
            }
            changed = applySapName(
                    matches,
                    dept.getSerDepartmentId(),
                    dept.getTxtDepartmentName(),
                    name,
                    HrTblDepartment::getTxtDepartmentName,
                    HrTblDepartment::getSerDepartmentId,
                    dept::setTxtDepartmentName) || changed;
            if (isBlank(dept.getTxtDepartmentCode())) {
                dept.setTxtDepartmentCode(code);
                changed = true;
            }
            if (changed) {
                dept.setDteModifiedDate(now);
                departmentJpaRepository.save(dept);
            }
            return;
        }

        dept = new HrTblDepartment();
        dept.setSerDivisionId(divisionId);
        dept.setTxtDepartmentName(name);
        dept.setTxtDepartmentCode(code);
        dept.setBlnStatus(true);
        dept.setBlIsActive(true);
        dept.setBlIsDeleted(false);
        dept.setDteCreatedDate(now);
        dept.setDteModifiedDate(now);
        departmentJpaRepository.save(dept);
    }

    /**
     * Keep the oldest row that still carries the SAP code. Renaming the display name
     * does not change that code, so the next sync updates the same row.
     */
    private static <T> T pickSapMatch(List<T> rows,
                                      String name,
                                      String code,
                                      Function<T, String> nameFn,
                                      Function<T, String> codeFn,
                                      Function<T, Integer> idFn) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        T byCode = null;
        T byName = null;
        for (T row : rows) {
            if (sameText(codeFn.apply(row), code)) {
                byCode = older(byCode, row, idFn);
            }
            if (sameText(nameFn.apply(row), name)) {
                byName = older(byName, row, idFn);
            }
        }
        return byCode != null ? byCode : byName;
    }

    private static <T> T older(T current, T candidate, Function<T, Integer> idFn) {
        if (current == null) {
            return candidate;
        }
        Integer currentId = idFn.apply(current);
        Integer candidateId = idFn.apply(candidate);
        if (currentId == null) {
            return candidate;
        }
        if (candidateId == null) {
            return current;
        }
        return candidateId < currentId ? candidate : current;
    }

    private static <T> boolean applySapName(List<T> rows,
                                            Integer selfId,
                                            String current,
                                            String sapName,
                                            Function<T, String> nameFn,
                                            Function<T, Integer> idFn,
                                            Consumer<String> setter) {
        if (sapName.equals(current)) {
            return false;
        }
        if (rows != null) {
            for (T row : rows) {
                if (!sameText(nameFn.apply(row), sapName)) {
                    continue;
                }
                Integer id = idFn.apply(row);
                if (id != null && !id.equals(selfId)) {
                    return false;
                }
            }
        }
        setter.accept(sapName);
        return true;
    }

    private static boolean sameText(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
