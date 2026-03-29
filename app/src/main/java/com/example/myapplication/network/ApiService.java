package com.example.myapplication.network;

import com.example.myapplication.model.dto.*;
import com.example.myapplication.model.entity.*;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthDto.AuthResponse> login(@Body AuthDto.LoginRequest request);

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
                                      @Body AccountDto.UpdateRoleRequest request);

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
    Call<List<Task>> getMyTasks(@Path("empId") Long empId);

    @GET("api/tasks/department/{deptId}")
    Call<List<Task>> getTasksByDept(@Path("deptId") Long deptId);

    @POST("api/tasks")
    Call<Task> createTask(@Body TaskDto.CreateTaskRequest req);

    @PUT("api/tasks/{id}/accept")
    Call<Task> acceptTask(
            @Path("id") Long taskId,
            @Body TaskDto.AcceptTaskRequest req);

    @PUT("api/tasks/{id}/status")
    Call<Task> updateTaskStatus(
            @Path("id") Long taskId,
            @Query("updatedById") Long updatedById,
            @Body TaskDto.UpdateTaskStatusRequest req);

    @DELETE("api/tasks/{id}")
    Call<Void> deleteTask(@Path("id") Long taskId);

    // ── REQUEST (đơn từ) ──────────────────────────────────────────
    @GET("api/requests/employee/{empId}")
    Call<List<Request>> getMyRequests(@Path("empId") Long empId);

    @POST("api/requests/employee/{empId}")
    Call<Request> createRequest(
            @Path("empId") Long empId,
            @Body RequestDto.CreateRequestBody req);

    @PUT("api/requests/{id}/employee/{empId}")
    Call<Request> updateRequest(
            @Path("id") Long id,
            @Path("empId") Long empId,
            @Body RequestDto.CreateRequestBody req);

    @DELETE("api/requests/{id}/employee/{empId}")
    Call<Void> cancelRequest(@Path("id") Long id, @Path("empId") Long empId);

    @GET("api/requests/department/{deptId}/pending")
    Call<List<Request>> getPendingRequests(
            @Path("deptId") Long deptId);

    @PUT("api/requests/{id}/review/employee/{reviewerId}")
    Call<Request> reviewRequest(
            @Path("id") Long id,
            @Path("reviewerId") Long reviewerId,
            @Body RequestDto.ReviewRequestBody req);

    @GET("api/requests")
    Call<List<Request>> getAllRequests();

    // ── NOTIFICATION ──────────────────────────────────────────────
    @GET("api/notifications/department/{deptId}")
    Call<List<Notification>> getNotifications(
            @Path("deptId") Long deptId,
            @Query("userId") Long userId);

    @POST("api/notifications")
    Call<Notification> createNotification(
            @Body NotificationDto.CreateNotificationRequest req);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotiRead(@Path("id") Long notiId, @Query("userId") Long userId);

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(@Path("id") Long id);

    @PUT("api/notifications/{id}")
    Call<Notification> updateNotification(
            @Path("id") Long id,
            @Body NotificationDto.UpdateNotificationRequest req);

    @GET("api/notifications/unread-count")
    Call<Long> getUnreadCount(@Query("userId") Long userId);

    // ── COMPANY SETTINGS (Admin) ──────────────────────────────────
    @GET("api/settings")
    Call<CompanySettings> getCompanySettings();

    @PUT("api/settings")
    Call<CompanySettings> updateCompanySettings(
            @Body AdminDto.UpdateSettingsRequest req);

    // ── SYSTEM LOG (Admin) ────────────────────────────────────────
    @GET("api/admin/logs")
    Call<List<SystemLog>> getSystemLogs();

    @GET("api/admin/logs/filter")
    Call<List<SystemLog>> filterLogsByAction(
            @Query("action") String action);

    // ── ADMIN DASHBOARD STATS (MỚI) ──────────────────────────────
    @GET("api/admin/dashboard/stats")
    Call<Map<String, Object>> getDashboardStats();

    // ── EMPLOYEE RESIGN CHECK (MỚI) ──────────────────────────────
    @GET("api/employees/{id}/resign-check")
    Call<Map<String, Object>> checkResignImpact(@Path("id") Long empId);

    // ── EMPLOYEE RESIGN CASCADE (CẬP NHẬT - giờ trả JSON) ───────
    // Method resignEmployee() hiện có vẫn hoạt động vì chỉ check isSuccessful()
    // Nhưng nếu muốn đọc kết quả chi tiết, dùng method mới này:
    @PUT("api/employees/{id}/resign")
    Call<Map<String, Object>> resignEmployeeWithDetails(@Path("id") Long id);

    // ── BACKUP/RESTORE (MỚI) ─────────────────────────────────────
    @POST("api/admin/backup")
    Call<Map<String, Object>> createBackup();

    @GET("api/admin/backup/list")
    Call<List<Map<String, Object>>> listBackups();

    @POST("api/admin/backup/{filename}/restore")
    Call<Map<String, Object>> restoreBackup(@Path("filename") String filename);

    @DELETE("api/admin/backup/{filename}")
    Call<Void> deleteBackup(@Path("filename") String filename);

    @GET("api/admin/backup/status")
    Call<Map<String, Object>> getBackupStatus();

    // ── ATTENDANCE (cho cross-ref chi tiết NV) ───────────────────
    // Đã có: getAttendanceByMonth — dùng lại cho tab chấm công
    @GET("api/attendance/employee/{empId}/month")
    Call<List<Map<String, Object>>> getAttendanceByMonth(
            @Path("empId") Long empId,
            @Query("month") int month,
            @Query("year") int year);

}