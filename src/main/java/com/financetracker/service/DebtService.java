package com.financetracker.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financetracker.model.Debt;
import com.financetracker.repository.DebtRepository;

@Service
public class DebtService {

	private final DebtRepository repository;

	public DebtService(DebtRepository repository) {
		this.repository = repository;
	}

	public List<Debt> findAll() {
		return repository.findAll();
	}

	/** Highest interest first (Avalanche). */
	public Optional<Debt> avalancheFocus() {
		return repository.findAll().stream()
				.filter(debt -> debt.getBalance() != null && debt.getBalance().signum() > 0)
				.max(Comparator.comparing(
						debt -> debt.getInterestRate() == null ? BigDecimal.ZERO : debt.getInterestRate()));
	}

	@Transactional
	public Debt save(Debt debt) {
		return repository.save(debt);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
