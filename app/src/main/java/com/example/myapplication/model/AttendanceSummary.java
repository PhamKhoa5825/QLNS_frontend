package com.example.myapplication.model;

import java.io.Serializable;

public class AttendanceSummary implements Serializable {
    private String date;
    private String status;
    private String description;
    private String colorCode;
    private Double salaryImpact;
    private Boolean isPaid;
    private Double dayValue;
    private String topColor;
    private String middleColor;
    private String bottomColor;
    private String borderColor; // New for OT border
    private Boolean isBold; // New to highlight Today

    public AttendanceSummary() {
    }

    public AttendanceSummary(String date, String status, String description, String colorCode) {
        this.date = date;
        this.status = status;
        this.description = description;
        this.colorCode = colorCode;
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public Double getSalaryImpact() { return salaryImpact; }
    public void setSalaryImpact(Double salaryImpact) { this.salaryImpact = salaryImpact; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public Double getDayValue() { return dayValue; }
    public void setDayValue(Double dayValue) { this.dayValue = dayValue; }

    public String getTopColor() { return topColor; }
    public void setTopColor(String topColor) { this.topColor = topColor; }

    public String getMiddleColor() { return middleColor; }
    public void setMiddleColor(String middleColor) { this.middleColor = middleColor; }

    public String getBottomColor() { return bottomColor; }
    public void setBottomColor(String bottomColor) { this.bottomColor = bottomColor; }

    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }

    public Boolean getIsBold() { return isBold != null && isBold; }
    public void setIsBold(Boolean isBold) { this.isBold = isBold; }
}
