package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.Employee;
import com.alahlia.actionsheet.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<Employee> getAllEmployees(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return employeeService.searchEmployees(search);
        }
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{email}")
    public ResponseEntity<Employee> getEmployee(@PathVariable String email) {
        Employee employee = employeeService.getEmployee(email);
        return employee != null ? ResponseEntity.ok(employee) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.createEmployee(employee);
    }

    @PutMapping("/{email}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable String email, @RequestBody Employee employee) {
        Employee updated = employeeService.updateEmployee(email, employee);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String email) {
        boolean deleted = employeeService.deleteEmployee(email);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
