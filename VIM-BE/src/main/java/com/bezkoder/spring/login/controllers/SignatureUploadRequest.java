package com.bezkoder.spring.login.controllers;

public class SignatureUploadRequest {
    private String signature;
    private String fileType;

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
}
