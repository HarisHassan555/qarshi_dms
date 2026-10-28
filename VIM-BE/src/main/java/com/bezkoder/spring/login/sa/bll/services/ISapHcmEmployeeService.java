package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.SapHcmEmployeeDto;
import com.bezkoder.spring.login.sa.bll.dto.SapHcmXmlSyncResult;

import java.io.InputStream;
import java.util.List;

public interface ISapHcmEmployeeService {

    List<SapHcmEmployeeDto> getAllEmployees();

    SapHcmXmlSyncResult importFromXml(InputStream xmlStream);

    SapHcmXmlSyncResult importFromSap();
}
