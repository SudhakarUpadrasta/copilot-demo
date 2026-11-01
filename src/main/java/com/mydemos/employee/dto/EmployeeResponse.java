package com.mydemos.employee.dto;

import com.mydemos.employee.entity.Employee;
import java.time.LocalDateTime;

public record EmployeeResponse(Long id,
                               String firstName,
                               String lastName,
                               String email,
                               String department,
                               String position,
                               Double salary,
                               Boolean active,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getSalary(),
                employee.getActive(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
