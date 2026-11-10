package com.bezkoder.spring.login.sa.bll.dto;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

public class LoginResponse {

    private String token;
    private String refreshToken;
    private CfgTblUser user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public CfgTblUser getUser() {
        return user;
    }

    public void setUser(CfgTblUser user) {
        this.user = user;
    }
}
