package com.financetracker.views;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {

    private static final Locale KENYA = Locale.of("en", "KE");

    private MoneyFormat() {
    }

    public static String kes(BigDecimal amount) {
        if (amount == null) {
            return "Ksh 0";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(KENYA);
        format.setCurrency(java.util.Currency.getInstance("KES"));
        return format.format(amount);
    }

    public static String kesSigned(BigDecimal amount) {
        if (amount == null) {
            return kes(BigDecimal.ZERO);
        }
        String formatted = kes(amount.abs());
        if (amount.signum() < 0) {
            return "−" + formatted;
        }
        if (amount.signum() > 0) {
            return "+" + formatted;
        }
        return formatted;
    }
}
