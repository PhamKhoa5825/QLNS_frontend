package com.example.myapplication.network;

import com.example.myapplication.model.*;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthModels.AuthResponse> login(@Body AuthModels.LoginRequest request);

    // ── EMPLOYEE ──────────────────────────────────────────────────
    @GET("api/employees")
    Call<List<Employee>> getEmployees();

    @GET("api/employees/{id}")
    Call<Employee> getEmployeeById(@Path("id") Long id);

    @GET("api/employees/search")
    Call<List<Employee>> searchEmployees(@Query("keyword") String keyword);

    @GET("api/employees/department/{deptId}")
    Call<List<Employee>> getEmployeesByDept(@Path("deptId") Long deptId);

    @POST("api/employees")
    Call<Employee> createEmployee(@Body CreateEmployeeRequest request);

    @PUT("api/employees/{id}")
    Call<Employee> updateEmployee(@Path("id") Long id, @Body CreateEmployeeRequest request);

    @PUT("api/employees/{id}/resign")
    Call<Void> resignEmployee(@Path("id") Long id);

    // ĐÃ THÊM: Khôi phục nhân viên đã nghỉ việc (RESIGNED → ACTIVE)
    @PUT("api/employees/{id}/reactivate")
    Call<Void> reactivateEmployee(@Path("id") Long id);

    // ĐÃ THÊM: Endpoint đổi role nhân viên (EMPLOYEE / MANAGER / ADMIN)
    @PUT("api/employees/{id}/role")
    Call<Employee> updateEmployeeRole(@Path("id") Long employeeId,
                                      @Body AccountModels.UpdateRoleRequest request);

    // ── DEPARTMENT ────────────────────────────────────────────────
    @GET("api/departments")
    Call<List<Department>> getDepartments();

    @POST("api/departments")
    Call<Department> createDepartment(@Body Department department);

    @PUT("api/departments/{id}")
    Call<Department> updateDepartment(@Path("id") Long id, @Body Department department);

    @DELETE("api/departments/{id}")
    Call<Void> deleteDepartment(@Path("id") Long id);

    @PUT("api/departments/{deptId}/manager/{empId}")
    Call<Department> setManager(@Path("deptId") Long deptId, @Path("empId") Long empId);

    @GET("api/departments/{id}/employees")
    Call<List<Employee>> getDeptEmployees(@Path("id") Long deptId);

    // ── ACCOUNT MANAGEMENT (Admin) ────────────────────────────────
    @PUT("api/admin/accounts/{userId}/reset-password")
    Call<Void> resetPassword(@Path("userId") Long userId,
                             @Query("newPassword") String newPassword);

    @PUT("api/admin/accounts/{userId}/status")
    Call<Void> updateAccountStatus(@Path("userId") Long userId,
                                   @Query("status") String status);

    // ── TASK ──────────────────────────────────────────────────────
    @GET("api/tasks/my/{empId}")
    Call<List<TaskModels.TaskResponse>> getMyTasks(@Path("empId") Long empId);

    @GET("api/tasks/department/{deptId}")
    Call<List<TaskModels.TaskResponse>> getTasksByDept(@Path("deptId") Long deptId);

    @POST("api/tasks")
    Call<TaskModels.TaskResponse> createTask(@Body TaskModels.CreateTaskRequest req);

    @PUT("api/tasks/{id}/accept")
    Call<TaskModels.TaskResponse> acceptTask(
            @Path("id") Long taskId,
            @Body TaskModels.AcceptTaskRequest req);

    @PUT("api/tasks/{id}/status")
    Call<TaskModels.TaskResponse> updateTaskStatus(
            @Path("id") Long taskId,
            @Query("updatedById") Long updatedById,
            @Body TaskModels.UpdateTaskStatusRequest req);

    @DELETE("api/tasks/{id}")
    Call<Void> deleteTask(@Path("id") Long taskId);

    // ── REQUEST (đơn từ) ──────────────────────────────────────────
    @GET("api/requests/employee/{empId}")
    Call<List<RequestModels.RequestResponse>> getMyRequests(@Path("empId") Long empId);

    @POST("api/requests/employee/{empId}")
    Call<RequestModels.RequestResponse> createRequest(
            @Path("empId") Long empId,
            @Body RequestModels.CreateRequestBody req);

    @PUT("api/requests/{id}/employee/{empId}")
    Call<RequestModels.RequestResponse> updateRequest(
            @Path("id") Long id,
            @Path("empId") Long empId,
            @Body RequestModels.CreateRequestBody req);

    @DELETE("api/requests/{id}/employee/{empId}")
    Call<Void> cancelRequest(@Path("id") Long id, @Path("empId") Long empId);

    @GET("api/requests/department/{deptId}/pending")
    Call<List<RequestModels.RequestResponse>> getPendingRequests(
            @Path("deptId") Long deptId);

    @PUT("api/requests/{id}/review/employee/{reviewerId}")
    Call<RequestModels.RequestResponse> reviewRequest(
            @Path("id") Long id,
            @Path("reviewerId") Long reviewerId,
            @Body RequestModels.ReviewRequestBody req);

    @GET("api/requests")
    Call<List<RequestModels.RequestResponse>> getAllRequests();

    // ── NOTIFICATION ──────────────────────────────────────────────
    @GET("api/notifications/department/{deptId}")
    Call<List<NotificationModels.NotificationResponse>> getNotifications(
            @Path("deptId") Long deptId,
            @Query("userId") Long userId);

    @POST("api/notifications")
    Call<NotificationModels.NotificationResponse> createNotification(
            @Body NotificationModels.CreateNotificationRequest req);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotiRead(@Path("id") Long notiId, @Query("userId") Long userId);

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(@Path("id") Long id);

    @GET("api/notifications/unread-count")
    Call<Long> getUnreadCount(@Query("userId") Long userId);

    // ── COMPANY SETTINGS (Admin) ──────────────────────────────────
    @GET("api/settings")
    Call<AdminModels.CompanySettings> getCompanySettings();

    @PUT("api/settings")
    Call<AdminModels.CompanySettings> updateCompanySettings(
            @Body AdminModels.UpdateSettingsRequest req);

    // ── SYSTEM LOG (Admin) ────────────────────────────────────────
    @GET("api/admin/logs")
    Call<List<AdminModels.SystemLogResponse>> getSystemLogs();

    @GET("api/admin/logs/filter")
    Call<List<AdminModels.SystemLogResponse>> filterLogsByAction(
            @Query("action") String action);
}