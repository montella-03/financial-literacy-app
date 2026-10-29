package com.financetracker.service;

import com.financetracker.model.Debt;
import com.financetracker.repository.DebtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DebtService {

    private final DebtRepository repository;
    private final com.financetracker.repository.BillRepository bills;

    public DebtService(DebtRepository repository, com.financetracker.repository.BillRepository bills) {
        this.repository = repository;
        this.bills = bills;
    }

    public List<Debt> findAll() {
        return repository.findByOwnerId(WorkspaceAccess.owner());
    }

    /**
     * Highest interest first (Avalanche).
     */
    public Optional<Debt> avalancheFocus() {
        return repository.findByOwnerId(WorkspaceAccess.owner()).stream()
                .filter(debt -> debt.getBalance() != null && debt.getBalance().signum() > 0)
                .max(Comparator.comparing(
                        debt -> debt.getInterestRate() == null ? BigDecimal.ZERO : debt.getInterestRate()));
    }

    @Transactional
    public Debt save(Debt debt) {
        WorkspaceAccess.prepare(debt, repository);
        return repository.save(debt);
    }

    @Transactional
    public void delete(Long id) {
        if (bills.findByOwnerId(WorkspaceAccess.owner()).stream().anyMatch(b -> id.equals(b.getDebtId()) && b.getPaidOn() == null))
            throw new IllegalArgumentException("Remove or unlink unpaid bills for this debt before deleting it.");
        WorkspaceAccess.delete(id, repository);
    }
}
