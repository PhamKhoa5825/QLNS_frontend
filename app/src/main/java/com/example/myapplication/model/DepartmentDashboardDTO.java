package com.example.myapplication.model;

public class DepartmentDashboardDTO {
    private int totalEmployees;
    private int presentToday;
    private int lateToday;
    private int pendingTasks;
    private int inProgressTasks;
    private int doneTasks;

    public int getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }

    public int getPresentToday() { return presentToday; }
    public void setPresentToday(int presentToday) { this.presentToday = presentToday; }

    public int getLateToday() { return lateToday; }
    public void setLateToday(int lateToday) { this.lateToday = lateToday; }

    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }

    public int getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(int inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public int getDoneTasks() { return doneTasks; }
    public void setDoneTasks(int doneTasks) { this.doneTasks = doneTasks; }
}
