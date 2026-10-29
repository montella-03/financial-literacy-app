package com.financetracker.service;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import com.financetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<Transaction> findAll() {
        return repository.findByOwnerId(WorkspaceAccess.owner()).stream().sorted(java.util.Comparator.comparing(Transaction::getTransactionDate).reversed()).toList();
    }

    @Transactional
    public Transaction save(Transaction transaction) {
        WorkspaceAccess.prepare(transaction, repository);
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }
        validate(transaction);
        return repository.save(transaction);
    }

    public void validate(Transaction t) {
        if (t.getType() == null || t.getAmount() == null || t.getAmount().signum() <= 0 || t.getAmount().scale() > 2 || t.getAmount().precision() > 17)
            throw new IllegalArgumentException("Enter a positive amount with at most two decimal places.");
        if (t.getTransactionDate() == null || t.getTransactionDate().toLocalDate().isAfter(java.time.LocalDate.now()))
            throw new IllegalArgumentException("Transaction date cannot be in the future.");
        for (String field : new String[]{t.getCategory(), t.getMerchant(), t.getAccountName(), t.getDestinationAccount()})
            if (field != null && field.length() > 255)
                throw new IllegalArgumentException("Fields must be 255 characters or fewer.");
        if (t.getDescription() != null && t.getDescription().length() > 500)
            throw new IllegalArgumentException("Description must be 500 characters or fewer.");
        if (t.getType() == TransactionType.TRANSFER && (t.getAccountName() == null || t.getAccountName().isBlank() || t.getDestinationAccount() == null || t.getDestinationAccount().isBlank() || t.getAccountName().equalsIgnoreCase(t.getDestinationAccount())))
            throw new IllegalArgumentException("Transfers need two different accounts.");
    }

    @Transactional
    public void delete(Long id) {
        WorkspaceAccess.delete(id, repository);
    }

    public BigDecimal sumIncome(YearMonth month) {
        return sum(TransactionType.INCOME, month);
    }

    public BigDecimal sumExpenses(YearMonth month) {
        return sum(TransactionType.EXPENSE, month);
    }

    public BigDecimal surplus(YearMonth month) {
        return sumIncome(month).subtract(sumExpenses(month));
    }

    private BigDecimal sum(TransactionType type, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal total = findAll().stream().filter(t -> t.getType() == type && !t.getTransactionDate().isBefore(start) && t.getTransactionDate().isBefore(end)).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total == null ? BigDecimal.ZERO : total;
    }
}
