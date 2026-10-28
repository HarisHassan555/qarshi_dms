package com.bezkoder.spring.login.sa.bll.dto;

public class SapHcmXmlSyncResult {

    private String status;
    private String message;
    private int totalItems;
    private int inserted;
    private int updated;
    private int picturesSaved;
    private int skipped;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getPicturesSaved() {
        return picturesSaved;
    }

    public void setPicturesSaved(int picturesSaved) {
        this.picturesSaved = picturesSaved;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }
}
