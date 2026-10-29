package com.financetracker.repository;

import com.financetracker.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    java.util.List<Debt> findByOwnerId(String ownerId);

}
