package com.bezkoder.spring.login.backgroundjob.dto;

public class TemplatePostApprovalEmailJobPayload {

    private Integer serApplicationId;
    private String contextLabel;
    private boolean refreshPdfBeforeEmail;
    private String initiatorPdfBase64;
    private String approverPdfBase64;
    private String pdfName;
    private String pdfMime;
    /** When true, worker loads PDF from application record (after updateApplicationPdf). */
    private boolean useStoredApplicationPdf;

    public Integer getSerApplicationId() {
        return serApplicationId;
    }

    public void setSerApplicationId(Integer serApplicationId) {
        this.serApplicationId = serApplicationId;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public void setContextLabel(String contextLabel) {
        this.contextLabel = contextLabel;
    }

    public boolean isRefreshPdfBeforeEmail() {
        return refreshPdfBeforeEmail;
    }

    public void setRefreshPdfBeforeEmail(boolean refreshPdfBeforeEmail) {
        this.refreshPdfBeforeEmail = refreshPdfBeforeEmail;
    }

    public String getInitiatorPdfBase64() {
        return initiatorPdfBase64;
    }

    public void setInitiatorPdfBase64(String initiatorPdfBase64) {
        this.initiatorPdfBase64 = initiatorPdfBase64;
    }

    public String getApproverPdfBase64() {
        return approverPdfBase64;
    }

    public void setApproverPdfBase64(String approverPdfBase64) {
        this.approverPdfBase64 = approverPdfBase64;
    }

    public String getPdfName() {
        return pdfName;
    }

    public void setPdfName(String pdfName) {
        this.pdfName = pdfName;
    }

    public String getPdfMime() {
        return pdfMime;
    }

    public void setPdfMime(String pdfMime) {
        this.pdfMime = pdfMime;
    }

    public boolean hasAttachedPdfs() {
        return (initiatorPdfBase64 != null && !initiatorPdfBase64.isEmpty())
                || (approverPdfBase64 != null && !approverPdfBase64.isEmpty());
    }

    public boolean isUseStoredApplicationPdf() {
        return useStoredApplicationPdf;
    }

    public void setUseStoredApplicationPdf(boolean useStoredApplicationPdf) {
        this.useStoredApplicationPdf = useStoredApplicationPdf;
    }
}
