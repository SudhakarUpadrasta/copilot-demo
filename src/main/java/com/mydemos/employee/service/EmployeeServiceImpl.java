package com.mydemos.employee.service;

import com.mydemos.employee.dto.EmployeeRequest;
import com.mydemos.employee.dto.EmployeeResponse;
import com.mydemos.employee.entity.Employee;
import com.mydemos.employee.exception.ResourceNotFoundException;
import com.mydemos.employee.repository.EmployeeRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        return EmployeeResponse.from(findEmployeeByIdOrThrow(id));
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Employee with email already exists: " + request.getEmail());
        }

        Employee employee = new Employee(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getDepartment(),
                request.getPosition(),
                request.getSalary(),
                request.getActive()
        );

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findEmployeeByIdOrThrow(id);

        if (!Objects.equals(employee.getEmail(), request.getEmail()) && employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Employee with email already exists: " + request.getEmail());
        }

        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setSalary(request.getSalary());
        employee.setActive(request.getActive());

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployeeByIdOrThrow(id);
        employeeRepository.delete(employee);
    }

    private Employee findEmployeeByIdOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }
}
