package com.example.myapplication.network;

import com.example.myapplication.model.*;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── AUTHENTICATION ───────────────────────────
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @GET("api/auth/validate")
    Call<String> validateToken();

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

    @GET("api/requests/employee/{empId}")
    Call<List<Request>> getMyRequests(@Path("empId") Long empId);

    @POST("api/requests/employee/{empId}")
    Call<Request> createRequest(@Path("empId") Long empId, @Body CreateRequestRequest request);

    @GET("api/requests")
    Call<List<Request>> getAllRequests();

    @GET("api/requests/status")
    Call<List<Request>> getAllRequestsByStatus(@Query("status") String status);

    @PUT("api/requests/{id}/status")
    Call<Request> updateRequestStatus(@Path("id") Long id, @Query("status") String status);

    // ── TASKS ────────────────────────────────────
    @GET("api/tasks/my/{empId}")
    Call<List<Task>> getMyTasks(@Path("empId") Long empId);

    @GET("api/tasks/department/{deptId}")
    Call<List<Task>> getTasksByDepartment(@Path("deptId") Long deptId);

    @GET("api/tasks")
    Call<List<Task>> getAllTasks();

    @POST("api/tasks")
    Call<Task> createTask(@Body CreateTaskRequest request);

    @PUT("api/tasks/{id}")
    Call<Task> updateTask(@Path("id") Long id, @Body Task task);

    @PUT("api/tasks/{id}/status")
    Call<Task> updateTaskStatus(@Path("id") Long id, @Body UpdateTaskStatusRequest request, @Query("updatedById") Long updatedById);

    @PUT("api/tasks/{id}/accept")
    Call<Task> acceptTask(@Path("id") Long id, @Body java.util.Map<String, Long> body);

    // ── TIMEKEEPING ──────────────────────────────
    @GET("api/attendance/department/{deptId}/date")
    Call<List<Attendance>> getDepartmentAttendanceByDate(@Path("deptId") Long deptId, @Query("date") String date);

    // ── CHAT ─────────────────────────────────────
    @POST("api/chat/rooms/group")
    Call<ChatRoom> createDepartmentGroupChat(@Query("name") String name, @Query("departmentId") Long deptId, @Query("creatorId") Long creatorId);

    // ── NOTIFICATIONS ────────────────────────────
    @GET("api/notifications/department/{deptId}")
    Call<List<Notification>> getNotifications(@Path("deptId") Long deptId, @Query("userId") Long userId);

    @GET("api/notifications")
    Call<List<Notification>> getAllNotifications();

    @POST("api/notifications")
    Call<Notification> createNotification(@Body CreateNotificationRequest notification);

    @PUT("api/notifications/{id}/read")
    Call<Void> markNotificationAsRead(@Path("id") Long id, @Query("userId") Long userId);

    // --- Attendance API ---
    @POST("/api/attendance/checkin")
    Call<Attendance> checkIn(@Body TimekeepingRequest request);

    @PUT("/api/attendance/checkout")
    Call<Attendance> checkOut(@Body TimekeepingRequest request);

    @GET("/api/attendance/employee/{empId}")
    Call<List<Attendance>> getEmployeeAttendanceHistory(@Path("empId") Long empId);

    @GET("/api/attendance/employee/{empId}/statistics")
    Call<AttendanceStatsResponse> getEmployeeAttendanceStats(
            @Path("empId") Long empId,
            @Query("month") int month,
            @Query("year") int year
    );

    @GET("/api/attendance/department/{deptId}/statistics")
    Call<List<EmployeeAttendanceStats>> getDepartmentAttendanceStats(
            @Path("deptId") Long deptId,
            @Query("month") int month,
            @Query("year") int year
    );

    // --- Chat API ---
    @GET("/api/chat/rooms/user/{userId}")
    Call<List<ChatRoom>> getChatRooms(@Path("userId") Long userId);

    @POST("/api/chat/rooms/private")
    Call<ChatRoom> getOrCreatePrivateRoom(@Query("userId1") Long userId1, @Query("userId2") Long userId2);

    @GET("/api/chat/messages/{roomId}")
    Call<List<Message>> getMessages(@Path("roomId") Long roomId);

    @POST("/api/chat/messages")
    Call<Message> sendMessage(@Body SendMessageRequest request);
}