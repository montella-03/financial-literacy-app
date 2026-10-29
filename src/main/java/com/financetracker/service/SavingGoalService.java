package com.financetracker.service;

import com.financetracker.model.SavingGoal;
import com.financetracker.repository.SavingGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SavingGoalService {

    private final SavingGoalRepository repository;
    private final com.financetracker.repository.SavingMovementRepository movements;

    public SavingGoalService(SavingGoalRepository repository, com.financetracker.repository.SavingMovementRepository movements) {
        this.repository = repository;
        this.movements = movements;
    }

    public List<SavingGoal> findAll() {
        return repository.findByOwnerId(WorkspaceAccess.owner());
    }

    public List<SavingGoal> findActive() {
        return repository.findByOwnerId(WorkspaceAccess.owner()).stream()
                .filter(goal -> !goal.isCompleted())
                .toList();
    }

    @Transactional
    public SavingGoal save(SavingGoal goal) {
        WorkspaceAccess.prepare(goal, repository);
        MoneyChecks.positive(goal.getTargetAmount());
        if (goal.getName() == null || goal.getName().isBlank())
            throw new IllegalArgumentException("A goal needs a name.");
        if (goal.getCurrentAmount() == null) goal.setCurrentAmount(BigDecimal.ZERO);
        MoneyChecks.nonnegative(goal.getCurrentAmount());
        SavingGoal stored = goal.getId() == null ? null : repository.findById(goal.getId()).orElseThrow();
        if (stored == null || stored.getStartDate() == null) {
            goal.setStartDate(java.time.LocalDate.now());
            goal.setOpeningAmount(stored == null ? goal.getCurrentAmount() : stored.getCurrentAmount());
        } else {
            goal.setStartDate(stored.getStartDate());
            goal.setOpeningAmount(stored.getOpeningAmount());
            if (goal.getCurrentAmount().compareTo(stored.getCurrentAmount()) != 0)
                throw new IllegalArgumentException("Use a deposit or withdrawal to change savings.");
        }
        if (goal.getPriority() == null) goal.setPriority("MEDIUM");
        goal.setCompleted(goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0);
        SavingGoal saved = repository.save(goal);
        if (stored != null && stored.getStartDate() == null && saved.getCurrentAmount().compareTo(saved.getOpeningAmount()) != 0) {
            record(saved, saved.getCurrentAmount().subtract(saved.getOpeningAmount()), java.time.LocalDate.now(), "Balance adjustment");
        }
        return saved;
    }

    public List<com.financetracker.model.SavingMovement> history(Long goalId) {
        ownedGoal(goalId);
        return movements.findByOwnerId(WorkspaceAccess.owner()).stream().filter(m -> goalId.equals(m.getGoalId()))
                .sorted(java.util.Comparator.comparing(com.financetracker.model.SavingMovement::getDate).thenComparing(com.financetracker.model.SavingMovement::getId)).toList();
    }

    private SavingGoal ownedGoal(Long id) {
        SavingGoal goal = repository.findById(id).orElseThrow();
        WorkspaceAccess.prepare(goal, repository);
        return goal;
    }

    @Transactional
    public void contribute(Long id, BigDecimal amount, java.time.LocalDate date, String note) {
        if (amount == null || amount.signum() == 0) throw new IllegalArgumentException("Enter a nonzero movement.");
        MoneyChecks.positive(amount.abs());
        SavingGoal goal = ownedGoal(id);
        if (goal.getStartDate() == null) {
            goal.setStartDate(java.time.LocalDate.now());
            goal.setOpeningAmount(goal.getCurrentAmount());
        }
        if (date == null || date.isAfter(java.time.LocalDate.now()) || date.isBefore(goal.getStartDate()))
            throw new IllegalArgumentException("Choose a date between the start of tracking and today.");
        var history = history(id);
        BigDecimal running = goal.getOpeningAmount();
        java.util.TreeMap<java.time.LocalDate, BigDecimal> days = new java.util.TreeMap<>();
        history.forEach(m -> days.merge(m.getDate(), m.getAmount(), BigDecimal::add));
        days.merge(date, amount, BigDecimal::add);
        for (var delta : days.values()) {
            running = running.add(delta);
            if (running.signum() < 0)
                throw new IllegalArgumentException("Withdrawal would make historical savings negative.");
        }
        goal.setCurrentAmount(running);
        goal.setCompleted(running.compareTo(goal.getTargetAmount()) >= 0);
        repository.save(goal);
        record(goal, amount, date, note);
    }

    private void record(SavingGoal goal, BigDecimal amount, java.time.LocalDate date, String note) {
        var movement = new com.financetracker.model.SavingMovement();
        movement.setOwnerId(WorkspaceAccess.owner());
        movement.setGoalId(goal.getId());
        movement.setDate(date);
        movement.setAmount(amount);
        movement.setNote(note == null ? "" : note.substring(0, Math.min(255, note.length())));
        movements.save(movement);
    }

    @Transactional
    public void delete(Long id) {
        ownedGoal(id);
        movements.deleteAll(history(id));
        WorkspaceAccess.delete(id, repository);
    }
}
