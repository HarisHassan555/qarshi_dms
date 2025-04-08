package com.bezkoder.spring.login.sa.bll.dto;

public class SubMenuDTO {
    private long subMenuId;
    private String subMenuName;

    private String subMenuAction;
    private String roles;

    private int submenuOrder;

    public int getSubmenuOrder() {
        return submenuOrder;
    }

    public void setSubmenuOrder(int submenuOrder) {
        this.submenuOrder = submenuOrder;
    }

    public long getSubMenuId() {
        return subMenuId;
    }

    public void setSubMenuId(long subMenuId) {
        this.subMenuId = subMenuId;
    }

    public String getSubMenuName() {
        return subMenuName;
    }

    public void setSubMenuName(String subMenuName) {
        this.subMenuName = subMenuName;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getSubMenuAction() {
        return subMenuAction;
    }

    public void setSubMenuAction(String subMenuAction) {
        this.subMenuAction = subMenuAction;
    }
}
