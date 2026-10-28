package com.bezkoder.spring.login.admin.bll.servicesimpl;

import com.bezkoder.spring.login.admin.utility.common.OrganizationLogoStorageUtil;
import com.bezkoder.spring.login.admin.utility.common.SignatureStorageUtil;
import com.bezkoder.spring.login.admin.utility.common.SapHcmEmployeeStorageUtil;
import com.bezkoder.spring.login.admin.utility.common.UserDelegationAttachmentStorageUtil;
import com.bezkoder.spring.login.admin.utility.common.UserProfilePictureStorageUtil;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationStorageInfoService {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> buildStorageInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Success");

        File uploadRoot = new File(SignatureStorageUtil.uploadRoot());
        long uploadRootBytes = directorySize(uploadRoot);
        List<Map<String, Object>> uploadBreakdown = buildUploadBreakdown(uploadRoot);

        Map<String, Object> fileStorage = new HashMap<>();
        fileStorage.put("sourceLabel", "File upload root");
        fileStorage.put("sourcePath", uploadRoot.getAbsolutePath());
        fileStorage.put("usedBytes", uploadRootBytes);
        fileStorage.put("breakdown", uploadBreakdown);
        result.put("fileStorage", fileStorage);

        Map<String, Object> hostVolume = buildHostVolumeInfo(uploadRoot);
        result.put("hostVolume", hostVolume);

        Map<String, Object> databaseStorage = buildDatabaseStorageInfo();
        result.put("databaseStorage", databaseStorage);

        long applicationContentBytes = toLong(databaseStorage.get("applicationContentBytes"));
        long applicationUsedBytes = uploadRootBytes + applicationContentBytes;
        result.put("applicationUsedBytes", applicationUsedBytes);
        result.put("applicationUsedLabel", "Upload files + application PDF/JSON data in database");

        return result;
    }

    private List<Map<String, Object>> buildUploadBreakdown(File uploadRoot) {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        breakdown.add(folderEntry("Signatures", new File(uploadRoot, "signatures")));
        breakdown.add(folderEntry("Organization logos", new File(uploadRoot, OrganizationLogoStorageUtil.LOGOS_DIR)));
        breakdown.add(folderEntry("User profile pictures", new File(uploadRoot, UserProfilePictureStorageUtil.PROFILES_DIR)));
        breakdown.add(folderEntry("Delegation attachments", new File(uploadRoot, UserDelegationAttachmentStorageUtil.DELEGATIONS_DIR)));
        breakdown.add(folderEntry("SAP HCM employee photos", new File(uploadRoot, SapHcmEmployeeStorageUtil.PHOTOS_DIR)));

        long knownBytes = 0;
        for (Map<String, Object> entry : breakdown) {
            knownBytes += toLong(entry.get("bytes"));
        }
        long otherBytes = Math.max(0, directorySize(uploadRoot) - knownBytes);
        if (otherBytes > 0 || breakdown.isEmpty()) {
            Map<String, Object> other = new HashMap<>();
            other.put("label", "Other upload files");
            other.put("bytes", otherBytes);
            breakdown.add(other);
        }
        return breakdown;
    }

    private Map<String, Object> folderEntry(String label, File folder) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("label", label);
        entry.put("bytes", directorySize(folder));
        entry.put("path", folder.getAbsolutePath());
        return entry;
    }

    private Map<String, Object> buildHostVolumeInfo(File pathFile) {
        Map<String, Object> hostVolume = new HashMap<>();
        long totalBytes = pathFile.getTotalSpace();
        long usableBytes = pathFile.getUsableSpace();
        long usedBytes = totalBytes > 0 && usableBytes >= 0 ? Math.max(0, totalBytes - usableBytes) : 0;
        hostVolume.put("sourceLabel", "Server disk (upload volume)");
        hostVolume.put("mountPath", pathFile.getAbsolutePath());
        hostVolume.put("totalBytes", totalBytes);
        hostVolume.put("usableBytes", usableBytes);
        hostVolume.put("usedBytes", usedBytes);
        if (totalBytes > 0) {
            hostVolume.put("usedPercent", Math.min(100d, (usedBytes * 100d) / totalBytes));
        } else {
            hostVolume.put("usedPercent", 0d);
        }
        return hostVolume;
    }

    private Map<String, Object> buildDatabaseStorageInfo() {
        Map<String, Object> databaseStorage = new HashMap<>();
        String schemaName = querySingleString("SELECT DATABASE()");
        databaseStorage.put("sourceLabel", "MySQL database");
        databaseStorage.put("schemaName", schemaName == null ? "" : schemaName);

        Number databaseTotalBytes = querySingleNumber(
                "SELECT COALESCE(SUM(data_length + index_length), 0) "
                        + "FROM information_schema.tables WHERE table_schema = DATABASE()");
        databaseStorage.put("databaseTotalBytes", databaseTotalBytes == null ? 0L : databaseTotalBytes.longValue());

        Number applicationTableBytes = querySingleNumber(
                "SELECT COALESCE(data_length + index_length, 0) "
                        + "FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'cfg_tbl_custom_form_application'");
        databaseStorage.put("applicationTableBytes", applicationTableBytes == null ? 0L : applicationTableBytes.longValue());

        Object[] contentRow = querySingleRow(
                "SELECT COALESCE(SUM("
                        + "COALESCE(LENGTH(blb_pdf_data), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_0), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_1), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_2), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_3), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_4), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_5), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_6), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_7), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_8), 0) + "
                        + "COALESCE(LENGTH(blb_pdf_stage_9), 0) + "
                        + "COALESCE(LENGTH(txt_application_data), 0) + "
                        + "COALESCE(LENGTH(txt_approval_history), 0) + "
                        + "COALESCE(LENGTH(txt_prior_approvals), 0) + "
                        + "COALESCE(LENGTH(txt_remarks), 0)"
                        + "), 0), "
                        + "COUNT(*), "
                        + "COALESCE(SUM(CASE WHEN blb_pdf_data IS NOT NULL AND LENGTH(blb_pdf_data) > 0 THEN 1 ELSE 0 END), 0) "
                        + "FROM cfg_tbl_custom_form_application "
                        + "WHERE (bl_is_deleted IS NULL OR bl_is_deleted = false)");

        long applicationContentBytes = 0L;
        long applicationCount = 0L;
        long applicationPdfCount = 0L;
        if (contentRow != null && contentRow.length >= 3) {
            applicationContentBytes = toLong(contentRow[0]);
            applicationCount = toLong(contentRow[1]);
            applicationPdfCount = toLong(contentRow[2]);
        }
        databaseStorage.put("applicationContentBytes", applicationContentBytes);
        databaseStorage.put("applicationCount", applicationCount);
        databaseStorage.put("applicationPdfCount", applicationPdfCount);

        return databaseStorage;
    }

    private String querySingleString(String sql) {
        try {
            Object value = entityManager.createNativeQuery(sql).getSingleResult();
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private Number querySingleNumber(String sql) {
        try {
            Object value = entityManager.createNativeQuery(sql).getSingleResult();
            return value instanceof Number ? (Number) value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Object[] querySingleRow(String sql) {
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            return rows.get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    private static long directorySize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        long total = 0L;
        for (File child : children) {
            total += directorySize(child);
        }
        return total;
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
