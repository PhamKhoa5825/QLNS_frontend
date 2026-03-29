package com.example.myapplication.model;

import java.util.List;

public class AttendanceMonthlyResponse {
    private List<AttendanceSummary> days;
    private Double baseSalary;
    private Double estimatedSalarySoFar;
    private Double totalDeductions;
    private Double totalBonuses;
    private Integer standardWorkingDays;

    public AttendanceMonthlyResponse() {
    }

    public List<AttendanceSummary> getDays() { return days; }
    public void setDays(List<AttendanceSummary> days) { this.days = days; }

    public Double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Double baseSalary) { this.baseSalary = baseSalary; }

    public Double getEstimatedSalarySoFar() { return estimatedSalarySoFar; }
    public void setEstimatedSalarySoFar(Double estimatedSalarySoFar) { this.estimatedSalarySoFar = estimatedSalarySoFar; }

    public Double getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(Double totalDeductions) { this.totalDeductions = totalDeductions; }

    public Double getTotalBonuses() { return totalBonuses; }
    public void setTotalBonuses(Double totalBonuses) { this.totalBonuses = totalBonuses; }

    public Integer getStandardWorkingDays() { return standardWorkingDays; }
    public void setStandardWorkingDays(Integer standardWorkingDays) { this.standardWorkingDays = standardWorkingDays; }
}
