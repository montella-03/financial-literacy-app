package com.financetracker.service;

import com.financetracker.model.Transaction;
import com.financetracker.model.Transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class TransactionImportService {
    private final TransactionService transactions;

    public TransactionImportService(TransactionService transactions) {
        this.transactions = transactions;
    }

    public static String fingerprint(Transaction t) {
        String raw = CsvSupport.row(t.getTransactionDate().toLocalDate(), t.getType(), t.getAmount().stripTrailingZeros().toPlainString(), clean(t.getCategory()), clean(t.getMerchant()), clean(t.getDescription()), clean(t.getAccountName()), clean(t.getDestinationAccount()));
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Result importCsv(String csv) {
        var rows = CsvSupport.parse(csv);
        if (rows.isEmpty() || !rows.getFirst().equals(List.of("date", "type", "amount", "category", "merchant", "description", "account", "destination")))
            throw new IllegalArgumentException("Use the CSV template headers exactly.");
        Set<String> seen = new HashSet<>();
        transactions.findAll().forEach(t -> seen.add(fingerprint(t)));
        List<Transaction> pending = new ArrayList<>();
        int duplicates = 0;
        for (int i = 1; i < rows.size(); i++) {
            var r = rows.get(i);
            try {
                if (r.size() != 8) throw new IllegalArgumentException();
                Transaction t = new Transaction();
                t.setTransactionDate(LocalDate.parse(r.get(0).trim()).atTime(12, 0));
                t.setType(TransactionType.valueOf(r.get(1).trim().toUpperCase(Locale.ROOT)));
                t.setAmount(new BigDecimal(r.get(2).trim()));
                t.setCategory(r.get(3).trim());
                t.setMerchant(r.get(4).trim());
                t.setDescription(r.get(5).trim());
                t.setAccountName(r.get(6).trim());
                t.setDestinationAccount(r.get(7).trim());
                transactions.validate(t);
                String key = fingerprint(t);
                if (!seen.add(key)) {
                    duplicates++;
                    continue;
                }
                t.setImportKey(WorkspaceAccess.owner() + ":" + key);
                pending.add(t);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid CSV row " + (i + 1) + ". Check date, type, positive amount, field lengths, and transfer accounts.");
            }
        }
        pending.forEach(transactions::save);
        return new Result(pending.size(), duplicates);
    }

    public record Result(int imported, int duplicates) {
    }
}
