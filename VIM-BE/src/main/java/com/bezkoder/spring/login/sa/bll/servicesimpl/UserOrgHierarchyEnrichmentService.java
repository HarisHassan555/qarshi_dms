package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves Organization → Subarea → Division from the user's assigned department (ser_division_id chain).
 */
@Service
public class UserOrgHierarchyEnrichmentService {

    @Autowired
    private HrTblOrganizationRepository organizationRepository;

    @Autowired
    private HrTblSubareaRepository subareaRepository;

    @Autowired
    private HrTblDivisionRepository divisionRepository;

    public void enrichUsers(List<CfgTblUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        Map<Integer, HrTblDivision> divisionById = new HashMap<>();
        for (HrTblDivision d : divisionRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtDivisionNameAsc()) {
            if (d.getSerDivisionId() != null) {
                divisionById.put(d.getSerDivisionId(), d);
            }
        }

        Map<Integer, HrTblSubarea> subareaById = new HashMap<>();
        for (HrTblSubarea s : subareaRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtSubareaNameAsc()) {
            if (s.getSerSubareaId() != null) {
                subareaById.put(s.getSerSubareaId(), s);
            }
        }

        Map<Integer, HrTblOrganization> orgById = new HashMap<>();
        for (HrTblOrganization o : organizationRepository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc()) {
            if (o.getSerOrganizationId() != null) {
                orgById.put(o.getSerOrganizationId(), o);
            }
        }

        for (CfgTblUser user : users) {
            HrTblDepartment department = user.getHrTblDepartment();
            if (department == null) {
                clearOrgFields(user);
                continue;
            }

            if (user.getTxtDepartmentName() == null || user.getTxtDepartmentName().trim().isEmpty()) {
                user.setTxtDepartmentName(department.getTxtDepartmentName());
            }

            Integer divisionId = department.getSerDivisionId();
            if (divisionId == null) {
                user.setTxtDivisionName(null);
                user.setTxtSubareaName(null);
                user.setTxtOrganizationName(null);
                continue;
            }

            HrTblDivision division = divisionById.get(divisionId);
            if (division == null) {
                clearOrgFields(user);
                continue;
            }
            user.setTxtDivisionName(division.getTxtDivisionName());
            user.setSerDivisionId(divisionId);

            HrTblSubarea subarea = subareaById.get(division.getSerSubareaId());
            if (subarea == null) {
                user.setTxtSubareaName(null);
                user.setTxtOrganizationName(null);
                user.setSerSubareaId(null);
                user.setSerOrganizationId(null);
                continue;
            }
            user.setTxtSubareaName(subarea.getTxtSubareaName());
            user.setSerSubareaId(subarea.getSerSubareaId());

            HrTblOrganization org = orgById.get(subarea.getSerOrganizationId());
            if (org != null) {
                user.setTxtOrganizationName(org.getTxtOrganizationName());
                user.setSerOrganizationId(org.getSerOrganizationId());
            } else {
                user.setTxtOrganizationName(null);
                user.setSerOrganizationId(null);
            }
        }
    }

    private void clearOrgFields(CfgTblUser user) {
        user.setTxtOrganizationName(null);
        user.setTxtSubareaName(null);
        user.setTxtDivisionName(null);
        user.setSerOrganizationId(null);
        user.setSerSubareaId(null);
        user.setSerDivisionId(null);
    }
}
