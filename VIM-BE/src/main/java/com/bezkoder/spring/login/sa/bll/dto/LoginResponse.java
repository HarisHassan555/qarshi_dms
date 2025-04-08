package com.bezkoder.spring.login.sa.bll.dto;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

public class LoginResponse {

    private String token;
    private CfgTblUser user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CfgTblUser getUser() {
        return user;
    }

    public void setUser(CfgTblUser user) {
        this.user = user;
    }
}
