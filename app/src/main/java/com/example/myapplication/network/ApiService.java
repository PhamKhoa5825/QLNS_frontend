package com.example.myapplication.network;

import com.example.myapplication.model.*;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    // ── AUTH ─────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthenticationResponse> login(@Body AuthenticationRequest request);

    @GET("api/auth/validate")
    Call<String> validateToken();

    @POST("api/auth/change-password")
    Call<Void> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/logout")
    Call<Void> logout();

    @GET("api/employees/{id}/detail")
    Call<Employee> getMyProfile(@Path("id") Long id);

    @GET("api/employees/{id}/summary")
    Call<EmployeeSummary> getEmployeeSummary(@Path("id") Long id);

    @POST("api/requests/employee/{empId}")
    Call<Request> createRequest(
            @Path("empId") Long empId,
            @Body CreateRequestRequest request
    );

    // ── ATTENDANCE ───────────────────────────────
    @POST("api/attendance/checkin")
    Call<Attendance> checkIn(@Body TimekeepingRequest data);

    @PUT("api/attendance/checkout")
    Call<Attendance> checkOut(@Body TimekeepingRequest data);

    @GET("api/attendance/employee/{empId}/today")
    Call<Attendance> getTodayAttendance(@Path("empId") Long empId);

    @GET("api/attendance/employee/{id}/stats")
    Call<AttendanceStats> getAttendanceStats(
            @Path("id") Long id,
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("api/attendance/employee/{id}/month")
    Call<List<AttendanceRecord>> getAttendanceMonthly(
            @Path("id") Long id,
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("api/attendance/employee/{id}/summary")
    Call<AttendanceMonthlyResponse> getAttendanceSummary(
            @Path("id") Long id,
            @Query("month") int month,
            @Query("year") int year
    );

    // Được dùng bởi TimekeepingActivity (trả về AttendanceStatsResponse có onTime/late/absent)
    @GET("api/attendance/employee/{id}/stats")
    Call<AttendanceStatsResponse> getEmployeeAttendanceStats(
            @Path("id") Long id,
            @Query("month") int month,
            @Query("year") int year
    );

    // Lịch sử chấm công của nhân viên
    @GET("api/attendance/employee/{id}/history")
    Call<List<Attendance>> getEmployeeAttendanceHistory(@Path("id") Long id);

    // Thống kê chấm công theo phòng ban
    @GET("api/attendance/department/{deptId}/stats")
    Call<List<EmployeeAttendanceStats>> getDepartmentAttendanceStats(
            @Path("deptId") Long deptId,
            @Query("month") int month,
            @Query("year") int year
    );

    // Dữ liệu chấm công theo tháng (dùng bởi RequestActivity)
    @GET("api/attendance/employee/{id}/month-list")
    Call<List<Attendance>> getAttendanceByMonth(
            @Path("id") Long id,
            @Query("month") Integer month,
            @Query("year") Integer year
    );

    // ── DEPARTMENT ────────────────────────────────
    @GET("api/departments")
    Call<List<Department>> getDepartments();

    @GET("api/departments/{id}")
    Call<Department> getDepartmentById(@Path("id") Long id);

    @GET("api/departments/{id}/dashboard")
    Call<DepartmentDashboardDTO> getDepartmentDashboard(@Path("id") Long id);

    @GET("api/departments/{id}/employees")
    Call<List<Employee>> getEmployeesByDepartment(@Path("id") Long deptId);

    @POST("api/departments")
    Call<Department> createDepartment(@Body Department department);

    @PUT("api/departments/{id}")
    Call<Department> updateDepartment(@Path("id") Long id, @Body Department department);

    @DELETE("api/departments/{id}")
    Call<Void> deleteDepartment(@Path("id") Long id);

    // ── EMPLOYEE ─────────────────────────────────
    @GET("api/employees")
    Call<List<Employee>> getEmployees();

    @GET("api/employees/{id}")
    Call<Employee> getEmployeeById(@Path("id") Long id);

    @GET("api/employees/{id}/detail")
    Call<Employee> getEmployeeDetail(@Path("id") Long id);

    @PUT("api/employees/{id}")
    Call<Employee> updateEmployeeProfile(@Path("id") Long id, @Body Map<String, Object> data);

    @GET("api/employees/department/{deptId}")
    Call<List<Employee>> getEmployeesByDepartmentId(@Path("deptId") Long deptId);

    @GET("api/employees/search")
    Call<List<Employee>> searchEmployees(@Query("keyword") String keyword);

    @POST("api/employees")
    Call<Employee> createEmployee(@Body CreateEmployeeRequest request);

    @PUT("api/employees/{id}")
    @Headers("Content-Type: application/json")
    Call<Employee> updateEmployee(@Path("id") Long id, @Body CreateEmployeeRequest request);

    @PUT("api/employees/{id}/resign")
    Call<Void> resignEmployee(@Path("id") Long id);

    @GET("api/employees/{id}/salary")
    Call<Double> getBaseSalary(@Path("id") Long id);

    @PUT("api/employees/{id}/salary")
    Call<Void> updateEmployeeBaseSalary(@Path("id") Long id, @Body Map<String, Double> data);

    // ── TASKS ────────────────────────────────────
    @GET("api/tasks/my/{empId}")
    Call<List<Task>> getMyTasks(@Path("empId") Long empId);

    @GET("api/tasks/all")
    Call<List<Task>> getAllTasks();

    @GET("api/tasks/department/{deptId}")
    Call<List<Task>> getTasksByDepartment(@Path("deptId") Long deptId);

    @POST("api/tasks")
    Call<Task> createTask(@Body CreateTaskRequest request);

    @PUT("api/tasks/{id}/status")
    Call<Task> updateTaskStatus(
            @Path("id") Long id,
            @Body UpdateTaskStatusRequest request,
            @Query("updatedById") Long updatedById
    );

    @PUT("api/tasks/{id}/accept")
    Call<Task> acceptTask(@Path("id") Long id, @Body Map<String, Long> body);

    // ── NOTIFICATIONS ────────────────────────────
    @GET("api/notifications/department/{deptId}")
    Call<List<Notification>> getNotifications(@Path("deptId") Long deptId, @Query("userId") Long userId);

    @GET("api/notifications/all")
    Call<List<Notification>> getAllNotifications();

    @POST("api/notifications")
    Call<Notification> createNotification(@Body CreateNotificationRequest request);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotificationAsRead(@Path("id") Long id, @Query("userId") Long userId);

    // ── REQUESTS ─────────────────────────────────
    @GET("api/requests/employee/{empId}")
    Call<List<Request>> getMyRequests(
            @Path("empId") Long empId,
            @Query("month") Integer month,
            @Query("year") Integer year
    );

    @GET("api/requests/department/{deptId}")
    Call<List<Request>> getRequestsByDepartmentAndStatus(
            @Path("deptId") Long deptId,
            @Query("status") String status,
            @Query("month") Integer month,
            @Query("year") Integer year,
            @Query("empId") Long empId
    );

    @GET("api/requests/status")
    Call<List<Request>> getAllRequestsByStatus(
            @Query("status") String status,
            @Query("month") Integer month,
            @Query("year") Integer year,
            @Query("deptId") Long deptId,
            @Query("empId") Long empId
    );

    @PUT("api/requests/{id}/cancel")
    Call<Void> cancelRequest(@Path("id") Long id, @Query("empId") Long empId);

    @PUT("api/requests/{id}/status")
    Call<Request> updateRequestStatus(@Path("id") Long id, @Query("status") String status);

    // ── FILE UPLOAD ──────────────────────────────
    @Multipart
    @POST("api/upload/image")
    Call<Map<String, String>> uploadImage(@Part MultipartBody.Part file);

    // ── PAYROLL ──────────────────────────────────
    @GET("api/payroll/all/summary")
    Call<List<SalaryRecord>> getAllPayrollSummary(
            @Query("month") int month,
            @Query("year") int year
    );

    @POST("api/payroll/generate")
    Call<List<SalaryRecord>> generateAllPayroll(
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("api/payroll/me")
    Call<SalaryRecord> getMyPayroll(
            @Query("empId") Long empId,
            @Query("month") int month,
            @Query("year") int year
    );

    @PUT("api/payroll/{id}/finalize")
    Call<SalaryRecord> finalizePayroll(@Path("id") Long id);

    // ── SETTINGS ─────────────────────────────────
    @GET("api/settings/company")
    Call<CompanySettings> getCompanySettings();

    @PUT("api/settings/company")
    Call<CompanySettings> updateCompanySettings(@Body CompanySettings settings);

    // ── ADMIN EXTRAS ─────────────────────────────
    @PUT("api/admin/accounts/{userId}/reset-password")
    Call<Void> resetPassword(@Path("userId") Long userId, @Query("newPassword") String newPassword);

    @GET("api/admin/logs")
    Call<List<SystemLog>> getSystemLogs();

    @GET("api/admin/logs/filter")
    Call<List<SystemLog>> filterLogsByAction(@Query("action") String action);

    @POST("api/admin/backup")
    Call<Map<String, Object>> createBackup();

    @GET("api/admin/backup/list")
    Call<List<Map<String, Object>>> listBackups();

    @POST("api/admin/backup/{filename}/restore")
    Call<Map<String, Object>> restoreBackup(@Path("filename") String filename);

    @GET("api/admin/backup/status")
    Call<Map<String, Object>> getBackupStatus();

    @DELETE("api/admin/backup/{filename}")
    Call<Void> deleteBackup(@Path("filename") String filename);

    @PUT("api/admin/accounts/{userId}/status")
    Call<Void> updateAccountStatus(@Path("userId") Long userId, @Query("status") String status);

    @PUT("api/admin/employees/{id}/role")
    Call<Employee> updateEmployeeRole(@Path("id") Long id, @Body AccountDto.UpdateRoleRequest request);

    @GET("api/admin/dashboard/stats")
    Call<Map<String, Object>> getAdminDashboardStats();

    @GET("api/employees/{id}/resign-check")
    Call<Map<String, Object>> checkResignImpact(@Path("id") Long id);

    @PUT("api/employees/{id}/reactivate")
    Call<Void> reactivateEmployee(@Path("id") Long id);

    @PUT("api/requests/{id}/review/employee/{reviewerId}")
    Call<Request> reviewRequest(
            @Path("id") Long id,
            @Path("reviewerId") Long reviewerId,
            @Body ReviewRequestRequest body
    );
}
