package com.bezkoder.spring.login.sa.bll.dto;

public class HrSubareaDto {

    private Integer serSubareaId;
    private Integer serOrganizationId;
    private String organizationName;
    private String txtSubareaName;
    private String txtSubareaCode;
    private Boolean blnStatus;

    public Integer getSerSubareaId() {
        return serSubareaId;
    }

    public void setSerSubareaId(Integer serSubareaId) {
        this.serSubareaId = serSubareaId;
    }

    public Integer getSerOrganizationId() {
        return serOrganizationId;
    }

    public void setSerOrganizationId(Integer serOrganizationId) {
        this.serOrganizationId = serOrganizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getTxtSubareaName() {
        return txtSubareaName;
    }

    public void setTxtSubareaName(String txtSubareaName) {
        this.txtSubareaName = txtSubareaName;
    }

    public String getTxtSubareaCode() {
        return txtSubareaCode;
    }

    public void setTxtSubareaCode(String txtSubareaCode) {
        this.txtSubareaCode = txtSubareaCode;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }
}
