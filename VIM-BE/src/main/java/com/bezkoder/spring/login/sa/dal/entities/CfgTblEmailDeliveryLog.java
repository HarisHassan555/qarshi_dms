package com.bezkoder.spring.login.sa.dal.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "cfg_tbl_email_delivery_log")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CfgTblEmailDeliveryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ser_email_delivery_log_id")
    private Long serEmailDeliveryLogId;

    @Column(name = "txt_trigger_type", nullable = false, length = 64)
    private String txtTriggerType;

    @Column(name = "txt_template_code", length = 64)
    private String txtTemplateCode;

    @Column(name = "ser_application_id")
    private Integer serApplicationId;

    @Column(name = "ser_recipient_user_id")
    private Integer serRecipientUserId;

    @Column(name = "txt_recipient_email", nullable = false, length = 255)
    private String txtRecipientEmail;

    @Column(name = "txt_subject", length = 512)
    private String txtSubject;

    @Column(name = "txt_body_html", columnDefinition = "LONGTEXT")
    private String txtBodyHtml;

    @Column(name = "txt_body_text", columnDefinition = "LONGTEXT")
    private String txtBodyText;

    @Column(name = "txt_status", nullable = false, length = 32)
    private String txtStatus;

    @Column(name = "txt_error_message", columnDefinition = "LONGTEXT")
    private String txtErrorMessage;

    @Column(name = "bln_has_attachments", nullable = false)
    private boolean blnHasAttachments;

    @Column(name = "ser_triggered_by_user_id")
    private Integer serTriggeredByUserId;

    @Column(name = "ser_background_job_id")
    private Long serBackgroundJobId;

    @Column(name = "txt_context_label", length = 255)
    private String txtContextLabel;

    @Column(name = "dte_sent_at", nullable = false)
    private Timestamp dteSentAt;

    @Column(name = "bl_is_deleted", nullable = false)
    private boolean blIsDeleted;

    @Column(name = "bln_status", nullable = false)
    private boolean blnStatus;

    @Column(name = "dte_created_date")
    private Timestamp dteCreatedDate;

    @Column(name = "dte_modified_date")
    private Timestamp dteModifiedDate;

    public Long getSerEmailDeliveryLogId() {
        return serEmailDeliveryLogId;
    }

    public void setSerEmailDeliveryLogId(Long serEmailDeliveryLogId) {
        this.serEmailDeliveryLogId = serEmailDeliveryLogId;
    }

    public String getTxtTriggerType() {
        return txtTriggerType;
    }

    public void setTxtTriggerType(String txtTriggerType) {
        this.txtTriggerType = txtTriggerType;
    }

    public String getTxtTemplateCode() {
        return txtTemplateCode;
    }

    public void setTxtTemplateCode(String txtTemplateCode) {
        this.txtTemplateCode = txtTemplateCode;
    }

    public Integer getSerApplicationId() {
        return serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public Integer getSerRecipientUserId() {
        return serRecipientUserId;
    }

    public void setSerRecipientUserId(Integer serRecipientUserId) {
        this.serRecipientUserId = serRecipientUserId;
    }

    public String getTxtRecipientEmail() {
        return txtRecipientEmail;
    }

    public void setTxtRecipientEmail(String txtRecipientEmail) {
        this.txtRecipientEmail = txtRecipientEmail;
    }

    public String getTxtSubject() {
        return txtSubject;
    }

    public void setTxtSubject(String txtSubject) {
        this.txtSubject = txtSubject;
    }

    public String getTxtBodyHtml() {
        return txtBodyHtml;
    }

    public void setTxtBodyHtml(String txtBodyHtml) {
        this.txtBodyHtml = txtBodyHtml;
    }

    public String getTxtBodyText() {
        return txtBodyText;
    }

    public void setTxtBodyText(String txtBodyText) {
        this.txtBodyText = txtBodyText;
    }

    public String getTxtStatus() {
        return txtStatus;
    }

    public void setTxtStatus(String txtStatus) {
        this.txtStatus = txtStatus;
    }

    public String getTxtErrorMessage() {
        return txtErrorMessage;
    }

    public void setTxtErrorMessage(String txtErrorMessage) {
        this.txtErrorMessage = txtErrorMessage;
    }

    public boolean isBlnHasAttachments() {
        return blnHasAttachments;
    }

    public void setBlnHasAttachments(boolean blnHasAttachments) {
        this.blnHasAttachments = blnHasAttachments;
    }

    public Integer getSerTriggeredByUserId() {
        return serTriggeredByUserId;
    }

    public void setSerTriggeredByUserId(Integer serTriggeredByUserId) {
        this.serTriggeredByUserId = serTriggeredByUserId;
    }

    public Long getSerBackgroundJobId() {
        return serBackgroundJobId;
    }

    public void setSerBackgroundJobId(Long serBackgroundJobId) {
        this.serBackgroundJobId = serBackgroundJobId;
    }

    public String getTxtContextLabel() {
        return txtContextLabel;
    }

    public void setTxtContextLabel(String txtContextLabel) {
        this.txtContextLabel = txtContextLabel;
    }

    public Timestamp getDteSentAt() {
        return dteSentAt;
    }

    public void setDteSentAt(Timestamp dteSentAt) {
        this.dteSentAt = dteSentAt;
    }

    public boolean isBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public boolean isBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(boolean blnStatus) {
        this.blnStatus = blnStatus;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }
}