package com.bezkoder.spring.login.sa.bll.dto;

public class EmailLayoutDto {

    private Integer id;
    private String layoutCode;
    private String layoutName;
    private String description;
    private String htmlWrapper;
    private boolean active;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLayoutCode() {
        return layoutCode;
    }

    public void setLayoutCode(String layoutCode) {
        this.layoutCode = layoutCode;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlWrapper() {
        return htmlWrapper;
    }

    public void setHtmlWrapper(String htmlWrapper) {
        this.htmlWrapper = htmlWrapper;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
