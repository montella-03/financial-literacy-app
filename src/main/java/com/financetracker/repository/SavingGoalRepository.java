package com.financetracker.repository;

import com.financetracker.model.SavingGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingGoalRepository extends JpaRepository<SavingGoal, Long> {
    List<SavingGoal> findByOwnerId(String ownerId);
}
