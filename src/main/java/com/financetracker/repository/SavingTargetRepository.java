package com.financetracker.repository;

import com.financetracker.model.SavingTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingTargetRepository extends JpaRepository<SavingTarget, Long> {
    List<SavingTarget> findByOwnerId(String ownerId);
}
