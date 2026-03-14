package com.example.myapplication.network;

import com.example.myapplication.model.*;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {

    // ── AUTH ─────────────────────────────────────
    @POST("api/auth/login")
    Call<AuthenticationResponse> login(@Body AuthenticationRequest request);

    @POST("api/auth/change-password")
    Call<Void> changePassword(@Body Map<String, String> request);

    @GET("api/employees/profile")
    Call<Employee> getMyProfile();

    @GET("api/employees/{id}/summary")
    Call<EmployeeSummary> getEmployeeSummary(@Path("id") Long id);

    // ── ATTENDANCE ───────────────────────────────
    @POST("api/attendance/checkin")
    Call<Attendance> checkIn(@Body Map<String, Object> data);

    @PUT("api/attendance/checkout")
    Call<Attendance> checkOut(@Body Map<String, Object> data);

    @GET("api/attendance/today")
    Call<List<Attendance>> getTodayAttendance();

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

    // ── EMPLOYEE ───────────────────────────
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

    @PUT("api/employees/{id}/resign")
    Call<Void> resignEmployee(@Path("id") Long id);
}
