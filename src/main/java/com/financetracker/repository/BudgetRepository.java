package com.financetracker.repository;

import com.financetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    java.util.List<Budget> findByOwnerId(String ownerId);
}
