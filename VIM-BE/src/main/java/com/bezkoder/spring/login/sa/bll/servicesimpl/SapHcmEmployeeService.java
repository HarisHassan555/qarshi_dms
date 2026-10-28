package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.servicesimpl.SapHcmSoapClient;
import com.bezkoder.spring.login.sa.bll.dto.SapHcmEmployeeDto;
import com.bezkoder.spring.login.sa.bll.dto.SapHcmXmlSyncResult;
import com.bezkoder.spring.login.sa.bll.services.ISapHcmEmployeeService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSapHcmEmployee;
import com.bezkoder.spring.login.sa.dal.entities.SapHcmEmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SapHcmEmployeeService implements ISapHcmEmployeeService {

    private static final Logger logger = LogManager.getLogger(SapHcmEmployeeService.class);

    private static final String TAG_ITEM = "item";
    private static final String TAG_PICTURE = "PICTURE_BASE64";

    @Autowired
    private SapHcmEmployeeRepository employeeRepository;

    @Autowired
    private SapHcmSoapClient sapHcmSoapClient;

    @Autowired
    private SapHcmOrgStructureSyncService sapHcmOrgStructureSyncService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<SapHcmEmployeeDto> getAllEmployees() {
        List<SapHcmEmployeeDto> result = new ArrayList<>();
        for (CfgTblSapHcmEmployee row : employeeRepository.findByBlIsDeletedFalseOrderByTxtEmpNameAsc()) {
            result.add(toDto(row));
        }
        return result;
    }

    @Override
    @Transactional
    public SapHcmXmlSyncResult importFromSap() {
        SapHcmXmlSyncResult result = new SapHcmXmlSyncResult();
        try {
            if (!sapHcmSoapClient.isConfigured()) {
                result.setStatus("Failure");
                result.setMessage("SAP HCM SOAP endpoint is not configured in application.properties");
                return result;
            }
            try (InputStream xml = sapHcmSoapClient.fetchEmployeeSoapXml()) {
                return importFromXml(xml);
            }
        } catch (Exception ex) {
            logger.error("SAP HCM live sync failed: " + ex.getMessage(), ex);
            result.setStatus("Failure");
            result.setMessage(ex.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public SapHcmXmlSyncResult importFromXml(InputStream xmlStream) {
        SapHcmXmlSyncResult result = new SapHcmXmlSyncResult();
        SyncCounters counters = new SyncCounters();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(xmlStream, new SapHcmItemHandler(counters));
            result.setStatus("Success");
            result.setMessage("SAP HCM XML import completed");
        } catch (Exception ex) {
            logger.error("SAP HCM XML import failed: " + ex.getMessage(), ex);
            result.setStatus("Failure");
            result.setMessage(ex.getMessage());
        }

        result.setTotalItems(counters.totalItems);
        result.setInserted(counters.inserted);
        result.setUpdated(counters.updated);
        result.setPicturesSaved(counters.picturesSaved);
        result.setSkipped(counters.skipped);
        return result;
    }

    private SapHcmEmployeeDto toDto(CfgTblSapHcmEmployee row) {
        SapHcmEmployeeDto dto = new SapHcmEmployeeDto();
        dto.setEmployee(row.getTxtEmployeeId());
        dto.setEmpName(row.getTxtEmpName());
        dto.setDepartmentName(row.getTxtDepartmentName());
        dto.setDivision(row.getTxtDivision());
        dto.setDesignation(row.getTxtDesignation());
        dto.setEmail(row.getTxtEmail());
        dto.setPayScale(row.getTxtPayScale());
        dto.setSubarea(row.getTxtSubarea());
        dto.setCompanyName(row.getTxtCompanyName());
        dto.setHiringDate(row.getTxtHiringDate());
        dto.setConfirmationDate(row.getTxtConfirmationDate());
        dto.setRetirementDate(row.getTxtRetirementDate());
        dto.setSeparationDate(row.getTxtSeparationDate());
        dto.setRehiringDate(row.getTxtRehiringDate());
        dto.setAnnualIncType(row.getTxtAnnualIncType());
        dto.setActive(row.getTxtActive());
        dto.setReportTo(row.getTxtReportTo());
        dto.setReportToName(row.getTxtReportToName());
        boolean hasPicture = Boolean.TRUE.equals(row.getBlHasPicture())
                || (row.getTxtPicturePath() != null && !row.getTxtPicturePath().trim().isEmpty());
        dto.setHasPicture(hasPicture);
        if (hasPicture && row.getTxtEmployeeId() != null) {
            dto.setPictureUrl("getSapHcmEmployeePhoto?employeeId=" + row.getTxtEmployeeId());
        }
        return dto;
    }

    private void persistItem(Map<String, String> fields, SyncCounters counters) {
        String employeeId = trim(fields.get("EMPLOYEE"));
        if (employeeId == null || employeeId.isEmpty()) {
            counters.skipped++;
            return;
        }

        counters.totalItems++;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Optional<CfgTblSapHcmEmployee> existingOpt = employeeRepository
                .findByTxtEmployeeIdAndBlIsDeletedFalse(employeeId);
        CfgTblSapHcmEmployee entity = existingOpt.orElseGet(CfgTblSapHcmEmployee::new);
        boolean isNew = entity.getSerSapHcmEmployeeId() == null;

        boolean hasPictureInDb = !isNew && (Boolean.TRUE.equals(entity.getBlHasPicture())
                || (entity.getTxtPicturePath() != null && !entity.getTxtPicturePath().trim().isEmpty()));

        Map<String, String> jsonMap = new LinkedHashMap<>(fields);
        jsonMap.remove(TAG_PICTURE);
        jsonMap.put("hasPicture", String.valueOf(hasPictureInDb));
        String recordJson;
        try {
            recordJson = objectMapper.writeValueAsString(jsonMap);
        } catch (Exception ex) {
            recordJson = "{}";
        }

        entity.setTxtEmployeeId(employeeId);
        entity.setTxtEmpName(trim(fields.get("EMP_NAME")));
        String companyName = trim(fields.get("COMPANY_NAME"));
        String subarea = trim(fields.get("SUBAREA"));
        String division = trim(fields.get("DIVISION"));
        String departmentName = trim(fields.get("DEPARTMENT_NAME"));

        sapHcmOrgStructureSyncService.upsertFromSapFields(companyName, subarea, division, departmentName);

        entity.setTxtDepartmentName(departmentName);
        entity.setTxtDivision(division);
        entity.setTxtDesignation(trim(fields.get("DESIGNATION")));
        entity.setTxtEmail(trim(fields.get("EMAIL")));
        entity.setTxtPayScale(trim(fields.get("PAY_SCALE")));
        entity.setTxtSubarea(subarea);
        entity.setTxtCompanyName(companyName);
        entity.setTxtHiringDate(trim(fields.get("HIRING_DATE")));
        entity.setTxtConfirmationDate(trim(fields.get("CONFIRMATION_DATE")));
        entity.setTxtRetirementDate(trim(fields.get("RETIREMENT_DATE")));
        entity.setTxtSeparationDate(trim(fields.get("SEPARATION_DATE")));
        entity.setTxtRehiringDate(trim(fields.get("REHIRING_DATE")));
        entity.setTxtAnnualIncType(trim(fields.get("ANNUAL_INC_TYPE")));
        entity.setTxtActive(trim(fields.get("ACTIVE")));
        entity.setTxtReportTo(trim(fields.get("REPORT_TO")));
        entity.setTxtReportToName(trim(fields.get("REPORT_TO_NAME")));
        // Photos are not created or updated during SAP fetch (existing files/flags are preserved on update).
        if (isNew) {
            entity.setBlHasPicture(false);
            entity.setTxtPicturePath(null);
        }
        entity.setTxtRecordJson(recordJson);
        entity.setDteLastSyncedDate(now);
        entity.setBlIsDeleted(false);
        if (isNew) {
            entity.setDteCreatedDate(now);
            counters.inserted++;
        } else {
            counters.updated++;
        }
        entity.setDteModifiedDate(now);
        employeeRepository.save(entity);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private final class SapHcmItemHandler extends DefaultHandler {

        private final SyncCounters counters;
        private boolean inItem;
        private String currentTag;
        private final Map<String, String> currentFields = new LinkedHashMap<>();
        private final StringBuilder currentText = new StringBuilder();
        private boolean skipPictureContent;

        private SapHcmItemHandler(SyncCounters counters) {
            this.counters = counters;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String tag = localName != null && !localName.isEmpty() ? localName : qName;
            if (TAG_ITEM.equalsIgnoreCase(tag)) {
                inItem = true;
                currentFields.clear();
                skipPictureContent = false;
                return;
            }
            if (!inItem) {
                return;
            }
            currentTag = tag;
            currentText.setLength(0);
            if (TAG_PICTURE.equalsIgnoreCase(tag)) {
                skipPictureContent = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (!inItem || currentTag == null || skipPictureContent) {
                return;
            }
            currentText.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String tag = localName != null && !localName.isEmpty() ? localName : qName;
            if (TAG_ITEM.equalsIgnoreCase(tag)) {
                inItem = false;
                persistItem(currentFields, counters);
                currentFields.clear();
                skipPictureContent = false;
                currentTag = null;
                return;
            }
            if (!inItem || currentTag == null) {
                return;
            }
            if (TAG_PICTURE.equalsIgnoreCase(tag)) {
                skipPictureContent = false;
            } else if (!TAG_PICTURE.equalsIgnoreCase(currentTag)) {
                currentFields.put(currentTag, currentText.toString().trim());
            }
            currentTag = null;
            currentText.setLength(0);
        }
    }

    private static final class SyncCounters {
        int totalItems;
        int inserted;
        int updated;
        int picturesSaved;
        int skipped;
    }
}
