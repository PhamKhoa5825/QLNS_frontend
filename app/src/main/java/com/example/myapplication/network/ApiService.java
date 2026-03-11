package com.example.myapplication.network;

import com.example.myapplication.model.*;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── AUTHENTICATION ───────────────────────────
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    // ── DEPARTMENT (TV1) ────────────────────────
    @GET("api/departments")
    Call<List<Department>> getDepartments();

    @GET("api/departments/{id}")
    Call<Department> getDepartmentById(@Path("id") Long id);

    @GET("api/departments/{id}/employees")
    Call<List<Employee>> getEmployeesByDepartment(@Path("id") Long deptId);

    @POST("api/departments")
    Call<Department> createDepartment(@Body Department department);

    @PUT("api/departments/{id}")
    Call<Department> updateDepartment(@Path("id") Long id, @Body Department department);

    @DELETE("api/departments/{id}")
    Call<Void> deleteDepartment(@Path("id") Long id);

    // ── EMPLOYEE (TV1) ───────────────────────────
    @GET("api/employees")
    Call<List<Employee>> getEmployees();

    @GET("api/employees/{id}")
    Call<Employee> getEmployeeById(@Path("id") Long id);

    @GET("api/employees/department/{deptId}")
    Call<List<Employee>> getEmployeesByDepartmentId(@Path("deptId") Long deptId);

    @GET("api/employees/search")
    Call<List<Employee>> searchEmployees(@Query("keyword") String keyword);

    @POST("api/employees")
    Call<Employee> createEmployee(@Body CreateEmployeeRequest request);

    @PUT("api/employees/{id}/resign")
    Call<Void> resignEmployee(@Path("id") Long id);

    // ── DASHBOARD ───────────────────────────────
    @GET("api/departments/{deptId}/dashboard")
    Call<DepartmentDashboardDTO> getDepartmentDashboard(@Path("deptId") Long deptId);

    // ── REQUESTS ─────────────────────────────────
    @GET("api/requests/department/{deptId}/status")
    Call<List<Request>> getRequestsByDepartmentAndStatus(@Path("deptId") Long deptId, @Query("status") String status);

    @PUT("api/requests/{id}/status")
    Call<Request> updateRequestStatus(@Path("id") Long id, @Query("status") String status);

    // ── TASKS ────────────────────────────────────
    @GET("api/tasks/department/{deptId}")
    Call<List<Task>> getTasksByDepartment(@Path("deptId") Long deptId);

    @POST("api/tasks")
    Call<Task> createTask(@Body CreateTaskRequest request);

    @PUT("api/tasks/{id}")
    Call<Task> updateTask(@Path("id") Long id, @Body Task task);

    // ── TIMEKEEPING ──────────────────────────────
    @GET("api/attendance/department/{deptId}/date")
    Call<List<Attendance>> getDepartmentAttendanceByDate(@Path("deptId") Long deptId, @Query("date") String date);

    // ── CHAT ─────────────────────────────────────
    @POST("api/chat/rooms/group")
    Call<ChatRoom> createDepartmentGroupChat(@Query("name") String name, @Query("departmentId") Long deptId, @Query("creatorId") Long creatorId);

    // ── NOTIFICATIONS ────────────────────────────
    @GET("api/notifications/department/{deptId}")
    Call<List<Notification>> getNotifications(@Path("deptId") Long deptId, @Query("userId") Long userId);

    @POST("api/notifications")
    Call<Notification> createNotification(@Body CreateNotificationRequest notification);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotificationAsRead(@Path("id") Long id, @Query("userId") Long userId);
}