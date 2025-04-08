package com.bezkoder.spring.login.sa.bll.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class CfgTblSubMenuDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer serSubMenuId;
    private Boolean blIsActive;
    private Boolean blnStatus;
    private Timestamp dteCreatedDate;
    private Timestamp dteModifiedDate;
    private Integer intSubMenuOrder;
    private Integer serCreatedUser;
    private Integer serModifiedUser;
    private String txtSubMenuName;
    private String txtSubMenuUrl;
    private Boolean blIsDeleted;
    private Boolean blIsview;
    private Boolean blIsAdd;
    private Boolean blIsDelete;
    private Boolean blIsUpdate;
    private Boolean blIsApprove;
    private List<Integer> cfgTblRoleIds;
    private String txtMenuName;
    private Integer serMenuId;
    public CfgTblSubMenuDTO() {}

    // Getters and Setters

    public String getTxtMenuName() {
        return txtMenuName;
    }

    public void setTxtMenuName(String txtMenuName) {
        this.txtMenuName = txtMenuName;
    }

    public Integer getSerMenuId() {
        return serMenuId;
    }

    public void setSerMenuId(Integer serMenuId) {
        this.serMenuId = serMenuId;
    }

    public Integer getSerSubMenuId() {
        return serSubMenuId;
    }

    public void setSerSubMenuId(Integer serSubMenuId) {
        this.serSubMenuId = serSubMenuId;
    }

    public Boolean getBlIsActive() {
        return blIsActive;
    }

    public void setBlIsActive(Boolean blIsActive) {
        this.blIsActive = blIsActive;
    }

    public Boolean getBlnStatus() {
        return blnStatus;
    }

    public void setBlnStatus(Boolean blnStatus) {
        this.blnStatus = blnStatus;
    }

    public Timestamp getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Timestamp dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Timestamp getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Timestamp dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }

    public Integer getIntSubMenuOrder() {
        return intSubMenuOrder;
    }

    public void setIntSubMenuOrder(Integer intSubMenuOrder) {
        this.intSubMenuOrder = intSubMenuOrder;
    }

    public Integer getSerCreatedUser() {
        return serCreatedUser;
    }

    public void setSerCreatedUser(Integer serCreatedUser) {
        this.serCreatedUser = serCreatedUser;
    }

    public Integer getSerModifiedUser() {
        return serModifiedUser;
    }

    public void setSerModifiedUser(Integer serModifiedUser) {
        this.serModifiedUser = serModifiedUser;
    }

    public String getTxtSubMenuName() {
        return txtSubMenuName;
    }

    public void setTxtSubMenuName(String txtSubMenuName) {
        this.txtSubMenuName = txtSubMenuName;
    }

    public String getTxtSubMenuUrl() {
        return txtSubMenuUrl;
    }

    public void setTxtSubMenuUrl(String txtSubMenuUrl) {
        this.txtSubMenuUrl = txtSubMenuUrl;
    }

    public Boolean getBlIsDeleted() {
        return blIsDeleted;
    }

    public void setBlIsDeleted(Boolean blIsDeleted) {
        this.blIsDeleted = blIsDeleted;
    }

    public Boolean getBlIsview() {
        return blIsview;
    }

    public void setBlIsview(Boolean blIsview) {
        this.blIsview = blIsview;
    }

    public Boolean getBlIsAdd() {
        return blIsAdd;
    }

    public void setBlIsAdd(Boolean blIsAdd) {
        this.blIsAdd = blIsAdd;
    }

    public Boolean getBlIsDelete() {
        return blIsDelete;
    }

    public void setBlIsDelete(Boolean blIsDelete) {
        this.blIsDelete = blIsDelete;
    }

    public Boolean getBlIsUpdate() {
        return blIsUpdate;
    }

    public void setBlIsUpdate(Boolean blIsUpdate) {
        this.blIsUpdate = blIsUpdate;
    }

    public Boolean getBlIsApprove() {
        return blIsApprove;
    }

    public void setBlIsApprove(Boolean blIsApprove) {
        this.blIsApprove = blIsApprove;
    }

    public List<Integer> getCfgTblRoleIds() {
        return cfgTblRoleIds;
    }

    public void setCfgTblRoleIds(List<Integer> cfgTblRoleIds) {
        this.cfgTblRoleIds = cfgTblRoleIds;
    }
}
