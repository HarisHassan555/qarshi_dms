package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_app_activity_log")
@NamedQuery(name = "CfgTblAppActivityLog.findAll", query = "SELECT a FROM CfgTblAppActivityLog a ORDER BY a.dteCreatedDate DESC")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblAppActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ser_activity_log_id")
    private Integer serActivityLogId;

    @Column(name = "txt_action_type", length = 64)
    private String txtActionType;

    @Column(name = "ser_user_id")
    private Integer serUserId;

    @Column(name = "txt_username", length = 255)
    private String txtUsername;

    @Column(name = "txt_ip_address", length = 64)
    private String txtIpAddress;

    @Column(name = "txt_device", length = 512)
    private String txtDevice;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "txt_status", length = 32)
    private String txtStatus;

    @Column(name = "txt_message", length = 2000)
    private String txtMessage;

    @Column(name = "ser_entity_id")
    private Integer serEntityId;

    @Column(name = "txt_entity_type", length = 64)
    private String txtEntityType;

    @Lob
    @Column(name = "txt_payload")
    private String txtPayload;

    @Column(name = "txt_error_message", length = 2000)
    private String txtErrorMessage;

    public CfgTblAppActivityLog() {
    }

    public Integer getSerActivityLogId() {
        return serActivityLogId;
    }

    public void setSerActivityLogId(Integer serActivityLogId) {
        this.serActivityLogId = serActivityLogId;
    }

    public String getTxtActionType() {
        return txtActionType;
    }

    public void setTxtActionType(String txtActionType) {
        this.txtActionType = txtActionType;
    }

    public Integer getSerUserId() {
        return serUserId;
    }

    public void setSerUserId(Integer serUserId) {
        this.serUserId = serUserId;
    }

    public String getTxtUsername() {
        return txtUsername;
    }

    public void setTxtUsername(String txtUsername) {
        this.txtUsername = txtUsername;
    }

    public String getTxtIpAddress() {
        return txtIpAddress;
    }

    public void setTxtIpAddress(String txtIpAddress) {
        this.txtIpAddress = txtIpAddress;
    }

    public String getTxtDevice() {
        return txtDevice;
    }

    public void setTxtDevice(String txtDevice) {
        this.txtDevice = txtDevice;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public String getTxtMessage() {
        return txtMessage;
    }

    public void setTxtMessage(String txtMessage) {
        this.txtMessage = txtMessage;
    }

    public Integer getSerEntityId() {
        return serEntityId;
    }

    public void setSerEntityId(Integer serEntityId) {
        this.serEntityId = serEntityId;
    }

    public String getTxtEntityType() {
        return txtEntityType;
    }

    public void setTxtEntityType(String txtEntityType) {
        this.txtEntityType = txtEntityType;
    }

    public String getTxtPayload() {
        return txtPayload;
    }

    public void setTxtPayload(String txtPayload) {
        this.txtPayload = txtPayload;
    }

    public String getTxtErrorMessage() {
        return txtErrorMessage;
    }

    public void setTxtErrorMessage(String txtErrorMessage) {
        this.txtErrorMessage = txtErrorMessage;
    }
}
