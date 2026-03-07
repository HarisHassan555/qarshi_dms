package com.bezkoder.spring.login.controllers;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SignatureUploadRequest {
    private String signature;
    private String fileType;
    @JsonAlias({"txtDepartmentName", "departmentName"})
    private String department;
    @JsonAlias({"txtDesignation", "approverDesignation"})
    private String designation;

    public SignatureUploadRequest() {
    }

    public SignatureUploadRequest(String signature, String fileType) {
        this.signature = signature;
        this.fileType = fileType;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
