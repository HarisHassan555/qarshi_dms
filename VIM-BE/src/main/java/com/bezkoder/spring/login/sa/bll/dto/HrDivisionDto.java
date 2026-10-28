package com.bezkoder.spring.login.sa.bll.dto;

public class HrDivisionDto {

    private Integer serDivisionId;
    private Integer serSubareaId;
    private String subareaName;
    private String organizationName;
    private String txtDivisionName;
    private String txtDivisionCode;
    private Boolean blnStatus;

    public Integer getSerDivisionId() {
        return serDivisionId;
    }

    public void setSerDivisionId(Integer serDivisionId) {
        this.serDivisionId = serDivisionId;
    }

    public Integer getSerSubareaId() {
        return serSubareaId;
    }

    public void setSerSubareaId(Integer serSubareaId) {
        this.serSubareaId = serSubareaId;
    }

    public String getSubareaName() {
        return subareaName;
    }

    public void setSubareaName(String subareaName) {
        this.subareaName = subareaName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getTxtDivisionName() {
        return txtDivisionName;
    }

    public void setTxtDivisionName(String txtDivisionName) {
        this.txtDivisionName = txtDivisionName;
    }

    public String getTxtDivisionCode() {
        return txtDivisionCode;
    }

    public void setTxtDivisionCode(String txtDivisionCode) {
        this.txtDivisionCode = txtDivisionCode;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }
}
