package com.bezkoder.spring.login.sa.bll.dto;

/**
 * API shape for SAP HCM employee list (matches Angular {@code SapHcmEmployeeRecord}).
 */
public class SapHcmEmployeeDto {

    private String employee;
    private String empName;
    private String departmentName;
    private String division;
    private String designation;
    private String email;
    private String payScale;
    private String subarea;
    private String companyName;
    private String hiringDate;
    private String confirmationDate;
    private String retirementDate;
    private String separationDate;
    private String rehiringDate;
    private String annualIncType;
    private String active;
    private String reportTo;
    private String reportToName;
    private boolean hasPicture;
    private String pictureUrl;

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPayScale() {
        return payScale;
    }

    public void setPayScale(String payScale) {
        this.payScale = payScale;
    }

    public String getSubarea() {
        return subarea;
    }

    public void setSubarea(String subarea) {
        this.subarea = subarea;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getHiringDate() {
        return hiringDate;
    }

    public void setHiringDate(String hiringDate) {
        this.hiringDate = hiringDate;
    }

    public String getConfirmationDate() {
        return confirmationDate;
    }

    public void setConfirmationDate(String confirmationDate) {
        this.confirmationDate = confirmationDate;
    }

    public String getRetirementDate() {
        return retirementDate;
    }

    public void setRetirementDate(String retirementDate) {
        this.retirementDate = retirementDate;
    }

    public String getSeparationDate() {
        return separationDate;
    }

    public void setSeparationDate(String separationDate) {
        this.separationDate = separationDate;
    }

    public String getRehiringDate() {
        return rehiringDate;
    }

    public void setRehiringDate(String rehiringDate) {
        this.rehiringDate = rehiringDate;
    }

    public String getAnnualIncType() {
        return annualIncType;
    }

    public void setAnnualIncType(String annualIncType) {
        this.annualIncType = annualIncType;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getReportTo() {
        return reportTo;
    }

    public void setReportTo(String reportTo) {
        this.reportTo = reportTo;
    }

    public String getReportToName() {
        return reportToName;
    }

    public void setReportToName(String reportToName) {
        this.reportToName = reportToName;
    }

    public boolean isHasPicture() {
        return hasPicture;
    }

    public void setHasPicture(boolean hasPicture) {
        this.hasPicture = hasPicture;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }
}
