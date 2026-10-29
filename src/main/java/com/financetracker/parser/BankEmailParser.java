package com.financetracker.parser;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses common bank email / SMS notification formats.
 * Extend the patterns for your specific banks (M-Pesa, Equity, KCB, Absa, etc.).
 */
@Component
public class BankEmailParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:KES|Ksh|KSH|USD|\\$|EUR|€|GBP|£|R|ZAR|NGN|GHS)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DEBIT_KEYWORDS = Pattern.compile(
            "(debited|withdrawn|purchase|spent|paid|payment|transfer to|sent|ATM|POS|bought)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CREDIT_KEYWORDS = Pattern.compile(
            "(credited|received|deposit|salary|refund|incoming|transferred from|you have received)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?:at|to|from|merchant|for)\\s+([A-Za-z0-9 &.'-]{3,50})",
            Pattern.CASE_INSENSITIVE);

    public Optional<Transaction> parse(String subject, String body, String messageId) {
        String text = ((subject != null ? subject : "") + " " + (body != null ? body : ""))
                .replaceAll("\\s+", " ").trim();

        if (text.isBlank()) {
            return Optional.empty();
        }

        TransactionType type;
        if (CREDIT_KEYWORDS.matcher(text).find()) {
            type = TransactionType.INCOME;
        } else if (DEBIT_KEYWORDS.matcher(text).find()) {
            type = TransactionType.EXPENSE;
        } else {
            type = TransactionType.EXPENSE;
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        BigDecimal amount = null;
        while (amountMatcher.find()) {
            String raw = amountMatcher.group(1).replace(",", "");
            try {
                BigDecimal candidate = new BigDecimal(raw);
                if (candidate.compareTo(BigDecimal.ZERO) > 0) {
                    amount = candidate;
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (amount == null) {
            return Optional.empty();
        }

        String merchant = null;
        Matcher merchantMatcher = MERCHANT_PATTERN.matcher(text);
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1).trim();
        }

        String category = guessCategory(text, merchant);

        return Optional.of(Transaction.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .merchant(merchant)
                .description(text.length() > 250 ? text.substring(0, 250) + "..." : text)
                .transactionDate(LocalDateTime.now())
                .sourceEmailId(messageId)
                .rawEmailSnippet(text.length() > 400 ? text.substring(0, 400) : text)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String guessCategory(String text, String merchant) {
        String lower = (text + " " + (merchant != null ? merchant : "")).toLowerCase();

        if (lower.contains("salary") || lower.contains("payroll") || lower.contains("wage")) return "Salary";
        if (lower.contains("uber") || lower.contains("bolt") || lower.contains("taxi") ||
                lower.contains("fuel") || lower.contains("petrol") || lower.contains("matatu")) return "Transport";
        if (lower.contains("supermarket") || lower.contains("naivas") || lower.contains("carrefour") ||
                lower.contains("food") || lower.contains("restaurant") || lower.contains("cafe") ||
                lower.contains("kfc") || lower.contains("java")) return "Food";
        if (lower.contains("airtime") || lower.contains("data") || lower.contains("safaricom") ||
                lower.contains("mpesa") || lower.contains("airtel")) return "Mobile/Airtime";
        if (lower.contains("rent") || lower.contains("house") || lower.contains("apartment")) return "Housing";
        if (lower.contains("electricity") || lower.contains("water") || lower.contains("utility") ||
                lower.contains("kplc") || lower.contains("nairobi water")) return "Utilities";
        if (lower.contains("netflix") || lower.contains("spotify") || lower.contains("dstv") ||
                lower.contains("subscription")) return "Subscriptions";
        if (lower.contains("transfer") || lower.contains("sent to") || lower.contains("send money")) return "Transfer";
        if (lower.contains("atm") || lower.contains("cash withdrawal")) return "Cash Withdrawal";
        if (lower.contains("school") || lower.contains("fees") || lower.contains("tuition")) return "Education";
        if (lower.contains("hospital") || lower.contains("clinic") || lower.contains("pharmacy") ||
                lower.contains("medical")) return "Health";

        return "Other";
    }
}
