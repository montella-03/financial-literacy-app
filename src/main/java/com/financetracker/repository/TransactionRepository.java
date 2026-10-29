package com.financetracker.repository;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findBySourceEmailId(String sourceEmailId);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDateTime start, LocalDateTime end);

    List<Transaction> findAllByOrderByTransactionDateDesc();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDateRange(@Param("type") TransactionType type,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :start AND :end GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumExpensesByCategory(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
