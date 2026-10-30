package com.financetracker.service;

import com.financetracker.model.SavingMovement;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SavingMetricsTest {
    @Test
    void aggregatesGoalsAndWithdrawalsAndResetsAtYearBoundary() {
        var months = SavingMetrics.monthly(List.of(
                movement(1L, "2025-12-31", "900"),
                movement(1L, "2026-01-01", "100.25"),
                movement(2L, "2026-01-20", "50.50"),
                movement(1L, "2026-02-01", "-70.10"),
                movement(2L, "2026-12-31", "20")), 2026, LocalDate.of(2026, 12, 31));
        assertThat(months).hasSize(12);
        assertThat(months.getFirst().net()).isEqualByComparingTo("150.75");
        assertThat(months.get(1).withdrawals()).isEqualByComparingTo("70.10");
        assertThat(months.get(1).net()).isEqualByComparingTo("-70.10");
        assertThat(months.get(2).net()).isEqualByComparingTo("0");
        assertThat(months.get(2).cumulative()).isEqualByComparingTo("80.65");
        assertThat(months.getLast().cumulative()).isEqualByComparingTo("100.65");
    }

    @Test
    void currentMonthIsToDateAndFutureMonthsAreMarked() {
        var months = SavingMetrics.monthly(List.of(
                movement(1L, "2026-10-30", "10"),
                movement(1L, "2026-10-31", "100"),
                movement(1L, "2026-11-01", "200")), 2026, LocalDate.of(2026, 10, 30));
        assertThat(months.get(9).net()).isEqualByComparingTo("10");
        assertThat(months.get(9).future()).isFalse();
        assertThat(months.get(10).future()).isTrue();
        assertThat(months.get(10).net()).isZero();
    }

    @Test
    void emptyYearHasTwelveZeroMonths() {
        var months = SavingMetrics.monthly(List.of(), 2025, LocalDate.of(2026, 10, 30));
        assertThat(months).hasSize(12).allSatisfy(month -> {
            assertThat(month.net()).isZero();
            assertThat(month.cumulative()).isZero();
            assertThat(month.future()).isFalse();
        });
    }

    private SavingMovement movement(Long goal, String date, String amount) {
        SavingMovement movement = new SavingMovement();
        movement.setGoalId(goal);
        movement.setDate(LocalDate.parse(date));
        movement.setAmount(new BigDecimal(amount));
        return movement;
    }
}
