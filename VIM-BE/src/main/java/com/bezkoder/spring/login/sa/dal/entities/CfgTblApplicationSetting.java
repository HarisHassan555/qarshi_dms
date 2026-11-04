package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_application_setting")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblApplicationSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_application_setting_id")
    private Integer serApplicationSettingId;

    @Column(name = "txt_application_name", nullable = false, length = 200)
    private String txtApplicationName;

    @Column(name = "txt_address", length = 500)
    private String txtAddress;

    @Column(name = "txt_email", length = 200)
    private String txtEmail;

    @Column(name = "txt_logo_path", length = 500)
    private String txtLogoPath;

    @Column(name = "txt_dashboard_hero_image_path", length = 500)
    private String txtDashboardHeroImagePath;

    @Column(name = "txt_smtp_host", length = 200)
    private String txtSmtpHost;

    @Column(name = "txt_smtp_port")
    private Integer txtSmtpPort;

    @Column(name = "txt_smtp_username", length = 200)
    private String txtSmtpUsername;

    @Column(name = "txt_smtp_password", length = 255)
    private String txtSmtpPassword;

    @Column(name = "bl_smtp_auth")
    private Boolean blSmtpAuth;

    @Column(name = "bl_smtp_ssl_enable")
    private Boolean blSmtpSslEnable;

    @Column(name = "bl_smtp_starttls_enable")
    private Boolean blSmtpStarttlsEnable;

    @Column(name = "bl_is_active")
    private Boolean blIsActive;

    @Column(name = "bl_is_demo")
    private Boolean blIsDemo;

    @Column(name = "bl_is_deleted")
    private Boolean blIsDeleted;

    @Column(name = "ser_created_user")
    private Integer serCreatedUser;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "ser_modified_user")
    private Integer serModifiedUser;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    public Integer getSerApplicationSettingId() {
        return serApplicationSettingId;
    }

    public void setSerApplicationSettingId(Integer serApplicationSettingId) {
        this.serApplicationSettingId = serApplicationSettingId;
    }

    public String getTxtApplicationName() {
        return txtApplicationName;
    }

    public void setTxtApplicationName(String txtApplicationName) {
        this.txtApplicationName = txtApplicationName;
    }

    public String getTxtAddress() {
        return txtAddress;
    }

    public void setTxtAddress(String txtAddress) {
        this.txtAddress = txtAddress;
    }

    public String getTxtEmail() {
        return txtEmail;
    }

    public void setTxtEmail(String txtEmail) {
        this.txtEmail = txtEmail;
    }

    public String getTxtLogoPath() {
        return txtLogoPath;
    }

    public void setTxtLogoPath(String txtLogoPath) {
        this.txtLogoPath = txtLogoPath;
    }

    public String getTxtDashboardHeroImagePath() {
        return txtDashboardHeroImagePath;
    }

    public void setTxtDashboardHeroImagePath(String txtDashboardHeroImagePath) {
        this.txtDashboardHeroImagePath = txtDashboardHeroImagePath;
    }

    public String getTxtSmtpHost() {
        return txtSmtpHost;
    }

    public void setTxtSmtpHost(String txtSmtpHost) {
        this.txtSmtpHost = txtSmtpHost;
    }

    public Integer getTxtSmtpPort() {
        return txtSmtpPort;
    }

    public void setTxtSmtpPort(Integer txtSmtpPort) {
        this.txtSmtpPort = txtSmtpPort;
    }

    public String getTxtSmtpUsername() {
        return txtSmtpUsername;
    }

    public void setTxtSmtpUsername(String txtSmtpUsername) {
        this.txtSmtpUsername = txtSmtpUsername;
    }

    public String getTxtSmtpPassword() {
        return txtSmtpPassword;
    }

    public void setTxtSmtpPassword(String txtSmtpPassword) {
        this.txtSmtpPassword = txtSmtpPassword;
    }

    public Boolean getBlSmtpAuth() {
        return blSmtpAuth;
    }

    public void setBlSmtpAuth(Boolean blSmtpAuth) {
        this.blSmtpAuth = blSmtpAuth;
    }

    public Boolean getBlSmtpSslEnable() {
        return blSmtpSslEnable;
    }

    public void setBlSmtpSslEnable(Boolean blSmtpSslEnable) {
        this.blSmtpSslEnable = blSmtpSslEnable;
    }

    public Boolean getBlSmtpStarttlsEnable() {
        return blSmtpStarttlsEnable;
    }

    public void setBlSmtpStarttlsEnable(Boolean blSmtpStarttlsEnable) {
        this.blSmtpStarttlsEnable = blSmtpStarttlsEnable;
    }

    public Boolean getBlIsActive() {
        return blIsActive;
    }

    public void setBlIsActive(Boolean blIsActive) {
        this.blIsActive = blIsActive;
    }

    public Boolean getBlIsDemo() {
        return blIsDemo;
    }

    public void setBlIsDemo(Boolean blIsDemo) {
        this.blIsDemo = blIsDemo;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public Integer getSerCreatedUser() {
        return serCreatedUser;
    }

    public void setSerCreatedUser(Integer serCreatedUser) {
        this.serCreatedUser = serCreatedUser;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Integer getSerModifiedUser() {
        return serModifiedUser;
    }

    public void setSerModifiedUser(Integer serModifiedUser) {
        this.serModifiedUser = serModifiedUser;
    }

    public Timestamp getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }
}
