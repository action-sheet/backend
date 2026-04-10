package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.Employee;
import com.alahlia.actionsheet.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findByActiveTrueOrderByNameAsc();
    }

    public Employee getEmployee(String email) {
        return employeeRepository.findById(email).orElse(null);
    }

    public List<Employee> searchEmployees(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEmployees();
        }
        return employeeRepository.searchByKeyword(keyword);
    }

    public Employee createEmployee(Employee employee) {
        employee.setActive(true);
        if (employee.getHierarchyLevel() == null) {
            employee.setHierarchyLevel(5); // Default level
        }
        
        log.info("Creating employee: {} - {}", employee.getEmail(), employee.getName());
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(String email, Employee updatedEmployee) {
        Employee existing = employeeRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + email));
        
        existing.setName(updatedEmployee.getName());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setPosition(updatedEmployee.getPosition());
        existing.setRole(updatedEmployee.getRole());
        existing.setHierarchyLevel(updatedEmployee.getHierarchyLevel());
        existing.setBossEmail(updatedEmployee.getBossEmail());
        
        log.info("Updated employee: {}", email);
        return employeeRepository.save(existing);
    }

    public boolean deleteEmployee(String email) {
        Employee employee = employeeRepository.findById(email).orElse(null);
        if (employee != null) {
            employee.setActive(false);
            employeeRepository.save(employee);
            log.info("Deactivated employee: {}", email);
            return true;
        }
        return false;
    }
}
