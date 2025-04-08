package com.bezkoder.spring.login.sa.bll.dto;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;

import java.io.Serializable;
import java.time.Instant;

public class CfgTblSubMenuRoleDTO implements Serializable {
    private Integer serSubMenuRoleId;
    private Boolean blIsActive;
    private Boolean blnStatus;
   /* private Instant dteCreatedDate;
    private Instant dteModifiedDate;*/
    private Integer serCreatedUser;
    private Integer serModifiedUser;
    private CfgTblRole cfgTblRole;
    private CfgTblSubMenuDTO cfgTblSubMenu;

    public CfgTblRole getCfgTblRole() {
        return cfgTblRole;
    }

    public void setCfgTblRole(CfgTblRole cfgTblRole) {
        this.cfgTblRole = cfgTblRole;
    }

    public CfgTblSubMenuDTO getCfgTblSubMenu() {
        return cfgTblSubMenu;
    }

    public void setCfgTblSubMenu(CfgTblSubMenuDTO cfgTblSubMenu) {
        this.cfgTblSubMenu = cfgTblSubMenu;
    }

    private Integer cfgTblUser;
    private Boolean blIsDeleted;
    private Boolean blIsview;
    private Boolean blIsAdd;
    private Boolean blIsDelete;
    private Boolean blIsUpdate;
    private Boolean blIsApprove;
    private Boolean blIsAll;

    public Integer getSerSubMenuRoleId() {
        return serSubMenuRoleId;
    }

    public void setSerSubMenuRoleId(Integer serSubMenuRoleId) {
        this.serSubMenuRoleId = serSubMenuRoleId;
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

    /*public Instant getDteCreatedDate() {
        return dteCreatedDate;
    }

    public void setDteCreatedDate(Instant dteCreatedDate) {
        this.dteCreatedDate = dteCreatedDate;
    }

    public Instant getDteModifiedDate() {
        return dteModifiedDate;
    }

    public void setDteModifiedDate(Instant dteModifiedDate) {
        this.dteModifiedDate = dteModifiedDate;
    }*/

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



    public Integer getCfgTblUser() {
        return cfgTblUser;
    }

    public void setCfgTblUser(Integer cfgTblUser) {
        this.cfgTblUser = cfgTblUser;
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

    public Boolean getBlIsAll() {
        return blIsAll;
    }

    public void setBlIsAll(Boolean blIsAll) {
        this.blIsAll = blIsAll;
    }
}
