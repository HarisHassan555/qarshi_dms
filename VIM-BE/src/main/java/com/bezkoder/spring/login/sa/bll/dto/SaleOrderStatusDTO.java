package com.bezkoder.spring.login.sa.bll.dto;

import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;

public class SaleOrderStatusDTO {
    private SlsTblSaleOrder saleOrder;
    private String status;

    public SaleOrderStatusDTO(SlsTblSaleOrder saleOrder, String status) {
        this.saleOrder = saleOrder;
        this.status = status;
    }

    // Getters and Setters
    public SlsTblSaleOrder getSaleOrder() {
        return saleOrder;
    }

    public void setSaleOrder(SlsTblSaleOrder saleOrder) {
        this.saleOrder = saleOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}