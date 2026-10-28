package com.bezkoder.spring.login.chat.bll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatUserDto {
    private Integer userId;
    private String displayName;
    private String loginName;
    private String organizationName;
    private String divisionName;
    private String departmentName;
    @JsonProperty("hasProfilePicture")
    private boolean hasProfilePicture;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getDivisionName() {
        return divisionName;
    }

    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public boolean getHasProfilePicture() {
        return hasProfilePicture;
    }

    public void setHasProfilePicture(boolean hasProfilePicture) {
        this.hasProfilePicture = hasProfilePicture;
    }
}
