package com.mydemos.employee.service;

import com.mydemos.employee.dto.EmployeeRequest;
import com.mydemos.employee.dto.EmployeeResponse;
import java.util.List;

public interface EmployeeService {

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse getEmployeeById(Long id);

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}
