package com.bezkoder.spring.login.sa.bll.dto;

public class HrOrganizationBrandingUpdateDto {

    private Integer serOrganizationId;
    private String txtOrganizationName;
    private String txtAddress;
    private String logo;
    /** Optional data URL or raw base64; when set, saved to upload storage and `logo` path is updated. */
    private String logoBase64;

    public Integer getSerOrganizationId() {
        return serOrganizationId;
    }

    public void setSerOrganizationId(Integer serOrganizationId) {
        this.serOrganizationId = serOrganizationId;
    }

    public String getTxtOrganizationName() {
        return txtOrganizationName;
    }

    public void setTxtOrganizationName(String txtOrganizationName) {
        this.txtOrganizationName = txtOrganizationName;
    }

    public String getTxtAddress() {
        return txtAddress;
    }

    public void setTxtAddress(String txtAddress) {
        this.txtAddress = txtAddress;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }
}
