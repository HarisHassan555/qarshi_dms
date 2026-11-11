package com.bezkoder.spring.login.sa.bll.dto;

public class UserDelegationOnBehalfListItemDto {

    private Integer serOnBehalfUserId;
    private String onBehalfUserName;
    private Boolean blnAllowFormFillOnBehalf = Boolean.TRUE;
    private Boolean blnAllowFormSubmitOnBehalf = Boolean.TRUE;

    public Integer getSerOnBehalfUserId() {
        return serOnBehalfUserId;
    }

    public void setSerOnBehalfUserId(Integer serOnBehalfUserId) {
        this.serOnBehalfUserId = serOnBehalfUserId;
    }

    public String getOnBehalfUserName() {
        return onBehalfUserName;
    }

    public void setOnBehalfUserName(String onBehalfUserName) {
        this.onBehalfUserName = onBehalfUserName;
    }

    public Boolean getBlnAllowFormFillOnBehalf() {
        return blnAllowFormFillOnBehalf;
    }

    public void setBlnAllowFormFillOnBehalf(Boolean blnAllowFormFillOnBehalf) {
        this.blnAllowFormFillOnBehalf = blnAllowFormFillOnBehalf;
    }

    public Boolean getBlnAllowFormSubmitOnBehalf() {
        return blnAllowFormSubmitOnBehalf;
    }

    public void setBlnAllowFormSubmitOnBehalf(Boolean blnAllowFormSubmitOnBehalf) {
        this.blnAllowFormSubmitOnBehalf = blnAllowFormSubmitOnBehalf;
    }
}
