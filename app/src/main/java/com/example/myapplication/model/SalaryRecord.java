package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class SalaryRecord {

    @SerializedName("id")
    private Long id;

    @SerializedName("employeeId")
    private Long employeeId;

    @SerializedName("employeeName")
    private String employeeName;

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("departmentName")
    private String departmentName;

    @SerializedName("month")
    private Integer month;

    @SerializedName("year")
    private Integer year;

    @SerializedName("workingDaysStandard")
    private Integer workingDaysStandard;

    @SerializedName("daysWorked")
    private Double daysWorked;

    @SerializedName("daysAbsentExcused")
    private Double daysAbsentExcused; // Changed to Double

    @SerializedName("daysAbsentUnexcused")
    private Double daysAbsentUnexcused;

    @SerializedName("totalLateMinutes")
    private Integer totalLateMinutes;

    @SerializedName("totalOvertimeHours")
    private Double totalOvertimeHours;

    @SerializedName("baseSalary")
    private Double baseSalary;

    @SerializedName("deductionLate")
    private Double deductionLate;

    @SerializedName("deductionUnexcused")
    private Double deductionUnexcused;

    @SerializedName("taskBonus")
    private Double taskBonus;

    @SerializedName("overtimeBonus")
    private Double overtimeBonus;

    @SerializedName("grossSalary")
    private Double grossSalary;

    @SerializedName("daysSick")
    private Double daysSick;

    @SerializedName("deductionSick")
    private Double deductionSick;

    @SerializedName("businessTripDays")
    private Double businessTripDays;

    @SerializedName("performanceScore")
    private Double performanceScore;

    @SerializedName("performanceGrade")
    private String performanceGrade;

    @SerializedName("note")
    private String note;

    @SerializedName("status")
    private String status;

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }
    public Integer getWorkingDaysStandard() { return workingDaysStandard; }
    public Double getDaysWorked() { return daysWorked; }
    public Double getDaysAbsentExcused() { return daysAbsentExcused; }
    public Double getDaysAbsentUnexcused() { return daysAbsentUnexcused; }
    public Integer getTotalLateMinutes() { return totalLateMinutes; }
    public Double getTotalOvertimeHours() { return totalOvertimeHours; }
    public Double getBaseSalary() { return baseSalary; }
    public Double getDeductionLate() { return deductionLate; }
    public Double getDeductionUnexcused() { return deductionUnexcused; }
    public Double getTaskBonus() { return taskBonus; }
    public Double getOvertimeBonus() { return overtimeBonus; }
    public Double getGrossSalary() { return grossSalary; }
    public Double getDaysSick() { return daysSick; }
    public Double getDeductionSick() { return deductionSick; }
    public Double getPerformanceScore() { return performanceScore; }
    public String getPerformanceGrade() { return performanceGrade; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public Double getBusinessTripDays() { return businessTripDays != null ? businessTripDays : 0.0; }
}
