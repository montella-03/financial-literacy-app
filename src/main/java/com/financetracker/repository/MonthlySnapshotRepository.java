package com.financetracker.repository;

import com.financetracker.model.MonthlySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlySnapshotRepository extends JpaRepository<MonthlySnapshot, Long> {
    java.util.List<MonthlySnapshot> findByOwnerId(String ownerId);
}
