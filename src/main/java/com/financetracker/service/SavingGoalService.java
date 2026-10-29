package com.financetracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financetracker.model.SavingGoal;
import com.financetracker.repository.SavingGoalRepository;

@Service
public class SavingGoalService {

	private final SavingGoalRepository repository;

	public SavingGoalService(SavingGoalRepository repository) {
		this.repository = repository;
	}

	public List<SavingGoal> findAll() {
		return repository.findAll();
	}

	public List<SavingGoal> findActive() {
		return repository.findAll().stream()
				.filter(goal -> !goal.isCompleted())
				.toList();
	}

	@Transactional
	public SavingGoal save(SavingGoal goal) {
		if (goal.getCurrentAmount() == null) {
			goal.setCurrentAmount(BigDecimal.ZERO);
		}
		if (goal.getPriority() == null || goal.getPriority().isBlank()) {
			goal.setPriority("MEDIUM");
		}
		if (goal.getTargetAmount() != null
				&& goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
			goal.setCompleted(true);
		}
		return repository.save(goal);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
