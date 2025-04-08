package com.bezkoder.spring.login.sa.bll.dto;

import java.util.Date;

public class ServiceDTO {

    private String sapCode;
    private String saleOrderNo;
    private String  lineItem;

    private String  quantity;



    private Date postedDate;

    public String getSaleOrderDetailId() {
        return saleOrderDetailId;
    }

    public void setSaleOrderDetailId(String saleOrderDetailId) {
        this.saleOrderDetailId = saleOrderDetailId;
    }

    private String saleOrderDetailId;
    public String getSaleOrderNo() {
        return saleOrderNo;
    }

    public void setSaleOrderNo(String saleOrderNo) {
        this.saleOrderNo = saleOrderNo;
    }

    private String  price;

    public String getSapCode() {
        return sapCode;
    }

    public void setSapCode(String sapCode) {
        this.sapCode = sapCode;
    }


    public String getLineItem() {
        return lineItem;
    }

    public void setLineItem(String lineItem) {
        this.lineItem = lineItem;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Date getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(Date postedDate) {
        this.postedDate = postedDate;
    }
}
