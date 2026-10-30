package com.financetracker.views;

import com.financetracker.model.SavingMovement;
import com.financetracker.service.SavingMetrics;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class SavingMetricsViewTest {
    @Test
    void comparesMonthlyActualWithTargetsIncludingFuturePlans() throws Exception {
        var movement = new SavingMovement();
        movement.setDate(LocalDate.of(2026, 1, 1));
        movement.setAmount(new BigDecimal("-500"));
        var today = LocalDate.of(2026, 2, 15);
        String svg = SavingMetricsView.svg(SavingMetrics.monthly(List.of(movement), 2026, today),
                Map.of(1, new BigDecimal("1000"), 12, new BigDecimal("2000")), today);
        assertThat(svg).contains("2026-01 actual: " + MoneyFormat.kes(new BigDecimal("-500")), "2026-12 expected: " + MoneyFormat.kes(new BigDecimal("2000")), "Today")
                .doesNotContain("2026-12 actual:", "NaN", "Infinity");
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void emptyPlanStillRendersFiniteAxes() {
        assertThat(SavingMetricsView.svg(SavingMetrics.monthly(List.of(), 2026, LocalDate.of(2026, 1, 1)),
                Map.of(), LocalDate.of(2026, 1, 1))).doesNotContain("NaN", "Infinity");
    }
}
