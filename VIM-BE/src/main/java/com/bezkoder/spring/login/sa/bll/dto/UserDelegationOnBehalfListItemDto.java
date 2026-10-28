package com.bezkoder.spring.login.sa.bll.dto;

public class UserDelegationOnBehalfListItemDto {

    private Integer serOnBehalfUserId;
    private String onBehalfUserName;

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
}
