package com.financetracker.service;

import com.financetracker.model.SavingMovement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class SavingMetrics {
    private SavingMetrics() { }

    public record Month(YearMonth month, BigDecimal deposits, BigDecimal withdrawals,
                        BigDecimal net, BigDecimal cumulative, boolean future) { }

    public static List<Month> monthly(List<SavingMovement> movements, int year, LocalDate today) {
        List<Month> result = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            YearMonth month = YearMonth.of(year, m);
            BigDecimal deposits = BigDecimal.ZERO, withdrawals = BigDecimal.ZERO;
            for (SavingMovement movement : movements) {
                if (YearMonth.from(movement.getDate()).equals(month) && !movement.getDate().isAfter(today)) {
                    if (movement.getAmount().signum() > 0) deposits = deposits.add(movement.getAmount());
                    else withdrawals = withdrawals.subtract(movement.getAmount());
                }
            }
            BigDecimal net = deposits.subtract(withdrawals);
            cumulative = cumulative.add(net);
            result.add(new Month(month, deposits, withdrawals, net, cumulative, month.isAfter(YearMonth.from(today))));
        }
        return List.copyOf(result);
    }
}
