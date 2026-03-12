package com.example.myapplication.network;

import com.example.myapplication.model.*;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ApiService {

    // ── AUTH (TV1) ─────────────────────────────
    @GET("api/employees/profile")
    Call<Employee> getMyProfile();

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
}