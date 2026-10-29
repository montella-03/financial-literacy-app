package com.financetracker.repository;

import com.financetracker.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    java.util.List<Bill> findByOwnerId(String ownerId);
}
