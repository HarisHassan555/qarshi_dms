package com.bezkoder.spring.login.sa.bll.dto;

public class HrOrganizationDto {

    private Integer serOrganizationId;
    private String logo;
    private String txtOrganizationName;
    private String txtOrganizationCode;
    private String txtAddress;
    private Boolean blnStatus;

    public Integer getSerOrganizationId() {
        return serOrganizationId;
    }

    public void setSerOrganizationId(Integer serOrganizationId) {
        this.serOrganizationId = serOrganizationId;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getTxtOrganizationName() {
        return txtOrganizationName;
    }

    public void setTxtOrganizationName(String txtOrganizationName) {
        this.txtOrganizationName = txtOrganizationName;
    }

    public String getTxtOrganizationCode() {
        return txtOrganizationCode;
    }

    public void setTxtOrganizationCode(String txtOrganizationCode) {
        this.txtOrganizationCode = txtOrganizationCode;
    }

    public String getTxtAddress() {
        return txtAddress;
    }

    public void setTxtAddress(String txtAddress) {
        this.txtAddress = txtAddress;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }
}
