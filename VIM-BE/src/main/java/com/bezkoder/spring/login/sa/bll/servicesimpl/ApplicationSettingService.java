package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.utility.common.ApplicationSettingImageStorageUtil;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblApplicationSetting;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblApplicationSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

@Service
public class ApplicationSettingService {

    @Autowired
    private CfgTblApplicationSettingRepository repository;

    public List<CfgTblApplicationSetting> getAll() {
        return repository.findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderBySerApplicationSettingIdDesc();
    }

    public CfgTblApplicationSetting getCurrent() {
        return repository.findFirstByBlIsDeletedFalseOrBlIsDeletedIsNullOrderBySerApplicationSettingIdDesc()
                .orElse(null);
    }

    public CfgTblApplicationSetting getById(int id) {
        CfgTblApplicationSetting row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application setting was not found"));
        if (Boolean.TRUE.equals(row.getBlIsDeleted())) {
            throw new IllegalArgumentException("Application setting was not found");
        }
        return row;
    }

    @Transactional
    public CfgTblApplicationSetting create(SettingInput input, MultipartFile logo, MultipartFile hero) {
        CfgTblApplicationSetting row = new CfgTblApplicationSetting();
        applyText(row, input);
        row.setBlIsDeleted(false);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        row.setSerCreatedUser(1);
        row = repository.save(row);
        applyImages(row, logo, hero, false, false);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        return repository.save(row);
    }

    @Transactional
    public CfgTblApplicationSetting update(int id, SettingInput input, MultipartFile logo, MultipartFile hero) {
        CfgTblApplicationSetting row = getById(id);
        applyText(row, input);
        applyImages(row, logo, hero, input.clearLogo, input.clearHero);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        row.setSerModifiedUser(1);
        return repository.save(row);
    }

    @Transactional
    public void delete(int id) {
        CfgTblApplicationSetting row = getById(id);
        ApplicationSettingImageStorageUtil.deleteQuietly(row.getTxtLogoPath());
        ApplicationSettingImageStorageUtil.deleteQuietly(row.getTxtDashboardHeroImagePath());
        row.setTxtLogoPath(null);
        row.setTxtDashboardHeroImagePath(null);
        row.setBlIsDeleted(true);
        row.setBlIsActive(false);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        row.setSerModifiedUser(1);
        repository.save(row);
    }

    private void applyText(CfgTblApplicationSetting row, SettingInput input) {
        row.setTxtApplicationName(requiredText(input.applicationName, "Application name is required", 200));
        row.setTxtAddress(optionalText(input.address, "Address", 500));
        row.setTxtEmail(optionalEmail(input.email));
        row.setTxtSmtpHost(optionalText(input.smtpHost, "SMTP host", 200));
        row.setTxtSmtpPort(parsePort(input.smtpPort));
        row.setTxtSmtpUsername(optionalText(input.smtpUsername, "SMTP username", 200));
        row.setTxtSmtpPassword(optionalText(input.smtpPassword, "SMTP password", 255));
        row.setBlSmtpAuth(input.smtpAuth);
        row.setBlSmtpSslEnable(input.smtpSsl);
        row.setBlSmtpStarttlsEnable(input.smtpStarttls);
        row.setBlIsActive(input.active == null ? Boolean.TRUE : input.active);
    }

    private void applyImages(CfgTblApplicationSetting row, MultipartFile logo, MultipartFile hero,
                             boolean clearLogo, boolean clearHero) {
        int id = row.getSerApplicationSettingId();
        if (hasFile(logo)) {
            String previous = row.getTxtLogoPath();
            String stored = saveImage(id, "logo", logo);
            row.setTxtLogoPath(stored);
            if (previous != null && !previous.equals(stored)) {
                ApplicationSettingImageStorageUtil.deleteQuietly(previous);
            }
        } else if (clearLogo) {
            ApplicationSettingImageStorageUtil.deleteQuietly(row.getTxtLogoPath());
            row.setTxtLogoPath(null);
        }

        if (hasFile(hero)) {
            String previous = row.getTxtDashboardHeroImagePath();
            String stored = saveImage(id, "hero", hero);
            row.setTxtDashboardHeroImagePath(stored);
            if (previous != null && !previous.equals(stored)) {
                ApplicationSettingImageStorageUtil.deleteQuietly(previous);
            }
        } else if (clearHero) {
            ApplicationSettingImageStorageUtil.deleteQuietly(row.getTxtDashboardHeroImagePath());
            row.setTxtDashboardHeroImagePath(null);
        }
    }

    private String saveImage(int id, String kind, MultipartFile file) {
        try {
            return ApplicationSettingImageStorageUtil.save(id, kind, file);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not store the image file");
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String requiredText(String value, String message, int max) {
        String text = optionalText(value, "Application name", max);
        if (text == null) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String optionalText(String value, String label, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:") || lower.contains("base64,")) {
            throw new IllegalArgumentException(label + " cannot contain an embedded image");
        }
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(label + " is too long");
        }
        return trimmed;
    }

    private String optionalEmail(String value) {
        String email = optionalText(value, "Email", 200);
        if (email != null && !email.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return email;
    }

    private Integer parsePort(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("SMTP port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("SMTP port must be a number");
        }
    }

    public static final class SettingInput {
        public String applicationName;
        public String address;
        public String email;
        public String smtpHost;
        public String smtpPort;
        public String smtpUsername;
        public String smtpPassword;
        public Boolean smtpAuth;
        public Boolean smtpSsl;
        public Boolean smtpStarttls;
        public Boolean active;
        public boolean clearLogo;
        public boolean clearHero;
    }
}
