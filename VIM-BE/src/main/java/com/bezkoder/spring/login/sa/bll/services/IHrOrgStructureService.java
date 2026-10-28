package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.HrDivisionDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationBrandingUpdateDto;
import com.bezkoder.spring.login.sa.bll.dto.HrOrganizationDto;
import com.bezkoder.spring.login.sa.bll.dto.HrSubareaDto;

import java.util.List;

public interface IHrOrgStructureService {

    List<HrOrganizationDto> getAllOrganizations();

    HrOrganizationDto updateOrganizationBranding(HrOrganizationBrandingUpdateDto request);

    List<HrSubareaDto> getAllSubareas();

    List<HrDivisionDto> getAllDivisions();
}
