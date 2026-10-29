package com.financetracker.service;

import com.financetracker.model.OwnedRecord;
import com.financetracker.session.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

public final class WorkspaceAccess {
    private WorkspaceAccess() {
    }

    public static String owner() {
        return AppSession.ownerId();
    }

    public static <T extends OwnedRecord> void prepare(T row, JpaRepository<T, Long> repository) {
        String owner = owner();
        if (row.getId() != null) {
            T stored = repository.findById(row.getId()).orElseThrow(() -> new IllegalArgumentException("Record not found"));
            if (!owner.equals(stored.getOwnerId())) throw new SecurityException("Record not found");
        } else if (row.getOwnerId() != null && !owner.equals(row.getOwnerId())) {
            throw new SecurityException("Record not found");
        }
        row.setOwnerId(owner);
    }

    public static <T extends OwnedRecord> void delete(Long id, JpaRepository<T, Long> repository) {
        T row = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Record not found"));
        prepare(row, repository);
        repository.delete(row);
    }
}
