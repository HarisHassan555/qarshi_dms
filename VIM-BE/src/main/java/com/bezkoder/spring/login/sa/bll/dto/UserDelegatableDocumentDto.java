package com.bezkoder.spring.login.sa.bll.dto;

public class UserDelegatableDocumentDto {

    private Integer serFormId;
    private String txtTemplateName;
    private String txtCodeConvention;

    public Integer getSerFormId() {
        return serFormId;
    }

    public void setSerFormId(Integer serFormId) {
        this.serFormId = serFormId;
    }

    public String getTxtTemplateName() {
        return txtTemplateName;
    }

    public void setTxtTemplateName(String txtTemplateName) {
        this.txtTemplateName = txtTemplateName;
    }

    public String getTxtCodeConvention() {
        return txtCodeConvention;
    }

    public void setTxtCodeConvention(String txtCodeConvention) {
        this.txtCodeConvention = txtCodeConvention;
    }
}
