package com.financetracker.repository;

import com.financetracker.model.SavingMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingMovementRepository extends JpaRepository<SavingMovement, Long> {
    java.util.List<SavingMovement> findByOwnerId(String ownerId);
}
