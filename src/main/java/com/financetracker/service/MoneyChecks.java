package com.financetracker.service;

import java.math.BigDecimal;

public final class MoneyChecks {
    private MoneyChecks() {
    }

    public static void positive(BigDecimal value) {
        nonnegative(value);
        if (value.signum() == 0) throw new IllegalArgumentException("Amount must be positive.");
    }

    public static void nonnegative(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.scale() > 2 || value.precision() > 17)
            throw new IllegalArgumentException("Use a nonnegative amount with at most two decimal places.");
    }
}
