package com.alahlia.actionsheet.repository;

import com.alahlia.actionsheet.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByActiveTrueOrderByNameAsc();

    List<Employee> findByRole(String role);

    @Query("SELECT e FROM Employee e WHERE e.active = true AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.department) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Employee> searchByKeyword(@Param("keyword") String keyword);

    List<Employee> findByBossEmail(String bossEmail);
}
