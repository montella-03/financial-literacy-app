package com.financetracker.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import com.financetracker.repository.TransactionRepository;

@Service
public class TransactionService {

	private final TransactionRepository repository;

	public TransactionService(TransactionRepository repository) {
		this.repository = repository;
	}

	public List<Transaction> findAll() {
		return repository.findAllByOrderByTransactionDateDesc();
	}

	@Transactional
	public Transaction save(Transaction transaction) {
		if (transaction.getCreatedAt() == null) {
			transaction.setCreatedAt(LocalDateTime.now());
		}
		if (transaction.getTransactionDate() == null) {
			transaction.setTransactionDate(LocalDateTime.now());
		}
		return repository.save(transaction);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
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
		BigDecimal total = repository.sumByTypeAndDateRange(type, start, end);
		return total == null ? BigDecimal.ZERO : total;
	}
}
