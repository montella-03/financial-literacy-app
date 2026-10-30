package com.financetracker.service;

import com.financetracker.model.SavingTarget;
import com.financetracker.repository.SavingTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class SavingTargetService {
    private final SavingTargetRepository repository;
    public SavingTargetService(SavingTargetRepository repository) { this.repository = repository; }
    public List<SavingTarget> findAll() { return repository.findByOwnerId(WorkspaceAccess.owner()); }

    @Transactional
    public void saveYear(int year, Map<Integer, BigDecimal> amounts) {
        if (year < 1900 || year > 9999) throw new IllegalArgumentException("Choose a valid year.");
        amounts.forEach((month, amount) -> {
            YearMonth.of(year, month);
            if (amount != null) MoneyChecks.nonnegative(amount);
        });
        var existing = findAll();
        amounts.forEach((month, amount) -> {
            String key = YearMonth.of(year, month).toString();
            SavingTarget target = existing.stream().filter(t -> key.equals(t.getMonth())).findFirst().orElse(null);
            if (amount == null) {
                if (target != null) repository.delete(target);
                return;
            }
            if (target == null) target = new SavingTarget();
            target.setOwnerId(WorkspaceAccess.owner());
            target.setMonth(key);
            target.setAmount(amount);
            repository.save(target);
        });
    }
}
